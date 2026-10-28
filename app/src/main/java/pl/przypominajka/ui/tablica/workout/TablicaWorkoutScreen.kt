@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.przypominajka.ui.tablica.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pl.przypominajka.alarm.RestState
import pl.przypominajka.alarm.TrainingSettings
import pl.przypominajka.alarm.TrainingTrigger
import pl.przypominajka.data.*
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.ui.workout.*
import pl.przypominajka.util.TimeFormatter
import java.time.DayOfWeek
import java.time.LocalDate

private data class FolderRequest(val editing: WorkoutFolder?)
private data class PlanRequest(val editing: WorkoutPlan?)
private data class ExerciseRequest(val planId: Long, val editing: WorkoutExercise?)

/** Foldery, plany i ćwiczenia jako przypięte do tablicy listy. */
@Composable
fun TablicaWorkoutScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val restPresets by viewModel.restPresets.collectAsStateWithLifecycle()
    var customRest by rememberSaveable { mutableStateOf(false) }
    val training by viewModel.training.collectAsStateWithLifecycle()
    val colors = LookTheme.tablica
    val folder = state.selectedFolder
    var folderDialog by remember { mutableStateOf<FolderRequest?>(null) }
    var planDialog by remember { mutableStateOf<PlanRequest?>(null) }
    var exerciseDialog by remember { mutableStateOf<ExerciseRequest?>(null) }
    var deleteFolder by remember { mutableStateOf<WorkoutFolder?>(null) }
    var deletePlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var folderMenu by remember { mutableStateOf(false) }
    var weekendDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    var completion by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val permissions = rememberTablicaPermissions()

    TablicaScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = dockContentPadding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item("header") {
                BoardHeader("Trening", subtitle = "Twoje plany i małe kroki") {
                    RoundIconButton(TablicaIcons.Back, "Wróć", onBack)
                }
            }
            if (state.isLoading) {
                item("loading") { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else {
                item("folders") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.folders, key = { it.id }) { entry ->
                            TablicaChip(entry.name, entry.id == folder?.id, { viewModel.selectFolder(entry.id) })
                        }
                        item("new") { OutlineInkButton("Nowy folder", { folderDialog = FolderRequest(null) }, icon = TablicaIcons.Plus) }
                    }
                }
                if (!permissions.allGranted && (training.settings.enabled || rest != null)) {
                    item("permissions") { TablicaPermissionNotes(permissions = permissions) }
                }
                item("training") {
                    TrainingNote(training, viewModel::setTrainingEnabled, viewModel::toggleTrainingDay, { weekendDay = it })
                }
                if (folder == null) {
                    item("empty") {
                        BoardEmptyState("Zacznij od folderu", "Folder to np. Siłownia albo Bieganie. W środku zrobisz plany i listy ćwiczeń.",
                            action = { InkButton("Nowy folder", { folderDialog = FolderRequest(null) }) })
                    }
                } else {
                    item("folder") {
                        Note(colors.noteFor(folder.id), decoration = NoteDecoration.Pin, tilt = -0.7f, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).background(Color(folder.colorArgb), CircleShape))
                                Text(folder.name, style = TablicaType.NoteTitle, modifier = Modifier.weight(1f).padding(start = 8.dp))
                                Box {
                                    RoundIconButton(Icons.Rounded.MoreVert, "Opcje folderu", { folderMenu = true }, style = RoundIconStyle.Plain)
                                    DropdownMenu(folderMenu, { folderMenu = false }) {
                                        DropdownMenuItem(text = { Text("Zmień nazwę i kolor") }, onClick = { folderMenu = false; folderDialog = FolderRequest(folder) })
                                        DropdownMenuItem(text = { Text("Usuń folder") }, onClick = { folderMenu = false; deleteFolder = folder })
                                    }
                                }
                            }
                            if (state.total > 0) {
                                Text("${state.doneCount * 100 / state.total}%", style = TablicaType.ScreenTitle)
                                Text(if (state.doneCount == state.total) "Trening zrobiony – brawo!" else "Zrobione ${state.doneCount} z ${state.total}", style = TablicaType.Body)
                                LinearProgressIndicator(progress = { state.doneCount.toFloat() / state.total }, color = LocalContentColor.current, trackColor = LocalContentColor.current.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                            }
                            InkButton("Nowy plan", { planDialog = PlanRequest(null) }, icon = TablicaIcons.Plus)
                        }
                    }
                    item("rest") { RestNote(rest, restPresets, viewModel::startRest, { customRest = true }, viewModel::extendRest, viewModel::stopRest) }
                    if (state.plans.isEmpty()) {
                        item("presets") {
                            SectionLabel("Wybierz gotowy podział")
                            Text("Dodamy puste plany, a ćwiczenia wpiszesz sam. Możesz też zrobić własny podział.", style = TablicaType.BodyLight)
                        }
                        items(WORKOUT_PRESETS, key = { it.first }) { (label, names) ->
                            Note(colors.mint, onClick = { viewModel.addPlans(names) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
                                Text(label, style = TablicaType.NoteTitle)
                                Text(names.joinToString(" · "), style = TablicaType.Hint)
                                Text("Dodaj podział", style = TablicaType.Link)
                            }
                        }
                        item("custom") { OutlineInkButton("Własny plan", { planDialog = PlanRequest(null) }) }
                    }
                    items(state.plans, key = { "plan_${it.plan.id}" }) { plan ->
                        PlanNote(
                            plan = plan,
                            onExpand = { viewModel.toggleExpanded(plan.plan.id) },
                            onCheck = { exercise, done ->
                                if (done) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (done && !exercise.done && plan.doneCount == plan.total - 1) completion++
                                viewModel.setDone(exercise, done)
                            },
                            onEdit = { exerciseDialog = ExerciseRequest(plan.plan.id, it) },
                            onAdd = { exerciseDialog = ExerciseRequest(plan.plan.id, null) },
                            onReset = { viewModel.resetPlan(plan.plan.id) },
                            onRename = { planDialog = PlanRequest(plan.plan) },
                            onDelete = { deletePlan = plan.plan },
                        )
                    }
                }
            }
        }
        CompletionBurst(completion)
    }
    if (customRest) {
        RestTimeDialog(
            restPresets, onDismiss = { customRest = false },
            onStart = { viewModel.startCustomRest(it); customRest = false }, onRemove = viewModel::removeRestPreset,
        )
    }
    weekendDay?.let { day ->
        TablicaTimePickerDialog(initial = training.settings.weekendTime(day), title = "Trening: ${DAY_NAMES[day.value - 1]}",
            onDismiss = { weekendDay = null }, onConfirm = { viewModel.setTrainingWeekendTime(day, it); weekendDay = null })
    }
    folderDialog?.let { request ->
        FolderDialog(request.editing, { folderDialog = null }) { name, color ->
            request.editing?.let { viewModel.updateFolder(it, name, color) } ?: viewModel.addFolder(name, color)
            folderDialog = null
        }
    }
    planDialog?.let { request ->
        PlanDialog(request.editing, { planDialog = null }) { name ->
            request.editing?.let { viewModel.renamePlan(it, name) } ?: viewModel.addPlans(listOf(name))
            planDialog = null
        }
    }
    exerciseDialog?.let { request ->
        ExerciseDialog(request.editing, { exerciseDialog = null }, { name, details, keepOpen ->
            request.editing?.let { viewModel.updateExercise(it, name, details) } ?: viewModel.addExercise(request.planId, name, details)
            if (!keepOpen || request.editing != null) exerciseDialog = null
        }, request.editing?.let { exercise -> { viewModel.deleteExercise(exercise); exerciseDialog = null } })
    }
    deleteFolder?.let { target ->
        ConfirmNoteDialog("Usunąć folder „${target.name}”?", "Razem z nim znikną wszystkie plany i ćwiczenia w tym folderze.", "Usuń",
            { viewModel.deleteFolder(target); deleteFolder = null }, { deleteFolder = null })
    }
    deletePlan?.let { target ->
        ConfirmNoteDialog("Usunąć plan „${target.name}”?", "Lista ćwiczeń w tym planie zostanie usunięta.", "Usuń",
            { viewModel.deletePlan(target); deletePlan = null }, { deletePlan = null })
    }
}

