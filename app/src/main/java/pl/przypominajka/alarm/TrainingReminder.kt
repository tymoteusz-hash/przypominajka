package pl.przypominajka.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.przypominajka.appContainer
import pl.przypominajka.data.SchoolTimes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Przypomnienie o treningu: włączone i w które dni tygodnia. */
data class TrainingSettings(
    val enabled: Boolean = false,
    val days: Set<DayOfWeek> = DEFAULT_DAYS,
    /** Własna godzina w sobotę i niedzielę (gdy plan z Librusa nie ma tego dnia lekcji). */
    val weekendTimes: Map<DayOfWeek, LocalTime> = WEEKEND.associateWith { DEFAULT_WEEKEND_TIME },
) {
    fun weekendTime(day: DayOfWeek): LocalTime = weekendTimes[day] ?: DEFAULT_WEEKEND_TIME

    companion object {
        val DEFAULT_DAYS: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val WEEKEND: List<DayOfWeek> = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val DEFAULT_WEEKEND_TIME: LocalTime = LocalTime.of(15, 0)
    }
}

/** Najbliższe przypomnienie o treningu i skąd wzięła się godzina. */
data class TrainingTrigger(val at: LocalDateTime, val source: Source) {
    enum class Source { LESSONS, WEEKEND, NO_SCHOOL }
}

/**
 * Przypomnienie o treningu w chwili końca lekcji (plan z Librusa, a gdy go brak – godziny z Ustawień)
 * w wybrane dni tygodnia. Jak przy przypomnieniach: w AlarmManagerze jest tylko JEDEN alarm –
 * najbliższe wystąpienie; po jego wywołaniu planujemy kolejne.
 */
class TrainingReminder(private val context: Context, private val prefs: SharedPreferences) {

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<TrainingSettings> = _settings.asStateFlow()

    fun setEnabled(enabled: Boolean) = save(_settings.value.copy(enabled = enabled))

    fun toggleDay(day: DayOfWeek) {
        val days = _settings.value.days
        save(_settings.value.copy(days = if (day in days) days - day else days + day))
    }

    fun setWeekendTime(day: DayOfWeek, time: LocalTime) {
        if (day !in TrainingSettings.WEEKEND) return
        save(_settings.value.copy(weekendTimes = _settings.value.weekendTimes + (day to time)))
    }

    /** Planuje najbliższe przypomnienie (albo kasuje alarm, gdy wyłączone / brak dni). */
    fun reschedule(schoolTimes: SchoolTimes) {
        val next = nextTrigger(_settings.value, schoolTimes, LocalDateTime.now())
        if (next == null) {
            alarmManager.cancel(pendingIntent())
            return
        }
        val at = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pendingIntent()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, TrainingReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun read(): TrainingSettings {
        val stored = prefs.getString(KEY_DAYS, null)
        val days = stored
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull()?.takeIf { v -> v in 1..7 }?.let(DayOfWeek::of) }
            ?.toSet()
            ?: TrainingSettings.DEFAULT_DAYS
        val weekendTimes = TrainingSettings.WEEKEND.associateWith { day ->
            val minutes = prefs.getInt(timeKey(day), -1)
            if (minutes in 0 until 24 * 60) LocalTime.of(minutes / 60, minutes % 60) else TrainingSettings.DEFAULT_WEEKEND_TIME
        }
        return TrainingSettings(enabled = prefs.getBoolean(KEY_ENABLED, false), days = days, weekendTimes = weekendTimes)
    }

    private fun save(settings: TrainingSettings) {
        prefs.edit {
            putBoolean(KEY_ENABLED, settings.enabled)
            putString(KEY_DAYS, settings.days.map { it.value }.sorted().joinToString(","))
            settings.weekendTimes.forEach { (day, time) -> putInt(timeKey(day), time.hour * 60 + time.minute) }
        }
        _settings.value = settings
    }

    companion object {
        private const val REQUEST_CODE = 7_002
        private const val KEY_ENABLED = "training_enabled"
        private const val KEY_DAYS = "training_days"

        private fun timeKey(day: DayOfWeek) = "training_time_${day.value}"

        /**
         * Najbliższy koniec lekcji w wybrany dzień, późniejszy niż [now] (szukamy przez tydzień do przodu).
         * W sobotę i niedzielę bez lekcji – o godzinie ustawionej na weekend; w inny dzień bez lekcji
         * (np. święto) – o domyślnej godzinie końca lekcji.
         */
        fun nextTrigger(settings: TrainingSettings, schoolTimes: SchoolTimes, now: LocalDateTime): TrainingTrigger? {
            if (!settings.enabled || settings.days.isEmpty()) return null
            val today = now.toLocalDate()
            for (offset in 0..7L) {
                val date: LocalDate = today.plusDays(offset)
                if (date.dayOfWeek !in settings.days) continue
                val end: LocalTime? = schoolTimes.lessonsEnd(date)
                val trigger = when {
                    end != null -> TrainingTrigger(date.atTime(end), TrainingTrigger.Source.LESSONS)
                    date.dayOfWeek in TrainingSettings.WEEKEND ->
                        TrainingTrigger(date.atTime(settings.weekendTime(date.dayOfWeek)), TrainingTrigger.Source.WEEKEND)
                    else -> TrainingTrigger(date.atTime(schoolTimes.defaultEnd), TrainingTrigger.Source.NO_SCHOOL)
                }
                if (trigger.at.isAfter(now)) return trigger
            }
            return null
        }
    }
}

/** Koniec lekcji w dzień treningowy: powiadomienie i plan na kolejny dzień. */
class TrainingReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = context.appContainer
        container.notificationHelper.showTrainingReminder()
        launchAsync { container.repository.rescheduleTraining() }
    }
}
