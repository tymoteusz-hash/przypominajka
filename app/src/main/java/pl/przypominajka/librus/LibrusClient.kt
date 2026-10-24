package pl.przypominajka.librus

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Błąd z komunikatem gotowym do pokazania użytkownikowi. */
class LibrusException(
    val userMessage: String,
    /** true – warto spróbować później (sieć, przerwa techniczna); false – np. złe hasło. */
    val retryable: Boolean = false,
    cause: Throwable? = null,
) : Exception(userMessage, cause)

/** Dziennik kroków synchronizacji – pokazywany w aplikacji. Nigdy nie zawiera hasła ani ciasteczek. */
class SyncLog {
    private val lines = mutableListOf<String>()
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun add(line: String) {
        lines += "${LocalTime.now().format(formatter)}  $line"
    }

    fun text(): String = lines.joinToString("\n")
}

/**
 * Nieoficjalny klient Librusa Synergia. Loguje się tak samo jak przeglądarka na synergia.librus.pl
 * (login i hasło do Synergii), a dane pobiera z bramki JSON, z której korzysta strona Synergii.
 *
 * Przebieg (na podstawie otwartoźródłowych klientów librus-apix i librus-api):
 * 1. GET  synergia.librus.pl/loguj/portalRodzina  → przekierowania do formularza na api.librus.pl
 * 2. POST api.librus.pl/OAuth/Authorization?client_id=46  (action=login, login, pass) → JSON {status, goTo}
 * 3. GET  goTo (lub /OAuth/Authorization/2FA) → przekierowania kończące się ciasteczkami DZIENNIKSID/SDZIENNIKSID
 * 4. GET  synergia.librus.pl/refreshToken → ciasteczko oauth_token
 * 5. GET  synergia.librus.pl/gateway/api/2.0/... (JSON)
 *
 * Wszystkie metody są blokujące – wywołuj je z Dispatchers.IO.
 */
class LibrusClient(private val log: SyncLog) {

