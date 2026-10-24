package pl.przypominajka.ui.edit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.FolderDot
import pl.przypominajka.ui.components.FormSection
import pl.przypominajka.ui.components.IconBadge
import pl.przypominajka.ui.components.NoticeCard
import pl.przypominajka.ui.components.PrimaryButton
import pl.przypominajka.ui.components.ReminderTimePickerDialog
import pl.przypominajka.ui.components.RoundIconButton
import pl.przypominajka.ui.components.accent
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.icon
import pl.przypominajka.ui.components.pastel
import pl.przypominajka.ui.components.pressScale
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.util.DateFormatter
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.dayName
import pl.przypominajka.util.formatShortDay
import pl.przypominajka.util.WEEKDAY_SHORT
import pl.przypominajka.util.leadDaysText
import pl.przypominajka.util.weekdaysText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

/** Stan okna wyboru godziny: [editing] == null oznacza dodawanie nowej godziny. */
private data class TimeDialogRequest(val editing: LocalTime?)

/** Które pole daty edytujemy w kalendarzu. */
private enum class DateTarget { START, EVENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    onBack: () -> Unit,
    onOpenQuiz: (reminderId: Long) -> Unit,
    viewModel: EditReminderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var datePickerFor by rememberSaveable { mutableStateOf<DateTarget?>(null) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var timeDialog by remember { mutableStateOf<TimeDialogRequest?>(null) }

    LaunchedEffect(state.done) {
        if (state.done) onBack()
    }

    Scaffold(
        containerColor = AppSurfaces.page,
        topBar = {
            BellTopBar(
                title = if (state.isNew) "Nowe przypomnienie" else "Edycja",
                eyebrow = if (state.isNew) "Dzwon da znać" else "Przypomnienie",
                onBack = onBack,
            ) {
                if (!state.isNew) {
                    RoundIconButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Usuń przypomnienie",
                        onClick = { showDeleteConfirm = true },
                    )
                }
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.isFromLibrus) {
                NoticeCard(
                    title = "Wpis z Librusa",
                    body = "Treść i termin uaktualnia synchronizacja, więc Twoje zmiany w nich mogą zostać " +
                        "nadpisane. Możesz go wyłączyć przełącznikiem albo usunąć – wtedy już nie wróci.",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    icon = Icons.Rounded.Info,
                )
            }

            FolderAndTypeSection(
                state = state,
                onFolderChange = viewModel::onFolderChange,
                onSchoolTypeChange = viewModel::onSchoolTypeChange,
            )

            FormSection(title = "Treść") {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(state.schoolType?.titleLabel ?: "O czym przypomnieć? *") },
                    isError = state.titleError,
                    supportingText = if (state.titleError) {
                        { Text("Tytuł jest wymagany") }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text(if (state.isEventMode) "Zakres materiału / notatka" else "Notatka (opcjonalnie)") },
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.isEventMode) {
                EventDateSection(
                    state = state,
                    onPickDate = { datePickerFor = DateTarget.EVENT },
                    onQuickDate = viewModel::onEventDateChange,
                    onLeadDaysChange = viewModel::onLeadDaysChange,
                )
            } else {
                GenericWhenSection(
                    state = state,
                    onPickDate = { datePickerFor = DateTarget.START },
                    onEndlessChange = viewModel::onEndlessChange,
                    onDaysTextChange = viewModel::onDaysTextChange,
                    onChangeDaysBy = viewModel::changeDaysBy,
                    onSetDays = viewModel::setDays,
                    onToggleWeekday = viewModel::toggleWeekday,
                )
            }

            TimesSection(
                state = state,
                onAfterSchoolChange = viewModel::onAfterSchoolChange,
                onEditTime = { time -> timeDialog = TimeDialogRequest(editing = time) },
                onAddTime = { timeDialog = TimeDialogRequest(editing = null) },
                onRemoveTime = viewModel::removeTime,
            )

            if (!state.isNew && state.schoolType?.supportsQuiz == true) {
                OutlinedButton(
                    onClick = { onOpenQuiz(state.reminderId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Rounded.Quiz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Quiz do nauki na ${state.schoolType?.label?.lowercase()}")
                }
            }

            PrimaryButton(
                text = if (state.isNew) "Dodaj przypomnienie" else "Zapisz zmiany",
                icon = Icons.Rounded.Check,
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    // ---------------------------------------------------------------- okna dialogowe

    datePickerFor?.let { target ->
        val initial = if (target == DateTarget.EVENT) state.eventDate else state.startDate
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        val date = it.utcMillisToLocalDate()
                        if (target == DateTarget.EVENT) viewModel.onEventDateChange(date) else viewModel.onStartDateChange(date)
                    }
                    datePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerFor = null }) { Text("Anuluj") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    timeDialog?.let { request ->
        ReminderTimePickerDialog(
            initialTime = request.editing ?: defaultNewTime(state.times),
            title = if (request.editing == null) "Dodaj godzinę" else "Zmień godzinę",
            onDismiss = { timeDialog = null },
            onConfirm = { picked ->
                viewModel.onTimePicked(request.editing, picked)
                timeDialog = null
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Usunąć przypomnienie?") },
            text = { Text("„${state.title}” zostanie usunięte, a zaplanowane powiadomienia anulowane.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Anuluj") }
            },
        )
    }
}

// ---------------------------------------------------------------- folder i rodzaj

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FolderAndTypeSection(
    state: EditReminderUiState,
    onFolderChange: (Long) -> Unit,
    onSchoolTypeChange: (SchoolType) -> Unit,
) {
    FormSection(
        title = "Gdzie",
        description = if (state.isSchoolFolder) "Wybierz, co to za wpis." else null,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.folders.forEach { folder ->
                val selected = folder.id == state.folderId
                val container by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                    label = "folderChip",
                )
                val content by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    label = "folderChipText",
                )
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onFolderChange(folder.id) },
                    shape = RoundedCornerShape(50),
                    color = container,
                    contentColor = content,
                    interactionSource = interaction,
                    modifier = Modifier.pressScale(interaction, pressedScale = 0.92f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FolderDot(color = Color(folder.colorArgb), size = 9.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(folder.name, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (state.isSchoolFolder) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SchoolType.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { type ->
                        SchoolTypeTile(
                            type = type,
                            selected = state.schoolType == type,
                            onClick = { onSchoolTypeChange(type) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchoolTypeTile(
    type: SchoolType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pastel = type.pastel
    val container by animateColorAsState(
        if (selected) pastel.container else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        label = "typeTile",
    )
    val borderColor by animateColorAsState(
        if (selected) pastel.content else Color.Transparent,
        label = "typeTileBorder",
    )
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = container,
        border = BorderStroke(2.dp, borderColor),
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) pastel.content else pastel.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else pastel.content,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = type.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ---------------------------------------------------------------- termin wydarzenia szkolnego

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EventDateSection(
    state: EditReminderUiState,
    onPickDate: () -> Unit,
    onQuickDate: (LocalDate) -> Unit,
    onLeadDaysChange: (Int) -> Unit,
) {
    val type = state.schoolType ?: return
    val today = LocalDate.now()
    val accent = type.accent

    FormSection(title = "Termin") {
        val dateInteraction = remember { MutableInteractionSource() }
        Surface(
            onClick = onPickDate,
            shape = RoundedCornerShape(22.dp),
            color = type.pastel.container,
            interactionSource = dateInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(dateInteraction, pressedScale = 0.97f),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(icon = Icons.Rounded.CalendarMonth, color = accent)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(type.dateLabel, style = MaterialTheme.typography.labelMedium, color = accent)
                    Text(
                        text = "${dayName(state.eventDate).replaceFirstChar { it.uppercase() }}, " +
                            state.eventDate.format(DateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = countdownText(state.eventDate, today) + lessonsInfo(state.schoolTimes, state.eventDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        state.eventDateError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        // Bez duplikatów – np. w niedzielę "Jutro" i "Pn." to ten sam dzień.
        val quickDates = listOf(
            "Jutro" to today.plusDays(1),
            "Pojutrze" to today.plusDays(2),
            "Pn." to today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)),
            "Za tydzień" to today.plusWeeks(1),
        ).distinctBy { it.second }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quickDates.forEach { (label, date) ->
                val selected = date == state.eventDate
                Surface(
                    onClick = { onQuickDate(date) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Zacznij przypominać", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(leadDaysText(state.leadDays), style = MaterialTheme.typography.titleMedium, color = accent)
        }
        Slider(
            value = state.leadDays.toFloat(),
            onValueChange = { onLeadDaysChange(Math.round(it)) },
            valueRange = SchoolPrefs.MIN_LEAD_DAYS.toFloat()..SchoolPrefs.MAX_LEAD_DAYS.toFloat(),
            steps = SchoolPrefs.MAX_LEAD_DAYS - SchoolPrefs.MIN_LEAD_DAYS - 1,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = Color.White.copy(alpha = 0.7f),
                inactiveTickColor = accent.copy(alpha = 0.5f),
            ),
        )
        val first = state.eventFirstDay
        val last = state.eventLastDay
        Text(
            text = if (last.isBefore(first)) {
                "Termin jest zbyt blisko – wybierz późniejszy dzień."
            } else if (first == last) {
                "Powiadomienie: ${formatShortDay(first)}"
            } else {
                "Powiadomienia: od ${formatShortDay(first)} do ${formatShortDay(last)}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

// ---------------------------------------------------------------- zwykłe przypomnienie

@Composable
private fun GenericWhenSection(
    state: EditReminderUiState,
    onPickDate: () -> Unit,
    onEndlessChange: (Boolean) -> Unit,
    onDaysTextChange: (String) -> Unit,
    onChangeDaysBy: (Int) -> Unit,
    onSetDays: (Int) -> Unit,
    onToggleWeekday: (Int) -> Unit,
) {
    FormSection(title = "Kiedy") {
        // ------------------------------------------------ dni tygodnia
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("W które dni", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = weekdaysText(state.daysOfWeek),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WEEKDAY_SHORT.forEachIndexed { index, label ->
                val selected = state.daysOfWeek != 0 && (state.daysOfWeek and (1 shl index)) != 0
                val container by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "weekday",
                )
                val content by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    label = "weekdayText",
                )
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onToggleWeekday(index) },
                    shape = CircleShape,
                    color = container,
                    contentColor = content,
                    interactionSource = interaction,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .pressScale(interaction, pressedScale = 0.85f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        Text(
            text = "Nic nie zaznaczone = codziennie.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        val isToday = state.startDate == LocalDate.now()
        OutlinedButton(
            onClick = onPickDate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Od: " + (if (isToday) "dziś, " else "${dayName(state.startDate)}, ") +
                    state.startDate.format(DateFormatter),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Bez końca", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Przypominaj bez daty zakończenia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.endless, onCheckedChange = onEndlessChange)
        }

        if (!state.endless) {
            val days = state.daysText.toIntOrNull()
            val lastDayInfo = if (days != null && days in MIN_DAYS..MAX_DAYS) {
                val lastDay = state.startDate.plusDays(days - 1L)
                "Ostatni dzień: ${dayName(lastDay)}, ${lastDay.format(DateFormatter)}"
            } else {
                "Od $MIN_DAYS do $MAX_DAYS dni"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { onChangeDaysBy(-1) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = state.daysText,
                    onValueChange = onDaysTextChange,
                    label = { Text("Liczba dni") },
                    isError = state.daysError != null,
                    supportingText = { Text(state.daysError ?: lastDayInfo) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = { onChangeDaysBy(1) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Dodaj dzień")
                }
            }
            // Szybki wybór najczęstszych długości.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DaysPresets.forEach { (value, label) ->
                    val selected = days == value
                    Surface(
                        selected = selected,
                        onClick = { onSetDays(value) },
                        shape = RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 9.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Przyciski szybkiego wyboru liczby dni. */
private val DaysPresets = listOf(1 to "1 dzień", 7 to "Tydzień", 14 to "2 tyg.", 30 to "Miesiąc")

// ---------------------------------------------------------------- godziny

@Composable
private fun TimesSection(
    state: EditReminderUiState,
    onAfterSchoolChange: (Boolean) -> Unit,
    onEditTime: (LocalTime) -> Unit,
    onAddTime: () -> Unit,
    onRemoveTime: (LocalTime) -> Unit,
) {
    FormSection(
        title = "Godzina powiadomienia",
        description = if (state.usesAfterSchool) {
            "Każdego dnia godzinę po ostatniej lekcji – według planu lekcji."
        } else {
            "Każdego dnia o tych godzinach przyjdzie powiadomienie."
        },
    ) {
        if (state.isSchoolFolder) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Godzinę po lekcjach", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Czas z planu lekcji (Librus) albo z ustawień w Koncie",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.afterSchool, onCheckedChange = onAfterSchoolChange)
            }
        }

        if (state.usesAfterSchool) {
            val days = state.previewDays
            if (days.isEmpty()) {
                Text(
                    text = "Brak dni z powiadomieniami.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            days.forEach { day -> AfterSchoolPreviewRow(day = day, schoolTimes = state.schoolTimes) }
        } else {
            if (state.times.isEmpty()) {
                Text(
                    text = "Nie dodano jeszcze żadnej godziny.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.times.forEach { time ->
                TimeRow(time = time, onClick = { onEditTime(time) }, onRemove = { onRemoveTime(time) })
            }
            state.timesError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onAddTime, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Dodaj godzinę")
            }
        }
    }
}

@Composable
private fun AfterSchoolPreviewRow(day: LocalDate, schoolTimes: SchoolTimes) {
    val source = schoolTimes.source(day)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatShortDay(day),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(88.dp),
            )
            Text(
                text = when (source) {
                    SchoolTimes.Source.TIMETABLE -> "lekcje do ${schoolTimes.lessonsEnd(day)?.format(TimeFormatter)} (plan)"
                    SchoolTimes.Source.WEEKDAY -> "lekcje do ${schoolTimes.lessonsEnd(day)?.format(TimeFormatter)}"
                    SchoolTimes.Source.NO_SCHOOL -> "bez lekcji"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = schoolTimes.afterSchoolTime(day).format(TimeFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRow(time: LocalTime, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = time.format(TimeFormatter),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Usuń godzinę ${time.format(TimeFormatter)}")
            }
        }
    }
}

/** Propozycja dla nowej godziny: godzinę po ostatniej na liście albo pełna następna godzina. */
private fun defaultNewTime(existing: List<LocalTime>): LocalTime {
    val last = existing.maxOrNull()
    return if (last != null && last.hour < 23) {
        last.plusHours(1)
    } else {
        LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    }
}

/** DatePicker Material 3 operuje na północy UTC. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
