package pl.przypominajka.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import pl.przypominajka.librus.SecretBox

/** Klucz API Anthropic do generowania quizów – zaszyfrowany, tylko na tym telefonie. */
class AiSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
    private val secretBox = SecretBox("przypominajka_ai_key")

    val hasKey: Boolean get() = prefs.contains(KEY_API_KEY)

    fun apiKey(): String? = prefs.getString(KEY_API_KEY, null)?.let(secretBox::decrypt)

    fun saveApiKey(key: String) {
        prefs.edit {
            putString(KEY_API_KEY, secretBox.encrypt(key.trim()))
            putString(KEY_HINT, key.trim().let { if (it.length > 12) it.take(7) + "…" + it.takeLast(4) else "•••" })
        }
    }

    fun clearApiKey() = prefs.edit {
        remove(KEY_API_KEY)
        remove(KEY_HINT)
    }

    /** Skrócony podgląd klucza, np. "sk-ant-…a1b2". */
    val keyHint: String get() = prefs.getString(KEY_HINT, "").orEmpty()

    fun observeHasKey(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(hasKey) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(hasKey)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    private companion object {
        const val KEY_API_KEY = "api_key_enc"
        const val KEY_HINT = "api_key_hint"
    }
}
