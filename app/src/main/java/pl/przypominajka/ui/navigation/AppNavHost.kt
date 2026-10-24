package pl.przypominajka.ui.navigation

import android.content.Intent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.przypominajka.appContainer
import pl.przypominajka.ui.account.AccountScreen
import pl.przypominajka.ui.components.BellController
import pl.przypominajka.ui.components.BellReveal
import pl.przypominajka.ui.components.LocalBell
import pl.przypominajka.ui.components.LocalBottomBarPadding
import pl.przypominajka.ui.edit.EditReminderScreen
import pl.przypominajka.ui.folders.FoldersScreen
import pl.przypominajka.ui.home.HubScreen
import pl.przypominajka.ui.home.RemindersScreen
import pl.przypominajka.ui.plan.PlanScreen
import pl.przypominajka.ui.quiz.QuizEditScreen
import pl.przypominajka.ui.quiz.QuizListScreen
import pl.przypominajka.ui.quiz.QuizMode
import pl.przypominajka.ui.quiz.QuizPlayScreen
import pl.przypominajka.ui.settings.SettingsScreen
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.workout.WorkoutScreen

object Routes {
    // Strona główna – dzwon, od którego prowadzą wszystkie drogi.
    const val HUB = "hub"

    // Działy
    const val REMINDERS = "reminders"
    const val PLAN = "plan"
    const val WORKOUT = "workout"
    const val QUIZZES = "quizzes"
    const val ACCOUNT = "account"
    const val SETTINGS = "settings"

    // Ekrany szczegółów
    const val FOLDERS = "folders"
    const val ARG_REMINDER_ID = "reminderId"
    const val ARG_FOLDER_ID = "folderId"
    const val ARG_QUIZ_ID = "quizId"
    const val ARG_MODE = "mode"
    const val EDIT = "edit?$ARG_REMINDER_ID={$ARG_REMINDER_ID}&$ARG_FOLDER_ID={$ARG_FOLDER_ID}"
    const val QUIZ_EDIT = "quiz?$ARG_QUIZ_ID={$ARG_QUIZ_ID}&$ARG_REMINDER_ID={$ARG_REMINDER_ID}"
    const val QUIZ_PLAY = "quizPlay/{$ARG_QUIZ_ID}/{$ARG_MODE}"

    /** -1 oznacza brak wartości (nowe przypomnienie / brak domyślnego folderu). */
    fun edit(reminderId: Long? = null, folderId: Long? = null): String =
        "edit?$ARG_REMINDER_ID=${reminderId ?: -1L}&$ARG_FOLDER_ID=${folderId ?: -1L}"

    fun quizEdit(quizId: Long? = null, reminderId: Long? = null): String =
        "quiz?$ARG_QUIZ_ID=${quizId ?: -1L}&$ARG_REMINDER_ID=${reminderId ?: -1L}"

    fun quizPlay(quizId: Long, mode: QuizMode): String = "quizPlay/$quizId/${mode.routeValue}"
}

/**
 * Co otworzyć od razu po starcie – ze skrótu na ikonie aplikacji (przytrzymanie ikony)
 * albo z widżetu. Adresy: przypominajka://open/new, …/plan, …/workout, …/quizzes.
 */
enum class StartRequest(val key: String) {
    NEW_REMINDER("new"),
    PLAN("plan"),
    WORKOUT("workout"),
    QUIZZES("quizzes"),
    ;

