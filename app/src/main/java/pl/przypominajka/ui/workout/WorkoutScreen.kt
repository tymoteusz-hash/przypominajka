package pl.przypominajka.ui.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pl.przypominajka.alarm.RestState
import pl.przypominajka.data.FolderColors
import pl.przypominajka.data.WorkoutExercise
import pl.przypominajka.data.WorkoutFolder
import pl.przypominajka.data.WorkoutPlan
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.AppFab
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.Confetti
import pl.przypominajka.ui.components.CountUpText
import pl.przypominajka.ui.components.HeroCard
import pl.przypominajka.ui.components.LocalBottomBarPadding
import pl.przypominajka.ui.components.PastelBadge
import pl.przypominajka.ui.components.PrimaryButton
import pl.przypominajka.ui.components.ProgressBar
import pl.przypominajka.ui.components.ProgressRing
import pl.przypominajka.ui.components.RoundIconButton
import pl.przypominajka.ui.components.ScreenHeader
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.floating
import pl.przypominajka.ui.components.pressScale
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.Pastel
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.pluralPl
import java.time.DayOfWeek
import java.time.LocalDate

private data class FolderDialogRequest(val editing: WorkoutFolder?)
private data class PlanDialogRequest(val editing: WorkoutPlan?)
private data class ExerciseDialogRequest(val planId: Long, val editing: WorkoutExercise?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val training by viewModel.training.collectAsStateWithLifecycle()
    val bottomBar = LocalBottomBarPadding.current
    val folder = state.selectedFolder
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex < 2 } }

    var folderDialog by remember { mutableStateOf<FolderDialogRequest?>(null) }
    var planDialog by remember { mutableStateOf<PlanDialogRequest?>(null) }
    var exerciseDialog by remember { mutableStateOf<ExerciseDialogRequest?>(null) }
    var deleteFolder by remember { mutableStateOf(false) }
    var deletePlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var folderMenu by remember { mutableStateOf(false) }
    // Licznik wybuchów konfetti – rośnie, gdy któryś plan zostanie w całości odhaczony.
    var confetti by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppSurfaces.page,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                BellTopBar(title = "Trening", eyebrow = "Plan treningowy", onBack = onBack) {
                    if (folder != null) {
                        Box {
                            RoundIconButton(
                                icon = Icons.Rounded.MoreVert,
                                contentDescription = "Opcje folderu",
                                onClick = { folderMenu = true },
                            )
                            DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Zmień nazwę i kolor") },
                                    onClick = {
                                        folderMenu = false
                                        folderDialog = FolderDialogRequest(folder)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Usuń folder", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        folderMenu = false
                                        deleteFolder = true
                                    },
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (folder != null) {
                    AppFab(
                        text = "Nowy plan",
                        icon = Icons.Rounded.Add,
                        expanded = fabExpanded,
                        onClick = { planDialog = PlanDialogRequest(null) },
                        modifier = Modifier.padding(bottom = bottomBar),
                    )
                }
            },
        ) { innerPadding ->
            if (state.isLoading) {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(top = 4.dp, bottom = bottomBar + 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "folders") {
                    FolderRow(
                        folders = state.folders,
                        selectedId = folder?.id,
                        onSelect = viewModel::selectFolder,
                        onAdd = { folderDialog = FolderDialogRequest(null) },
                        modifier = Modifier.appear(1),
                    )
                }

                item(key = "training") {
                    TrainingReminderCard(
                        training = training,
                        onEnabledChange = viewModel::setTrainingEnabled,
                        onToggleDay = viewModel::toggleTrainingDay,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .appear(2),
                    )
                }

                if (folder == null) {
                    item(key = "noFolder") {
                        EmptyState(
                            title = "Zacznij od folderu",
                            text = "Folder to np. Siłownia albo Bieganie. W środku zrobisz plany i listy ćwiczeń.",
                            actionText = "Nowy folder",
                            onAction = { folderDialog = FolderDialogRequest(null) },
                        )
                    }
                } else {
                    if (state.total > 0) {
                        item(key = "summary") {
                            Summary(
                                folderName = folder.name,
                                done = state.doneCount,
                                total = state.total,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .appear(2),
                            )
                        }
                    }
                    item(key = "rest") {
                        RestTimerCard(
                            rest = rest,
                            onStart = viewModel::startRest,
                            onExtend = viewModel::extendRest,
                            onStop = viewModel::stopRest,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .appear(3),
                        )
                    }
                    if (state.plans.isEmpty()) {
                        item(key = "noPlans") {
                            NoPlans(
                                onPreset = { names -> viewModel.addPlans(names) },
                                onCustom = { planDialog = PlanDialogRequest(null) },
                                modifier = Modifier.appear(4),
                            )
                        }
                    } else {
                        itemsIndexed(items = state.plans, key = { _, it -> it.plan.id }) { index, planUi ->
                            PlanCard(
                                planUi = planUi,
                                color = Color(folder.colorArgb),
                                onToggleExpanded = { viewModel.toggleExpanded(planUi.plan.id) },
                                onCheck = { exercise, done ->
                                    if (done && !exercise.done && planUi.doneCount == planUi.total - 1) confetti++
                                    viewModel.setDone(exercise, done)
                                },
                                onEditExercise = { exerciseDialog = ExerciseDialogRequest(planUi.plan.id, it) },
                                onAddExercise = { exerciseDialog = ExerciseDialogRequest(planUi.plan.id, null) },
                                onReset = { viewModel.resetPlan(planUi.plan.id) },
                                onRename = { planDialog = PlanDialogRequest(planUi.plan) },
                                onDelete = { deletePlan = planUi.plan },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .animateItem()
                                    .appear(4 + index),
                            )
                        }
                    }
                }
            }
        }
        Confetti(trigger = confetti, modifier = Modifier.fillMaxSize())
    }

    // ---------------------------------------------------------------- okna dialogowe

    folderDialog?.let { request ->
        FolderDialog(
            folder = request.editing,
            onDismiss = { folderDialog = null },
            onSave = { name, color ->
                val editing = request.editing
                if (editing == null) viewModel.addFolder(name, color) else viewModel.updateFolder(editing, name, color)
                folderDialog = null
            },
        )
    }

    planDialog?.let { request ->
        PlanDialog(
            plan = request.editing,
            onDismiss = { planDialog = null },
            onSave = { name ->
                val editing = request.editing
                if (editing == null) viewModel.addPlans(listOf(name)) else viewModel.renamePlan(editing, name)
                planDialog = null
            },
        )
    }

    exerciseDialog?.let { request ->
        ExerciseDialog(
            exercise = request.editing,
            onDismiss = { exerciseDialog = null },
            onSave = { name, details, keepOpen ->
                val editing = request.editing
                if (editing == null) {
                    viewModel.addExercise(request.planId, name, details)
                } else {
                    viewModel.updateExercise(editing, name, details)
                }
                if (!keepOpen || editing != null) exerciseDialog = null
            },
            onDelete = request.editing?.let { exercise ->
                {
                    viewModel.deleteExercise(exercise)
                    exerciseDialog = null
                }
            },
        )
    }

    if (deleteFolder && folder != null) {
        AlertDialog(
            onDismissRequest = { deleteFolder = false },
            title = { Text("Usunąć folder „${folder.name}”?") },
            text = { Text("Razem z nim znikną wszystkie plany i ćwiczenia w tym folderze.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder)
                    deleteFolder = false
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteFolder = false }) { Text("Anuluj") } },
        )
    }

    deletePlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { deletePlan = null },
            title = { Text("Usunąć plan „${plan.name}”?") },
            text = { Text("Lista ćwiczeń w tym planie zostanie usunięta.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(plan)
                    deletePlan = null
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletePlan = null }) { Text("Anuluj") } },
        )
    }
}

