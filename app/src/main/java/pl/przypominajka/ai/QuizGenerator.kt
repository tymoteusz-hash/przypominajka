package pl.przypominajka.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Komunikat błędu gotowy do pokazania użytkownikowi. */
class AiException(message: String) : Exception(message)

/**
 * Generuje pytania do quizu przez Claude API (klucz użytkownika).
 * Odpowiedź jest wymuszona schematem JSON (structured outputs). Schemat jest zapisany ręcznie –
 * automatyczne generowanie schematu z klas korzysta z refleksji niedostępnej na Androidzie.
 */
class QuizGenerator(private val settings: AiSettings) {

    suspend fun generate(
        title: String,
        topic: String,
        count: Int,
        existingQuestions: List<String>,
    ): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val apiKey = settings.apiKey()
            ?: throw AiException("Dodaj klucz API Anthropic w zakładce Konto, żeby generować pytania.")

        val client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        try {
            val params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(16000L)
                // Gdy zabezpieczenia modelu odmówią, serwer sam spróbuje innego modelu.
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .system(SYSTEM_PROMPT)
                .addUserMessage(userPrompt(title, topic, count, existingQuestions))
                .outputConfig(
                    OutputConfig.builder()
                        .format(JsonOutputFormat.builder().schema(QUIZ_SCHEMA).build())
                        .build(),
                )
                .build()

            val message = client.messages().create(params)
            if (message.stopReason().orElse(null) == StopReason.REFUSAL) {
                throw AiException("AI odmówiło wygenerowania pytań na ten temat. Spróbuj opisać go inaczej.")
            }
            val json = message.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
            val questions = try {
                JSONObject(json).optJSONArray("questions") ?: JSONArray()
            } catch (e: JSONException) {
                throw AiException("AI zwróciło nieczytelną odpowiedź. Spróbuj ponownie.")
            }

            (0 until questions.length())
                .mapNotNull { questions.optJSONObject(it) }
                .map { it.optString("question").trim() to it.optString("answer").trim() }
                .filter { (q, a) -> q.isNotEmpty() && a.isNotEmpty() }
                .distinctBy { it.first.lowercase() }
                .take(count)
                .ifEmpty { throw AiException("AI nie zwróciło pytań. Spróbuj ponownie.") }
        } catch (e: UnauthorizedException) {
            throw AiException("Klucz API jest nieprawidłowy. Sprawdź go w zakładce Konto.")
        } catch (e: PermissionDeniedException) {
            throw AiException("Ten klucz API nie ma dostępu do modelu. Sprawdź konto na console.anthropic.com.")
        } catch (e: RateLimitException) {
            throw AiException("Za dużo zapytań naraz. Odczekaj chwilę i spróbuj ponownie.")
        } catch (e: BadRequestException) {
            val billing = e.message.orEmpty().contains("credit", ignoreCase = true)
            throw AiException(
                if (billing) {
                    "Brak środków na koncie Anthropic. Doładuj je na console.anthropic.com."
                } else {
                    "Zapytanie zostało odrzucone (${e.message}). Spróbuj ponownie."
                },
            )
        } catch (e: AnthropicServiceException) {
            throw AiException("Serwer AI zwrócił błąd (${e.statusCode()}). Spróbuj za chwilę.")
        } catch (e: AnthropicIoException) {
            throw AiException("Brak połączenia z internetem.")
        } finally {
            client.close()
        }
    }

    private fun userPrompt(title: String, topic: String, count: Int, existing: List<String>): String = buildString {
        appendLine("Przygotuj $count pytań do nauki na kartkówkę/sprawdzian.")
        appendLine("Nazwa zestawu: $title")
        if (topic.isNotBlank()) appendLine("Temat i zakres materiału: $topic")
        if (existing.isNotEmpty()) {
            appendLine("Te pytania już są w zestawie – nie powtarzaj ich:")
            existing.take(50).forEach { appendLine("- $it") }
        }
    }

    private companion object {
        const val MODEL = "claude-opus-5-5"

        /** {"questions": [{"question": "...", "answer": "..."}]} */
        val QUIZ_SCHEMA: JsonOutputFormat.Schema = JsonOutputFormat.Schema.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty(
                "properties",
                JsonValue.from(
                    mapOf(
                        "questions" to mapOf(
                            "type" to "array",
                            "items" to mapOf(
                                "type" to "object",
                                "properties" to mapOf(
                                    "question" to mapOf(
                                        "type" to "string",
                                        "description" to "Pytanie po polsku, jak na kartkówce",
                                    ),
                                    "answer" to mapOf(
                                        "type" to "string",
                                        "description" to "Krótka, poprawna odpowiedź po polsku (najlepiej 1–8 słów)",
                                    ),
                                ),
                                "required" to listOf("question", "answer"),
                                "additionalProperties" to false,
                            ),
                        ),
                    ),
                ),
            )
            .putAdditionalProperty("required", JsonValue.from(listOf("questions")))
            .putAdditionalProperty("additionalProperties", JsonValue.from(false))
            .build()

        const val SYSTEM_PROMPT =
            "Jesteś nauczycielem w polskiej szkole i układasz pytania do nauki dla ucznia. " +
                "Pytania mają sprawdzać wiedzę z podanego tematu na poziomie odpowiednim do szkoły, " +
                "od łatwiejszych do trudniejszych. Odpowiedzi muszą być poprawne merytorycznie i krótkie, " +
                "bo uczeń będzie je ćwiczył jako fiszki i test wyboru – unikaj odpowiedzi typu " +
                "\"tak/nie\" i bardzo podobnych do siebie. Pisz po polsku."
    }
}
