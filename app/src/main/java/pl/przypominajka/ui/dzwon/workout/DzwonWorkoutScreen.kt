@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.przypominajka.ui.dzwon.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pl.przypominajka.alarm.RestState
import pl.przypominajka.alarm.TrainingSettings
import pl.przypominajka.alarm.TrainingTrigger
import pl.przypominajka.data.FolderColors
import pl.przypominajka.data.WorkoutExercise
import pl.przypominajka.data.WorkoutFolder
import pl.przypominajka.data.WorkoutPlan
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.workout.*
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.pluralPl
import java.time.DayOfWeek
import java.time.LocalDate

private data class FolderRequest(val editing: WorkoutFolder?)
private data class PlanRequest(val editing: WorkoutPlan?)
private data class ExerciseRequest(val planId: Long, val editing: WorkoutExercise?)

/** Foldery, plany i ćwiczenia we wspólnym arkuszu Dzwonu. Logikę przechowuje wspólny ViewModel. */
@Composable
fun DzwonWorkoutScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val training by viewModel.training.collectAsStateWithLifecycle()
    val folder = state.selectedFolder
    val c = LookTheme.dzwon
    var folderRequest by remember { mutableStateOf<FolderRequest?>(null) }
    var planRequest by remember { mutableStateOf<PlanRequest?>(null) }
    var exerciseRequest by remember { mutableStateOf<ExerciseRequest?>(null) }
    var deletingFolder by remember { mutableStateOf<WorkoutFolder?>(null) }
    var deletingPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var folderMenu by remember { mutableStateOf(false) }
    var weekendDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    var completion by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current

    DzwonSheetScaffold(
        title = "Trening",
        subtitle = folder?.name ?: "Twój plan treningowy",
        onClose = onBack,
        headerExtra = {
            DzwonBellInRing(
                progress = if (state.total == 0) 0f else state.doneCount.toFloat() / state.total,
                swingKey = completion.takeIf { it > 0 },
                contentDescription = "Zrobione ${state.doneCount} z ${state.total} ćwiczeń",
            )
        },
        actions = {
            if (folder != null) Box {
                DzwonRoundIconButton(Icons.Rounded.MoreVert, "Opcje folderu", { folderMenu = true })
                DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                    DropdownMenuItem(text = { Text("Zmień nazwę i kolor") }, onClick = {
                        folderMenu = false
                        folderRequest = FolderRequest(folder)
                    })
                    DropdownMenuItem(text = { Text("Usuń folder", color = MaterialTheme.colorScheme.error) }, onClick = {
                        folderMenu = false
                        deletingFolder = folder
                    })
                }
            }
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DzwonSecondaryButton("Nowy folder", { folderRequest = FolderRequest(null) }, Modifier.weight(1f))
                if (folder != null) DzwonPrimaryButton("Nowy plan", { planRequest = PlanRequest(null) }, Modifier.weight(1f))
            }
        },
    ) {
        if (state.isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.gold)
            }
        } else LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("folders") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.folders, key = { it.id }) { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(entry.colorArgb)))
                            DzwonChip(entry.name, { viewModel.selectFolder(entry.id) }, selected = entry.id == folder?.id)
                        }
                    }
                    item("add") {
                        DzwonChip("Folder", { folderRequest = FolderRequest(null) }, icon = DzwonIcons.Plus)
                    }
                }
            }
            item("permissions") { DzwonPermissionPrompts() }
            item("training") {
                TrainingCard(training, viewModel::setTrainingEnabled, viewModel::toggleTrainingDay, { weekendDay = it })
            }
            if (folder == null) {
                item("empty") {
                    DzwonEmptyState("Zacznij od folderu", "Folder to np. Siłownia albo Bieganie. W środku zrobisz plany i listy ćwiczeń.",
                        action = { DzwonPrimaryButton("Nowy folder", { folderRequest = FolderRequest(null) }) })
                }
            } else {
                if (state.total > 0) item("summary") {
                    DzwonCard(color = c.field, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                        DzwonSectionHeader(folder.name)
                        Text("${state.doneCount * 100 / state.total}%", style = DzwonType.ScreenTitle, color = c.goldText)
                        Text(if (state.doneCount == state.total) "Trening zrobiony – brawo!" else "Zrobione ${state.doneCount} z ${state.total}",
                            style = DzwonType.Body, color = c.ink2)
                    }
                }
                item("rest") { RestCard(rest, viewModel::startRest, viewModel::extendRest, viewModel::stopRest) }
                if (state.plans.isEmpty()) item("presets") {
                    DzwonSectionHeader("Wybierz gotowy podział")
                    Text("Dodamy puste plany, a ćwiczenia wpiszesz sam. Możesz też zrobić własny podział.",
                        style = DzwonType.Caption, color = c.ink2)
                    WORKOUT_PRESETS.forEach { (label, names) ->
                        DzwonCard(modifier = Modifier.padding(top = 10.dp).fillMaxWidth(), onClick = { viewModel.addPlans(names) }) {
                            DzwonListRow(label, subtitle = names.joinToString(" · "), trailing = {
                                DzwonRoundIconButton(DzwonIcons.Plus, "Dodaj podział $label", { viewModel.addPlans(names) })
                            })
                        }
                    }
                    DzwonSecondaryButton("Własny plan", { planRequest = PlanRequest(null) }, Modifier.fillMaxWidth().padding(top = 10.dp))
                } else items(state.plans, key = { it.plan.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        folderColor = Color(folder.colorArgb),
                        onExpand = { viewModel.toggleExpanded(plan.plan.id) },
                        onCheck = { exercise, checked ->
                            if (checked && !exercise.done) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (plan.doneCount == plan.total - 1) completion++
                            }
                            viewModel.setDone(exercise, checked)
                        },
                        onEdit = { exerciseRequest = ExerciseRequest(plan.plan.id, it) },
                        onAdd = { exerciseRequest = ExerciseRequest(plan.plan.id, null) },
                        onReset = { viewModel.resetPlan(plan.plan.id) },
                        onRename = { planRequest = PlanRequest(plan.plan) },
                        onDelete = { deletingPlan = plan.plan },
                    )
                }
            }
        }
    }

    weekendDay?.let { day ->
        DzwonTimePickerDialog(
            initialTime = training.settings.weekendTime(day), title = "Trening: ${DAY_NAMES[day.value - 1]}",
            onDismiss = { weekendDay = null },
            onConfirm = { viewModel.setTrainingWeekendTime(day, it); weekendDay = null },
        )
    }
    folderRequest?.let { request ->
        FolderEditor(request.editing, { folderRequest = null }) { name, color ->
            val editing = request.editing
            if (editing == null) viewModel.addFolder(name, color) else viewModel.updateFolder(editing, name, color)
            folderRequest = null
        }
    }
    planRequest?.let { request ->
        PlanEditor(request.editing, { planRequest = null }) { name ->
            val editing = request.editing
            if (editing == null) viewModel.addPlans(listOf(name)) else viewModel.renamePlan(editing, name)
            planRequest = null
        }
    }
    exerciseRequest?.let { request ->
        ExerciseEditor(request.editing, { exerciseRequest = null }, onSave = { name, details, keepOpen ->
            val editing = request.editing
            if (editing == null) viewModel.addExercise(request.planId, name, details) else viewModel.updateExercise(editing, name, details)
            if (!keepOpen || editing != null) exerciseRequest = null
        }, onDelete = request.editing?.let { exercise -> { viewModel.deleteExercise(exercise); exerciseRequest = null } })
    }
    deletingFolder?.let { target ->
        DzwonDialog(
            onDismissRequest = { deletingFolder = null }, title = "Usunąć folder „${target.name}”?",
            text = "Razem z nim znikną wszystkie plany i ćwiczenia w tym folderze.",
            confirmText = "Usuń", destructive = true,
            onConfirm = { viewModel.deleteFolder(target); deletingFolder = null },
        )
    }
    deletingPlan?.let { target ->
        DzwonDialog(
            onDismissRequest = { deletingPlan = null }, title = "Usunąć plan „${target.name}”?",
            text = "Lista ćwiczeń w tym planie zostanie usunięta.", confirmText = "Usuń", destructive = true,
            onConfirm = { viewModel.deletePlan(target); deletingPlan = null },
        )
    }
}

