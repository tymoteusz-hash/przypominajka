package pl.przypominajka.util

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import pl.przypominajka.data.SchoolType
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Ustawienia szkolne:
 * - o której kończą się lekcje w każdy dzień tygodnia (używane, gdy brak planu z Librusa),
 * - ile dni przed sprawdzianem / kartkówką / … zacząć przypominać.
 */
class SchoolPrefs(private val prefs: SharedPreferences) {

    /** Domyślny koniec lekcji (dla dni bez własnego ustawienia). */
    var endTime: LocalTime
        get() = readTime(KEY_END_MINUTES, DEFAULT_END_MINUTES)
        set(value) {
            prefs.edit { putInt(KEY_END_MINUTES, value.toMinutes()) }
        }

    fun weekdayEnd(day: DayOfWeek): LocalTime =
        if (prefs.contains(weekdayKey(day))) readTime(weekdayKey(day), DEFAULT_END_MINUTES) else endTime

    fun setWeekdayEnd(day: DayOfWeek, time: LocalTime) {
        prefs.edit { putInt(weekdayKey(day), time.toMinutes()) }
    }

    /** Pon–pt → koniec lekcji. W weekend szkoły nie ma (chyba że plan z Librusa mówi inaczej). */
    fun weekdayEnds(): Map<DayOfWeek, LocalTime> = SCHOOL_DAYS.associateWith { weekdayEnd(it) }

    fun leadDays(type: SchoolType): Int =
        prefs.getInt(leadKey(type), type.defaultLeadDays).coerceIn(MIN_LEAD_DAYS, MAX_LEAD_DAYS)

    fun setLeadDays(type: SchoolType, days: Int) {
        prefs.edit { putInt(leadKey(type), days.coerceIn(MIN_LEAD_DAYS, MAX_LEAD_DAYS)) }
    }

    /** Godzina przypomnienia dla domyślnego końca lekcji. */
    fun reminderTime(): LocalTime = reminderTimeAfter(endTime)

    /** Emituje przy każdej zmianie ustawień (np. godzin lekcji). */
    fun observeChanges(): Flow<Unit> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key.startsWith("school_") || key.startsWith("lead_")) trySend(Unit)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    private fun readTime(key: String, default: Int): LocalTime {
        val minutes = prefs.getInt(key, default).coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    private fun LocalTime.toMinutes(): Int = hour * 60 + minute

    private fun weekdayKey(day: DayOfWeek) = "school_end_day_${day.value}"

    private fun leadKey(type: SchoolType) = "lead_${type.name}"

    companion object {
        const val MIN_LEAD_DAYS = 1
        const val MAX_LEAD_DAYS = 7
        val SCHOOL_DAYS: List<DayOfWeek> = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )

        private const val KEY_END_MINUTES = "school_end_minutes"
        private const val DEFAULT_END_MINUTES = 15 * 60

        /** Godzinę po końcu lekcji (najpóźniej 23:59, żeby nie przeskoczyć na następny dzień). */
        fun reminderTimeAfter(end: LocalTime): LocalTime =
            if (end.hour >= 23) LocalTime.of(23, 59) else end.plusHours(1)
    }
}