    private val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)

    private class Response(
        val code: Int,
        val body: String,
        val url: URL,
        val location: String?,
    )

    // ---------------------------------------------------------------- logowanie

    fun login(login: String, password: String) {
        val ping = execute("$API_URL/", followRedirects = false)
        if (ping.code == 503) {
            throw LibrusException("Librus ma teraz przerwę techniczną. Spróbuj później.", retryable = true)
        }

        val loginPage = execute(
            "$SYNERGIA_URL/loguj/portalRodzina",
            headers = mapOf("Referer" to "https://portal.librus.pl/"),
        )
        log.add("Strona logowania: HTTP ${loginPage.code} (${loginPage.url.host}${loginPage.url.path})")

        // Formularz wysyłamy tam, gdzie zaprowadziły przekierowania (jeśli to oczekiwany adres).
        val postUrl = loginPage.url.takeIf {
            it.host == "api.librus.pl" && it.path == "/OAuth/Authorization"
        }?.toString() ?: "$API_URL/OAuth/Authorization?client_id=46"

        val response = execute(
            postUrl,
            method = "POST",
            form = mapOf("action" to "login", "login" to login, "pass" to password),
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "Origin" to API_URL,
                "Referer" to loginPage.url.toString(),
                "x-baner" to bannerHeader(),
            ),
            followRedirects = false,
        )
        log.add("Wysłanie loginu i hasła: HTTP ${response.code}")

        var next: String? = response.location
        val json = try {
            JSONObject(response.body)
        } catch (e: JSONException) {
            null
        }
        if (json != null) {
            if (json.optString("status") == "error") {
                val message = json.optJSONArray("errors")?.optJSONObject(0)?.optString("message").orEmpty()
                log.add("Librus odrzucił logowanie: $message")
                throw LibrusException(loginErrorMessage(message))
            }
            val goTo = json.optString("goTo")
            if (goTo.isNotBlank() && goTo != "null") next = goTo
        } else if (response.code >= 400) {
            if (response.body.contains("captcha", ignoreCase = true)) {
                throw LibrusException(CAPTCHA_MESSAGE)
            }
            throw LibrusException(
                "Librus nie przyjął logowania (HTTP ${response.code}). Spróbuj ponownie za chwilę.",
                retryable = response.code >= 500,
            )
        }

        val continuation = next?.let { URL(URL(API_URL), it).toString() }
            ?: "$API_URL/OAuth/Authorization/2FA?client_id=46"
        val authorized = execute(continuation, headers = mapOf("Referer" to postUrl))
        log.add("Autoryzacja: HTTP ${authorized.code} (${authorized.url.host}${authorized.url.path})")

        // Uwaga: ciasteczka DZIENNIKSID/SDZIENNIKSID Librus ustawia już PRZED zalogowaniem, więc nie
        // świadczą o sesji. Pewnym dowodem jest dopiero oauth_token z /refreshToken (bez logowania – 401).
        if (!tryRefreshGatewayToken()) {
            val twoFactor = execute("$API_URL/OAuth/Authorization/2FA?client_id=46")
            log.add("Krok 2FA: HTTP ${twoFactor.code} (${twoFactor.url.host}${twoFactor.url.path})")
            if (!tryRefreshGatewayToken()) {
                throw LibrusException(
                    "Librus nie utworzył sesji. Jeśli masz logowanie dwuetapowe albo Librus prosi o dodatkowe " +
                        "potwierdzenie, zaloguj się raz na synergia.librus.pl w przeglądarce i spróbuj ponownie.",
                )
            }
        }
        log.add("Zalogowano.")
    }

    /** Token dla bramki JSON (ciasteczko oauth_token) – ważny ograniczony czas, odświeżany przy 401. */
    private fun refreshGatewayToken() {
        if (!tryRefreshGatewayToken()) {
            throw LibrusException("Sesja Librusa wygasła i nie udało się jej odnowić.", retryable = true)
        }
    }

    private fun tryRefreshGatewayToken(): Boolean {
        val response = execute("$SYNERGIA_URL/refreshToken")
        log.add("Token API: HTTP ${response.code}")
        return response.code in 200..299 && cookie("oauth_token") != null
    }

    private fun cookie(name: String): String? =
        cookieManager.cookieStore.get(URI(SYNERGIA_URL)).firstOrNull { it.name == name }?.value

    // ---------------------------------------------------------------- dane

    /** GET z bramki JSON, np. "HomeWorks" albo "Timetables?weekStart=2026-10-12". */
    fun getJson(path: String): JSONObject {
        val name = path.substringBefore('?')
        var response = execute("$GATEWAY_URL/$path", headers = mapOf("Accept" to "application/json"))
        if (response.code == 401 || response.body.contains("TokenIsExpired")) {
            log.add("$name: token wygasł, odświeżam")
            refreshGatewayToken()
            response = execute("$GATEWAY_URL/$path", headers = mapOf("Accept" to "application/json"))
        }
        log.add("$name: HTTP ${response.code}")
        if (response.code !in 200..299) {
            throw LibrusException(
                "Librus nie udostępnił danych „$name” (HTTP ${response.code}).",
                retryable = response.code >= 500,
            )
        }
        return try {
            JSONObject(response.body)
        } catch (e: JSONException) {
            throw LibrusException("Librus zwrócił nieoczekiwaną odpowiedź dla „$name”.", cause = e)
        }
    }

    /** Lista obiektów spod klucza [key], np. getList("Subjects", "Subjects"). */
    fun getList(path: String, key: String): List<JSONObject> {
        val array = getJson(path).optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    // ---------------------------------------------------------------- HTTP

    /**
     * Żądanie HTTP z ręczną obsługą przekierowań, żeby ciasteczka z każdego kroku
     * (api.librus.pl ↔ synergia.librus.pl) trafiały do wspólnego słoika.
     */
    private fun execute(
        urlString: String,
        method: String = "GET",
        form: Map<String, String>? = null,
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
    ): Response {
        try {
            var url = URL(urlString)
            var currentMethod = method
            var currentForm = form
            var referer = headers["Referer"]

            repeat(MAX_REDIRECTS) {
                requireLibrusHost(url)
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 20_000
                    connection.readTimeout = 30_000
                    connection.requestMethod = currentMethod
                    connection.setRequestProperty("User-Agent", USER_AGENT)
                    connection.setRequestProperty("Accept-Language", "pl-PL,pl;q=0.9")
                    headers.forEach { (name, value) ->
                        if (name != "Referer") connection.setRequestProperty(name, value)
                    }
                    referer?.let { connection.setRequestProperty("Referer", it) }

                    val cookies = cookieManager.get(url.toURI(), emptyMap<String, List<String>>())["Cookie"].orEmpty()
                    if (cookies.isNotEmpty()) connection.setRequestProperty("Cookie", cookies.joinToString("; "))

                    val body = currentForm
                    if (currentMethod == "POST" && body != null) {
                        val bytes = body.entries
                            .joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
                            .toByteArray(Charsets.UTF_8)
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        connection.setFixedLengthStreamingMode(bytes.size)
                        connection.outputStream.use { it.write(bytes) }
                    }

                    val code = connection.responseCode
                    cookieManager.put(url.toURI(), connection.headerFields)
                    val location = connection.getHeaderField("Location")

                    if (followRedirects && code in 300..399 && location != null) {
                        referer = url.toString()
                        url = URL(url, location)
                        if (code == 303 || ((code == 301 || code == 302) && currentMethod == "POST")) {
                            currentMethod = "GET"
                            currentForm = null
                        }
                        return@repeat
                    }
                    return Response(code, readBody(connection), url, location)
                } finally {
                    connection.disconnect()
                }
            }
            throw LibrusException("Za dużo przekierowań podczas łączenia z Librusem.")
        } catch (e: IOException) {
            throw LibrusException("Brak połączenia z Librusem. Sprawdź internet.", retryable = true, cause = e)
        }
    }

    private fun readBody(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode >= 400) connection.errorStream else connection.inputStream
        return stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
    }

    /** Bezpiecznik: dane logowania i sesja nigdy nie trafią poza serwery Librusa. */
    private fun requireLibrusHost(url: URL) {
        val host = url.host.lowercase()
        if (url.protocol != "https" || !(host == "librus.pl" || host.endsWith(".librus.pl"))) {
            throw LibrusException("Librus przekierował na nieoczekiwany adres ($host) – przerwano.")
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** Nagłówek wysyłany przez stronę logowania Librusa (jak w librus-api). */
    private fun bannerHeader(): String {
        fun shift(text: String) = text.map { (it.code + 20).toChar() }.joinToString("")
        return "${shift(Math.random().toString())}_${shift(System.currentTimeMillis().toString())}"
    }

    private fun loginErrorMessage(librusMessage: String): String = when {
        librusMessage.contains("captcha", ignoreCase = true) -> CAPTCHA_MESSAGE
        librusMessage.isNotBlank() -> "Librus: $librusMessage"
        else -> "Nieprawidłowy login lub hasło."
    }

    private companion object {
        const val API_URL = "https://api.librus.pl"
        const val SYNERGIA_URL = "https://synergia.librus.pl"
        const val GATEWAY_URL = "$SYNERGIA_URL/gateway/api/2.0"
        const val MAX_REDIRECTS = 15
        const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        const val CAPTCHA_MESSAGE =
            "Librus chce potwierdzenia, że nie jesteś robotem. Zaloguj się raz na synergia.librus.pl " +
                "w przeglądarce telefonu, a potem spróbuj ponownie."
    }
}
