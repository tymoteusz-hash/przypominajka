@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.przypominajka.ui.tablica.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.edit.EditReminderUiState
import pl.przypominajka.ui.edit.EditReminderViewModel
import pl.przypominajka.ui.edit.MIN_DAYS
import pl.przypominajka.ui.edit.MAX_DAYS
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.util.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

private enum class DateTarget { START, EVENT }
private data class TimeRequest(val previous: LocalTime?)

/** Formularz na osobnych kartkach: treść, termin i godziny. Logika pozostaje we wspólnym ViewModelu. */
@Composable
fun TablicaEditReminderScreen(
    onBack: () -> Unit,
    onOpenQuiz: (Long) -> Unit,
    viewModel: EditReminderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dateTarget by rememberSaveable { mutableStateOf<DateTarget?>(null) }
    var timeRequest by remember { mutableStateOf<TimeRequest?>(null) }
    var deleteConfirm by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    TablicaScreen {
        Column(Modifier.fillMaxSize().imePadding()) {
            TablicaTopBar(
                title = if (state.isNew) "Nowe przypomnienie" else "Edycja",
                onBack = onBack,
                subtitle = "Przypomnienie na tablicy",
                actions = {
                    if (!state.isNew) RoundIconButton(Icons.Rounded.Delete, "Usuń przypomnienie", { deleteConfirm = true })
                },
            )
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Wczytywanie…", style = TablicaType.Body) }
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = LocalDockPadding.current + 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    if (state.isFromLibrus) {
                        FormNote("Wpis z Librusa", LookTheme.tablica.pink) {
                            Text("Treść i termin uaktualnia synchronizacja, więc Twoje zmiany w nich mogą zostać nadpisane. " +
                                "Możesz go wyłączyć przełącznikiem albo usunąć – wtedy już nie wróci.", style = TablicaType.BodyLight)
                        }
                    }
                    FormNote("Gdzie", LookTheme.tablica.mint) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.folders.forEach { folder ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Folder, null, tint = Color(folder.colorArgb), modifier = Modifier.size(16.dp))
                                    TablicaChip(folder.name, state.folderId == folder.id, { viewModel.onFolderChange(folder.id) })
                                }
                            }
                        }
                        if (state.isSchoolFolder) {
                            NoteDivider()
                            Text("Wybierz, co to za wpis. Dotknij ponownie, aby zdjąć wybór.", style = TablicaType.Hint)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SchoolType.entries.forEach { type ->
                                    TablicaChip(type.label, state.schoolType == type, { viewModel.onSchoolTypeChange(type) })
                                }
                            }
                        }
                    }
                    FormNote("Treść", LookTheme.tablica.yellow) {
                        val titleLabel = state.schoolType?.titleLabel ?: "O czym przypomnieć? *"
                        Text(titleLabel, style = TablicaType.Strong)
                        NoteTextField(
                            value = state.title, onValueChange = viewModel::onTitleChange,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            placeholder = titleLabel, label = titleLabel, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        )
                        if (state.titleError) ValidationText("Tytuł jest wymagany")
                        val noteLabel = if (state.isEventMode) "Zakres materiału / notatka" else "Notatka (opcjonalnie)"
                        Text(noteLabel, style = TablicaType.Hint)
                        NoteTextField(state.note, viewModel::onNoteChange, Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            placeholder = noteLabel, label = noteLabel, handwritten = false, maxLines = 5)
                    }
                    if (state.isEventMode) {
                        EventNote(state, { dateTarget = DateTarget.EVENT }, viewModel::onEventDateChange, viewModel::onLeadDaysChange)
                    } else {
                        GenericNote(state, viewModel, { dateTarget = DateTarget.START })
                    }
                    TimesNote(state, viewModel, { timeRequest = TimeRequest(it) })
                    if (!state.isNew && state.schoolType?.supportsQuiz == true) {
                        OutlineInkButton("Quiz do nauki na ${state.schoolType?.label?.lowercase()}",
                            { onOpenQuiz(state.reminderId) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Quiz)
                    }
                    InkButton(
                        text = if (state.isSaving) "Zapisywanie…" else if (state.isNew) "Dodaj przypomnienie" else "Zapisz zmiany",
                        onClick = viewModel::save, enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    dateTarget?.let { target ->
        TablicaDatePickerDialog(
            initial = if (target == DateTarget.EVENT) state.eventDate else state.startDate,
            minDate = null,
            confirmText = "OK",
            onDismiss = { dateTarget = null },
            onConfirm = {
                if (target == DateTarget.EVENT) viewModel.onEventDateChange(it) else viewModel.onStartDateChange(it)
                dateTarget = null
            },
        )
    }
    timeRequest?.let { request ->
        TablicaTimePickerDialog(
            initial = request.previous ?: defaultNewTime(state.times),
            title = if (request.previous == null) "Dodaj godzinę" else "Zmień godzinę",
            onDismiss = { timeRequest = null },
            onConfirm = { viewModel.onTimePicked(request.previous, it); timeRequest = null },
        )
    }
    if (deleteConfirm) ConfirmNoteDialog(
        "Usunąć przypomnienie?", "„${state.title}” zostanie usunięte, a zaplanowane powiadomienia anulowane.", "Usuń",
        { deleteConfirm = false; viewModel.delete() }, { deleteConfirm = false },
    )
}

@Composable
private fun FormNote(title: String, color: NoteColor, content: @Composable ColumnScope.() -> Unit) {
    Note(color, Modifier.fillMaxWidth(), decoration = NoteDecoration.Tape, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun ValidationText(text: String) {
    Text("Błąd: $text", style = TablicaType.Hint, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun EventNote(state: EditReminderUiState, onPick: () -> Unit, onQuick: (LocalDate) -> Unit, onLead: (Int) -> Unit) {
    val type = state.schoolType ?: return
    val today = LocalDate.now()
    FormNote("Termin", LookTheme.tablica.pink) {
        Text(type.dateLabel, style = TablicaType.Strong)
        OutlineInkButton("${dayName(state.eventDate)}, ${state.eventDate.format(DateFormatter)}", onPick, Modifier.fillMaxWidth())
        Text(countdownText(state.eventDate, today) + lessonsInfo(state.schoolTimes, state.eventDate), style = TablicaType.Hint)
        state.eventDateError?.let { ValidationText(it) }
        val dates = listOf("Jutro" to today.plusDays(1), "Pojutrze" to today.plusDays(2),
            "Pn." to today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), "Za tydzień" to today.plusWeeks(1)).distinctBy { it.second }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dates.forEach { (label, date) -> TablicaChip(label, date == state.eventDate, { onQuick(date) }) }
        }
        NoteDivider()
        Text("Zacznij przypominać: ${leadDaysText(state.leadDays)}", style = TablicaType.Strong)
        val ink = LocalContentColor.current
        Slider(
            value = state.leadDays.toFloat(), onValueChange = { onLead(Math.round(it)) },
            valueRange = SchoolPrefs.MIN_LEAD_DAYS.toFloat()..SchoolPrefs.MAX_LEAD_DAYS.toFloat(),
            steps = SchoolPrefs.MAX_LEAD_DAYS - SchoolPrefs.MIN_LEAD_DAYS - 1,
            colors = SliderDefaults.colors(thumbColor = ink, activeTrackColor = ink, inactiveTrackColor = ink.copy(alpha = 0.2f)),
        )
        val first = state.eventFirstDay
        val last = state.eventLastDay
        Text(when {
            last.isBefore(first) -> "Termin jest zbyt blisko – wybierz późniejszy dzień."
            first == last -> "Powiadomienie: ${formatShortDay(first)}"
            else -> "Powiadomienia: od ${formatShortDay(first)} do ${formatShortDay(last)}"
        }, style = TablicaType.Hint)
    }
}

@Composable
private fun GenericNote(state: EditReminderUiState, viewModel: EditReminderViewModel, onPick: () -> Unit) {
    FormNote("Kiedy", LookTheme.tablica.blue) {
        Text("W które dni: ${weekdaysText(state.daysOfWeek)}", style = TablicaType.Strong)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WEEKDAY_SHORT.forEachIndexed { index, label ->
                TablicaChip(label, state.daysOfWeek != 0 && (state.daysOfWeek and (1 shl index)) != 0, { viewModel.toggleWeekday(index) })
            }
        }
        Text("Nic nie zaznaczone = codziennie.", style = TablicaType.Hint)
        NoteDivider()
        OutlineInkButton("Od: " + (if (state.startDate == LocalDate.now()) "dziś" else dayName(state.startDate)) +
            ", " + state.startDate.format(DateFormatter), onPick, Modifier.fillMaxWidth())
        SwitchRow("Bez końca", state.endless, viewModel::onEndlessChange, subtitle = "Przypominaj bez daty zakończenia")
        if (!state.endless) {
            val days = state.daysText.toIntOrNull()
            Text("Liczba dni", style = TablicaType.Strong)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InkTextButton("−", { viewModel.changeDaysBy(-1) }, Modifier.semantics { contentDescription = "Odejmij dzień" })
                NoteTextField(state.daysText, viewModel::onDaysTextChange, Modifier.weight(1f).heightIn(min = 48.dp),
                    label = "Liczba dni", handwritten = false, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                RoundIconButton(Icons.Rounded.Add, "Dodaj dzień", { viewModel.changeDaysBy(1) })
            }
            state.daysError?.let { ValidationText(it) }
            Text(if (days != null && days in MIN_DAYS..MAX_DAYS) {
                val last = state.startDate.plusDays(days - 1L)
                "Ostatni dzień: ${dayName(last)}, ${last.format(DateFormatter)}"
            } else "Od $MIN_DAYS do $MAX_DAYS dni", style = TablicaType.Hint)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "1 dzień", 7 to "Tydzień", 14 to "2 tyg.", 30 to "Miesiąc").forEach { (value, label) ->
                    TablicaChip(label, days == value, { viewModel.setDays(value) })
                }
            }
        }
    }
}

