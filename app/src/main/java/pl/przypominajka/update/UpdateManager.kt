package pl.przypominajka.update

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Wydanie z GitHuba, nowsze od zainstalowanej wersji. */
data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Failed(val message: String) : UpdateState
    data class Available(val info: UpdateInfo, val error: String? = null) : UpdateState
    /** [progress] 0–1, albo null, gdy serwer nie podał rozmiaru. */
    data class Downloading(val info: UpdateInfo, val progress: Float?) : UpdateState
    data class ReadyToInstall(val info: UpdateInfo, val file: File) : UpdateState
}

/**
 * Aktualizacje z GitHub Releases: sprawdza najnowsze wydanie, pobiera Przypominajka.apk
 * i otwiera systemowy instalator. Jedna instancja na aplikację – stan widzi okno "Nowa wersja" i Ustawienia.
 */
class UpdateManager(private val context: Context, private val prefs: SharedPreferences) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Czy pokazać okno "Nowa wersja" (chowa je "Później"). */
    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean> = _dialogVisible.asStateFlow()

    private val _autoCheck = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CHECK, true))
    val autoCheck: StateFlow<Boolean> = _autoCheck.asStateFlow()

    val currentVersion: String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    fun setAutoCheck(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_CHECK, enabled) }
        _autoCheck.value = enabled
    }

    /** Przy starcie aplikacji: po cichu, najwyżej raz na kilka godzin. */
    fun checkOnStart() {
        if (!_autoCheck.value) return
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (System.currentTimeMillis() - last < AUTO_INTERVAL_MS) return
        check(silent = true)
    }

    /** [silent] – bez komunikatów o błędach i o braku aktualizacji (sprawdzanie w tle). */
    fun check(silent: Boolean = false) {
        if (job?.isActive == true) return
        job = scope.launch {
            if (!silent) _state.value = UpdateState.Checking
            val result = runCatching { withContext(Dispatchers.IO) { fetchLatest() } }
            prefs.edit { putLong(KEY_LAST_CHECK, System.currentTimeMillis()) }
            result.fold(
                onSuccess = { info ->
                    if (info != null) {
                        _state.value = UpdateState.Available(info)
                        _dialogVisible.value = true
                    } else if (!silent) {
                        _state.value = UpdateState.UpToDate
                    }
                },
                onFailure = { if (!silent) _state.value = UpdateState.Failed(errorText(it)) },
            )
        }
    }

    fun showDialog() {
        _dialogVisible.value = true
    }

    fun dismissDialog() {
        _dialogVisible.value = false
    }

    /** Pobiera APK (albo od razu instaluje, jeśli już jest pobrany). */
    fun downloadAndInstall() {
        when (val current = _state.value) {
            is UpdateState.ReadyToInstall -> install(current.file)
            is UpdateState.Available -> download(current.info)
            else -> Unit
        }
    }

    private fun download(info: UpdateInfo) {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = UpdateState.Downloading(info, null)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    downloadApk(info) { progress -> _state.value = UpdateState.Downloading(info, progress) }
                }
            }
            result.fold(
                onSuccess = { file ->
                    _state.value = UpdateState.ReadyToInstall(info, file)
                    install(file)
                },
                onFailure = { _state.value = UpdateState.Available(info, error = errorText(it)) },
            )
        }
    }

    /** Czy system pozwala tej aplikacji instalować inne aplikacje (Android 8+ pyta o to osobno). */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    private fun install(file: File) {
        if (!canInstall()) {
            // Użytkownik włącza "Zezwalaj z tego źródła" i wraca – wtedy jeszcze raz stuka "Zainstaluj".
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { _state.value = UpdateState.Failed("Nie udało się otworzyć instalatora") }
    }

    // ---------------------------------------------------------------- sieć

    private fun fetchLatest(): UpdateInfo? {
        val connection = open(LATEST_URL)
        try {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                // Repozytorium jeszcze bez żadnego wydania.
                HttpURLConnection.HTTP_NOT_FOUND -> return null
                else -> throw IOException("GitHub odpowiedział kodem ${connection.responseCode}")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val version = json.optString("tag_name").removePrefix("v").removePrefix("V").trim()
            if (version.isEmpty() || !isNewer(version, currentVersion)) return null
            val assets = json.optJSONArray("assets") ?: return null
            val apk = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .sortedByDescending { it.optString("name") == APK_NAME }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                ?: return null
            return UpdateInfo(
                version = version,
                notes = json.optString("body").trim(),
                apkUrl = apk.getString("browser_download_url"),
                sizeBytes = apk.optLong("size", -1L),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadApk(info: UpdateInfo, onProgress: (Float?) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, APK_NAME)
        val partial = File(dir, "$APK_NAME.part")

        // GitHub przekierowuje pobieranie na swój serwer plików (też https) – HttpURLConnection idzie za tym sam.
        val connection = open(info.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Pobieranie przerwane (kod ${connection.responseCode})")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes.takeIf { it > 0 }
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastReported = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total != null) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        checkApk(partial)
        if (!partial.renameTo(target)) throw IOException("Nie udało się zapisać pliku")
        return target
    }

    /** Pobrany plik musi być Przypominajką i to nowszą – inaczej nie proponujemy instalacji. */
    private fun checkApk(file: File) {
        val info = context.packageManager.getPackageArchiveInfo(file.path, 0)
            ?: throw IOException("Pobrany plik jest uszkodzony")
        if (info.packageName != context.packageName) throw IOException("Pobrany plik to nie Przypominajka")
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val newCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        @Suppress("DEPRECATION")
        val oldCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) installed.longVersionCode else installed.versionCode.toLong()
        if (newCode <= oldCode) throw IOException("Pobrana wersja nie jest nowsza")
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "Przypominajka/$currentVersion (Android)")
        }

    private fun errorText(error: Throwable): String = when (error) {
        is java.net.UnknownHostException -> "Brak internetu"
        is java.net.SocketTimeoutException -> "Serwer nie odpowiada – spróbuj później"
        is IOException -> error.message ?: "Błąd połączenia"
        else -> "Coś poszło nie tak"
    }

    companion object {
        const val REPO = "tymoteusz-hash/przypominajka"
        const val RELEASES_PAGE = "https://github.com/$REPO/releases"
        private const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"
        private const val APK_NAME = "Przypominajka.apk"
        private const val APK_MIME = "application/vnd.android.package-archive"
        private const val AUTO_INTERVAL_MS = 6 * 60 * 60 * 1000L

        private const val KEY_AUTO_CHECK = "update_auto_check"
        private const val KEY_LAST_CHECK = "update_last_check"

        /** "3.10" > "3.9"; brakujące części traktujemy jak 0 ("3.2" == "3.2.0"). */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split('.', '-').map { it.toIntOrNull() ?: 0 }
            val b = current.split('.', '-').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