    companion object {
        const val SCHEME = "przypominajka"
        const val HOST = "open"

        fun fromIntent(intent: Intent?): StartRequest? {
            val data = intent?.data ?: return null
            if (data.scheme != SCHEME || data.host != HOST) return null
            return entries.firstOrNull { it.key == data.lastPathSegment }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startRequest: StartRequest? = null,
    onStartRequestHandled: () -> Unit = {},
) {
    val container = LocalContext.current.appContainer
    val bell = remember { BellController(container.bellSound) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { container.bellSound.prepare() } }

    // Każda zmiana ekranu = uderzenie dzwonu. Rozpoznajemy też, czy to powrót (ekran już był na stosie).
    DisposableEffect(navController) {
        val stack = ArrayList<String>()
        var first = true
        val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
            val id = controller.currentBackStackEntry?.id ?: return@OnDestinationChangedListener
            val index = stack.indexOf(id)
            val pop = index >= 0
            if (pop) {
                while (stack.size > index + 1) stack.removeAt(stack.lastIndex)
            } else {
                stack.add(id)
            }
            bell.lastNavigationWasPop = pop
            if (first) first = false else bell.ring()
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    /** Przejście dalej – zawsze "od dzwonu" bieżącego ekranu. */
    val go: (String) -> Unit = { route ->
        bell.prepareNavigation()
        navController.navigate(route) { launchSingleTop = true }
    }
    val back: () -> Unit = {
        if (!navController.navigateUp()) go(Routes.HUB)
    }

    LaunchedEffect(startRequest) {
        val request = startRequest ?: return@LaunchedEffect
        when (request) {
            StartRequest.NEW_REMINDER -> go(Routes.edit())
            StartRequest.PLAN -> go(Routes.PLAN)
            StartRequest.WORKOUT -> go(Routes.WORKOUT)
            StartRequest.QUIZZES -> go(Routes.QUIZZES)
        }
        onStartRequestHandled()
    }

    val systemBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppSurfaces.page),
    ) {
        CompositionLocalProvider(
            LocalBell provides bell,
            LocalBottomBarPadding provides systemBarPadding,
        ) {
            AppNavGraph(navController = navController, go = go, back = back)
        }
    }
}

@Composable
private fun AppNavGraph(navController: NavHostController, go: (String) -> Unit, back: () -> Unit) {
    // Wszystkie przejścia robi BellReveal (koło od dzwonu) – standardowe animacje są wyłączone.
    NavHost(
        navController = navController,
        startDestination = Routes.HUB,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(Routes.HUB) {
            BellReveal {
                HubScreen(
                    onOpen = go,
                    onAddReminder = { go(Routes.edit()) },
                    onEditReminder = { id -> go(Routes.edit(reminderId = id)) },
                )
            }
        }

        // ------------------------------------------------ działy
        composable(Routes.REMINDERS) {
            BellReveal {
                RemindersScreen(
                    onBack = back,
                    onAddReminder = { folderId -> go(Routes.edit(folderId = folderId)) },
                    onEditReminder = { id -> go(Routes.edit(reminderId = id)) },
                )
            }
        }
        composable(Routes.PLAN) {
            BellReveal {
                PlanScreen(
                    onBack = back,
                    onOpenAccount = { go(Routes.ACCOUNT) },
                    onOpenReminder = { id -> go(Routes.edit(reminderId = id)) },
                )
            }
        }
        composable(Routes.WORKOUT) {
            BellReveal { WorkoutScreen(onBack = back) }
        }
        composable(Routes.QUIZZES) {
            BellReveal {
                QuizListScreen(
                    onBack = back,
                    onNewQuiz = { go(Routes.quizEdit()) },
                    onQuizForReminder = { reminderId -> go(Routes.quizEdit(reminderId = reminderId)) },
                    onOpenQuiz = { quizId -> go(Routes.quizEdit(quizId = quizId)) },
                    onPlay = { quizId, mode -> go(Routes.quizPlay(quizId, mode)) },
                )
            }
        }
        composable(Routes.ACCOUNT) {
            BellReveal {
                AccountScreen(onBack = back)
            }
        }

        composable(Routes.SETTINGS) {
            BellReveal { SettingsScreen(onBack = back, onOpenFolders = { go(Routes.FOLDERS) }) }
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
        ) {
            BellReveal {
                EditReminderScreen(
                    onBack = back,
                    onOpenQuiz = { reminderId -> go(Routes.quizEdit(reminderId = reminderId)) },
                )
            }
        }
        composable(Routes.FOLDERS) {
            BellReveal { FoldersScreen(onBack = back) }
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
            BellReveal {
                QuizEditScreen(
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
            BellReveal { QuizPlayScreen(onClose = back) }
        }
    }
}