private val DAY_NAMES = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")
private val DAY_LETTERS = listOf("PN", "WT", "ŚR", "CZ", "PT", "SB", "ND")

@Composable
private fun TrainingNote(training: TrainingUi, onEnabled: (Boolean) -> Unit, onDay: (DayOfWeek) -> Unit, onTime: (DayOfWeek) -> Unit) {
    val settings = training.settings
    Note(LookTheme.tablica.mint, modifier = Modifier.fillMaxWidth(), decoration = NoteDecoration.Tape) {
        SwitchRow("Przypomnienie o treningu", settings.enabled, onEnabled, subtitle = "Gdy kończą się lekcje")
        if (settings.enabled) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    TablicaChip(DAY_LETTERS[day.value - 1], day in settings.days, { onDay(day) },
                        modifier = Modifier.semantics { contentDescription = DAY_NAMES[day.value - 1] })
                }
            }
            TrainingSettings.WEEKEND.filter { it in settings.days }.forEach { day ->
                NoteRow(DAY_NAMES[day.value - 1].replaceFirstChar { it.uppercase() },
                    subtitle = settings.weekendTime(day).format(TimeFormatter),
                    trailing = { InkTextButton("Zmień", { onTime(day) }) })
            }
            Text(trainingNextText(training), style = TablicaType.BodyLight)
        }
    }
}

