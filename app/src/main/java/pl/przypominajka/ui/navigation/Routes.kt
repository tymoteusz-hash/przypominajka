package pl.przypominajka.ui.navigation

import android.content.Intent
import pl.przypominajka.ui.quiz.QuizMode

/** Adresy ekranów – wspólne dla wszystkich wyglądów. */
object Routes {
    // Strona główna – dzwon, od którego prowadzą wszystkie drogi.
    const val HUB = "hub"

    // Działy
    const val REMINDERS = "reminders"
    const val CHECKLIST = "checklist"
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
 * Co otworzyć od razu po starcie – ze skrótu na ikonie aplikacji (przytrzymanie ikony), z widżetu
 * albo z powiadomienia. Adresy: przypominajka://open/new, …/plan, …/workout, …/quizzes, …/checklist.
 */
enum class StartRequest(val key: String) {
    NEW_REMINDER("new"),
    PLAN("plan"),
    WORKOUT("workout"),
    QUIZZES("quizzes"),
    CHECKLIST("checklist"),
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
