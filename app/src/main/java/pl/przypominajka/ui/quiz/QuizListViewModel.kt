package pl.przypominajka.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pl.przypominajka.data.QuizRepository
import pl.przypominajka.data.QuizSummary
import pl.przypominajka.data.Reminder
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolType
import java.time.LocalDate

/** Nadchodzący sprawdzian / kartkówka bez quizu – propozycja "Przygotuj się". */
data class QuizSuggestion(val reminder: Reminder, val type: SchoolType, val date: LocalDate)

data class QuizListUiState(
    val quizzes: List<QuizSummary> = emptyList(),
    val suggestions: List<QuizSuggestion> = emptyList(),
    val isLoading: Boolean = true,
)

class QuizListViewModel(
    quizRepository: QuizRepository,
    reminderRepository: ReminderRepository,
) : ViewModel() {

    val state: StateFlow<QuizListUiState> = combine(
        quizRepository.observeSummaries(),
        reminderRepository.observeReminders(),
    ) { quizzes, reminders ->
        val today = LocalDate.now()
        val linkedReminders = quizzes.mapNotNull { it.quiz.reminderId }.toSet()
        val suggestions = reminders
            .mapNotNull { reminder ->
                val type = SchoolType.fromName(reminder.schoolType)?.takeIf { it.supportsQuiz } ?: return@mapNotNull null
                val date = reminder.eventDate?.takeIf { !it.isBefore(today) } ?: return@mapNotNull null
                if (reminder.id in linkedReminders) return@mapNotNull null
                QuizSuggestion(reminder, type, date)
            }
            .sortedBy { it.date }
            .take(5)
        QuizListUiState(quizzes = quizzes, suggestions = suggestions, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizListUiState())
}
