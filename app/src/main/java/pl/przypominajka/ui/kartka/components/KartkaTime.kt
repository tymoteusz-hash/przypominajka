package pl.przypominajka.ui.kartka.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import pl.przypominajka.util.WEEKDAY_SHORT
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Teksty dat i godzin w stylu Kartki (bez zera na początku godziny: „8:00”, jak w makietach).

/** „8:00”, „16:30”. */
val KartkaHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

private val DAY_SHORT = listOf("Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Niedz")
private val DAY_ACCUSATIVE = listOf(
    "w poniedziałek", "we wtorek", "w środę", "w czwartek", "w piątek", "w sobotę", "w niedzielę",
)
private val MONTHS_GENITIVE = listOf(
    "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
    "lipca", "sierpnia", "września", "października", "listopada", "grudnia",
)
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d.MM")

/** Nadtytuł strony głównej: „Pon · 26 października” ([KartkaEyebrow] zamienia na wielkie litery). */
fun kartkaDateLabel(date: LocalDate): String =
    "${DAY_SHORT[date.dayOfWeek.value - 1]} · ${date.dayOfMonth} ${MONTHS_GENITIVE[date.monthValue - 1]}"

/** Krótki dzień do kolumny z godzinami: „Dziś”, „Śr” (do 6 dni naprzód), dalej „4.11”. */
fun kartkaDayLead(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> "Dziś"
        days in 1L..6L -> WEEKDAY_SHORT[date.dayOfWeek.value - 1]
        else -> date.format(SHORT_DATE)
    }
}

/** Odliczanie do terminu na chipie: „dziś”, „jutro”, „za 2 dni”; minione – „minął”. */
fun kartkaCountdown(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date).toInt()
    return when {
        days < 0 -> "minął"
        days == 0 -> "dziś"
        days == 1 -> "jutro"
        else -> "za $days dni"
    }
}

/** Godzina przy punkcie: „dziś 19:00”, „jutro 8:00”, „wczoraj 7:30”, „Śr 16:00”, „4.11, 16:00”. */
fun kartkaWhenText(millis: Long, today: LocalDate): String {
    val dateTime = millis.toLocalDateTime()
    val date = dateTime.toLocalDate()
    val time = dateTime.toLocalTime().format(KartkaHourFormatter)
    return when (ChronoUnit.DAYS.between(today, date)) {
        0L -> "dziś $time"
        1L -> "jutro $time"
        -1L -> "wczoraj $time"
        in 2L..6L -> "${WEEKDAY_SHORT[date.dayOfWeek.value - 1]} $time"
        else -> "${date.format(SHORT_DATE)}, $time"
    }
}

/** Krótko, do wąskiej kolumny: „19:00” (dziś), „jutro”, „Śr”, „4.11”. */
fun kartkaWhenShort(millis: Long, today: LocalDate): String {
    val dateTime = millis.toLocalDateTime()
    val date = dateTime.toLocalDate()
    return when (ChronoUnit.DAYS.between(today, date)) {
        0L -> dateTime.toLocalTime().format(KartkaHourFormatter)
        1L -> "jutro"
        -1L -> "wczoraj"
        in 2L..6L -> WEEKDAY_SHORT[date.dayOfWeek.value - 1]
        else -> date.format(SHORT_DATE)
    }
}

/** Do zdania „Przypomnę …”: „dziś o 18:00”, „jutro o 8:00”, „w środę o 18:00”, „4.11 o 18:00”. */
fun kartkaWhenPhrase(millis: Long, today: LocalDate): String {
    val dateTime = millis.toLocalDateTime()
    val date = dateTime.toLocalDate()
    val time = dateTime.toLocalTime().format(KartkaHourFormatter)
    val day = when (ChronoUnit.DAYS.between(today, date)) {
        0L -> "dziś"
        1L -> "jutro"
        in 2L..6L -> DAY_ACCUSATIVE[date.dayOfWeek.value - 1]
        else -> date.format(SHORT_DATE)
    }
    return "$day o $time"
}

/**
 * Bieżący czas (epoch millis) odświeżany na początku każdej minuty – np. żeby minione godziny
 * zmieniły kolor bez zmiany danych. To zwykłe odliczanie, nie animacja.
 */
@Composable
fun rememberMinuteClock(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        val now = System.currentTimeMillis()
        delay(60_000L - now % 60_000L)
        value = System.currentTimeMillis()
    }
}
