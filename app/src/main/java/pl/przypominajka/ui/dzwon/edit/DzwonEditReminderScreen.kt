@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.przypominajka.ui.dzwon.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.edit.EditReminderUiState
import pl.przypominajka.ui.edit.EditReminderViewModel
import pl.przypominajka.ui.edit.MIN_DAYS
import pl.przypominajka.ui.edit.MAX_DAYS
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

@Composable
fun DzwonEditReminderScreen(
    onBack: () -> Unit,
    onOpenQuiz: (Long) -> Unit,
    viewModel: EditReminderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    var dateTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var editingTime by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    DzwonSheetScaffold(
        title = if (state.isNew) "Nowe przypomnienie" else "Edycja",
        subtitle = if (state.isNew) "Dzwon da znać" else "Przypomnienie",
        onClose = onBack,
        actions = {
            if (!state.isNew && !state.isLoading) DzwonRoundIconButton(DzwonIcons.Trash, "Usuń przypomnienie", { deleteConfirm = true })
        },
        bottomBar = {
            if (!state.isLoading) DzwonPrimaryButton(
                text = if (state.isSaving) "Zapisywanie…" else if (state.isNew) "Dodaj przypomnienie" else "Zapisz zmiany",
                onClick = viewModel::save, enabled = !state.isSaving, icon = DzwonIcons.Check,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        },
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.gold) }
        } else Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isFromLibrus) DzwonNotice(
                title = "Wpis z Librusa", containerColor = c.field,
                text = "Treść i termin uaktualnia synchronizacja, więc Twoje zmiany w nich mogą zostać nadpisane. Możesz go wyłączyć przełącznikiem albo usunąć – wtedy już nie wróci.",
            )
            Section("Gdzie") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.folders.forEach { folder ->
                        DzwonChip(folder.name, { viewModel.onFolderChange(folder.id) }, selected = folder.id == state.folderId)
                    }
                }
                if (state.isSchoolFolder) {
                    DzwonDivider()
                    Hint("Wybierz, co to za wpis. Dotknij ponownie, aby zdjąć wybór.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SchoolType.entries.forEach { type ->
                            DzwonChip(type.label, { viewModel.onSchoolTypeChange(type) }, selected = state.schoolType == type)
                        }
                    }
                }
            }
            Section("Treść") {
                DzwonTextField(state.title, viewModel::onTitleChange, label = state.schoolType?.titleLabel ?: "O czym przypomnieć? *")
                if (state.titleError) ErrorText("Tytuł jest wymagany")
                DzwonTextField(state.note, viewModel::onNoteChange,
                    label = if (state.isEventMode) "Zakres materiału / notatka" else "Notatka (opcjonalnie)", singleLine = false, maxLines = 5)
            }
            if (state.isEventMode) EventSection(state, viewModel, { dateTarget = "event" })
            else GenericSection(state, viewModel, { dateTarget = "start" })
            Section("Godzina powiadomienia") {
                Hint(if (state.usesAfterSchool) "Każdego dnia godzinę po ostatniej lekcji – według planu lekcji." else "Każdego dnia o tych godzinach przyjdzie powiadomienie.")
                if (state.isSchoolFolder) ToggleRow("Godzinę po lekcjach", "Czas z planu lekcji (Librus) albo z ustawień w Koncie", state.afterSchool, viewModel::onAfterSchoolChange)
                if (state.usesAfterSchool) {
                    if (state.previewDays.isEmpty()) Hint("Brak dni z powiadomieniami.")
                    state.previewDays.forEach { day ->
                        DzwonListRow(title = formatShortDay(day), subtitle = lessonsInfo(state.schoolTimes, day).removePrefix(" · "),
                            trailing = { Text(state.schoolTimes.afterSchoolTime(day).format(TimeFormatter), style = DzwonType.TimeLarge, color = c.goldText) }, divider = true)
                    }
                } else {
                    if (state.times.isEmpty()) Hint("Nie dodano jeszcze żadnej godziny.")
                    state.times.forEach { time ->
                        DzwonListRow(title = time.format(TimeFormatter), leading = { Text("Dzwon", style = DzwonType.Caption, color = c.goldText) },
                            onClick = { editingTime = time.toString(); showTime = true },
                            trailing = { DzwonRoundIconButton(DzwonIcons.Close, "Usuń godzinę ${time.format(TimeFormatter)}", { viewModel.removeTime(time) }, containerColor = c.field) })
                    }
                    state.timesError?.let { ErrorText(it) }
                    DzwonSecondaryButton("Dodaj godzinę", { editingTime = null; showTime = true }, Modifier.fillMaxWidth(), icon = DzwonIcons.Plus)
                }
            }
            if (!state.isNew && state.schoolType?.supportsQuiz == true) {
                DzwonSecondaryButton("Quiz do nauki na ${state.schoolType?.label?.lowercase()}", { onOpenQuiz(state.reminderId) }, Modifier.fillMaxWidth())
            }
            DzwonPermissionPrompts(containerColor = c.field)
        }
    }
    dateTarget?.let { target ->
        DzwonDatePickerDialog(if (target == "event") state.eventDate else state.startDate,
            onDismiss = { dateTarget = null }, onConfirm = {
                if (target == "event") viewModel.onEventDateChange(it) else viewModel.onStartDateChange(it)
                dateTarget = null
            })
    }
    if (showTime) {
        val previous = editingTime?.let(LocalTime::parse)
        DzwonTimePickerDialog(previous ?: defaultNewTime(state.times), onDismiss = { showTime = false },
            title = if (previous == null) "Dodaj godzinę" else "Zmień godzinę",
            onConfirm = { viewModel.onTimePicked(previous, it); showTime = false })
    }
    if (deleteConfirm) DzwonDialog(
        onDismissRequest = { deleteConfirm = false }, title = "Usunąć przypomnienie?",
        text = "„${state.title}” zostanie usunięte, a zaplanowane powiadomienia anulowane.",
        confirmText = "Usuń", destructive = true, onConfirm = { deleteConfirm = false; viewModel.delete() },
    )
}

