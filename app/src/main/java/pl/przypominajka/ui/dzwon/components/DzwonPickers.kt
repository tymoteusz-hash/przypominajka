package pl.przypominajka.ui.dzwon.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.ShortDateFormatter
import pl.przypominajka.util.dayShortName
import pl.przypominajka.util.formatLongDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// =====================================================================================================
// Wybór daty i godziny w kolorach Dzwonu (Material 3 DatePicker / TimePicker w oknach) + pomocnicze
// przeliczenia czasu.
// =====================================================================================================

/** Godzina bez zera wiodącego, jak w makiecie („8:00”, „19:00”). */
private val ShortTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Najbliższa pełna godzina po [now] (np. 17:20 → 18:00) – domyślna godzina nowego przypomnienia. */
fun nextFullHour(now: LocalDateTime = LocalDateTime.now()): LocalDateTime =
    now.truncatedTo(ChronoUnit.HOURS).plusHours(1)

/** Czas lokalny → epoch millis (strefa telefonu). */
fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Epoch millis → czas lokalny (strefa telefonu). */
fun epochMillisToLocalDateTime(millis: Long): LocalDateTime =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()

/**
 * Krótko, kiedy: „dziś 19:00”, „jutro 8:00”, „wczoraj 18:00”, „czw. 16:00” (do 6 dni naprzód),
 * dalej „24.10, 16:00”.
 */
fun formatDzwonWhen(millis: Long, today: LocalDate = LocalDate.now()): String {
    val dateTime = epochMillisToLocalDateTime(millis)
    val date = dateTime.toLocalDate()
    val time = dateTime.format(ShortTimeFormatter)
    return when (val days = ChronoUnit.DAYS.between(today, date)) {
        0L -> "dziś $time"
        1L -> "jutro $time"
        -1L -> "wczoraj $time"
        else -> if (days in 2L..6L) "${dayShortName(date)} $time" else "${date.format(ShortDateFormatter)}, $time"
    }
}

/** Jak [formatDzwonWhen], ale z „o”: „dziś o 18:00”, „jutro o 8:00”, „24.10 o 16:00”. */
fun formatDzwonAt(millis: Long, today: LocalDate = LocalDate.now()): String {
    val dateTime = epochMillisToLocalDateTime(millis)
    val date = dateTime.toLocalDate()
    val time = dateTime.format(ShortTimeFormatter)
    val day = when (ChronoUnit.DAYS.between(today, date)) {
        0L -> "dziś"
        1L -> "jutro"
        -1L -> "wczoraj"
        else -> date.format(ShortDateFormatter)
    }
    return "$day o $time"
}