// ---------------------------------------------------------------- foldery

@Composable
private fun FolderRow(
    folders: List<WorkoutFolder>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = folders, key = { it.id }) { folder ->
            val selected = folder.id == selectedId
            val container by animateColorAsState(if (selected) colors.onSurface else AppSurfaces.card, label = "wFolder")
            val content by animateColorAsState(if (selected) colors.surface else colors.onSurface, label = "wFolderText")
            val interaction = remember { MutableInteractionSource() }
            Surface(
                onClick = { onSelect(folder.id) },
                shape = CircleShape,
                color = container,
                contentColor = content,
                border = if (selected) null else BorderStroke(1.dp, AppSurfaces.cardBorder),
                interactionSource = interaction,
                modifier = Modifier.pressScale(interaction, pressedScale = 0.92f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(folder.colorArgb)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(folder.name, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item(key = "add") {
            val interaction = remember { MutableInteractionSource() }
            Surface(
                onClick = onAdd,
                shape = CircleShape,
                color = Color.Transparent,
                contentColor = colors.onSurface,
                border = BorderStroke(1.5.dp, colors.outline),
                interactionSource = interaction,
                modifier = Modifier.pressScale(interaction, pressedScale = 0.92f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Folder", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Ciemna karta postępu folderu: wielki procent "nabijany" od zera i pierścień. */
@Composable
private fun Summary(folderName: String, done: Int, total: Int, modifier: Modifier = Modifier) {
    val percent = done * 100 / total
    HeroCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
                CountUpText(
                    target = percent,
                    suffix = "%",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    text = if (done == total) "Trening zrobiony – brawo!" else "Zrobione $done z $total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(78.dp)) {
                ProgressRing(
                    progress = done.toFloat() / total,
                    color = Color(0xFFA8CFA3),
                    trackColor = Color.White.copy(alpha = 0.14f),
                    strokeWidth = 9.dp,
                    modifier = Modifier.fillMaxSize(),
                )
                Icon(
                    imageVector = if (done == total) Icons.Rounded.Check else Icons.Rounded.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- przypomnienie o treningu

/** Przełącznik i dni tygodnia; przypomnienie przychodzi o godzinie końca lekcji. */
@Composable
private fun TrainingReminderCard(
    training: TrainingUi,
    onEnabledChange: (Boolean) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val settings = training.settings
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PastelBadge(icon = Icons.Rounded.NotificationsActive, pastel = Pastel.SAGE, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Przypomnienie o treningu", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Gdy kończą się lekcje",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = settings.enabled, onCheckedChange = onEnabledChange)
            }
            AnimatedVisibility(
                visible = settings.enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            val selected = day in settings.days
                            val container by animateColorAsState(
                                if (selected) colors.primary else colors.surfaceVariant,
                                label = "trainingDay",
                            )
                            val content by animateColorAsState(
                                if (selected) colors.onPrimary else colors.onSurface,
                                label = "trainingDayText",
                            )
                            val interaction = remember { MutableInteractionSource() }
                            Surface(
                                selected = selected,
                                onClick = { onToggleDay(day) },
                                shape = CircleShape,
                                color = container,
                                contentColor = content,
                                interactionSource = interaction,
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(interaction, pressedScale = 0.9f),
                            ) {
                                Text(
                                    text = DAY_LETTERS[day.value - 1],
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = trainingNextText(training),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settings.days.isEmpty()) colors.error else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val DAY_LETTERS = listOf("PN", "WT", "ŚR", "CZ", "PT", "SB", "ND")

/** "Najbliższe: dziś o 14:35 – koniec lekcji". */
private fun trainingNextText(training: TrainingUi): String {
    if (training.settings.days.isEmpty()) return "Wybierz przynajmniej jeden dzień"
    val next = training.next ?: return "Brak terminu w najbliższym tygodniu"
    val date = next.at.toLocalDate()
    val today = LocalDate.now()
    val day = when (date) {
        today -> "dziś"
        today.plusDays(1) -> "jutro"
        else -> "w ${dayAccusative(date)}"
    }
    val reason = if (next.noSchool) "dzień bez lekcji, godzina domyślna" else "koniec lekcji"
    return "Najbliższe: $day o ${next.at.toLocalTime().format(TimeFormatter)} – $reason"
}

/** "w poniedziałek", "w środę", "w sobotę"… */
private fun dayAccusative(date: LocalDate): String =
    listOf("poniedziałek", "wtorek", "środę", "czwartek", "piątek", "sobotę", "niedzielę")[date.dayOfWeek.value - 1]

// ---------------------------------------------------------------- przerwa

/**
 * Przerwa między seriami: bez przerwy – przyciski 1:00 / 1:30 / 2:00 / 3:00;
 * w trakcie – odliczanie w pierścieniu, "+30 s" i "Stop". Koniec sygnalizuje powiadomienie z wibracją.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimerCard(
    rest: RestState?,
    onStart: (Int) -> Unit,
    onExtend: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = rest != null,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.92f)) },
        modifier = modifier,
        label = "restCard",
    ) { running ->
        if (running && rest != null) {
            RunningRest(rest = rest, onExtend = onExtend, onStop = onStop)
        } else {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PastelBadge(icon = Icons.Rounded.Timer, pastel = Pastel.TERRACOTTA, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Przerwa między seriami", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Zawibruje, gdy minie – także przy zablokowanym ekranie",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        REST_PRESETS.forEach { seconds ->
                            val interaction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = { onStart(seconds) },
                                shape = CircleShape,
                                color = Pastel.TERRACOTTA.container,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                interactionSource = interaction,
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(interaction, pressedScale = 0.9f),
                            ) {
                                Text(
                                    text = formatSeconds(seconds),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 11.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningRest(rest: RestState, onExtend: (Int) -> Unit, onStop: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
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
    val progress = if (rest.totalSeconds == 0) 0f else remainingMs / (rest.totalSeconds * 1000f)

    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
                ProgressRing(
                    progress = progress,
                    color = Color(0xFFF2C14E),
                    trackColor = Color.White.copy(alpha = 0.14f),
                    strokeWidth = 9.dp,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = formatSeconds(remaining),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (remaining == 0) "Koniec przerwy!" else "Odpoczywasz…",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(
                        text = "+30 s",
                        onClick = { onExtend(30) },
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White,
                    )
                    PrimaryButton(
                        text = "Stop",
                        onClick = onStop,
                        containerColor = Color(0xFFF2C14E),
                        contentColor = Color(0xFF2B1F05),
                    )
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

// ---------------------------------------------------------------- plan i ćwiczenia

@Composable
private fun PlanCard(
    planUi: WorkoutPlanUi,
    color: Color,
    onToggleExpanded: () -> Unit,
    onCheck: (WorkoutExercise, Boolean) -> Unit,
    onEditExercise: (WorkoutExercise) -> Unit,
    onAddExercise: () -> Unit,
    onReset: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val allDone = planUi.total > 0 && planUi.doneCount == planUi.total
    val chevron by animateFloatAsState(
        targetValue = if (planUi.expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "chevron",
    )
    val borderColor by animateColorAsState(if (allDone) color else AppSurfaces.cardBorder, label = "planBorder")

    AppCard(
        border = BorderStroke(if (allDone) 2.dp else 1.dp, borderColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(46.dp)) {
                    ProgressRing(
                        progress = if (planUi.total == 0) 0f else planUi.doneCount.toFloat() / planUi.total,
                        color = color,
                        trackColor = color.copy(alpha = 0.15f),
                        strokeWidth = 5.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Icon(
                        imageVector = if (allDone) Icons.Rounded.Check else Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = planUi.plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            planUi.total == 0 -> "Brak ćwiczeń"
                            allDone -> "Wszystko zrobione"
                            else -> "${planUi.doneCount} z ${planUi.total} " +
                                pluralPl(planUi.total, "ćwiczenia", "ćwiczeń", "ćwiczeń")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (planUi.expanded) "Zwiń" else "Rozwiń",
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer { rotationZ = chevron },
                )
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Opcje planu")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Zmień nazwę") },
                            onClick = {
                                menu = false
                                onRename()
                            },
                        )
                        if (planUi.doneCount > 0) {
                            DropdownMenuItem(
                                text = { Text("Wyczyść zaznaczenia") },
                                onClick = {
                                    menu = false
                                    onReset()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Usuń plan", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menu = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = planUi.expanded,
                enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) + fadeIn(),
                exit = shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) + fadeOut(),
            ) {
                Column {
                    if (planUi.total > 0) {
                        ProgressBar(
                            progress = planUi.doneCount.toFloat() / planUi.total,
                            color = color,
                            trackColor = color.copy(alpha = 0.12f),
                            height = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    planUi.exercises.forEach { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            color = color,
                            onCheck = { onCheck(exercise, it) },
                            onEdit = { onEditExercise(exercise) },
                        )
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = onAddExercise) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dodaj ćwiczenie")
                        }
                        if (planUi.doneCount > 0) {
                            TextButton(onClick = onReset) { Text("Wyczyść") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: WorkoutExercise,
    color: Color,
    onCheck: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val textAlpha by animateFloatAsState(if (exercise.done) 0.55f else 1f, label = "exerciseAlpha")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!exercise.done) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheck(!exercise.done)
            }
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(checked = exercise.done, color = color)
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .graphicsLayer { alpha = textAlpha },
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (exercise.done) TextDecoration.LineThrough else null,
            )
            if (exercise.details.isNotBlank()) {
                Text(
                    text = exercise.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onEdit) { Text("Edytuj", style = MaterialTheme.typography.labelMedium) }
    }
}

/** Okrągły "checkbox": po zaznaczeniu wypełnia się kolorem, a ptaszek wskakuje sprężyście. */
@Composable
private fun CheckCircle(checked: Boolean, color: Color) {
    val fill by animateColorAsState(if (checked) color else Color.Transparent, label = "checkFill")
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "checkScale",
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, if (checked) color else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = if (checked) "Zrobione" else "Do zrobienia",
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                },
        )
    }
}

// ---------------------------------------------------------------- puste stany

@Composable
private fun EmptyState(title: String, text: String, actionText: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp)
            .appear(2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PastelBadge(icon = Icons.Rounded.FitnessCenter, pastel = Pastel.SAGE, size = 84.dp, modifier = Modifier.floating())
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = actionText, icon = Icons.Rounded.Add, onClick = onAction)
    }
}

@Composable
private fun NoPlans(onPreset: (List<String>) -> Unit, onCustom: () -> Unit, modifier: Modifier = Modifier) {
    val pastels = listOf(Pastel.GOLD, Pastel.SAGE, Pastel.DUSK)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Wybierz gotowy podział", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Dodamy puste plany, a ćwiczenia wpiszesz sam. Możesz też zrobić własny podział.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WORKOUT_PRESETS.forEachIndexed { index, (label, names) ->
            val pastel = pastels[index % pastels.size]
            AppCard(
                onClick = { onPreset(names) },
                color = pastel.container,
                border = null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(pastel.content),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = names.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = pastel.content)
                }
            }
        }
        OutlinedButton(onClick = onCustom, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Własny plan") }
    }
}

// ---------------------------------------------------------------- dialogi

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderDialog(
    folder: WorkoutFolder?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorArgb: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(folder?.name.orEmpty()) }
    var color by rememberSaveable { mutableIntStateOf(folder?.colorArgb ?: FolderColors.palette.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "Nowy folder" else "Edytuj folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Nazwa") },
                    placeholder = { Text("np. Siłownia, Bieganie, Basen") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FolderColors.palette.forEach { option ->
                        val selected = option == color
                        val scale by animateFloatAsState(if (selected) 1.15f else 1f, spring(0.4f, 500f), label = "swatch")
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(CircleShape)
                                .background(Color(option))
                                .clickable { color = option },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, contentDescription = "Wybrany kolor", tint = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, color) }, enabled = name.isNotBlank()) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanDialog(plan: WorkoutPlan?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(plan?.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (plan == null) "Nowy plan" else "Zmień nazwę planu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nazwa planu") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (plan == null) {
                    Text("Podpowiedzi", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Push", "Pull", "Legs", "Upper", "Lower", "Full body", "Cardio", "Interwały").forEach { suggestion ->
                            Surface(
                                onClick = { name = suggestion },
                                shape = CircleShape,
                                color = if (name == suggestion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (name == suggestion) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

/** Dodawanie (z opcją „Dodaj kolejne”) albo edycja ćwiczenia. */
@Composable
private fun ExerciseDialog(
    exercise: WorkoutExercise?,
    onDismiss: () -> Unit,
    onSave: (name: String, details: String, keepOpen: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(exercise?.name.orEmpty()) }
    var details by rememberSaveable { mutableStateOf(exercise?.details.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exercise == null) "Nowe ćwiczenie" else "Edytuj ćwiczenie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Ćwiczenie") },
                    placeholder = { Text("np. Wyciskanie na ławce") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(80) },
                    label = { Text("Serie i powtórzenia") },
                    placeholder = { Text("np. 4 × 8, 60 kg") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Usuń ćwiczenie", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            Row {
                if (exercise == null) {
                    TextButton(
                        onClick = {
                            onSave(name, details, true)
                            name = ""
                            details = ""
                        },
                        enabled = name.isNotBlank(),
                    ) { Text("Dodaj kolejne") }
                }
                TextButton(onClick = { onSave(name, details, false) }, enabled = name.isNotBlank()) {
                    Text(if (exercise == null) "Dodaj" else "Zapisz")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (exercise == null) "Gotowe" else "Anuluj") } },
    )
}
