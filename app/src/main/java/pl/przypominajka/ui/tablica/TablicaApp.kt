package pl.przypominajka.ui.tablica

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.checklist.ChecklistViewModel
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.look.TablicaColors
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.ui.navigation.StartRequest
import pl.przypominajka.ui.tablica.checklist.NewNoteDialog
import pl.przypominajka.ui.tablica.checklist.TablicaChecklistScreen
import pl.przypominajka.ui.tablica.components.DockBottomMargin
import pl.przypominajka.ui.tablica.components.DockHeight
import pl.przypominajka.ui.tablica.components.DockTab
import pl.przypominajka.ui.tablica.components.LocalDockPadding
import pl.przypominajka.ui.tablica.components.SheetNoteRow
import pl.przypominajka.ui.tablica.components.TablicaDock
import pl.przypominajka.ui.tablica.components.TablicaIcons
import pl.przypominajka.ui.tablica.components.TablicaSheet
import pl.przypominajka.ui.tablica.plan.TablicaPlanScreen
import pl.przypominajka.ui.tablica.workout.TablicaWorkoutScreen
import pl.przypominajka.ui.tablica.home.TablicaHubScreen
import pl.przypominajka.ui.tablica.quiz.TablicaQuizListScreen
import pl.przypominajka.ui.tablica.quiz.TablicaQuizEditScreen
import pl.przypominajka.ui.tablica.quiz.TablicaQuizPlayScreen
import pl.przypominajka.ui.tablica.reminders.TablicaRemindersScreen
import pl.przypominajka.ui.tablica.edit.TablicaEditReminderScreen
import pl.przypominajka.ui.tablica.folders.TablicaFoldersScreen
import pl.przypominajka.ui.tablica.account.TablicaAccountScreen
import pl.przypominajka.ui.tablica.settings.TablicaSettingsScreen

/** Trasy szczegółów – bez doku, wjeżdżają od dołu. Reszta tras to działy z dokiem. */
private val DetailRoutes = setOf(Routes.EDIT, Routes.FOLDERS, Routes.QUIZ_EDIT, Routes.QUIZ_PLAY)

private fun NavBackStackEntry.isDetail(): Boolean = destination.route in DetailRoutes

/** Która zakładka doku świeci się na danej trasie (null = ekran szczegółów, dok schowany). */
private fun dockTabFor(route: String): DockTab? = when (route) {
    Routes.HUB -> DockTab.HUB
    Routes.CHECKLIST -> DockTab.CHECKLIST
    Routes.PLAN -> DockTab.PLAN
    Routes.REMINDERS, Routes.WORKOUT, Routes.QUIZZES, Routes.ACCOUNT, Routes.SETTINGS -> DockTab.MORE
    else -> null
}

/** Arkusz otwarty z doku. */
private enum class ShellSheet { ADD, MORE }

/** Pozycja arkusza „Więcej”. */
private data class MoreEntry(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val color: (TablicaColors) -> NoteColor,
)

private val MoreEntries = listOf(
    MoreEntry("Przypomnienia", "Wszystkie i foldery", Icons.Rounded.Notifications, Routes.REMINDERS) { it.yellow },
    MoreEntry("Trening", "Plany ćwiczeń i przerwy", Icons.Rounded.FitnessCenter, Routes.WORKOUT) { it.lilac },
    MoreEntry("Quizy", "Powtórki przed sprawdzianem", Icons.Rounded.Quiz, Routes.QUIZZES) { it.pink },
    MoreEntry("Konto", "Librus i klucz AI", Icons.Rounded.Person, Routes.ACCOUNT) { it.mint },
    MoreEntry("Ustawienia", "Wygląd, dźwięki, szkoła", Icons.Rounded.Settings, Routes.SETTINGS) { it.blue },
)

/** Zakładka ostatnio pokazana w doku – zostaje na czas chowania doku (zwykły schowek, nie stan). */
private class TabHolder(var tab: DockTab)

// Przejścia: między działami – przenikanie z lekkim „opadnięciem” karteczki (1.03 → 1),
// szczegóły – przenikanie i wjazd 24 dp od dołu. Wszystko ≤ 250 ms.
private const val TRANSITION_MS = 250
private const val EXIT_MS = 200

private fun tabEnter(): EnterTransition =
    fadeIn(tween(TRANSITION_MS)) + scaleIn(tween(TRANSITION_MS), initialScale = 1.03f)

private fun detailEnter(offsetPx: Int): EnterTransition =
    fadeIn(tween(TRANSITION_MS)) + slideInVertically(tween(TRANSITION_MS)) { offsetPx }

