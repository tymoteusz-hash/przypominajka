package pl.przypominajka.ui.kartka.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.formatLongDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Wybór dnia (Material 3 DatePicker w oknie) w kolorach Kartki.
 * @param minDate najwcześniejszy dzień do wyboru (np. dziś przy przypomnieniach); null = bez ograniczeń.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KartkaDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
    title: String = "Wybierz dzień",
    confirmLabel: String = "Gotowe",
) {
    val c = LookTheme.kartka
    val selectable: SelectableDates = remember(minDate) { notBefore(minDate) }
    val start = if (minDate != null && initialDate.isBefore(minDate)) minDate else initialDate
    val state = rememberDatePickerState(
        initialSelectedDateMillis = start.toUtcMillis(),
        selectableDates = selectable,
    )
    val colors = kartkaDatePickerColors()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            KartkaTextButton(
                text = confirmLabel,
                onClick = { state.selectedDateMillis?.let { onConfirm(it.utcMillisToDate()) } },
                enabled = state.selectedDateMillis != null,
            )
        },
        dismissButton = { KartkaTextButton(text = "Anuluj", onClick = onDismiss, color = c.ink2) },
        shape = KartkaDefaults.DialogShape,
        tonalElevation = 0.dp,
        colors = colors,
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = title,
                    style = KartkaType.Caption,
                    color = c.ink2,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            showModeToggle = false,
            colors = colors,
        )
    }
}

/**
 * Wybór godziny (Material 3 TimePicker, format 24 h) w oknie Kartki.
 * @param onDismiss zamknięcie oknem / gestem „wstecz”.
 * @param onCancel lewy przycisk (domyślnie to samo co [onDismiss]; np. „Wstecz” do wyboru dnia).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KartkaTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    title: String = "Wybierz godzinę",
    subtitle: String? = null,
    confirmLabel: String = "Gotowe",
    cancelLabel: String = "Anuluj",
    onCancel: () -> Unit = onDismiss,
    validate: (LocalTime) -> Boolean = { true },
) {
    var timeError by rememberSaveable { mutableStateOf(false) }
    val c = LookTheme.kartka
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(KartkaDefaults.DialogShape)
                .background(c.raised)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = KartkaType.Title,
                    color = c.ink,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) Text(text = subtitle, style = KartkaType.Caption, color = c.ink2)
            }
            TimePicker(state = state, colors = kartkaTimePickerColors())
            if (timeError) {
                Text(text = "Wybierz przyszłą datę i godzinę.", style = KartkaType.Caption, color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KartkaTextButton(text = cancelLabel, onClick = onCancel, color = c.ink)
                KartkaButton(
                    text = confirmLabel,
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

/**
 * Wybór daty i godziny w dwóch krokach: najpierw dzień ([KartkaDatePickerDialog]), potem godzina
 * ([KartkaTimePickerDialog] z przyciskiem „Wstecz”). Zwraca [LocalDateTime] w strefie telefonu.
 * @param allowPast false = nie da się wybrać dnia przed dzisiejszym.
 */
@Composable
fun KartkaDateTimePickerDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
    allowPast: Boolean = false,
) {
    var pickedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val date = pickedDate
    if (date == null) {
        KartkaDatePickerDialog(
            initialDate = initial.toLocalDate(),
            onDismiss = onDismiss,
            onConfirm = { pickedDate = it },
            minDate = if (allowPast) null else LocalDate.now(),
            title = "Kiedy przypomnieć?",
            confirmLabel = "Dalej",
        )
    } else {
        KartkaTimePickerDialog(
            initialTime = initial.toLocalTime(),
            onDismiss = onDismiss,
            onConfirm = { time -> onConfirm(LocalDateTime.of(date, time)) },
            validate = { time -> allowPast || LocalDateTime.of(date, time).toEpochMillis() > System.currentTimeMillis() },
            title = "O której?",
            subtitle = formatLongDate(date).replaceFirstChar { it.uppercase() },
            cancelLabel = "Wstecz",
            onCancel = { pickedDate = null },
        )
    }
}

/** Kolory DatePickera Kartki (akcent na zaznaczonym dniu, dzisiejszy dzień obwiedziony akcentem). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun kartkaDatePickerColors(): DatePickerColors {
    val c = LookTheme.kartka
    return DatePickerDefaults.colors(
        containerColor = c.raised,
        titleContentColor = c.ink2,
        headlineContentColor = c.ink,
        weekdayContentColor = c.ink2,
        subheadContentColor = c.ink2,
        navigationContentColor = c.ink,
        yearContentColor = c.ink,
        currentYearContentColor = c.accent,
        selectedYearContentColor = c.onAccent,
        selectedYearContainerColor = c.accent,
        dayContentColor = c.ink,
        selectedDayContentColor = c.onAccent,
        selectedDayContainerColor = c.accent,
        todayContentColor = c.accent,
        todayDateBorderColor = c.accent,
        dividerColor = c.line,
    )
}

/** Kolory TimePickera Kartki (tarcza w kolorze surface, wskazówka i zaznaczenie w akcencie). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun kartkaTimePickerColors(): TimePickerColors {
    val c = LookTheme.kartka
    return TimePickerDefaults.colors(
        clockDialColor = c.surface,
        clockDialSelectedContentColor = c.onAccent,
        clockDialUnselectedContentColor = c.ink,
        selectorColor = c.accent,
        containerColor = c.raised,
        timeSelectorSelectedContainerColor = c.accentSoft,
        timeSelectorUnselectedContainerColor = c.surface,
        timeSelectorSelectedContentColor = c.accent,
        timeSelectorUnselectedContentColor = c.ink,
    )
}

/** Domyślna godzina nowego przypomnienia: najbliższa pełna godzina (np. 17:20 → 18:00). */
fun kartkaNextFullHour(now: LocalDateTime = LocalDateTime.now()): LocalDateTime =
    now.truncatedTo(ChronoUnit.HOURS).plusHours(1)

/** Data i godzina w strefie telefonu → epoch millis (jak `ChecklistItem.remindAt`). */
fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Epoch millis → data i godzina w strefie telefonu. */
fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

/** Dni od [min] wzwyż (null = wszystkie). */
@OptIn(ExperimentalMaterial3Api::class)
private fun notBefore(min: LocalDate?): SelectableDates {
    val first: LocalDate = min ?: return DatePickerDefaults.AllDates
    return object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean = !utcTimeMillis.utcMillisToDate().isBefore(first)

        override fun isSelectableYear(year: Int): Boolean = year >= first.year
    }
}

/** DatePicker liczy dni w UTC (północ UTC). */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
