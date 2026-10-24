package pl.przypominajka.util

import pl.przypominajka.data.Reminder
import pl.przypominajka.data.endDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

private val DAY_NAMES = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")
private val DAY_SHORT_NAMES = listOf("pon.", "wt.", "śr.", "czw.", "pt.", "sob.", "niedz.")

private val MONTHS_GENITIVE = listOf(
    "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
    "lipca", "sierpnia", "września", "października", "listopada", "grudnia",
)
private val MONTHS_SHORT = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")
private val DAY_LETTERS = listOf("PN", "WT", "ŚR", "CZ", "PT", "SB", "ND")

fun dayName(date: LocalDate): String = DAY_NAMES[date.dayOfWeek.value - 1]

fun dayShortName(date: LocalDate): String = DAY_SHORT_NAMES[date.dayOfWeek.value - 1]

/** "PN", "WT"… – do wyboru dnia w planie lekcji. */
fun dayLetters(date: LocalDate): String = DAY_LETTERS[date.dayOfWeek.value - 1]

/** "sobota, 11 października". */
fun formatLongDate(date: LocalDate): String =
    "${dayName(date)}, ${date.dayOfMonth} ${MONTHS_GENITIVE[date.monthValue - 1]}"

/** Powitanie na ekranie głównym zależne od pory dnia. */
fun greeting(time: LocalTime = LocalTime.now()): String = when (time.hour) {
    in 5..10 -> "Dzień dobry"
    in 11..17 -> "Cześć"
    in 18..22 -> "Dobry wieczór"
    else -> "Dobranoc"
}

/** "czw. 16.10". */
fun formatShortDay(date: LocalDate): String = "${dayShortName(date)} ${date.format(ShortDateFormatter)}"

/** "13–17 paź" albo "29 wrz – 3 paź". */
fun formatWeekRange(from: LocalDate, to: LocalDate): String =
    if (from.month == to.month) {
        "${from.dayOfMonth}–${to.dayOfMonth} ${MONTHS_SHORT[to.monthValue - 1]}"
    } else {
        "${from.dayOfMonth} ${MONTHS_SHORT[from.monthValue - 1]} – ${to.dayOfMonth} ${MONTHS_SHORT[to.monthValue - 1]}"
    }

/** "dziś", "jutro", "pojutrze", "za 5 dni", "3 dni temu". */
fun countdownText(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date).toInt()
    return when {
        days == 0 -> "dziś"
        days == 1 -> "jutro"
        days == 2 -> "pojutrze"
        days > 2 -> "za ${daysText(days)}"
        days == -1 -> "wczoraj"
        else -> "${daysText(-days)} temu"
    }
}

/** Dwuliterowe skróty dni do przełączników: Pn, Wt, Śr, Cz, Pt, So, Nd. */
val WEEKDAY_SHORT: List<String> = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")

/** Maska dni tygodnia (bit 0 = pn) → "Codziennie", "Pn–Pt", "Weekend" albo "Pn, Śr, Pt". */
fun weekdaysText(mask: Int): String = when (mask and 0b1111111) {
    0, 0b1111111 -> "Codziennie"
    0b0011111 -> "Pn–Pt"
    0b1100000 -> "Weekend"
    else -> (0..6).filter { mask and (1 shl it) != 0 }.joinToString(", ") { WEEKDAY_SHORT[it] }
}

/** "1 dzień przed", "3 dni przed". */
fun leadDaysText(days: Int): String = "${daysText(days)} przed"

/** Np. "dziś 16:00", "jutro 16:00", "czw. 16:00", "24.10, 16:00". */
fun formatNextTrigger(millis: Long, today: LocalDate): String {
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val date = dateTime.toLocalDate()
    val time = dateTime.toLocalTime().format(TimeFormatter)
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> "dziś $time"
        days == 1L -> "jutro $time"
        days in 2L..6L -> "${DAY_SHORT_NAMES[date.dayOfWeek.value - 1]} $time"
        else -> "${date.format(ShortDateFormatter)}, $time"
    }
}

/** Krótko, kiedy: "dziś", "jutro", "czw.", a dalej "24.10". */
fun relativeDayLabel(millis: Long, today: LocalDate): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (ChronoUnit.DAYS.between(today, date)) {
        0L -> "dziś"
        1L -> "jutro"
        in 2L..6L -> DAY_SHORT_NAMES[date.dayOfWeek.value - 1]
        else -> date.format(ShortDateFormatter)
    }
}

/** Np. "dziś 14:02", "wczoraj 21:15", "08.10.2026 07:30". */
fun formatSyncTime(millis: Long, today: LocalDate): String {
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val date = dateTime.toLocalDate()
    val time = dateTime.toLocalTime().format(TimeFormatter)
    return when (ChronoUnit.DAYS.between(date, today)) {
        0L -> "dziś $time"
        1L -> "wczoraj $time"
        else -> "${date.format(DateFormatter)} $time"
    }
}

/** Polska odmiana liczebników: 1 przypomnienie, 2 przypomnienia, 5 przypomnień. */
fun pluralPl(count: Int, one: String, few: String, many: String): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        count == 1 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

fun daysText(count: Int): String = "$count ${if (count == 1) "dzień" else "dni"}"

fun remindersText(count: Int): String =
    "$count ${pluralPl(count, "przypomnienie", "przypomnienia", "przypomnień")}"

fun formatMillisAsTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(TimeFormatter)

/** Tekst statusu na liście: ile dni zostało / bez końca / zakończone / startuje za X dni. */
fun reminderStatusText(reminder: Reminder, today: LocalDate): String {
    val end = reminder.endDate
    if (reminder.finished || (end != null && today.isAfter(end))) return "Zakończone"

    if (today.isBefore(reminder.startDate)) {
        val daysToStart = ChronoUnit.DAYS.between(today, reminder.startDate).toInt()
        val start = if (daysToStart == 1) "Startuje jutro" else "Startuje za ${daysText(daysToStart)}"
        val duration = reminder.durationDays
        return if (duration == null) "$start · bez końca" else "$start · przez ${daysText(duration)}"
    }

    if (end == null) return "Bez końca"
    val daysLeft = ChronoUnit.DAYS.between(today, end).toInt() + 1
    return if (daysLeft == 1) {
        "Ostatni dzień"
    } else {
        "${pluralPl(daysLeft, "Został", "Zostały", "Zostało")} ${daysText(daysLeft)}"
    }
}
