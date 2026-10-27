package pl.przypominajka.util

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import pl.przypominajka.ui.look.Look

/** Motyw wybrany w Ustawieniach. */
enum class ThemeMode(val label: String) {
    SYSTEM("Jak w telefonie"),
    LIGHT("Jasny"),
    DARK("Ciemny"),
}

data class Appearance(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Dźwięk powiadomień o przypomnieniach. */
    val notificationTone: BellTone = BellTone.DEFAULT_NOTIFICATION,
    /** Wybrany wygląd; null = jeszcze nie wybrany (przed pierwszym przewodnikiem). */
    val look: Look? = null,
    /** Przewodnik na start (wygląd + motyw) został zakończony. */
    val onboardingDone: Boolean = false,
)

/** Ustawienia wyglądu i dźwięku powiadomień. */
class AppearancePrefs(private val prefs: SharedPreferences) {

    init {
        // Usunięte opcje ("Większy tekst" i dzwon przy przejściach z wersji 3.x) – sprzątamy ich stare zapisy.
        val removed = REMOVED_KEYS.filter { prefs.contains(it) }
        if (removed.isNotEmpty()) prefs.edit { removed.forEach { remove(it) } }
    }

    fun current(): Appearance = Appearance(
        theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM,
        notificationTone = notificationTone,
        look = look,
        onboardingDone = onboardingDone,
    )

    val notificationTone: BellTone
        get() = BellTone.fromKey(prefs.getString(KEY_NOTIFICATION_TONE, null), BellTone.DEFAULT_NOTIFICATION)

    val look: Look? get() = Look.fromKey(prefs.getString(KEY_LOOK, null))

    val onboardingDone: Boolean get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setTheme(mode: ThemeMode) = prefs.edit { putString(KEY_THEME, mode.name) }

    fun setNotificationTone(tone: BellTone) = prefs.edit { putString(KEY_NOTIFICATION_TONE, tone.key) }

    /** Zapis od razu na dysk – zaraz potem zmieniamy ikonę aplikacji (włączanie/wyłączanie komponentów). */
    fun setLook(look: Look) = prefs.edit(commit = true) { putString(KEY_LOOK, look.key) }

    fun setOnboardingDone(done: Boolean) = prefs.edit(commit = true) { putBoolean(KEY_ONBOARDING_DONE, done) }

    fun observe(): Flow<Appearance> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key.startsWith("appearance_")) trySend(current())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(current())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    private companion object {
        const val KEY_THEME = "appearance_theme"
        const val KEY_NOTIFICATION_TONE = "appearance_notification_tone"
        const val KEY_LOOK = "appearance_look"
        const val KEY_ONBOARDING_DONE = "appearance_onboarding_done"

        /** Klucze usuniętych opcji: "Większy tekst" (3.2) i dzwon przy przejściach (do 3.5). */
        val REMOVED_KEYS = listOf(
            "appearance_large_text",
            "appearance_bell_sound",
            "appearance_transition_tone",
            "appearance_bell_volume",
            "appearance_quiet_when_silent",
        )
    }
}