@Composable
private fun TrainingCard(training: TrainingUi, onEnabled: (Boolean) -> Unit, onDay: (DayOfWeek) -> Unit, onTime: (DayOfWeek) -> Unit) {
    val c = LookTheme.dzwon
    val settings = training.settings
    DzwonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Przypomnienie o treningu", style = DzwonType.SmallTitle, color = c.ink)
                Text("Gdy kończą się lekcje", style = DzwonType.Caption, color = c.ink2)
            }
            DzwonSwitch(settings.enabled, onEnabled,
                Modifier.semantics { contentDescription = "Przypomnienie o treningu" })
        }
        AnimatedVisibility(settings.enabled, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(150))) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        DzwonChip(DAY_LETTERS[day.value - 1], { onDay(day) }, selected = day in settings.days,
                            modifier = Modifier.semantics { contentDescription = DAY_NAMES[day.value - 1] })
                    }
                }
                TrainingSettings.WEEKEND.filter { it in settings.days }.forEach { day ->
                    DzwonListRow(
                        title = DAY_NAMES[day.value - 1].replaceFirstChar { it.uppercase() },
                        subtitle = settings.weekendTime(day).format(TimeFormatter),
                        trailing = { DzwonTextButton("Zmień", { onTime(day) }) },
                    )
                }
                Text(trainingNextText(training), style = DzwonType.CaptionStrong,
                    color = if (settings.days.isEmpty()) MaterialTheme.colorScheme.error else c.goldText)
            }
        }
    }
}