/** DatePicker Material 3 liczy dni od północy UTC. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Kolory kalendarza: tło `surface`, wybrany dzień złoty, dzisiejszy w złotej obwódce. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dzwonDatePickerColors(): DatePickerColors {
    val c = LookTheme.dzwon
    return DatePickerDefaults.colors(
        containerColor = c.surface,
        titleContentColor = c.ink2,
        headlineContentColor = c.ink,
        weekdayContentColor = c.ink2,
        navigationContentColor = c.ink,
        dayContentColor = c.ink,
        selectedDayContainerColor = c.gold,
        selectedDayContentColor = c.onGold,
        todayContentColor = c.goldText,
        todayDateBorderColor = c.gold,
        yearContentColor = c.ink,
        currentYearContentColor = c.goldText,
        selectedYearContainerColor = c.gold,
        selectedYearContentColor = c.onGold,
    )
}

/**
 * Okno wyboru dnia (kalendarz Material 3 w kolorach Dzwonu). [minDate] – wcześniejszych dni nie da się
 * wybrać (np. dziś dla przypomnień); null = bez ograniczeń.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DzwonDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    confirmText: String = "Gotowe",
    minDate: LocalDate? = null,
) {
    val c = LookTheme.dzwon
    val minMillis = minDate?.toUtcMillis()
    val selectable = remember(minMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minMillis == null || utcTimeMillis >= minMillis

            override fun isSelectableYear(year: Int): Boolean = minDate == null || year >= minDate.year
        }
    }
    val start = if (minDate != null && initialDate.isBefore(minDate)) minDate else initialDate
    val state = rememberDatePickerState(
        initialSelectedDateMillis = start.toUtcMillis(),
        selectableDates = selectable,
    )
    val colors = dzwonDatePickerColors()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            DzwonTextButton(
                text = confirmText,
                onClick = { state.selectedDateMillis?.let { onConfirm(it.utcMillisToLocalDate()) } },
                enabled = state.selectedDateMillis != null,
            )
        },
        dismissButton = { DzwonTextButton(text = "Anuluj", onClick = onDismiss, color = c.ink) },
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors)
    }
}

/**
 * Okno wyboru godziny (tarcza Material 3, 24 h) w kolorach Dzwonu; przyciskiem z ikoną klawiatury
 * można przełączyć na wpisywanie cyfr. [subtitle] – np. wybrany dzień.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DzwonTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    title: String = "Godzina",
    subtitle: String? = null,
    confirmText: String = "Gotowe",
    validate: (LocalTime) -> Boolean = { true },
) {
    var timeError by rememberSaveable { mutableStateOf(false) }
    val c = LookTheme.dzwon
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    var keyboard by rememberSaveable { mutableStateOf(false) }
    val colors = TimePickerDefaults.colors(
        clockDialColor = c.field,
        clockDialSelectedContentColor = c.onGold,
        clockDialUnselectedContentColor = c.ink,
        selectorColor = c.gold,
        containerColor = c.surface,
        timeSelectorSelectedContainerColor = c.gold,
        timeSelectorUnselectedContainerColor = c.field,
        timeSelectorSelectedContentColor = c.onGold,
        timeSelectorUnselectedContentColor = c.ink,
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CompositionLocalProvider(LocalContentColor provides c.ink) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
                    Text(
                        text = title,
                        style = DzwonType.SheetTitle,
                        color = c.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (subtitle != null) {
                        Text(text = subtitle, style = DzwonType.RowTitle, color = c.ink2)
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (keyboard) {
                    TimeInput(state = state, colors = colors)
                } else {
                    TimePicker(state = state, colors = colors)
                }
                if (timeError) {
                    Text(text = "Wybierz przyszłą datę i godzinę.", style = DzwonType.Caption, color = MaterialTheme.colorScheme.error)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DzwonRoundIconButton(
                        icon = if (keyboard) DzwonIcons.Clock else DzwonIcons.Keyboard,
                        contentDescription = if (keyboard) "Wybierz na tarczy" else "Wpisz z klawiatury",
                        onClick = { keyboard = !keyboard },
                        containerColor = Color.Transparent,
                        contentColor = c.ink2,
                        iconSize = 22.dp,
                    )
                    Spacer(Modifier.weight(1f))
                    DzwonTextButton(text = "Anuluj", onClick = onDismiss, color = c.ink)
                    DzwonPrimaryButton(
                        text = confirmText,
                        onClick = {
                            val time = LocalTime.of(state.hour, state.minute)
                            timeError = !validate(time)
                            if (!timeError) onConfirm(time)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Wybór daty i godziny w dwóch krokach: najpierw kalendarz („Dalej”), potem tarcza godziny.
 * [initial] – np. [nextFullHour]; [minDate] – domyślnie dziś (przypomnienia nie wstecz).
 */
@Composable
fun DzwonDateTimePickerDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
    minDate: LocalDate? = LocalDate.now(),
) {
    // Zapamiętujemy wybrany dzień jako epochDay (Long), żeby przetrwał obrót ekranu.
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val day = pickedDay?.let(LocalDate::ofEpochDay)
    if (day == null) {
        DzwonDatePickerDialog(
            initialDate = initial.toLocalDate(),
            onDismiss = onDismiss,
            onConfirm = { pickedDay = it.toEpochDay() },
            confirmText = "Dalej",
            minDate = minDate,
        )
    } else {
        DzwonTimePickerDialog(
            initialTime = initial.toLocalTime(),
            onDismiss = onDismiss,
            onConfirm = { time -> onConfirm(LocalDateTime.of(day, time)) },
            validate = { time -> LocalDateTime.of(day, time).toEpochMillis() > System.currentTimeMillis() },
            title = "Godzina",
            subtitle = formatLongDate(day).replaceFirstChar { it.uppercase() },
        )
    }
}
