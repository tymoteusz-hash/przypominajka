@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.przypominajka.ui.kartka.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
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
import pl.przypominajka.ui.kartka.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.DateFormatter
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.WEEKDAY_SHORT
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.dayName
import pl.przypominajka.util.formatShortDay
import pl.przypominajka.util.leadDaysText
import pl.przypominajka.util.weekdaysText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

private enum class DateTarget { START, EVENT }
private data class TimeRequest(val editing: LocalTime?)

/** Edytor Kartki: sekcje oddzielone liniami, szare pola i stały przycisk zapisu. */
@Composable
fun KartkaEditReminderScreen(
    onBack: () -> Unit,
    onOpenQuiz: (reminderId: Long) -> Unit,
    viewModel: EditReminderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LookTheme.kartka
    var dateTarget by rememberSaveable { mutableStateOf<DateTarget?>(null) }
    var timeRequest by remember { mutableStateOf<TimeRequest?>(null) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    KartkaScaffold(
        onBack = onBack,
        actions = {
            if (!state.isNew) {
                KartkaIconButton(KartkaIcons.Trash, "Usuń przypomnienie", { deleting = true })
            }
        },
        bottomBar = {
            if (!state.isLoading) {
                KartkaBottomBar {
                    KartkaButton(
                        text = if (state.isSaving) "Zapisywanie…" else if (state.isNew) "Dodaj przypomnienie" else "Zapisz zmiany",
                        icon = KartkaIcons.CheckThin,
                        onClick = viewModel::save,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()
                    .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            ) {
                KartkaPageTitle(if (state.isNew) "Nowe przypomnienie" else "Edycja", subtitle = "Treść, termin i powiadomienia")
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = KartkaDefaults.PagePadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    if (state.isFromLibrus) {
                        KartkaNotice(
                            title = "Wpis z Librusa",
                            text = "Treść i termin uaktualnia synchronizacja, więc Twoje zmiany w nich mogą zostać " +
                                "nadpisane. Możesz go wyłączyć przełącznikiem albo usunąć – wtedy już nie wróci.",
                        )
                    }
                    FolderSection(state, viewModel::onFolderChange, viewModel::onSchoolTypeChange)
                    EditSection("Treść") {
                        KartkaTextField(
                            value = state.title,
                            onValueChange = viewModel::onTitleChange,
                            label = state.schoolType?.titleLabel ?: "O czym przypomnieć? *",
                            isError = state.titleError,
                            supportingText = if (state.titleError) "Tytuł jest wymagany" else null,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        )
                        KartkaTextField(
                            value = state.note,
                            onValueChange = viewModel::onNoteChange,
                            label = if (state.isEventMode) "Zakres materiału / notatka" else "Notatka (opcjonalnie)",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 5,
                        )
                    }
                    if (state.isEventMode) {
                        EventSection(state, { dateTarget = DateTarget.EVENT }, viewModel::onEventDateChange, viewModel::onLeadDaysChange)
                    } else {
                        GenericSection(state, viewModel, { dateTarget = DateTarget.START })
                    }
                    TimesSection(
                        state = state,
                        onAfterSchoolChange = viewModel::onAfterSchoolChange,
                        onEditTime = { timeRequest = TimeRequest(it) },
                        onAddTime = { timeRequest = TimeRequest(null) },
                        onRemoveTime = viewModel::removeTime,
                    )
                    if (!state.isNew && state.schoolType?.supportsQuiz == true) {
                        KartkaOutlineButton(
                            text = "Quiz do nauki na ${state.schoolType?.label?.lowercase()}",
                            onClick = { onOpenQuiz(state.reminderId) },
                            icon = KartkaIcons.Quiz,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    dateTarget?.let { target ->
        ReminderDatePicker(
            initialDate = if (target == DateTarget.EVENT) state.eventDate else state.startDate,
            title = if (target == DateTarget.EVENT) state.schoolType?.dateLabel ?: "Termin" else "Od kiedy?",
            onDismiss = { dateTarget = null },
            onConfirm = {
                if (target == DateTarget.EVENT) viewModel.onEventDateChange(it) else viewModel.onStartDateChange(it)
                dateTarget = null
            },
        )
    }
    timeRequest?.let { request ->
        KartkaTimePickerDialog(
            initialTime = request.editing ?: defaultNewTime(state.times),
            title = if (request.editing == null) "Dodaj godzinę" else "Zmień godzinę",
            onDismiss = { timeRequest = null },
            onConfirm = { viewModel.onTimePicked(request.editing, it); timeRequest = null },
        )
    }
    if (deleting) {
        KartkaDialog(
            onDismissRequest = { deleting = false },
            title = "Usunąć przypomnienie?",
            text = "„${state.title}” zostanie usunięte, a zaplanowane powiadomienia anulowane.",
            confirmLabel = "Usuń",
            destructive = true,
            onConfirm = { deleting = false; viewModel.delete() },
        )
    }
}

@Composable
private fun EditSection(title: String, description: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KartkaSectionHeader(title)
        if (description != null) Text(description, style = KartkaType.BodySmall, color = LookTheme.kartka.ink2)
        content()
        KartkaDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FolderSection(state: EditReminderUiState, onFolder: (Long) -> Unit, onType: (SchoolType) -> Unit) {
    EditSection("Gdzie", if (state.isSchoolFolder) "Wybierz, co to za wpis." else null) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.folders.forEach { folder ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(folder.colorArgb)))
                    KartkaFilterChip(folder.name, folder.id == state.folderId, { onFolder(folder.id) })
                }
            }
        }
        if (state.isSchoolFolder) {
            KartkaDivider()
            SchoolType.entries.forEach { type ->
                KartkaOptionRow(
                    title = type.label,
                    selected = state.schoolType == type,
                    onClick = { onType(type) },
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
private fun EventSection(state: EditReminderUiState, onPick: () -> Unit, onDate: (LocalDate) -> Unit, onLead: (Int) -> Unit) {
    val type = state.schoolType ?: return
    val today = LocalDate.now()
    val c = LookTheme.kartka
    EditSection("Termin") {
        KartkaCard(onClick = onPick, onClickLabel = "Zmień termin", verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KartkaEyebrow(type.dateLabel)
            Text("${dayName(state.eventDate).replaceFirstChar { it.uppercase() }}, ${state.eventDate.format(DateFormatter)}",
                style = KartkaType.Title, color = c.ink)
            Text(countdownText(state.eventDate, today) + lessonsInfo(state.schoolTimes, state.eventDate),
                style = KartkaType.Caption, color = c.ink2)
            KartkaTextButton("Zmień datę", onPick, icon = KartkaIcons.Calendar)
        }
        state.eventDateError?.let { ErrorText(it) }
        val dates = listOf(
            "Jutro" to today.plusDays(1),
            "Pojutrze" to today.plusDays(2),
            "Pn." to today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)),
            "Za tydzień" to today.plusWeeks(1),
        ).distinctBy { it.second }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dates.forEach { (label, date) -> KartkaFilterChip(label, date == state.eventDate, { onDate(date) }) }
        }
        KartkaListRow("Zacznij przypominać", subtitle = leadDaysText(state.leadDays), showDivider = false)
        Slider(
            value = state.leadDays.toFloat(),
            onValueChange = { onLead(Math.round(it)) },
            valueRange = SchoolPrefs.MIN_LEAD_DAYS.toFloat()..SchoolPrefs.MAX_LEAD_DAYS.toFloat(),
            steps = SchoolPrefs.MAX_LEAD_DAYS - SchoolPrefs.MIN_LEAD_DAYS - 1,
            colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.surface),
            modifier = Modifier.heightIn(min = 48.dp),
        )
        val first = state.eventFirstDay
        val last = state.eventLastDay
        Text(
            when {
                last.isBefore(first) -> "Termin jest zbyt blisko – wybierz późniejszy dzień."
                first == last -> "Powiadomienie: ${formatShortDay(first)}"
                else -> "Powiadomienia: od ${formatShortDay(first)} do ${formatShortDay(last)}"
            },
            style = KartkaType.Caption, color = c.ink2,
        )
    }
}

@Composable
private fun GenericSection(state: EditReminderUiState, viewModel: EditReminderViewModel, onPick: () -> Unit) {
    val c = LookTheme.kartka
    EditSection("Kiedy") {
        Text("W które dni · ${weekdaysText(state.daysOfWeek)}", style = KartkaType.RowTitle, color = c.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WEEKDAY_SHORT.forEachIndexed { index, label ->
                KartkaFilterChip(
                    text = label,
                    selected = state.daysOfWeek != 0 && (state.daysOfWeek and (1 shl index)) != 0,
                    onClick = { viewModel.toggleWeekday(index) },
                )
            }
        }
        Text("Nic nie zaznaczone = codziennie.", style = KartkaType.Caption, color = c.ink2)
        KartkaOutlineButton(
            text = "Od: " + (if (state.startDate == LocalDate.now()) "dziś, " else "${dayName(state.startDate)}, ") + state.startDate.format(DateFormatter),
            onClick = onPick,
            icon = KartkaIcons.Calendar,
            modifier = Modifier.fillMaxWidth(),
        )
        KartkaSwitchRow("Bez końca", state.endless, viewModel::onEndlessChange,
            subtitle = "Przypominaj bez daty zakończenia", showDivider = false)
        if (!state.endless) {
            val days = state.daysText.toIntOrNull()
            val lastDay = if (days != null && days in MIN_DAYS..MAX_DAYS) state.startDate.plusDays(days - 1L) else null
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KartkaSecondaryButton("−", { viewModel.changeDaysBy(-1) },
                    modifier = Modifier.semantics { contentDescription = "Odejmij dzień" })
                KartkaTextField(
                    value = state.daysText,
                    onValueChange = viewModel::onDaysTextChange,
                    label = "Liczba dni",
                    isError = state.daysError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                KartkaIconButton(KartkaIcons.Plus, "Dodaj dzień", { viewModel.changeDaysBy(1) })
            }
            if (state.daysError != null) ErrorText(state.daysError)
            else Text(
                if (lastDay != null) "Ostatni dzień: ${dayName(lastDay)}, ${lastDay.format(DateFormatter)}" else "Od $MIN_DAYS do $MAX_DAYS dni",
                style = KartkaType.Caption, color = c.ink2,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to "1 dzień", 7 to "Tydzień", 14 to "2 tyg.", 30 to "Miesiąc").forEach { (value, label) ->
                    KartkaFilterChip(label, days == value, { viewModel.setDays(value) })
                }
            }
        }
    }
}

@Composable
private fun TimesSection(
    state: EditReminderUiState,
    onAfterSchoolChange: (Boolean) -> Unit,
    onEditTime: (LocalTime) -> Unit,
    onAddTime: () -> Unit,
    onRemoveTime: (LocalTime) -> Unit,
) {
    val c = LookTheme.kartka
    EditSection(
        "Godzina powiadomienia",
        if (state.usesAfterSchool) "Każdego dnia godzinę po ostatniej lekcji – według planu lekcji."
        else "Każdego dnia o tych godzinach przyjdzie powiadomienie.",
    ) {
        if (state.isSchoolFolder) {
            KartkaSwitchRow("Godzinę po lekcjach", state.afterSchool, onAfterSchoolChange,
                subtitle = "Czas z planu lekcji (Librus) albo z ustawień w Koncie", showDivider = false)
        }
        if (state.usesAfterSchool) {
            if (state.previewDays.isEmpty()) Text("Brak dni z powiadomieniami.", style = KartkaType.BodySmall, color = c.ink2)
            state.previewDays.forEach { day ->
                KartkaListRow(
                    title = formatShortDay(day),
                    subtitle = lessonsInfo(state.schoolTimes, day).removePrefix(" · "),
                    trailing = { KartkaTimeLabel(state.schoolTimes.afterSchoolTime(day).format(KartkaHourFormatter), color = c.accent) },
                )
            }
        } else {
            if (state.times.isEmpty()) Text("Nie dodano jeszcze żadnej godziny.", style = KartkaType.BodySmall, color = c.ink2)
            state.times.forEach { time ->
                KartkaListRow(
                    title = time.format(KartkaHourFormatter),
                    leading = { androidx.compose.material3.Icon(KartkaIcons.Clock, null, tint = c.accent) },
                    onClick = { onEditTime(time) },
                    onClickLabel = "Zmień godzinę",
                    trailing = { KartkaIconButton(KartkaIcons.Close, "Usuń godzinę ${time.format(KartkaHourFormatter)}", { onRemoveTime(time) }) },
                )
            }
            state.timesError?.let { ErrorText(it) }
            KartkaOutlineButton("Dodaj godzinę", onAddTime, icon = KartkaIcons.Plus, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = KartkaType.Caption, color = MaterialTheme.colorScheme.error)
}

/** Zachowuje także ręczne wpisywanie daty dostępne w starym edytorze. */
@Composable
private fun ReminderDatePicker(initialDate: LocalDate, title: String, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val colors = kartkaDatePickerColors()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = KartkaDefaults.DialogShape,
        colors = colors,
        tonalElevation = 0.dp,
        confirmButton = {
            KartkaTextButton(
                text = "OK",
                enabled = picker.selectedDateMillis != null,
                onClick = {
                    picker.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
            )
        },
        dismissButton = { KartkaTextButton("Anuluj", onDismiss, color = LookTheme.kartka.ink2) },
    ) {
        DatePicker(
            state = picker,
            colors = colors,
            title = { Text(title, style = KartkaType.Caption, color = LookTheme.kartka.ink2, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
        )
    }
}

private fun lessonsInfo(times: SchoolTimes, day: LocalDate): String = when (times.source(day)) {
    SchoolTimes.Source.TIMETABLE -> " · lekcje do ${times.lessonsEnd(day)?.format(KartkaHourFormatter)} (plan)"
    SchoolTimes.Source.WEEKDAY -> " · lekcje do ${times.lessonsEnd(day)?.format(KartkaHourFormatter)}"
    SchoolTimes.Source.NO_SCHOOL -> " · bez lekcji"
}

private fun defaultNewTime(existing: List<LocalTime>): LocalTime {
    val last = existing.maxOrNull()
    return if (last != null && last.hour < 23) last.plusHours(1)
    else LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
}
