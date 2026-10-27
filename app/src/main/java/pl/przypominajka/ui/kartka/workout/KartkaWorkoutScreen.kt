@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.przypominajka.ui.kartka.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
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
import pl.przypominajka.ui.kartka.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.workout.REST_PRESETS
import pl.przypominajka.ui.workout.WORKOUT_PRESETS
import pl.przypominajka.ui.workout.TrainingUi
import pl.przypominajka.ui.workout.WorkoutPlanUi
import pl.przypominajka.ui.workout.WorkoutViewModel
import java.time.DayOfWeek
import java.time.LocalDate

private data class FolderRequest(val editing: WorkoutFolder?)
private data class PlanRequest(val editing: WorkoutPlan?)
private data class ExerciseRequest(val planId: Long, val editing: WorkoutExercise?)

/** Trening Kartki: foldery i plany jako płaskie sekcje, ćwiczenia jak punkty checklisty. */
@Composable
fun KartkaWorkoutScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val training by viewModel.training.collectAsStateWithLifecycle()
    val folder = state.selectedFolder
    var folderDialog by remember { mutableStateOf<FolderRequest?>(null) }
    var planDialog by remember { mutableStateOf<PlanRequest?>(null) }
    var exerciseDialog by remember { mutableStateOf<ExerciseRequest?>(null) }
    var deleteFolder by remember { mutableStateOf<WorkoutFolder?>(null) }
    var deletePlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var folderMenu by remember { mutableStateOf(false) }
    var weekendDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    val snackbar = remember { SnackbarHostState() }
    var completed by remember { mutableIntStateOf(0) }
    LaunchedEffect(completed) {
        if (completed > 0) {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("Plan odhaczony – wszystko zrobione!")
        }
    }
    val c = LookTheme.kartka

    KartkaScaffold(
        onBack = onBack,
        snackbar = { KartkaSnackbarHost(snackbar) },
        actions = {
            if (folder != null) Box {
                KartkaIconButton(KartkaIcons.More, "Opcje folderu", { folderMenu = true })
                DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                    DropdownMenuItem(text = { Text("Zmień nazwę i kolor") }, onClick = {
                        folderMenu = false
                        folderDialog = FolderRequest(folder)
                    })
                    DropdownMenuItem(text = { Text("Usuń folder", color = MaterialTheme.colorScheme.error) }, onClick = {
                        folderMenu = false
                        deleteFolder = folder
                    })
                }
            }
        },
        bottomBar = {
            if (folder != null && !state.isLoading) KartkaBottomBar {
                KartkaButton("Nowy plan", { planDialog = PlanRequest(null) }, Modifier.fillMaxWidth(), icon = KartkaIcons.Plus)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(), contentPadding = padding.withExtraBottom(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("title") { KartkaPageTitle("Trening", subtitle = "Twoje plany i ćwiczenia") }
            if (state.isLoading) {
                item("loading") { Text("Wczytywanie treningów…", Modifier.padding(horizontal = 20.dp), style = KartkaType.BodySmall, color = c.ink2) }
            } else {
                item("folders") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.folders, key = { it.id }) { entry ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(Color(entry.colorArgb), KartkaDefaults.PillShape))
                                KartkaFilterChip(entry.name, entry.id == folder?.id, { viewModel.selectFolder(entry.id) })
                            }
                        }
                        item("add") { KartkaOutlineButton("Folder", { folderDialog = FolderRequest(null) }, icon = KartkaIcons.Plus) }
                    }
                }
                item("training") {
                    TrainingReminderSection(training, viewModel::setTrainingEnabled, viewModel::toggleTrainingDay, { weekendDay = it })
                }
                item("permissions") { KartkaPermissionNotices(Modifier.padding(horizontal = 20.dp)) }
                if (folder == null) {
                    item("empty") {
                        KartkaEmptyState("Zacznij od folderu", "Folder to np. Siłownia albo Bieganie. W środku zrobisz plany i listy ćwiczeń.", action = {
                            KartkaButton("Nowy folder", { folderDialog = FolderRequest(null) }, icon = KartkaIcons.Plus)
                        })
                    }
                } else {
                    if (state.total > 0) item("summary") {
                        KartkaCard(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            KartkaEyebrow(folder.name)
                            Text("${state.doneCount * 100 / state.total}%", style = KartkaType.Hero, color = c.accent)
                            Text(
                                if (state.doneCount == state.total) "Trening zrobiony – brawo!" else "Zrobione ${state.doneCount} z ${state.total}",
                                style = KartkaType.BodySmall, color = c.ink,
                            )
                            Progress(state.doneCount, state.total)
                        }
                    }
                    item("rest") { RestSection(rest, viewModel::startRest, viewModel::extendRest, viewModel::stopRest) }
                    if (state.plans.isEmpty()) item("presets") {
                        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            KartkaSectionHeader("Wybierz gotowy podział")
                            Text("Dodamy puste plany, a ćwiczenia wpiszesz sam. Możesz też zrobić własny podział.", style = KartkaType.BodySmall, color = c.ink2)
                            WORKOUT_PRESETS.forEach { (label, names) ->
                                KartkaListRow(label, subtitle = names.joinToString(" · "), onClick = { viewModel.addPlans(names) }, trailing = {
                                    Icon(KartkaIcons.Plus, contentDescription = null, tint = c.accent)
                                })
                            }
                            KartkaOutlineButton("Własny plan", { planDialog = PlanRequest(null) }, Modifier.fillMaxWidth())
                        }
                    } else items(state.plans, key = { it.plan.id }) { plan ->
                        PlanSection(
                            plan = plan,
                            onExpand = { viewModel.toggleExpanded(plan.plan.id) },
                            onCheck = { exercise, done ->
                                if (done && !exercise.done && plan.doneCount == plan.total - 1) completed++
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
    }
    weekendDay?.let { day ->
        KartkaTimePickerDialog(
            initialTime = training.settings.weekendTime(day), title = "Trening: ${DAY_NAMES[day.value - 1]}",
            onDismiss = { weekendDay = null },
            onConfirm = { viewModel.setTrainingWeekendTime(day, it); weekendDay = null },
        )
    }
    folderDialog?.let { request ->
        FolderDialog(request.editing, { folderDialog = null }) { name, color ->
            val editing = request.editing
            if (editing == null) viewModel.addFolder(name, color) else viewModel.updateFolder(editing, name, color)
            folderDialog = null
        }
    }
    planDialog?.let { request ->
        PlanDialog(request.editing, { planDialog = null }) { name ->
            val editing = request.editing
            if (editing == null) viewModel.addPlans(listOf(name)) else viewModel.renamePlan(editing, name)
            planDialog = null
        }
    }
    exerciseDialog?.let { request ->
        ExerciseDialog(
            request.editing, { exerciseDialog = null },
            onSave = { name, details, keepOpen ->
                val editing = request.editing
                if (editing == null) viewModel.addExercise(request.planId, name, details) else viewModel.updateExercise(editing, name, details)
                if (!keepOpen || editing != null) exerciseDialog = null
            },
            onDelete = request.editing?.let { exercise ->
                { viewModel.deleteExercise(exercise); exerciseDialog = null }
            },
        )
    }
    deleteFolder?.let { entry ->
        KartkaDialog(
            onDismissRequest = { deleteFolder = null }, title = "Usunąć folder „${entry.name}”?",
            text = "Razem z nim znikną wszystkie plany i ćwiczenia w tym folderze.",
            confirmLabel = "Usuń", destructive = true,
            onConfirm = { viewModel.deleteFolder(entry); deleteFolder = null },
        )
    }
    deletePlan?.let { plan ->
        KartkaDialog(
            onDismissRequest = { deletePlan = null }, title = "Usunąć plan „${plan.name}”?",
            text = "Lista ćwiczeń w tym planie zostanie usunięta.", confirmLabel = "Usuń", destructive = true,
            onConfirm = { viewModel.deletePlan(plan); deletePlan = null },
        )
    }
}

private val DAY_LETTERS = listOf("PN", "WT", "ŚR", "CZ", "PT", "SB", "ND")
private val DAY_NAMES = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")

@Composable
private fun TrainingReminderSection(training: TrainingUi, onEnabled: (Boolean) -> Unit, onDay: (DayOfWeek) -> Unit, onTime: (DayOfWeek) -> Unit) {
    val c = LookTheme.kartka
    val settings = training.settings
    KartkaOutlineCard(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KartkaSwitchRow("Przypomnienie o treningu", settings.enabled, onEnabled, subtitle = "Gdy kończą się lekcje", showDivider = false)
        if (settings.enabled) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { day ->
                    KartkaFilterChip(
                        DAY_LETTERS[day.value - 1], day in settings.days, { onDay(day) },
                        modifier = Modifier.semantics { contentDescription = DAY_NAMES[day.value - 1] }, role = Role.Checkbox,
                    )
                }
            }
            TrainingSettings.WEEKEND.filter { it in settings.days }.forEach { day ->
                KartkaListRow(
                    DAY_NAMES[day.value - 1].replaceFirstChar { it.uppercase() },
                    subtitle = settings.weekendTime(day).format(KartkaHourFormatter),
                    trailing = { KartkaTextButton("Zmień", { onTime(day) }) },
                )
            }
            Text(trainingNextText(training), style = KartkaType.BodySmall, color = if (settings.days.isEmpty()) MaterialTheme.colorScheme.error else c.ink2)
        }
    }
}

