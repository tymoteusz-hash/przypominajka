package pl.przypominajka.alarm

import pl.przypominajka.data.Reminder
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.endDate
import pl.przypominajka.data.firesOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Czyste obliczenia: kiedy przypomnienie ma zadzwonić następnym razem. */
object ReminderSchedule {

    /**
     * Najbliższy moment (epoch millis) ściśle po [afterMillis], w którym trzeba pokazać
     * powiadomienie – zwykła godzina albo odłożenie. Null, gdy nie ma już żadnych terminów.
     * [schoolTimes] jest potrzebne dla przypomnień "po lekcjach" (bez niego użyte zostaną [Reminder.times]).
     */
    fun nextTrigger(
        reminder: Reminder,
        afterMillis: Long,
        schoolTimes: SchoolTimes? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val regular = nextRegularTrigger(reminder, afterMillis, schoolTimes, zone)
        val snooze = reminder.snoozedUntil?.takeIf { it > afterMillis }
        return listOfNotNull(regular, snooze).minOrNull()
    }

    /** Godziny powiadomień w danym dniu. */
    fun timesOn(reminder: Reminder, date: LocalDate, schoolTimes: SchoolTimes?): List<LocalTime> =
        if (!reminder.firesOn(date)) {
            emptyList()
        } else if (reminder.afterSchool && schoolTimes != null) {
            listOf(schoolTimes.afterSchoolTime(date))
        } else {
            reminder.times.sorted()
        }

    fun nextRegularTrigger(
        reminder: Reminder,
        afterMillis: Long,
        schoolTimes: SchoolTimes? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (!reminder.afterSchool && reminder.times.isEmpty()) return null
        val end = reminder.endDate

        val afterDate = Instant.ofEpochMilli(afterMillis).atZone(zone).toLocalDate()
        var date = if (afterDate.isAfter(reminder.startDate)) afterDate else reminder.startDate

        // Tydzień + zapas: tyle wystarczy, żeby trafić na najbliższy wybrany dzień tygodnia
        // (i przejść przez zmianę czasu letniego/zimowego).
        repeat(9) {
            if (end != null && date.isAfter(end)) return null
            for (time in timesOn(reminder, date, schoolTimes)) {
                val millis = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
                if (millis > afterMillis) return millis
            }
            date = date.plusDays(1)
        }
        return null
    }

    /** Wszystkie dzisiejsze godziny powiadomień (także te, które już minęły). */
    fun allToday(
        reminder: Reminder,
        nowMillis: Long,
        schoolTimes: SchoolTimes?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<LocalTime> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        if (today.isBefore(reminder.startDate)) return emptyList()
        val end = reminder.endDate
        if (end != null && today.isAfter(end)) return emptyList()
        return timesOn(reminder, today, schoolTimes)
    }

    /** Godziny powiadomień, które jeszcze dziś przyjdą (do listy "Dziś" na ekranie głównym). */
    fun upcomingToday(
        reminder: Reminder,
        nowMillis: Long,
        schoolTimes: SchoolTimes?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<LocalTime> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return allToday(reminder, nowMillis, schoolTimes, zone).filter {
            ZonedDateTime.of(today, it, zone).toInstant().toEpochMilli() > nowMillis
        }
    }

    /** Ile powiadomień zostało jeszcze dziś (do podsumowania na ekranie głównym). */
    fun remainingToday(
        reminder: Reminder,
        nowMillis: Long,
        schoolTimes: SchoolTimes?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int = upcomingToday(reminder, nowMillis, schoolTimes, zone).size
}
