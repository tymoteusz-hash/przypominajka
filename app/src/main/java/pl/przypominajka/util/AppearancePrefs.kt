package pl.przypominajka.util

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** Motyw wybrany w Ustawieniach. */
enum class ThemeMode(val label: String) {
    SYSTEM("Jak w telefonie"),
    LIGHT("Jasny"),
    DARK("Ciemny"),
}

data class Appearance(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Dzwon przy przejściach między ekranami. */
    val bellSound: Boolean = true,
    val transitionTone: BellTone = BellTone.DEFAULT_TRANSITION,
    /** Głośność dzwonu w aplikacji (0–1, względem głośności multimediów). */
    val bellVolume: Float = DEFAULT_VOLUME,
    /** Nie dzwoń w aplikacji, gdy telefon jest wyciszony albo na wibracjach. */
    val quietWhenSilent: Boolean = true,
    /** Dźwięk powiadomień o przypomnieniach. */
    val notificationTone: BellTone = BellTone.DEFAULT_NOTIFICATION,
) {
    companion object {
        const val DEFAULT_VOLUME = 0.6f
    }
}

/** Ustawienia wyglądu i dźwięków. */
class AppearancePrefs(private val prefs: SharedPreferences) {

    init {
        // Opcja "Większy tekst" została usunięta – sprzątamy jej stary zapis.
        if (prefs.contains(KEY_LARGE_TEXT_REMOVED)) prefs.edit { remove(KEY_LARGE_TEXT_REMOVED) }
    }

    fun current(): Appearance = Appearance(
        theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM,
        bellSound = prefs.getBoolean(KEY_BELL_SOUND, true),
        transitionTone = transitionTone,
        bellVolume = bellVolume,
        quietWhenSilent = quietWhenSilent,
        notificationTone = notificationTone,
    )

    val bellSoundEnabled: Boolean get() = prefs.getBoolean(KEY_BELL_SOUND, true)

    val transitionTone: BellTone
        get() = BellTone.fromKey(prefs.getString(KEY_TRANSITION_TONE, null), BellTone.DEFAULT_TRANSITION)
            .takeIf { it.forTransitions } ?: BellTone.DEFAULT_TRANSITION

    val bellVolume: Float get() = prefs.getFloat(KEY_BELL_VOLUME, Appearance.DEFAULT_VOLUME).coerceIn(0f, 1f)

    val quietWhenSilent: Boolean get() = prefs.getBoolean(KEY_QUIET_WHEN_SILENT, true)

    val notificationTone: BellTone
        get() = BellTone.fromKey(prefs.getString(KEY_NOTIFICATION_TONE, null), BellTone.DEFAULT_NOTIFICATION)

    fun setTheme(mode: ThemeMode) = prefs.edit { putString(KEY_THEME, mode.name) }

    fun setBellSound(enabled: Boolean) = prefs.edit { putBoolean(KEY_BELL_SOUND, enabled) }

    fun setTransitionTone(tone: BellTone) = prefs.edit { putString(KEY_TRANSITION_TONE, tone.key) }

    fun setBellVolume(volume: Float) = prefs.edit { putFloat(KEY_BELL_VOLUME, volume.coerceIn(0f, 1f)) }

    fun setQuietWhenSilent(enabled: Boolean) = prefs.edit { putBoolean(KEY_QUIET_WHEN_SILENT, enabled) }

    fun setNotificationTone(tone: BellTone) = prefs.edit { putString(KEY_NOTIFICATION_TONE, tone.key) }

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
        const val KEY_BELL_SOUND = "appearance_bell_sound"
        const val KEY_TRANSITION_TONE = "appearance_transition_tone"
        const val KEY_BELL_VOLUME = "appearance_bell_volume"
        const val KEY_QUIET_WHEN_SILENT = "appearance_quiet_when_silent"
        const val KEY_NOTIFICATION_TONE = "appearance_notification_tone"
        const val KEY_LARGE_TEXT_REMOVED = "appearance_large_text"
    }
}
