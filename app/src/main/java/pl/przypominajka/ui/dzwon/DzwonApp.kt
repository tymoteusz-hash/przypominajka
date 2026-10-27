package pl.przypominajka.ui.dzwon

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.dzwon.account.DzwonAccountScreen
import pl.przypominajka.ui.dzwon.settings.DzwonSettingsScreen
import pl.przypominajka.ui.dzwon.quiz.DzwonQuizListScreen
import pl.przypominajka.ui.dzwon.quiz.DzwonQuizEditScreen
import pl.przypominajka.ui.dzwon.quiz.DzwonQuizPlayScreen
import pl.przypominajka.ui.dzwon.home.DzwonRemindersScreen
import pl.przypominajka.ui.dzwon.edit.DzwonEditReminderScreen
import pl.przypominajka.ui.dzwon.folders.DzwonFoldersScreen
import pl.przypominajka.ui.dzwon.plan.DzwonPlanScreen
import pl.przypominajka.ui.dzwon.workout.DzwonWorkoutScreen
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.ui.navigation.StartRequest

/** Czas wysuwania arkusza spod dzwonu (ms). */
private const val SHEET_MS = 300

/**
 * Powłoka wyglądu Dzwon: strona główna z dzwonem, a każdy inny ekran wysuwa się od dołu
 * jak arkusz „spod dzwonu” (przy powrocie zjeżdża w dół). Gest wstecz = zwinięcie arkusza.
 * [startRequest] (skrót z ikony, widżet, powiadomienie) otwiera od razu właściwy ekran.
 */
@Composable
fun DzwonApp(startRequest: StartRequest?, onStartRequestHandled: () -> Unit) {
    val navController = rememberNavController()
    val nav = remember(navController) { DzwonNavigator(navController) }

    LaunchedEffect(startRequest) {
        val request = startRequest ?: return@LaunchedEffect
        nav.navigate(
            when (request) {
                StartRequest.NEW_REMINDER -> Routes.edit()
                StartRequest.PLAN -> Routes.PLAN
                StartRequest.WORKOUT -> Routes.WORKOUT
                StartRequest.QUIZZES -> Routes.QUIZZES
                StartRequest.CHECKLIST -> Routes.CHECKLIST
            },
        )
        onStartRequestHandled()
    }

    val c = LookTheme.dzwon
    CompositionLocalProvider(LocalContentColor provides c.ink) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg),
        ) {
            DzwonNavGraph(navController = navController, nav = nav)
        }
    }
}

/**
 * Nawigacja z ochroną przed podwójnym dotknięciem: przejść dalej i wrócić może tylko ekran,
 * który jest na wierzchu i skończył już swoją animację (stan RESUMED).
 */
private class DzwonNavigator(private val navController: NavHostController) {

    /** Bez sprawdzania (np. prośba ze skrótu na starcie). */
    fun navigate(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    fun open(from: NavBackStackEntry, route: String) {
        if (from.isOnTop()) navigate(route)
    }

    /** Zwinięcie arkusza; gdy nie ma dokąd wrócić – strona główna. */
    fun back(from: NavBackStackEntry) {
        if (!from.isOnTop()) return
        if (!navController.navigateUp()) navigate(Routes.HUB)
    }

    private fun NavBackStackEntry.isOnTop(): Boolean =
        lifecycle.currentState == Lifecycle.State.RESUMED
}

// Arkusz wjeżdża od dołu (pełna wysokość) z lekkim pojawieniem się; ekran pod spodem zostaje,
// tylko delikatnie przygasa. Przy powrocie arkusz zjeżdża w dół.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.sheetEnter(): EnterTransition =
    slideInVertically(tween(SHEET_MS, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight } +
        fadeIn(tween(SHEET_MS), initialAlpha = 0.5f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.underExit(): ExitTransition =
    fadeOut(tween(SHEET_MS), targetAlpha = 0.85f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.underEnter(): EnterTransition =
    fadeIn(tween(SHEET_MS), initialAlpha = 0.85f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sheetExit(): ExitTransition =
    slideOutVertically(tween(SHEET_MS, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight } +
        fadeOut(tween(SHEET_MS), targetAlpha = 0.5f)

@Composable
private fun DzwonNavGraph(navController: NavHostController, nav: DzwonNavigator) {
    NavHost(
        navController = navController,
        startDestination = Routes.HUB,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { sheetEnter() },
        exitTransition = { underExit() },
        popEnterTransition = { underEnter() },
        popExitTransition = { sheetExit() },
    ) {
        composable(Routes.HUB) { entry ->
            DzwonStartScreen(onOpen = { route -> nav.open(entry, route) })
        }

        // ------------------------------------------------ działy
        composable(Routes.CHECKLIST) { entry ->
            DzwonChecklistScreen(onClose = { nav.back(entry) })
        }
        composable(Routes.REMINDERS) { entry ->
            DzwonRemindersScreen(
                onBack = { nav.back(entry) },
                onAddReminder = { folderId -> nav.open(entry, Routes.edit(folderId = folderId)) },
                onEditReminder = { id -> nav.open(entry, Routes.edit(reminderId = id)) },
            )
        }
        composable(Routes.PLAN) { entry ->
            DzwonPlanScreen(
                onBack = { nav.back(entry) },
                onOpenAccount = { nav.open(entry, Routes.ACCOUNT) },
                onOpenReminder = { id -> nav.open(entry, Routes.edit(reminderId = id)) },
            )
        }
        composable(Routes.WORKOUT) { entry ->
            DzwonWorkoutScreen(onBack = { nav.back(entry) })
        }
        composable(Routes.QUIZZES) { entry ->
            DzwonQuizListScreen(
                onBack = { nav.back(entry) },
                onNewQuiz = { nav.open(entry, Routes.quizEdit()) },
                onQuizForReminder = { reminderId -> nav.open(entry, Routes.quizEdit(reminderId = reminderId)) },
                onOpenQuiz = { quizId -> nav.open(entry, Routes.quizEdit(quizId = quizId)) },
                onPlay = { quizId, mode -> nav.open(entry, Routes.quizPlay(quizId, mode)) },
            )
        }
        composable(Routes.ACCOUNT) { entry ->
            DzwonAccountScreen(onBack = { nav.back(entry) })
        }
        composable(Routes.SETTINGS) { entry ->
            DzwonSettingsScreen(
                onBack = { nav.back(entry) },
                onOpenFolders = { nav.open(entry, Routes.FOLDERS) },
            )
        }

        // ------------------------------------------------ szczegóły
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
        ) { entry ->
            DzwonEditReminderScreen(
                onBack = { nav.back(entry) },
                onOpenQuiz = { reminderId -> nav.open(entry, Routes.quizEdit(reminderId = reminderId)) },
            )
        }
        composable(Routes.FOLDERS) { entry ->
            DzwonFoldersScreen(onBack = { nav.back(entry) })
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
        ) { entry ->
            DzwonQuizEditScreen(
                onBack = { nav.back(entry) },
                onPlay = { quizId, mode -> nav.open(entry, Routes.quizPlay(quizId, mode)) },
            )
        }
        composable(
            route = Routes.QUIZ_PLAY,
            arguments = listOf(
                navArgument(Routes.ARG_QUIZ_ID) { type = NavType.LongType },
                navArgument(Routes.ARG_MODE) { type = NavType.StringType },
            ),
        ) { entry ->
            DzwonQuizPlayScreen(onClose = { nav.back(entry) })
        }
    }
}
