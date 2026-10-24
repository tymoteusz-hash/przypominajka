package pl.przypominajka.librus

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LibrusCredentials(val login: String, val password: String) {
    override fun toString(): String = "LibrusCredentials(login=$login, password=***)"
}

data class LibrusStatus(
    val connected: Boolean = false,
    /** Częściowo ukryty login, np. "123•••u". */
    val loginHint: String = "",
    val includeTrips: Boolean = true,
    val promptDismissed: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastSyncOk: Boolean = false,
    val lastSyncMessage: String = "",
    val lastSyncLog: String = "",
)

/**
 * Ustawienia połączenia z Librusem. Login i hasło są szyfrowane kluczem AES z magazynu kluczy
 * Androida (klucz nie opuszcza urządzenia), a kopia zapasowa aplikacji jest wyłączona w manifeście.
 */
class LibrusSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("librus_prefs", Context.MODE_PRIVATE)
    private val secretBox = SecretBox("przypominajka_librus_credentials")

    val isConnected: Boolean
        get() = prefs.contains(KEY_LOGIN) && prefs.contains(KEY_PASSWORD)

    fun credentials(): LibrusCredentials? {
        val login = prefs.getString(KEY_LOGIN, null)?.let(secretBox::decrypt) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null)?.let(secretBox::decrypt) ?: return null
        return LibrusCredentials(login, password)
    }

    fun saveCredentials(login: String, password: String) {
        prefs.edit {
            putString(KEY_LOGIN, secretBox.encrypt(login))
            putString(KEY_PASSWORD, secretBox.encrypt(password))
            putString(KEY_LOGIN_HINT, maskLogin(login))
            putBoolean(KEY_PROMPT_DISMISSED, true)
        }
    }

    fun clearCredentials() {
        prefs.edit {
            remove(KEY_LOGIN)
            remove(KEY_PASSWORD)
            remove(KEY_LOGIN_HINT)
            remove(KEY_LAST_SYNC_AT)
            remove(KEY_LAST_SYNC_OK)
            remove(KEY_LAST_SYNC_MESSAGE)
            remove(KEY_LAST_SYNC_LOG)
        }
    }

    var includeTrips: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_TRIPS, true)
        set(value) = prefs.edit { putBoolean(KEY_INCLUDE_TRIPS, value) }

    var promptDismissed: Boolean
        get() = prefs.getBoolean(KEY_PROMPT_DISMISSED, false)
        set(value) = prefs.edit { putBoolean(KEY_PROMPT_DISMISSED, value) }

    fun recordSync(ok: Boolean, message: String, log: String) {
        prefs.edit {
            putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
            putBoolean(KEY_LAST_SYNC_OK, ok)
            putString(KEY_LAST_SYNC_MESSAGE, message)
            putString(KEY_LAST_SYNC_LOG, log)
        }
    }

    fun status(): LibrusStatus = LibrusStatus(
        connected = isConnected,
        loginHint = prefs.getString(KEY_LOGIN_HINT, "").orEmpty(),
        includeTrips = includeTrips,
        promptDismissed = promptDismissed,
        lastSyncAt = prefs.getLong(KEY_LAST_SYNC_AT, 0L),
        lastSyncOk = prefs.getBoolean(KEY_LAST_SYNC_OK, false),
        lastSyncMessage = prefs.getString(KEY_LAST_SYNC_MESSAGE, "").orEmpty(),
        lastSyncLog = prefs.getString(KEY_LAST_SYNC_LOG, "").orEmpty(),
    )

    /** Aktualny stan i każda jego zmiana (np. po synchronizacji w tle). */
    fun observe(): Flow<LibrusStatus> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(status()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(status())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    private fun maskLogin(login: String): String =
        if (login.length <= 3) "•••" else login.take(3) + "•••" + login.takeLast(1)

    private companion object {
        const val KEY_LOGIN = "login_enc"
        const val KEY_PASSWORD = "password_enc"
        const val KEY_LOGIN_HINT = "login_hint"
        const val KEY_INCLUDE_TRIPS = "include_trips"
        const val KEY_PROMPT_DISMISSED = "prompt_dismissed"
        const val KEY_LAST_SYNC_AT = "last_sync_at"
        const val KEY_LAST_SYNC_OK = "last_sync_ok"
        const val KEY_LAST_SYNC_MESSAGE = "last_sync_message"
        const val KEY_LAST_SYNC_LOG = "last_sync_log"
    }
}

/** Szyfrowanie AES-256/GCM kluczem przechowywanym w AndroidKeyStore (hasło Librusa, klucz API). */
internal class SecretBox(private val alias: String) {

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /** null, gdy danych nie da się odczytać (np. klucz usunięty po zmianie blokady ekranu). */
    fun decrypt(stored: String): String? {
        val parts = stored.split(":", limit = 2)
        if (parts.size != 2) return null
        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val data = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.doFinal(data).toString(Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