private val DAY_LETTERS = listOf("PN", "WT", "ŚR", "CZ", "PT", "SB", "ND")
private val DAY_NAMES = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")

private fun trainingNextText(training: TrainingUi): String {
    if (training.settings.days.isEmpty()) return "Wybierz przynajmniej jeden dzień"
    val next = training.next ?: return "Brak terminu w najbliższym tygodniu"
    val date = next.at.toLocalDate()
    val today = LocalDate.now()
    val day = when (date) {
        today -> "dziś"
        today.plusDays(1) -> "jutro"
        else -> "w " + listOf("poniedziałek", "wtorek", "środę", "czwartek", "piątek", "sobotę", "niedzielę")[date.dayOfWeek.value - 1]
    }
    val reason = when (next.source) {
        TrainingTrigger.Source.LESSONS -> "koniec lekcji"
        TrainingTrigger.Source.WEEKEND -> "Twoja godzina na weekend"
        TrainingTrigger.Source.NO_SCHOOL -> "dzień bez lekcji, godzina domyślna"
    }
    return "Najbliższe: $day o ${next.at.toLocalTime().format(TimeFormatter)} – $reason"
}

@Composable
private fun RestCard(rest: RestState?, onStart: (Int) -> Unit, onExtend: (Int) -> Unit, onStop: () -> Unit) {
    val c = LookTheme.dzwon
    DzwonCard(modifier = Modifier.fillMaxWidth(), color = c.field, contentPadding = PaddingValues(18.dp)) {
        DzwonSectionHeader("Przerwa między seriami")
        if (rest == null) {
            Text("Zawibruje, gdy minie – także przy zablokowanym ekranie", style = DzwonType.Caption, color = c.ink2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)) {
                REST_PRESETS.forEach { seconds -> DzwonChip(formatSeconds(seconds), { onStart(seconds) }, selected = true) }
            }
        } else {
            var now by remember(rest.endsAt) { mutableLongStateOf(System.currentTimeMillis()) }
            val haptics = LocalHapticFeedback.current
            LaunchedEffect(rest) {
                while (true) {
                    now = System.currentTimeMillis()
                    if (now >= rest.endsAt) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        break
                    }
                    delay(200)
                }
            }
            val remainingMs = (rest.endsAt - now).coerceAtLeast(0L)
            val remaining = ((remainingMs + 999) / 1000).toInt()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                    ProgressRing(if (rest.totalSeconds == 0) 0f else remainingMs / (rest.totalSeconds * 1000f),
                        modifier = Modifier.fillMaxSize(), animate = false)
                    Text(formatSeconds(remaining), style = DzwonType.SheetTitle, color = c.ink)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (remaining == 0) "Koniec przerwy!" else "Odpoczywasz…", style = DzwonType.SmallTitle, color = c.ink)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                        DzwonSecondaryButton("+30 s", { onExtend(30) })
                        DzwonPrimaryButton("Stop", onStop)
                    }
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

