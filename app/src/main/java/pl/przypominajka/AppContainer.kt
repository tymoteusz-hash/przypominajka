package pl.przypominajka

import android.content.Context
import android.content.SharedPreferences
import pl.przypominajka.ai.AiSettings
import pl.przypominajka.ai.QuizGenerator
import pl.przypominajka.alarm.ChecklistAlarms
import pl.przypominajka.alarm.NotificationHelper
import pl.przypominajka.alarm.ReminderScheduler
import pl.przypominajka.alarm.RestTimer
import pl.przypominajka.alarm.TrainingReminder
import pl.przypominajka.data.AppDatabase
import pl.przypominajka.data.ChecklistRepository
import pl.przypominajka.data.DismissedExternalIds
import pl.przypominajka.data.QuizRepository
import pl.przypominajka.data.WorkoutRepository
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.librus.LibrusSettings
import pl.przypominajka.librus.LibrusSyncScheduler
import pl.przypominajka.librus.LibrusSyncer
import pl.przypominajka.update.UpdateManager
import pl.przypominajka.ui.look.Look
import pl.przypominajka.util.AppIconSwitcher
import pl.przypominajka.util.AppearancePrefs
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.TonePreview

/** Prosty ręczny kontener zależności (jedna instancja na proces). */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val scheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }

    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext, appearancePrefs) }

    val preferences: SharedPreferences by lazy {
        appContext.getSharedPreferences("przypominajka_prefs", Context.MODE_PRIVATE)
    }

    val schoolPrefs: SchoolPrefs by lazy { SchoolPrefs(preferences) }

    val appearancePrefs: AppearancePrefs by lazy { AppearancePrefs(preferences) }

    val restTimer: RestTimer by lazy { RestTimer(appContext, preferences) }

    /** Odsłuch dźwięku powiadomień w Ustawieniach. */
    val tonePreview: TonePreview by lazy { TonePreview(appContext) }

    val repository: ReminderRepository by lazy {
        ReminderRepository(
            database, scheduler, notificationHelper, DismissedExternalIds(preferences), schoolPrefs, trainingReminder,
        )
    }

    val trainingReminder: TrainingReminder by lazy { TrainingReminder(appContext, preferences) }

    val quizRepository: QuizRepository by lazy { QuizRepository(database) }

    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository(database) }

    val checklistAlarms: ChecklistAlarms by lazy { ChecklistAlarms(appContext) }

    val checklistRepository: ChecklistRepository by lazy {
        ChecklistRepository(database, checklistAlarms, notificationHelper)
    }

    val aiSettings: AiSettings by lazy { AiSettings(appContext) }

    val quizGenerator: QuizGenerator by lazy { QuizGenerator(aiSettings) }

    val librusSettings: LibrusSettings by lazy { LibrusSettings(appContext) }

    val librusSyncer: LibrusSyncer by lazy { LibrusSyncer(repository, librusSettings, schoolPrefs) }

    val librusScheduler: LibrusSyncScheduler by lazy { LibrusSyncScheduler(appContext) }

    val updateManager: UpdateManager by lazy { UpdateManager(appContext, preferences) }

    /** Zmiana wyglądu (przewodnik na start albo Ustawienia): zapis + pasująca ikona aplikacji. */
    fun changeLook(look: Look) {
        appearancePrefs.setLook(look)
        AppIconSwitcher.apply(appContext, look)
    }
}
