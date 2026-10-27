@file:OptIn(ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.util.formatNextTrigger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

// =====================================================================================================
// Wybór daty i godziny w stylu Tablicy: Material 3 DatePicker/TimePicker na żółtej karteczce.
// =====================================================================================================

/**
 * Bieżąca data i godzina odświeżana co pełną minutę (do „teraz: …”, minionych godzin).
 * To nie animacja – jedno przeliczenie na minutę, tylko gdy ekran jest widoczny.
 */
@Composable
fun rememberMinuteClock(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        value = LocalDateTime.now()
    }
}

/** Najbliższa pełna godzina od teraz (14:20 → 15:00) jako epoch millis – domyślna godzina przypomnienia. */
fun nextFullHourMillis(now: LocalDateTime = LocalDateTime.now()): Long =
    now.truncatedTo(ChronoUnit.HOURS).plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Godzina przy punkcie: „dziś 19:00”, „jutro 8:00”, „czw. 16:00”, „24.10, 16:00”. */
fun reminderTimeText(millis: Long): String = formatNextTrigger(millis, LocalDate.now())

/** Kolory wyboru daty na karteczce [note] (papier + atrament). */
@Composable
private fun noteDatePickerColors(note: NoteColor): DatePickerColors = DatePickerDefaults.colors(
    containerColor = note.bg,
    titleContentColor = note.ink,
    headlineContentColor = note.ink,
    weekdayContentColor = note.ink,
    subheadContentColor = note.ink,
    navigationContentColor = note.ink,
    yearContentColor = note.ink,
    disabledYearContentColor = note.ink.copy(alpha = 0.38f),
    currentYearContentColor = note.ink,
    selectedYearContentColor = note.bg,
    selectedYearContainerColor = note.ink,
    dayContentColor = note.ink,
    disabledDayContentColor = note.ink.copy(alpha = 0.35f),
    selectedDayContentColor = note.bg,
    selectedDayContainerColor = note.ink,
    todayContentColor = note.ink,
    todayDateBorderColor = note.ink,
    dividerColor = note.rule,
)

/**
 * Wybór dnia (M3 DatePicker na żółtej karteczce). [minDate] – najwcześniejszy dzień do wyboru
 * (null = dowolny). [onConfirm] dostaje wybrany dzień.
 */
@Composable
fun TablicaDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    minDate: LocalDate? = LocalDate.now(),
    confirmText: String = "Dalej",
) {
    val note = LookTheme.tablica.yellow
    val start = if (minDate != null && initial.isBefore(minDate)) minDate else initial
    val minUtc = minDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val selectable = remember(minUtc) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = minUtc == null || utcTimeMillis >= minUtc
            override fun isSelectableYear(year: Int): Boolean = minDate == null || year >= minDate.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectable,
    )
    val colors = noteDatePickerColors(note)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        colors = colors,
        confirmButton = {
            NoteColors(note) {
                InkButton(
                    text = confirmText,
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        val millis = state.selectedDateMillis ?: return@InkButton
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    },
                )
            }
        },
        dismissButton = { NoteColors(note) { InkTextButton(text = "Anuluj", onClick = onDismiss) } },
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false)
    }
}

/** Wybór godziny (M3 TimePicker, 24 h) na żółtej karteczce. [onConfirm] dostaje wybraną godzinę. */
@Composable
fun TablicaTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    title: String = "O której?",
    confirmText: String = "Gotowe",
    validate: (LocalTime) -> Boolean = { true },
) {
    var timeError by rememberSaveable { mutableStateOf(false) }
    val note = LookTheme.tablica.yellow
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    val dial = lerp(note.bg, note.ink, 0.1f)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Note(
                color = note,
                tilt = 0f,
                decoration = NoteDecoration.Tape,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 22.dp,
                    end = 16.dp,
                    bottom = 12.dp,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = TablicaType.DialogTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                            .semantics { heading() },
                    )
                    Spacer(Modifier.height(12.dp))
                    TimePicker(
                        state = state,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = dial,
                            clockDialSelectedContentColor = note.bg,
                            clockDialUnselectedContentColor = note.ink,
                            selectorColor = note.ink,
                            containerColor = note.bg,
                            timeSelectorSelectedContainerColor = note.ink,
                            timeSelectorUnselectedContainerColor = dial,
                            timeSelectorSelectedContentColor = note.bg,
                            timeSelectorUnselectedContentColor = note.ink,
                        ),
                    )
                }
                if (timeError) {
                    Text(text = "Wybierz przyszłą datę i godzinę.", style = TablicaType.Hint, color = PinInk)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InkTextButton(text = "Anuluj", onClick = onDismiss)
                    InkButton(
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
 * Wybór daty i godziny w dwóch krokach (najpierw dzień, potem godzina). [initialMillis] = null –
 * zaczyna od najbliższej pełnej godziny. [onConfirm] dostaje epoch millis.
 */
@Composable
fun TablicaDateTimePicker(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initial = Instant.ofEpochMilli(initialMillis ?: nextFullHourMillis()).atZone(zone).toLocalDateTime()
    // Wybrany dzień (epochDay) – po nim pokazujemy wybór godziny.
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val day = pickedDay
    if (day == null) {
        TablicaDatePickerDialog(
            initial = initial.toLocalDate(),
            onDismiss = onDismiss,
            onConfirm = { pickedDay = it.toEpochDay() },
        )
    } else {
        TablicaTimePickerDialog(
            initial = initial.toLocalTime(),
            onDismiss = onDismiss,
            validate = { time ->
                LocalDate.ofEpochDay(day).atTime(time).atZone(zone).toInstant().toEpochMilli() > System.currentTimeMillis()
            },
            onConfirm = { time ->
                val millis = LocalDate.ofEpochDay(day).atTime(time).atZone(zone).toInstant().toEpochMilli()
                onConfirm(millis)
            },
        )
    }
}

/** Kolory karteczki dla przycisków w slotach okien Material 3 (one same kolorów nie przekazują). */
@Composable
private fun NoteColors(note: NoteColor, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides note.ink, LocalPaper provides note.bg, content = content)
}
