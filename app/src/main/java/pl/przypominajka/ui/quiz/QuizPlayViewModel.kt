package pl.przypominajka.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.przypominajka.data.QuizQuestion
import pl.przypominajka.data.QuizRepository
import pl.przypominajka.ui.navigation.Routes

enum class QuizMode(val routeValue: String) {
    /** Fiszki: pytanie → odwróć kartę → "Umiem" / "Jeszcze nie". */
    CARDS("cards"),

    /** Test ABCD: błędne odpowiedzi losowane z innych pytań zestawu. */
    CHOICE("choice"),
    ;

    companion object {
        /** Test ABCD ma sens od 2 pytań (co najmniej dwie różne odpowiedzi). */
        const val MIN_CHOICE_QUESTIONS = 2

        fun fromRoute(value: String?): QuizMode = entries.firstOrNull { it.routeValue == value } ?: CARDS
    }
}

data class QuizItem(
    val question: QuizQuestion,
    /** Odpowiedzi do wyboru (tylko test ABCD). */
    val options: List<String> = emptyList(),
)

data class QuizPlayUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val mode: QuizMode = QuizMode.CARDS,
    val items: List<QuizItem> = emptyList(),
    val index: Int = 0,
    /** Fiszki: czy karta jest odwrócona. */
    val revealed: Boolean = false,
    /** Test: wybrana odpowiedź w bieżącym pytaniu. */
    val selectedOption: String? = null,
    val correctIds: Set<Long> = emptySet(),
    val wrongIds: Set<Long> = emptySet(),
    val finished: Boolean = false,
    /** Powtórka tylko trudnych pytań – wynik nie trafia do statystyk. */
    val isReplay: Boolean = false,
) {
    val current: QuizItem? get() = items.getOrNull(index)
    val total: Int get() = items.size
    val scorePercent: Int get() = if (total == 0) 0 else correctIds.size * 100 / total
}

class QuizPlayViewModel(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
) : ViewModel() {

    private val quizId: Long = savedStateHandle.get<Long>(Routes.ARG_QUIZ_ID) ?: -1L
    private val mode: QuizMode = QuizMode.fromRoute(savedStateHandle.get<String>(Routes.ARG_MODE))

    private var allQuestions: List<QuizQuestion> = emptyList()

    private val _state = MutableStateFlow(QuizPlayUiState(mode = mode))
    val state: StateFlow<QuizPlayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val quiz = quizRepository.getQuiz(quizId)
            allQuestions = quizRepository.getQuestions(quizId)
            _state.update { it.copy(title = quiz?.title.orEmpty()) }
            start(allQuestions, replay = false)
        }
    }

    private fun start(questions: List<QuizQuestion>, replay: Boolean) {
        val items = questions.shuffled().map { question ->
            QuizItem(question = question, options = if (mode == QuizMode.CHOICE) optionsFor(question) else emptyList())
        }
        _state.update {
            it.copy(
                isLoading = false,
                items = items,
                index = 0,
                revealed = false,
                selectedOption = null,
                correctIds = emptySet(),
                wrongIds = emptySet(),
                finished = items.isEmpty(),
                isReplay = replay,
            )
        }
    }

    /** Poprawna odpowiedź + do 3 innych odpowiedzi z zestawu, w losowej kolejności. */
    private fun optionsFor(question: QuizQuestion): List<String> {
        val distractors = allQuestions
            .map { it.answer }
            .filter { !it.equals(question.answer, ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .shuffled()
            .take(3)
        return (distractors + question.answer).shuffled()
    }

    fun reveal() = _state.update { it.copy(revealed = true) }

    /** Fiszki: ocena własna po odwróceniu karty. */
    fun answerCard(known: Boolean) {
        val item = _state.value.current ?: return
        _state.update {
            it.copy(
                correctIds = if (known) it.correctIds + item.question.id else it.correctIds,
                wrongIds = if (known) it.wrongIds else it.wrongIds + item.question.id,
            )
        }
        next()
    }

    /** Test: wybór odpowiedzi (tylko raz na pytanie). */
    fun selectOption(option: String) {
        val s = _state.value
        val item = s.current ?: return
        if (s.selectedOption != null) return
        val correct = option == item.question.answer
        _state.update {
            it.copy(
                selectedOption = option,
                correctIds = if (correct) it.correctIds + item.question.id else it.correctIds,
                wrongIds = if (correct) it.wrongIds else it.wrongIds + item.question.id,
            )
        }
    }

    fun next() {
        val s = _state.value
        if (s.index + 1 >= s.total) {
            _state.update { it.copy(finished = true) }
            if (!s.isReplay) {
                val score = _state.value.scorePercent
                viewModelScope.launch { quizRepository.recordResult(quizId, score) }
            }
        } else {
            _state.update { it.copy(index = it.index + 1, revealed = false, selectedOption = null) }
        }
    }

    fun restart() = start(allQuestions, replay = false)

    fun replayWrong() {
        val wrong = _state.value.wrongIds
        start(allQuestions.filter { it.id in wrong }, replay = true)
    }
}