private fun detailExit(offsetPx: Int): ExitTransition =
    fadeOut(tween(EXIT_MS)) + slideOutVertically(tween(EXIT_MS)) { offsetPx }

/**
 * Powłoka wyglądu Tablica: własny NavHost ze wszystkimi trasami, pływający dok na dole (na ekranach
 * działów), arkusze „Co dodać?” i „Więcej” oraz okno szybkiego dopisania karteczki.
 * Obsługuje [startRequest] (skróty z ikony, widżety, powiadomienia).
 */
@Composable
fun TablicaApp(startRequest: StartRequest?, onStartRequestHandled: () -> Unit) {
    val colors = LookTheme.tablica
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val activeTab = dockTabFor(entry?.destination?.route ?: Routes.HUB)
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Miejsce zajęte przez dok: pasek systemowy + 20 dp + 72 dp doku + 12 dp oddechu (tu stoi pasek „Cofnij”).
    val dockSpace: Dp = navBarBottom + DockBottomMargin + DockHeight + 12.dp
    val slidePx = with(density) { 24.dp.roundToPx() }

    // Szybkie dopisanie karteczki działa z każdego działu – ViewModel na poziomie aktywności.
    val checklistViewModel: ChecklistViewModel = viewModel(factory = AppViewModelProvider.Factory)
    var sheet by rememberSaveable { mutableStateOf<ShellSheet?>(null) }
    var showNewNote by rememberSaveable { mutableStateOf(false) }

    /** Przejście do działu jak zakładką: jeden ekran działu na stosie nad stroną główną, stan zapamiętany. */
    val openTab: (String) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo(Routes.HUB) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    /** Przejście do ekranu szczegółów (nad bieżącym). */
    val go: (String) -> Unit = { target ->
        navController.navigate(target) { launchSingleTop = true }
    }
    val back: () -> Unit = {
        if (!navController.navigateUp()) openTab(Routes.HUB)
    }

    LaunchedEffect(startRequest) {
        val request = startRequest ?: return@LaunchedEffect
        sheet = null
        showNewNote = false
        val target = when (request) {
            StartRequest.NEW_REMINDER -> Routes.edit()
            StartRequest.PLAN -> Routes.PLAN
            StartRequest.WORKOUT -> Routes.WORKOUT
            StartRequest.QUIZZES -> Routes.QUIZZES
            StartRequest.CHECKLIST -> Routes.CHECKLIST
        }
        navController.navigate(target) {
            popUpTo(Routes.HUB) { saveState = false }
            launchSingleTop = true
            restoreState = false
        }
        onStartRequestHandled()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.board),
    ) {
        CompositionLocalProvider(LocalDockPadding provides dockSpace) {
            NavHost(
                navController = navController,
                startDestination = Routes.HUB,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { if (targetState.isDetail()) detailEnter(slidePx) else tabEnter() },
                exitTransition = { fadeOut(tween(EXIT_MS)) },
                popEnterTransition = { if (initialState.isDetail()) fadeIn(tween(EXIT_MS)) else tabEnter() },
                popExitTransition = { if (initialState.isDetail()) detailExit(slidePx) else fadeOut(tween(EXIT_MS)) },
            ) {
                tablicaGraph(
                    openTab = openTab,
                    go = go,
                    back = back,
                    navBarBottom = navBarBottom,
                    onNewNote = { showNewNote = true },
                )
            }
        }

        val lastTab = remember { TabHolder(DockTab.HUB) }
        if (activeTab != null) lastTab.tab = activeTab
        AnimatedVisibility(
            visible = activeTab != null && !imeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(200)) + slideInVertically(tween(TRANSITION_MS)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(EXIT_MS)) { it / 2 },
        ) {
            TablicaDock(
                active = lastTab.tab,
                onTabClick = { tab ->
                    when (tab) {
                        DockTab.HUB -> openTab(Routes.HUB)
                        DockTab.CHECKLIST -> openTab(Routes.CHECKLIST)
                        DockTab.PLAN -> openTab(Routes.PLAN)
                        DockTab.MORE -> sheet = ShellSheet.MORE
                    }
                },
                onAddClick = { sheet = ShellSheet.ADD },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = DockBottomMargin),
            )
        }
    }

    when (sheet) {
        ShellSheet.ADD -> TablicaSheet(onDismissRequest = { sheet = null }, title = "Co dodać?") { close ->
            SheetNoteRow(
                text = "Nowa karteczka",
                subtitle = "Na listę rzeczy do zrobienia",
                icon = TablicaIcons.Checklist,
                color = colors.yellow,
                tilt = -0.8f,
                onClick = { close { showNewNote = true } },
            )
            SheetNoteRow(
                text = "Nowe przypomnienie",
                subtitle = "O stałych porach, z powiadomieniem",
                icon = Icons.Rounded.Alarm,
                color = colors.blue,
                tilt = 0.6f,
                onClick = { close { go(Routes.edit()) } },
            )
        }
        ShellSheet.MORE -> TablicaSheet(onDismissRequest = { sheet = null }, title = "Więcej") { close ->
            MoreEntries.forEachIndexed { index, item ->
                SheetNoteRow(
                    text = item.label,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    color = item.color(colors),
                    tilt = if (index % 2 == 0) -0.6f else 0.6f,
                    onClick = { close { openTab(item.route) } },
                )
            }
        }
        null -> Unit
    }

    if (showNewNote) {
        NewNoteDialog(
            onDismiss = { showNewNote = false },
            onAdd = { text, remindAt -> checklistViewModel.add(text, remindAt) },
        )
    }
}