private fun trainingNextText(training: TrainingUi): String {
    if (training.settings.days.isEmpty()) return "Wybierz przynajmniej jeden dzień"
    val next = training.next ?: return "Brak terminu w najbliższym tygodniu"
    val today = LocalDate.now()
    val day = when (next.at.toLocalDate()) {
        today -> "dziś"
        today.plusDays(1) -> "jutro"
        else -> "w " + listOf("poniedziałek", "wtorek", "środę", "czwartek", "piątek", "sobotę", "niedzielę")[next.at.dayOfWeek.value - 1]
    }
    val reason = when (next.source) {
        TrainingTrigger.Source.LESSONS -> "koniec lekcji"
        TrainingTrigger.Source.WEEKEND -> "Twoja godzina na weekend"
        TrainingTrigger.Source.NO_SCHOOL -> "dzień bez lekcji, godzina domyślna"
    }
    return "Najbliższe: $day o ${next.at.toLocalTime().format(TimeFormatter)} – $reason"
}

@Composable
private fun RestNote(rest: RestState?, presets: List<Int>, onStart: (Int) -> Unit, onCustom: () -> Unit, onExtend: (Int) -> Unit, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(rest) {
        if (rest != null) {
            while (true) {
                now = System.currentTimeMillis()
                if (now >= rest.endsAt) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); break }
                delay(200)
            }
        }
    }
    Note(LookTheme.tablica.pink, modifier = Modifier.fillMaxWidth(), tilt = 0.6f) {
        Text("Przerwa między seriami", style = TablicaType.NoteTitle)
        if (rest == null) {
            Text("Zawibruje, gdy minie – także przy zablokowanym ekranie", style = TablicaType.Hint)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { seconds -> OutlineInkButton(formatSeconds(seconds), { onStart(seconds) }) }
                OutlineInkButton("Własny", onCustom, icon = TablicaIcons.Plus)
            }
        } else {
            val remainingMs = (rest.endsAt - now).coerceAtLeast(0L)
            val remaining = ((remainingMs + 999L) / 1000L).toInt()
            Text(if (remaining == 0) "Koniec przerwy!" else "Odpoczywasz…", style = TablicaType.Body)
            Text(formatSeconds(remaining), style = TablicaType.ScreenTitle)
            LinearProgressIndicator(progress = { if (rest.totalSeconds <= 0) 0f else (remainingMs / (rest.totalSeconds * 1000f)).coerceIn(0f, 1f) }, color = LocalContentColor.current, trackColor = LocalContentColor.current.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineInkButton("+30 s", { onExtend(30) })
                InkButton("Stop", onStop)
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private val RestKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)

/** Własna długość przerwy (minuty + sekundy) i usuwanie czasów z szybkiego wyboru. */
@Composable
private fun RestTimeDialog(presets: List<Int>, onDismiss: () -> Unit, onStart: (Int) -> Unit, onRemove: (Int) -> Unit) {
    var minutes by rememberSaveable { mutableStateOf("") }
    var seconds by rememberSaveable { mutableStateOf("") }
    val total = parseRestTime(minutes, seconds)
    NoteDialog(onDismiss, "Własny czas przerwy", modifier = Modifier.imePadding(), buttons = {
        NoteDialogButtons("Start", { total?.let(onStart) }, onDismiss, confirmEnabled = total != null)
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Minuty", style = TablicaType.Hint)
                NoteTextField(minutes, { minutes = it.filter(Char::isDigit).take(2) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    label = "Minuty", placeholder = "0", singleLine = true, keyboardOptions = RestKeyboard)
            }
            Column(Modifier.weight(1f)) {
                Text("Sekundy", style = TablicaType.Hint)
                NoteTextField(seconds, { seconds = it.filter(Char::isDigit).take(2) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    label = "Sekundy", placeholder = "0", singleLine = true, keyboardOptions = RestKeyboard)
            }
        }
        Text(restTimeHint(total), style = TablicaType.Hint, modifier = Modifier.padding(top = 8.dp))
        if (presets.isNotEmpty()) {
            Text("Szybki wybór – dotknij, aby usunąć", style = TablicaType.Strong, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.forEach { preset ->
                    TablicaChip(
                        "${formatSeconds(preset)}  ✕", false, { onRemove(preset) },
                        modifier = Modifier.semantics { contentDescription = "Usuń ${formatSeconds(preset)} z szybkiego wyboru" },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanNote(
    plan: WorkoutPlanUi, onExpand: () -> Unit, onCheck: (WorkoutExercise, Boolean) -> Unit,
    onEdit: (WorkoutExercise) -> Unit, onAdd: () -> Unit, onReset: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val allDone = plan.total > 0 && plan.doneCount == plan.total
    Note(LookTheme.tablica.noteFor(plan.plan.id), modifier = Modifier.fillMaxWidth(), decoration = NoteDecoration.Tape) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClickLabel = if (plan.expanded) "Zwiń plan" else "Rozwiń plan", onClick = onExpand).heightIn(min = 48.dp)) {
                Text(plan.plan.name, style = TablicaType.NoteTitle)
                Text(when { plan.total == 0 -> "Brak ćwiczeń"; allDone -> "Wszystko zrobione"; else -> "Zrobione ${plan.doneCount} z ${plan.total}" }, style = TablicaType.Hint)
            }
            Box {
                RoundIconButton(Icons.Rounded.MoreVert, "Opcje planu ${plan.plan.name}", { menu = true }, style = RoundIconStyle.Plain)
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Zmień nazwę") }, onClick = { menu = false; onRename() })
                    if (plan.doneCount > 0) DropdownMenuItem(text = { Text("Wyczyść zaznaczenia") }, onClick = { menu = false; onReset() })
                    DropdownMenuItem(text = { Text("Usuń plan") }, onClick = { menu = false; onDelete() })
                }
            }
        }
        InkTextButton(if (plan.expanded) "Zwiń" else "Rozwiń", onExpand)
        if (plan.expanded) {
            if (plan.total > 0) LinearProgressIndicator(progress = { plan.doneCount.toFloat() / plan.total }, color = LocalContentColor.current, trackColor = LocalContentColor.current.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth())
            plan.exercises.forEach { exercise ->
                key(exercise.id) {
                    NoteDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NoteCheckbox(exercise.done, { onCheck(exercise, it) }, (if (exercise.done) "Odznacz: " else "Odhacz: ") + exercise.name)
                        Column(Modifier.weight(1f).toggleable(value = exercise.done, role = Role.Checkbox, onValueChange = { onCheck(exercise, it) }).heightIn(min = 48.dp).padding(vertical = 8.dp).alpha(if (exercise.done) 0.55f else 1f)) {
                            Text(exercise.name, style = TablicaType.Strong, textDecoration = if (exercise.done) TextDecoration.LineThrough else null)
                            if (exercise.details.isNotBlank()) Text(exercise.details, style = TablicaType.Hint)
                        }
                        InkTextButton("Edytuj", { onEdit(exercise) })
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InkTextButton("Dodaj ćwiczenie", onAdd, icon = TablicaIcons.Plus)
                if (plan.doneCount > 0) InkTextButton("Wyczyść", onReset)
            }
        }
    }
}

@Composable
private fun FolderDialog(folder: WorkoutFolder?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf(folder?.name.orEmpty()) }
    var color by rememberSaveable { mutableIntStateOf(folder?.colorArgb ?: FolderColors.palette.first()) }
    NoteDialog(onDismiss, if (folder == null) "Nowy folder" else "Edytuj folder", modifier = Modifier.imePadding(), buttons = {
        NoteDialogButtons("Zapisz", { onSave(name, color) }, onDismiss, confirmEnabled = name.isNotBlank())
    }) {
        Text("Nazwa", style = TablicaType.Hint)
        NoteTextField(name, { name = it.take(30) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), label = "Nazwa", placeholder = "np. Siłownia, Bieganie, Basen", singleLine = true)
        Text("Kolor folderu", style = TablicaType.Strong, modifier = Modifier.padding(top = 14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FolderColors.palette.forEachIndexed { index, option ->
                Box(Modifier.size(48.dp).selectable(selected = option == color, role = Role.RadioButton, onClick = { color = option })
                    .semantics { contentDescription = "Kolor ${index + 1}" + if (option == color) ", wybrany" else "" }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(34.dp).background(Color(option), CircleShape), contentAlignment = Alignment.Center) {
                        if (option == color) Icon(TablicaIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDialog(plan: WorkoutPlan?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(plan?.name.orEmpty()) }
    NoteDialog(onDismiss, if (plan == null) "Nowy plan" else "Zmień nazwę planu", modifier = Modifier.imePadding(), buttons = {
        NoteDialogButtons("Zapisz", { onSave(name) }, onDismiss, confirmEnabled = name.isNotBlank())
    }) {
        Text("Nazwa planu", style = TablicaType.Hint)
        NoteTextField(name, { name = it.take(40) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), label = "Nazwa planu", placeholder = "Nazwa planu", singleLine = true)
        if (plan == null) {
            Text("Podpowiedzi", style = TablicaType.Strong, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Push", "Pull", "Legs", "Upper", "Lower", "Full body", "Cardio", "Interwały").forEach { suggestion ->
                    TablicaChip(suggestion, name == suggestion, { name = suggestion })
                }
            }
        }
    }
}

@Composable
private fun ExerciseDialog(exercise: WorkoutExercise?, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit, onDelete: (() -> Unit)?) {
    var name by rememberSaveable { mutableStateOf(exercise?.name.orEmpty()) }
    var details by rememberSaveable { mutableStateOf(exercise?.details.orEmpty()) }
    NoteDialog(onDismiss, if (exercise == null) "Nowe ćwiczenie" else "Edytuj ćwiczenie", modifier = Modifier.imePadding(), buttons = {
        NoteDialogButtons(if (exercise == null) "Dodaj" else "Zapisz", { onSave(name, details, false) }, onDismiss,
            dismissText = if (exercise == null) "Gotowe" else "Anuluj", confirmEnabled = name.isNotBlank())
    }) {
        Text("Ćwiczenie", style = TablicaType.Hint)
        NoteTextField(name, { name = it.take(60) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), label = "Ćwiczenie", placeholder = "np. Wyciskanie na ławce", singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Next))
        Spacer(Modifier.height(10.dp))
        Text("Serie i powtórzenia", style = TablicaType.Hint)
        NoteTextField(details, { details = it.take(80) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), label = "Serie i powtórzenia", placeholder = "np. 4 × 8, 60 kg", singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done))
        if (onDelete != null) InkTextButton("Usuń ćwiczenie", onDelete)
        if (exercise == null) OutlineInkButton("Dodaj kolejne", {
            onSave(name, details, true); name = ""; details = ""
        }, enabled = name.isNotBlank(), modifier = Modifier.padding(top = 12.dp))
    }
}

/** Krótki rozsyp skrawków po ukończeniu planu. */
@Composable
private fun CompletionBurst(trigger: Int) {
    val progress = remember { Animatable(1f) }
    val colors = LookTheme.tablica.notes
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(300))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        if (p < 1f) {
            repeat(24) { index ->
                val x = size.width * ((index * 37 % 101) / 100f)
                val y = size.height * (0.15f + p * 0.45f) + (index % 4) * 12.dp.toPx()
                drawCircle(colors[index % colors.size].bg.copy(alpha = 1f - p), 5.dp.toPx(), Offset(x, y))
            }
        }
    }
}
