package pl.przypominajka.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Folder::class, Reminder::class, TimetableLesson::class, Quiz::class, QuizQuestion::class,
        WorkoutFolder::class, WorkoutPlan::class, WorkoutExercise::class, ChecklistItem::class,
    ],
    version = 8,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao

    abstract fun reminderDao(): ReminderDao

    abstract fun timetableDao(): TimetableDao

    abstract fun quizDao(): QuizDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun checklistDao(): ChecklistDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "przypominajka.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8,
                )
                .addCallback(SeedDefaultFoldersCallback)
                .build()
    }
}

/** Wersja 8: checklista – jedna lista punktów do odhaczenia (opcjonalnie z godziną). */
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `checklist_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `text` TEXT NOT NULL, " +
                "`done` INTEGER NOT NULL, `position` INTEGER NOT NULL, `remindAt` INTEGER, " +
                "`createdAt` INTEGER NOT NULL)",
        )
    }
}

/** Wersja 7: jaskrawe kolory folderów zamienione na przygaszone odpowiedniki z nowej palety. */
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        FolderColors.legacyToNew.forEach { (old, new) ->
            db.execSQL("UPDATE folders SET colorArgb = ? WHERE colorArgb = ?", arrayOf<Any>(new, old))
            db.execSQL("UPDATE workout_folders SET colorArgb = ? WHERE colorArgb = ?", arrayOf<Any>(new, old))
        }
    }
}

/** Wersja 2: rodzaj wpisu szkolnego (zadanie domowe, kartkówka, sprawdzian, wyjście). */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN schoolType TEXT")
    }
}

/** Wersja 3: powiązanie z wpisem z Librusa (lub innego źródła zewnętrznego). */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN externalId TEXT")
    }
}

/** Wersja 4: dzień wydarzenia, przypomnienia "po lekcjach", plan lekcji i quizy. */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN eventDate INTEGER")
        db.execSQL("ALTER TABLE reminders ADD COLUMN afterSchool INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `timetable_lessons` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, " +
                "`lessonNo` INTEGER NOT NULL, `subject` TEXT NOT NULL, `startTime` INTEGER NOT NULL, " +
                "`endTime` INTEGER NOT NULL, `canceled` INTEGER NOT NULL, `teacher` TEXT NOT NULL, " +
                "`classroom` TEXT NOT NULL, `substitution` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetable_lessons_date` ON `timetable_lessons` (`date`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quizzes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                "`subject` TEXT NOT NULL, `reminderId` INTEGER, `createdAt` INTEGER NOT NULL, " +
                "`lastScore` INTEGER, `bestScore` INTEGER, `lastPlayedAt` INTEGER)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quiz_questions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quizId` INTEGER NOT NULL, " +
                "`question` TEXT NOT NULL, `answer` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`quizId`) REFERENCES `quizzes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_questions_quizId` ON `quiz_questions` (`quizId`)")
    }
}

/** Wersja 5: wybrane dni tygodnia (0 = codziennie). */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN daysOfWeek INTEGER NOT NULL DEFAULT 0")
    }
}

/** Wersja 6: trening – foldery, plany i ćwiczenia z checkboxami (domyślnie „Siłownia” i „Bieganie”). */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_folders` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`colorArgb` INTEGER NOT NULL, `position` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_plans` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `folderId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`folderId`) REFERENCES `workout_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_plans_folderId` ON `workout_plans` (`folderId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_exercises` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `planId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, `details` TEXT NOT NULL, `done` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`planId`) REFERENCES `workout_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_planId` ON `workout_exercises` (`planId`)")
        seedWorkoutFolders(db)
    }
}

private fun seedWorkoutFolders(db: SupportSQLiteDatabase) {
    listOf("Siłownia" to FolderColors.PURPLE, "Bieganie" to FolderColors.GREEN).forEachIndexed { index, (name, color) ->
        db.execSQL(
            "INSERT INTO workout_folders (name, colorArgb, position) VALUES (?, ?, ?)",
            arrayOf<Any>(name, color, index),
        )
    }
}

/** Przy tworzeniu bazy dodaje domyślne foldery: Szkoła, Dom, Praca oraz systemowy "Inne". */
private object SeedDefaultFoldersCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val defaults = listOf(
            Triple(1L, "Szkoła", FolderColors.BLUE),
            Triple(2L, "Dom", FolderColors.GREEN),
            Triple(3L, "Praca", FolderColors.ORANGE),
            Triple(Folder.OTHER_FOLDER_ID, "Inne", FolderColors.GREY),
        )
        defaults.forEach { (id, name, color) ->
            db.execSQL(
                "INSERT INTO folders (id, name, colorArgb) VALUES (?, ?, ?)",
                arrayOf<Any>(id, name, color),
            )
        }
        seedWorkoutFolders(db)
    }
}
