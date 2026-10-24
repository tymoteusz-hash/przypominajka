package pl.przypominajka.data

import pl.przypominajka.util.SchoolPrefs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * O której kończy się szkoła w danym dniu:
 * 1. plan lekcji z Librusa (koniec ostatniej nieodwołanej lekcji),
 * 2. ustawienie dla dnia tygodnia (pon–pt),
 * 3. brak lekcji (weekend) – wtedy przypomnienie "po lekcjach" przychodzi po domyślnym końcu lekcji.
 */
data class SchoolTimes(
    val timetableEnds: Map<LocalDate, LocalTime> = emptyMap(),
    val weekdayEnds: Map<DayOfWeek, LocalTime> = emptyMap(),
    val defaultEnd: LocalTime = LocalTime.of(15, 0),
) {
    enum class Source { TIMETABLE, WEEKDAY, NO_SCHOOL }

    fun source(date: LocalDate): Source = when {
        timetableEnds.containsKey(date) -> Source.TIMETABLE
        weekdayEnds.containsKey(date.dayOfWeek) -> Source.WEEKDAY
        else -> Source.NO_SCHOOL
    }

    /** Koniec lekcji albo null, gdy tego dnia nie ma szkoły. */
    fun lessonsEnd(date: LocalDate): LocalTime? = timetableEnds[date] ?: weekdayEnds[date.dayOfWeek]

    /** Godzina przypomnienia "po lekcjach" w danym dniu. */
    fun afterSchoolTime(date: LocalDate): LocalTime =
        SchoolPrefs.reminderTimeAfter(lessonsEnd(date) ?: defaultEnd)
}