@Composable
private fun EventSection(state: EditReminderUiState, vm: EditReminderViewModel, onPick: () -> Unit) {
    val type = state.schoolType ?: return
    val c = LookTheme.dzwon
    val today = LocalDate.now()
    Section("Termin") {
        DzwonCard(color = c.field, onClick = onPick, contentPadding = PaddingValues(16.dp)) {
            Text(type.dateLabel, style = DzwonType.Caption, color = c.goldText)
            Text("${dayName(state.eventDate)}, ${state.eventDate.format(DateFormatter)}", style = DzwonType.SectionTitle, color = c.ink)
            Hint(countdownText(state.eventDate, today) + lessonsInfo(state.schoolTimes, state.eventDate))
            DzwonTextButton("Zmień termin", onPick, icon = DzwonIcons.Calendar)
        }
        state.eventDateError?.let { ErrorText(it) }
        val quick = listOf("Jutro" to today.plusDays(1), "Pojutrze" to today.plusDays(2),
            "Pn." to today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), "Za tydzień" to today.plusWeeks(1)).distinctBy { it.second }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quick.forEach { (label, date) -> DzwonChip(label, { vm.onEventDateChange(date) }, selected = date == state.eventDate) }
        }
        DzwonDivider()
        Text("Zacznij przypominać: ${leadDaysText(state.leadDays)}", style = DzwonType.RowTitle, color = c.goldText)
        Slider(value = state.leadDays.toFloat(), onValueChange = { vm.onLeadDaysChange(Math.round(it)) },
            valueRange = SchoolPrefs.MIN_LEAD_DAYS.toFloat()..SchoolPrefs.MAX_LEAD_DAYS.toFloat(),
            steps = SchoolPrefs.MAX_LEAD_DAYS - SchoolPrefs.MIN_LEAD_DAYS - 1,
            modifier = Modifier.semantics { contentDescription = "Ile dni przed terminem zacząć przypominać" },
            colors = SliderDefaults.colors(thumbColor = c.gold, activeTrackColor = c.gold, inactiveTrackColor = c.track))
        val first = state.eventFirstDay
        val last = state.eventLastDay
        Hint(when {
            last.isBefore(first) -> "Termin jest zbyt blisko – wybierz późniejszy dzień."
            first == last -> "Powiadomienie: ${formatShortDay(first)}"
            else -> "Powiadomienia: od ${formatShortDay(first)} do ${formatShortDay(last)}"
        })
    }
}