private fun trainingNextText(training: TrainingUi): String {
    if (training.settings.days.isEmpty()) return "Wybierz przynajmniej jeden dzień"
    val next = training.next ?: return "Brak terminu w najbliższym tygodniu"
    val date = next.at.toLocalDate()
    val today = LocalDate.now()
    val day = when (date) {
        today -> "dziś"
        today.plusDays(1) -> "jutro"
        else -> listOf("w poniedziałek", "we wtorek", "w środę", "w czwartek", "w piątek", "w sobotę", "w niedzielę")[date.dayOfWeek.value - 1]
    }
    val reason = when (next.source) {
        TrainingTrigger.Source.LESSONS -> "koniec lekcji"
        TrainingTrigger.Source.WEEKEND -> "Twoja godzina na weekend"
        TrainingTrigger.Source.NO_SCHOOL -> "dzień bez lekcji, godzina domyślna"
    }
    return "Najbliższe: $day o ${next.at.toLocalTime().format(KartkaHourFormatter)} – $reason"
}

@Composable
private fun RestSection(rest: RestState?, onStart: (Int) -> Unit, onExtend: (Int) -> Unit, onStop: () -> Unit) {
    val c = LookTheme.kartka
    KartkaCard(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KartkaEyebrow("Przerwa między seriami")
        if (rest == null) {
            Text("Zawibruje, gdy minie – także przy zablokowanym ekranie", style = KartkaType.Caption, color = c.ink2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REST_PRESETS.forEach { seconds -> KartkaOutlineButton(formatSeconds(seconds), { onStart(seconds) }) }
            }
        } else {
            val haptics = LocalHapticFeedback.current
            var now by remember(rest.endsAt) { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(rest.endsAt) {
                while (now < rest.endsAt) {
                    delay(200)
                    now = System.currentTimeMillis()
                }
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            val ms = (rest.endsAt - now).coerceAtLeast(0L)
            val seconds = ((ms + 999) / 1000).toInt()
            Text(formatSeconds(seconds), style = KartkaType.Hero, color = c.accent)
            Text(if (seconds == 0) "Koniec przerwy!" else "Odpoczywasz…", style = KartkaType.BodySmall, color = c.ink)
            LinearProgressIndicator(
                progress = { if (rest.totalSeconds <= 0) 0f else (ms / (rest.totalSeconds * 1000f)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(), color = c.accent, trackColor = c.line,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KartkaOutlineButton("+30 s", { onExtend(30) })
                KartkaButton("Stop", onStop)
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

@Composable
private fun Progress(done: Int, total: Int) {
    val c = LookTheme.kartka
    LinearProgressIndicator(
        progress = { if (total == 0) 0f else done.toFloat() / total },
        modifier = Modifier.fillMaxWidth(), color = c.accent, trackColor = c.line,
    )
}

@Composable
private fun PlanSection(
    plan: WorkoutPlanUi, onExpand: () -> Unit, onCheck: (WorkoutExercise, Boolean) -> Unit,
    onEdit: (WorkoutExercise) -> Unit, onAdd: () -> Unit, onReset: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
) {
    val c = LookTheme.kartka
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    KartkaOutlineCard(Modifier.padding(horizontal = 20.dp), contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clip(KartkaDefaults.ButtonShape).clickable(onClickLabel = if (plan.expanded) "Zwiń plan" else "Rozwiń plan", onClick = onExpand).heightIn(min = 56.dp).padding(4.dp)) {
                Text(plan.plan.name, style = KartkaType.Title, color = c.ink)
                Text(
                    when {
                        plan.total == 0 -> "Brak ćwiczeń"
                        plan.doneCount == plan.total -> "Wszystko zrobione"
                        else -> "Zrobione ${plan.doneCount} z ${plan.total}"
                    }, style = KartkaType.Caption, color = c.ink2,
                )
            }
            KartkaIconButton(
                if (plan.expanded) KartkaIcons.ChevronDown else KartkaIcons.ChevronRight,
                if (plan.expanded) "Zwiń plan" else "Rozwiń plan", onExpand,
            )
            Box {
                KartkaIconButton(KartkaIcons.More, "Opcje planu ${plan.plan.name}", { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Zmień nazwę") }, onClick = { menu = false; onRename() })
                    if (plan.doneCount > 0) DropdownMenuItem(text = { Text("Wyczyść zaznaczenia") }, onClick = { menu = false; onReset() })
                    DropdownMenuItem(text = { Text("Usuń plan", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        if (plan.expanded) {
            if (plan.total > 0) Progress(plan.doneCount, plan.total)
            plan.exercises.forEach { exercise ->
                key(exercise.id) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .toggleable(value = exercise.done, role = Role.Checkbox, onValueChange = {
                                if (it) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCheck(exercise, it)
                            }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KartkaCheckbox(exercise.done, null)
                        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                            Text(exercise.name, style = KartkaType.Body, color = if (exercise.done) c.ink2 else c.ink, textDecoration = if (exercise.done) TextDecoration.LineThrough else null)
                            if (exercise.details.isNotBlank()) Text(exercise.details, style = KartkaType.Caption, color = c.ink2)
                        }
                        KartkaIconButton(KartkaIcons.Edit, "Edytuj ćwiczenie ${exercise.name}", { onEdit(exercise) })
                    }
                    KartkaDivider()
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KartkaTextButton("Dodaj ćwiczenie", onAdd, icon = KartkaIcons.Plus)
                if (plan.doneCount > 0) KartkaTextButton("Wyczyść", onReset, color = c.ink2)
            }
        }
    }
}

@Composable
private fun FolderDialog(folder: WorkoutFolder?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var color by rememberSaveable(folder?.id) { mutableIntStateOf(folder?.colorArgb ?: FolderColors.palette.first()) }
    val c = LookTheme.kartka
    KartkaDialog(
        onDismissRequest = onDismiss, title = if (folder == null) "Nowy folder" else "Edytuj folder",
        modifier = Modifier.imePadding(), confirmLabel = "Zapisz", confirmEnabled = name.isNotBlank(), onConfirm = { onSave(name, color) },
    ) {
        KartkaTextField(name, { name = it.take(30) }, label = "Nazwa", placeholder = "np. Siłownia, Bieganie, Basen")
        Text("Kolor folderu", style = KartkaType.CaptionStrong, color = c.ink2)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderColors.palette.forEachIndexed { index, option ->
                Box(
                    Modifier.size(48.dp).clip(KartkaDefaults.ButtonShape)
                        .background(if (color == option) c.accentSoft else c.surface)
                        .selectable(selected = color == option, role = Role.RadioButton, onClick = { color = option })
                        .semantics { contentDescription = "Kolor ${index + 1}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(28.dp).background(Color(option), KartkaDefaults.ButtonShape), contentAlignment = Alignment.Center) {
                        if (color == option) Icon(KartkaIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDialog(plan: WorkoutPlan?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable(plan?.id) { mutableStateOf(plan?.name.orEmpty()) }
    KartkaDialog(
        onDismissRequest = onDismiss, title = if (plan == null) "Nowy plan" else "Zmień nazwę planu",
        modifier = Modifier.imePadding(), confirmLabel = "Zapisz", confirmEnabled = name.isNotBlank(), onConfirm = { onSave(name) },
    ) {
        KartkaTextField(name, { name = it.take(40) }, label = "Nazwa planu")
        if (plan == null) {
            KartkaEyebrow("Podpowiedzi")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Push", "Pull", "Legs", "Upper", "Lower", "Full body", "Cardio", "Interwały").forEach { suggestion ->
                    KartkaFilterChip(suggestion, name == suggestion, { name = suggestion })
                }
            }
        }
    }
}

@Composable
private fun ExerciseDialog(exercise: WorkoutExercise?, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit, onDelete: (() -> Unit)?) {
    var name by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.name.orEmpty()) }
    var details by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.details.orEmpty()) }
    KartkaDialog(
        onDismissRequest = onDismiss, title = if (exercise == null) "Nowe ćwiczenie" else "Edytuj ćwiczenie",
        modifier = Modifier.imePadding(), confirmLabel = if (exercise == null) "Dodaj" else "Zapisz",
        dismissLabel = if (exercise == null) "Gotowe" else "Anuluj", confirmEnabled = name.isNotBlank(),
        onConfirm = { onSave(name, details, false) },
    ) {
        KartkaTextField(
            name, { name = it.take(60) }, label = "Ćwiczenie", placeholder = "np. Wyciskanie na ławce",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        KartkaTextField(
            details, { details = it.take(80) }, label = "Serie i powtórzenia", placeholder = "np. 4 × 8, 60 kg",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        if (exercise == null) KartkaOutlineButton("Dodaj kolejne", {
            onSave(name, details, true)
            name = ""
            details = ""
        }, enabled = name.isNotBlank())
        if (onDelete != null) KartkaTextButton("Usuń ćwiczenie", onDelete, color = MaterialTheme.colorScheme.error, icon = KartkaIcons.Trash)
    }
}