@Composable
private fun PlanCard(
    plan: WorkoutPlanUi,
    folderColor: Color,
    onExpand: () -> Unit,
    onCheck: (WorkoutExercise, Boolean) -> Unit,
    onEdit: (WorkoutExercise) -> Unit,
    onAdd: () -> Unit,
    onReset: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = LookTheme.dzwon
    var menu by remember { mutableStateOf(false) }
    val allDone = plan.total > 0 && plan.doneCount == plan.total
    DzwonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(end = 8.dp).size(40.dp), contentAlignment = Alignment.Center) {
                if (plan.total > 0) ProgressRing(plan.doneCount.toFloat() / plan.total, modifier = Modifier.fillMaxSize(), strokeWidth = 3.dp)
                Box(Modifier.size(10.dp).clip(CircleShape).background(folderColor))
            }
            Column(Modifier.weight(1f).heightIn(min = 56.dp).clickable(onClick = onExpand).padding(vertical = 8.dp)) {
                Text(plan.plan.name, style = DzwonType.SectionTitle, color = c.ink)
                Text(when {
                    plan.total == 0 -> "Brak ćwiczeń"
                    allDone -> "Wszystko zrobione"
                    else -> "${plan.doneCount} z ${plan.total} ${pluralPl(plan.total, "ćwiczenia", "ćwiczeń", "ćwiczeń")}"
                }, style = DzwonType.Caption, color = c.ink2)
            }
            DzwonRoundIconButton(if (plan.expanded) DzwonIcons.ChevronDown else DzwonIcons.ChevronRight,
                if (plan.expanded) "Zwiń plan" else "Rozwiń plan", onExpand, size = 36.dp)
            Box {
                DzwonRoundIconButton(Icons.Rounded.MoreVert, "Opcje planu", { menu = true }, size = 36.dp)
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Zmień nazwę") }, onClick = { menu = false; onRename() })
                    if (plan.doneCount > 0) DropdownMenuItem(text = { Text("Wyczyść zaznaczenia") }, onClick = { menu = false; onReset() })
                    DropdownMenuItem(text = { Text("Usuń plan", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        AnimatedVisibility(plan.expanded, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(150))) {
            Column {
                DzwonDivider()
                plan.exercises.forEach { exercise ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        DzwonCheckbox(exercise.done, { onCheck(exercise, it) },
                            contentDescription = (if (exercise.done) "Odznacz: " else "Odhacz: ") + exercise.name,
                            uncheckedFill = c.surface)
                        Column(Modifier.weight(1f).heightIn(min = 56.dp).clickable { onCheck(exercise, !exercise.done) }.padding(vertical = 10.dp)) {
                            Text(exercise.name, style = DzwonType.Body, color = if (exercise.done) c.ink2 else c.ink,
                                textDecoration = if (exercise.done) TextDecoration.LineThrough else null)
                            if (exercise.details.isNotBlank()) Text(exercise.details, style = DzwonType.Caption, color = c.ink2)
                        }
                        DzwonRoundIconButton(DzwonIcons.Pencil, "Edytuj ćwiczenie: ${exercise.name}", { onEdit(exercise) }, size = 36.dp)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DzwonTextButton("Dodaj ćwiczenie", onAdd, icon = DzwonIcons.Plus)
                    if (plan.doneCount > 0) DzwonTextButton("Wyczyść", onReset)
                }
            }
        }
    }
}

@Composable
private fun FolderEditor(folder: WorkoutFolder?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var color by rememberSaveable(folder?.id) { mutableIntStateOf(folder?.colorArgb ?: FolderColors.palette.first()) }
    DzwonBottomSheet(onDismissRequest = onDismiss, title = if (folder == null) "Nowy folder" else "Edytuj folder") {
        DzwonTextField(name, { name = it.take(30) }, label = "Nazwa", placeholder = "np. Siłownia, Bieganie, Basen")
        DzwonSectionLabel("Kolor folderu")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderColors.palette.forEachIndexed { index, option ->
                Box(Modifier.size(48.dp).clip(CircleShape).clickable { color = option }
                    .semantics { contentDescription = "Kolor folderu ${index + 1}" + if (color == option) ", wybrany" else "" },
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(option)), contentAlignment = Alignment.Center) {
                        if (color == option) androidx.compose.material3.Icon(DzwonIcons.Check, null, tint = Color.White)
                    }
                }
            }
        }
        EditorActions(onDismiss, "Zapisz", name.isNotBlank()) { onSave(name, color) }
    }
}