/** Wszystkie trasy aplikacji. Działy bez nowych ekranów wołają tymczasowo stare (TablicaLegacy.kt). */
private fun NavGraphBuilder.tablicaGraph(
    openTab: (String) -> Unit,
    go: (String) -> Unit,
    back: () -> Unit,
    navBarBottom: Dp,
    onNewNote: () -> Unit,
) {
    composable(Routes.HUB) {
        TablicaHubScreen(onOpenTab = openTab, onNewNote = onNewNote)
    }

    // ------------------------------------------------ działy (z dokiem)
    composable(Routes.CHECKLIST) {
        TablicaChecklistScreen()
    }
    composable(Routes.REMINDERS) {
        TablicaRemindersScreen(
            onBack = back,
            onAddReminder = { folderId -> go(Routes.edit(folderId = folderId)) },
            onEditReminder = { id -> go(Routes.edit(reminderId = id)) },
        )
    }
    composable(Routes.PLAN) {
        TablicaPlanScreen(
            onBack = back,
            onOpenAccount = { openTab(Routes.ACCOUNT) },
            onOpenReminder = { id -> go(Routes.edit(reminderId = id)) },
        )
    }
    composable(Routes.WORKOUT) {
        TablicaWorkoutScreen(onBack = back)
    }
    composable(Routes.QUIZZES) {
        TablicaQuizListScreen(
            onBack = back,
            onNewQuiz = { go(Routes.quizEdit()) },
            onQuizForReminder = { reminderId -> go(Routes.quizEdit(reminderId = reminderId)) },
            onOpenQuiz = { quizId -> go(Routes.quizEdit(quizId = quizId)) },
            onPlay = { quizId, mode -> go(Routes.quizPlay(quizId, mode)) },
        )
    }
    composable(Routes.ACCOUNT) {
        TablicaAccountScreen(onBack = back)
    }
    composable(Routes.SETTINGS) {
        TablicaSettingsScreen(onBack = back, onOpenFolders = { go(Routes.FOLDERS) })
    }

    // ------------------------------------------------ szczegóły (bez doku)
    composable(
        route = Routes.EDIT,
        arguments = listOf(
            navArgument(Routes.ARG_REMINDER_ID) {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument(Routes.ARG_FOLDER_ID) {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
    ) {
        CompositionLocalProvider(LocalDockPadding provides navBarBottom) {
            TablicaEditReminderScreen(
                onBack = back,
                onOpenQuiz = { reminderId -> go(Routes.quizEdit(reminderId = reminderId)) },
            )
        }
    }
    composable(Routes.FOLDERS) {
        CompositionLocalProvider(LocalDockPadding provides navBarBottom) {
            TablicaFoldersScreen(onBack = back)
        }
    }
    composable(
        route = Routes.QUIZ_EDIT,
        arguments = listOf(
            navArgument(Routes.ARG_QUIZ_ID) {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument(Routes.ARG_REMINDER_ID) {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
    ) {
        CompositionLocalProvider(LocalDockPadding provides navBarBottom) {
            TablicaQuizEditScreen(
                onBack = back,
                onPlay = { quizId, mode -> go(Routes.quizPlay(quizId, mode)) },
            )
        }
    }
    composable(
        route = Routes.QUIZ_PLAY,
        arguments = listOf(
            navArgument(Routes.ARG_QUIZ_ID) { type = NavType.LongType },
            navArgument(Routes.ARG_MODE) { type = NavType.StringType },
        ),
    ) {
        CompositionLocalProvider(LocalDockPadding provides navBarBottom) {
            TablicaQuizPlayScreen(onClose = back)
        }
    }
}
