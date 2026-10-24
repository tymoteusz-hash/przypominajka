package pl.przypominajka

import android.content.Context
import android.content.SharedPreferences
import pl.przypominajka.ai.AiSettings
import pl.przypominajka.ai.QuizGenerator
import pl.przypominajka.alarm.NotificationHelper
import pl.przypominajka.alarm.ReminderScheduler
import pl.przypominajka.alarm.RestTimer
import pl.przypominajka.alarm.TrainingReminder
import pl.przypominajka.data.AppDatabase
import pl.przypominajka.data.DismissedExternalIds
import pl.przypominajka.data.QuizRepository
import pl.przypominajka.data.WorkoutRepository
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.librus.LibrusSettings
import pl.przypominajka.librus.LibrusSyncScheduler
import pl.przypominajka.librus.LibrusSyncer
import pl.przypominajka.update.UpdateManager
import pl.przypominajka.util.AppearancePrefs
import pl.przypominajka.util.BellSound
import pl.przypominajka.util.SchoolPrefs

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

    val bellSound: BellSound by lazy { BellSound(appContext, appearancePrefs) }

    val repository: ReminderRepository by lazy {
        ReminderRepository(
            database, scheduler, notificationHelper, DismissedExternalIds(preferences), schoolPrefs, trainingReminder,
        )
    }

    val trainingReminder: TrainingReminder by lazy { TrainingReminder(appContext, preferences) }

    val quizRepository: QuizRepository by lazy { QuizRepository(database) }

    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository(database) }

    val aiSettings: AiSettings by lazy { AiSettings(appContext) }

    val quizGenerator: QuizGenerator by lazy { QuizGenerator(aiSettings) }

    val librusSettings: LibrusSettings by lazy { LibrusSettings(appContext) }

    val librusSyncer: LibrusSyncer by lazy { LibrusSyncer(repository, librusSettings, schoolPrefs) }

    val librusScheduler: LibrusSyncScheduler by lazy { LibrusSyncScheduler(appContext) }

    val updateManager: UpdateManager by lazy { UpdateManager(appContext, preferences) }
}
