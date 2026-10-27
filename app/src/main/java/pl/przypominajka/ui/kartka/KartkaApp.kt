package pl.przypominajka.ui.kartka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.przypominajka.ui.kartka.checklist.KartkaChecklistScreen
import pl.przypominajka.ui.kartka.account.KartkaAccountScreen
import pl.przypominajka.ui.kartka.settings.KartkaSettingsScreen
import pl.przypominajka.ui.kartka.components.KartkaMotion
import pl.przypominajka.ui.kartka.home.KartkaHomeScreen
import pl.przypominajka.ui.kartka.quiz.KartkaQuizListScreen
import pl.przypominajka.ui.kartka.quiz.KartkaQuizEditScreen
import pl.przypominajka.ui.kartka.quiz.KartkaQuizPlayScreen
import pl.przypominajka.ui.kartka.plan.KartkaPlanScreen
import pl.przypominajka.ui.kartka.workout.KartkaWorkoutScreen
import pl.przypominajka.ui.kartka.reminders.KartkaRemindersScreen
import pl.przypominajka.ui.kartka.edit.KartkaEditReminderScreen
import pl.przypominajka.ui.kartka.folders.KartkaFoldersScreen
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.ui.navigation.StartRequest

/**
 * Powłoka wyglądu Kartka: zwykły stos ekranów (strona główna → działy → szczegóły). Nowy ekran wjeżdża
 * lekko z prawej i się pojawia, powrót odwrotnie ([KartkaMotion]). Ekrany, których Kartka jeszcze nie ma,
 * otwierają tymczasowo stare wersje ([KartkaLegacy.kt]).
 *
 * @param startRequest co otworzyć od razu (skrót z ikony, widżet, powiadomienie).
 */
@Composable
fun KartkaApp(
    startRequest: StartRequest?,
    onStartRequestHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val go: (String) -> Unit = remember(navController) {
        { route: String -> navController.navigate(route) { launchSingleTop = true } }
    }
    // Gdy nie ma dokąd wrócić (np. ekran otwarty ze skrótu jako pierwszy) – wracamy na stronę główną.
    val back: () -> Unit = remember(navController) {
        { if (!navController.navigateUp()) go(Routes.HUB) }
    }

    KartkaNavGraph(navController = navController, go = go, back = back)

    LaunchedEffect(startRequest) {
        val request = startRequest ?: return@LaunchedEffect
        when (request) {
            StartRequest.NEW_REMINDER -> go(Routes.edit())
            StartRequest.PLAN -> go(Routes.PLAN)
            StartRequest.WORKOUT -> go(Routes.WORKOUT)
            StartRequest.QUIZZES -> go(Routes.QUIZZES)
            StartRequest.CHECKLIST -> go(Routes.CHECKLIST)
        }
        onStartRequestHandled()
    }
}

@Composable
private fun KartkaNavGraph(navController: NavHostController, go: (String) -> Unit, back: () -> Unit) {
    NavHost(
        navController = navController,
        startDestination = Routes.HUB,
        modifier = Modifier
            .fillMaxSize()
            .background(LookTheme.kartka.bg),
        enterTransition = { KartkaMotion.Enter },
        exitTransition = { KartkaMotion.Exit },
        popEnterTransition = { KartkaMotion.PopEnter },
        popExitTransition = { KartkaMotion.PopExit },
    ) {
        composable(Routes.HUB) {
            KartkaHomeScreen(
                onOpen = go,
                onEditReminder = { id -> go(Routes.edit(reminderId = id)) },
                onAddReminder = { go(Routes.edit()) },
            )
        }

        // ------------------------------------------------ działy
        composable(Routes.CHECKLIST) {
            KartkaChecklistScreen(onBack = back)
        }
        composable(Routes.REMINDERS) {
            KartkaRemindersScreen(
                onBack = back,
                onAddReminder = { folderId -> go(Routes.edit(folderId = folderId)) },
                onEditReminder = { id -> go(Routes.edit(reminderId = id)) },
            )
        }
        composable(Routes.PLAN) {
            KartkaPlanScreen(
                onBack = back,
                onOpenAccount = { go(Routes.ACCOUNT) },
                onOpenReminder = { id -> go(Routes.edit(reminderId = id)) },
            )
        }
        composable(Routes.WORKOUT) { KartkaWorkoutScreen(onBack = back) }
        composable(Routes.QUIZZES) {
            KartkaQuizListScreen(
                onBack = back,
                onNewQuiz = { go(Routes.quizEdit()) },
                onQuizForReminder = { reminderId -> go(Routes.quizEdit(reminderId = reminderId)) },
                onOpenQuiz = { quizId -> go(Routes.quizEdit(quizId = quizId)) },
                onPlay = { quizId, mode -> go(Routes.quizPlay(quizId, mode)) },
            )
        }
        composable(Routes.ACCOUNT) { KartkaAccountScreen(onBack = back) }
        composable(Routes.SETTINGS) {
            KartkaSettingsScreen(onBack = back, onOpenFolders = { go(Routes.FOLDERS) })
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
            KartkaEditReminderScreen(
                onBack = back,
                onOpenQuiz = { reminderId -> go(Routes.quizEdit(reminderId = reminderId)) },
            )
        }
        composable(Routes.FOLDERS) { KartkaFoldersScreen(onBack = back) }
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
            KartkaQuizEditScreen(
                onBack = back,
                onPlay = { quizId, mode -> go(Routes.quizPlay(quizId, mode)) },
            )
        }
        composable(
            route = Routes.QUIZ_PLAY,
            arguments = listOf(
                navArgument(Routes.ARG_QUIZ_ID) { type = NavType.LongType },
                navArgument(Routes.ARG_MODE) { type = NavType.StringType },
            ),
        ) {
            KartkaQuizPlayScreen(onClose = back)
        }
    }
}