@Composable
private fun GenericSection(state: EditReminderUiState, vm: EditReminderViewModel, onPick: () -> Unit) {
    val c = LookTheme.dzwon
    Section("Kiedy") {
        Text("W które dni: ${weekdaysText(state.daysOfWeek)}", style = DzwonType.RowTitle, color = c.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WEEKDAY_SHORT.forEachIndexed { index, label ->
                DzwonChip(label, { vm.toggleWeekday(index) }, selected = state.daysOfWeek != 0 && (state.daysOfWeek and (1 shl index)) != 0)
            }
        }
        Hint("Nic nie zaznaczone = codziennie.")
        DzwonSecondaryButton("Od: ${dayName(state.startDate)}, ${state.startDate.format(DateFormatter)}", onPick, Modifier.fillMaxWidth(), icon = DzwonIcons.Calendar)
        ToggleRow("Bez końca", "Przypominaj bez daty zakończenia", state.endless, vm::onEndlessChange)
        if (!state.endless) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DzwonTextButton("−", { vm.changeDaysBy(-1) }, Modifier.semantics { contentDescription = "Odejmij dzień" })
                DzwonTextField(state.daysText, vm::onDaysTextChange, Modifier.weight(1f), label = "Liczba dni", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                DzwonSquareIconButton(DzwonIcons.Plus, "Dodaj dzień", { vm.changeDaysBy(1) })
            }
            val days = state.daysText.toIntOrNull()
            if (state.daysError != null) ErrorText(state.daysError!!)
            else Hint(if (days != null && days in MIN_DAYS..MAX_DAYS) {
                val last = state.startDate.plusDays(days - 1L)
                "Ostatni dzień: ${dayName(last)}, ${last.format(DateFormatter)}"
            } else "Od $MIN_DAYS do $MAX_DAYS dni")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "1 dzień", 7 to "Tydzień", 14 to "2 tyg.", 30 to "Miesiąc").forEach { (value, label) ->
                    DzwonChip(label, { vm.setDays(value) }, selected = days == value)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DzwonSectionHeader(title)
        content()
        DzwonDivider(Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DzwonType.RowTitle, color = LookTheme.dzwon.ink)
            Hint(description)
        }
        DzwonSwitch(checked, onChange, Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun Hint(text: String) { Text(text, style = DzwonType.Caption, color = LookTheme.dzwon.ink2) }

@Composable
private fun ErrorText(text: String) { Text(text, style = DzwonType.Caption, color = MaterialTheme.colorScheme.error) }

private fun lessonsInfo(times: SchoolTimes, date: LocalDate): String = when (times.source(date)) {
    SchoolTimes.Source.TIMETABLE -> " · lekcje do ${times.lessonsEnd(date)?.format(TimeFormatter)} (plan)"
    SchoolTimes.Source.WEEKDAY -> " · lekcje do ${times.lessonsEnd(date)?.format(TimeFormatter)}"
    SchoolTimes.Source.NO_SCHOOL -> " · bez lekcji"
}

private fun defaultNewTime(existing: List<LocalTime>): LocalTime {
    val last = existing.maxOrNull()
    return if (last != null && last.hour < 23) last.plusHours(1)
    else LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
}

