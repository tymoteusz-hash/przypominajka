package pl.przypominajka.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pl.przypominajka.AppContainer
import pl.przypominajka.PrzypominajkaApp
import pl.przypominajka.ui.account.AccountViewModel
import pl.przypominajka.ui.checklist.ChecklistViewModel
import pl.przypominajka.ui.edit.EditReminderViewModel
import pl.przypominajka.ui.folders.FoldersViewModel
import pl.przypominajka.ui.home.HomeViewModel
import pl.przypominajka.ui.plan.PlanViewModel
import pl.przypominajka.ui.quiz.QuizEditViewModel
import pl.przypominajka.ui.quiz.QuizListViewModel
import pl.przypominajka.ui.quiz.QuizPlayViewModel
import pl.przypominajka.ui.settings.SettingsViewModel
import pl.przypominajka.ui.workout.WorkoutViewModel

/** Fabryka wszystkich ViewModeli – wstrzykuje zależności z AppContainer. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(container().repository, container().librusSettings, container().checklistRepository)
        }
        initializer {
            ChecklistViewModel(container().checklistRepository)
        }
        initializer {
            EditReminderViewModel(createSavedStateHandle(), container().repository, container().schoolPrefs)
        }
        initializer {
            FoldersViewModel(container().repository)
        }
        initializer {
            val container = container()
            PlanViewModel(container.repository, container.librusSettings, container.librusSyncer)
        }
        initializer {
            val container = container()
            WorkoutViewModel(
                container.workoutRepository,
                container.restTimer,
                container.trainingReminder,
                container.repository,
            )
        }
        initializer {
            QuizListViewModel(container().quizRepository, container().repository)
        }
        initializer {
            val container = container()
            QuizEditViewModel(
                createSavedStateHandle(),
                container.quizRepository,
                container.repository,
                container.quizGenerator,
                container.aiSettings,
            )
        }
        initializer {
            QuizPlayViewModel(createSavedStateHandle(), container().quizRepository)
        }
        initializer {
            val container = container()
            SettingsViewModel(
                appearancePrefs = container.appearancePrefs,
                schoolPrefs = container.schoolPrefs,
                librusSettings = container.librusSettings,
                librusSyncer = container.librusSyncer,
                repository = container.repository,
                notificationHelper = container.notificationHelper,
                tonePreview = container.tonePreview,
                changeLook = container::changeLook,
                updates = container.updateManager,
            )
        }
        initializer {
            val container = container()
            AccountViewModel(
                settings = container.librusSettings,
                syncer = container.librusSyncer,
                scheduler = container.librusScheduler,
                repository = container.repository,
                schoolPrefs = container.schoolPrefs,
                aiSettings = container.aiSettings,
                appearancePrefs = container.appearancePrefs,
            )
        }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PrzypominajkaApp).container