@Composable
private fun TimesNote(state: EditReminderUiState, viewModel: EditReminderViewModel, onTime: (LocalTime?) -> Unit) {
    FormNote("Godzina powiadomienia", LookTheme.tablica.lilac) {
        Text(if (state.usesAfterSchool) "Każdego dnia godzinę po ostatniej lekcji – według planu lekcji."
            else "Każdego dnia o tych godzinach przyjdzie powiadomienie.", style = TablicaType.Hint)
        if (state.isSchoolFolder) SwitchRow("Godzinę po lekcjach", state.afterSchool, viewModel::onAfterSchoolChange,
            subtitle = "Czas z planu lekcji (Librus) albo z ustawień w Koncie")
        if (state.usesAfterSchool) {
            if (state.previewDays.isEmpty()) Text("Brak dni z powiadomieniami.", style = TablicaType.BodyLight)
            state.previewDays.forEach { day ->
                NoteRow(title = "${formatShortDay(day)} · ${state.schoolTimes.afterSchoolTime(day).format(TimeFormatter)}",
                    subtitle = lessonsInfo(state.schoolTimes, day).removePrefix(" · "))
                NoteDivider()
            }
        } else {
            if (state.times.isEmpty()) Text("Nie dodano jeszcze żadnej godziny.", style = TablicaType.BodyLight)
            state.times.forEach { time ->
                NoteRow(
                    title = time.format(TimeFormatter), onClick = { onTime(time) }, onClickLabel = "Zmień godzinę",
                    leading = { Icon(TablicaIcons.Clock, null) },
                    trailing = { RoundIconButton(Icons.Rounded.Close, "Usuń godzinę ${time.format(TimeFormatter)}", { viewModel.removeTime(time) }) },
                )
                NoteDivider()
            }
            state.timesError?.let { ValidationText(it) }
            OutlineInkButton("Dodaj godzinę", { onTime(null) }, Modifier.fillMaxWidth(), icon = TablicaIcons.Plus)
        }
    }
}

private fun lessonsInfo(schoolTimes: SchoolTimes, date: LocalDate): String {
    val end = schoolTimes.lessonsEnd(date)
    return when (schoolTimes.source(date)) {
        SchoolTimes.Source.TIMETABLE -> " · lekcje do ${end?.format(TimeFormatter)} (plan)"
        SchoolTimes.Source.WEEKDAY -> " · lekcje do ${end?.format(TimeFormatter)}"
        SchoolTimes.Source.NO_SCHOOL -> " · bez lekcji"
    }
}

private fun defaultNewTime(existing: List<LocalTime>): LocalTime {
    val last = existing.maxOrNull()
    return if (last != null && last.hour < 23) last.plusHours(1)
        else LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
}