@Composable
private fun PlanEditor(plan: WorkoutPlan?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable(plan?.id) { mutableStateOf(plan?.name.orEmpty()) }
    DzwonBottomSheet(onDismissRequest = onDismiss, title = if (plan == null) "Nowy plan" else "Zmień nazwę planu") {
        DzwonTextField(name, { name = it.take(40) }, label = "Nazwa planu")
        if (plan == null) {
            DzwonSectionLabel("Podpowiedzi")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Push", "Pull", "Legs", "Upper", "Lower", "Full body", "Cardio", "Interwały").forEach { suggestion ->
                    DzwonChip(suggestion, { name = suggestion }, selected = name == suggestion)
                }
            }
        }
        EditorActions(onDismiss, "Zapisz", name.isNotBlank()) { onSave(name) }
    }
}

@Composable
private fun ExerciseEditor(exercise: WorkoutExercise?, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit, onDelete: (() -> Unit)?) {
    var name by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.name.orEmpty()) }
    var details by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.details.orEmpty()) }
    DzwonBottomSheet(onDismissRequest = onDismiss, title = if (exercise == null) "Nowe ćwiczenie" else "Edytuj ćwiczenie") {
        DzwonTextField(name, { name = it.take(60) }, label = "Ćwiczenie", placeholder = "np. Wyciskanie na ławce",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next))
        DzwonTextField(details, { details = it.take(80) }, label = "Serie i powtórzenia", placeholder = "np. 4 × 8, 60 kg",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
        if (onDelete != null) DzwonTextButton("Usuń ćwiczenie", onDelete, icon = DzwonIcons.Trash, color = MaterialTheme.colorScheme.error)
        if (exercise == null) DzwonSecondaryButton("Dodaj kolejne", {
            onSave(name, details, true)
            name = ""
            details = ""
        }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth())
        EditorActions(onDismiss, if (exercise == null) "Dodaj" else "Zapisz", name.isNotBlank(),
            dismissText = if (exercise == null) "Gotowe" else "Anuluj") { onSave(name, details, false) }
    }
}

@Composable
private fun EditorActions(onDismiss: () -> Unit, text: String, enabled: Boolean, dismissText: String = "Anuluj", onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
        DzwonTextButton(dismissText, onDismiss, color = LookTheme.dzwon.ink)
        DzwonPrimaryButton(text, onSave, enabled = enabled)
    }
}
