package pl.przypominajka.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.przypominajka.ai.AiException
import pl.przypominajka.ai.AiSettings
import pl.przypominajka.ai.QuizGenerator
import pl.przypominajka.data.QuizQuestion
import pl.przypominajka.data.QuizRepository
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.util.pluralPl

data class QuizEditUiState(
    val isLoading: Boolean = true,
    /** null – quiz jeszcze nie zapisany (powstaje przy pierwszym pytaniu). */
    val quizId: Long? = null,
    val reminderId: Long? = null,
    val title: String = "",
    val subject: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val newQuestion: String = "",
    val newAnswer: String = "",
    val inputError: String? = null,
    /** Czy w Koncie zapisano klucz API (AI może generować pytania). */
    val aiAvailable: Boolean = false,
    val isGenerating: Boolean = false,
    val aiError: String? = null,
    /** Jednorazowy komunikat (np. "Dodano 12 pytań"). */
    val message: String? = null,
    val deleted: Boolean = false,
)

class QuizEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
    private val reminderRepository: ReminderRepository,
    private val quizGenerator: QuizGenerator,
    aiSettings: AiSettings,
) : ViewModel() {

    private val argQuizId: Long = savedStateHandle.get<Long>(Routes.ARG_QUIZ_ID) ?: -1L
    private val argReminderId: Long = savedStateHandle.get<Long>(Routes.ARG_REMINDER_ID) ?: -1L

    private val _state = MutableStateFlow(QuizEditUiState())
    val state: StateFlow<QuizEditUiState> = _state.asStateFlow()

    private var questionsJob: Job? = null

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch {
            aiSettings.observeHasKey().collect { has -> _state.update { it.copy(aiAvailable = has) } }
        }
    }

    /** Pytania wygenerowane przez AI na podstawie nazwy i tematu zestawu. */
    fun generateWithAi(count: Int, extraTopic: String) {
        val s = _state.value
        if (s.isGenerating) return
        val topic = listOf(s.subject, extraTopic).filter { it.isNotBlank() }.joinToString("\n")
        if (s.title.isBlank() && topic.isBlank()) {
            _state.update { it.copy(aiError = "Wpisz nazwę zestawu albo temat, żeby AI wiedziało, o co pytać.") }
            return
        }
        _state.update { it.copy(isGenerating = true, aiError = null) }
        viewModelScope.launch {
            try {
                val pairs = quizGenerator.generate(
                    title = s.title,
                    topic = topic,
                    count = count,
                    existingQuestions = s.questions.map { it.question },
                )
                val id = ensureQuiz()
                quizRepository.addQuestions(id, pairs)
                _state.update {
                    it.copy(
                        isGenerating = false,
                        message = "AI dodało ${pairs.size} ${pluralPl(pairs.size, "pytanie", "pytania", "pytań")}",
                    )
                }
            } catch (e: AiException) {
                _state.update { it.copy(isGenerating = false, aiError = e.message) }
            }
        }
    }

    fun aiErrorShown() = _state.update { it.copy(aiError = null) }

    private suspend fun load() {
        val quiz = when {
            argQuizId > 0 -> quizRepository.getQuiz(argQuizId)
            argReminderId > 0 -> quizRepository.findByReminder(argReminderId)
            else -> null
        }
        if (quiz != null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    quizId = quiz.id,
                    reminderId = quiz.reminderId,
                    title = quiz.title,
                    subject = quiz.subject,
                )
            }
            observeQuestions(quiz.id)
            return
        }

        // Nowy quiz – jeśli powstaje do sprawdzianu/kartkówki, uzupełniamy tytuł i zakres materiału.
        val reminder = if (argReminderId > 0) reminderRepository.getReminder(argReminderId) else null
        val type = SchoolType.fromName(reminder?.schoolType)
        val material = reminder?.note?.lines()
            ?.filterNot { it.startsWith("Termin:") }
            ?.joinToString(" ")
            ?.trim()
            .orEmpty()
        _state.update {
            it.copy(
                isLoading = false,
                reminderId = reminder?.id,
                title = when {
                    reminder == null -> ""
                    type != null -> "${type.label}: ${reminder.title}"
                    else -> reminder.title
                },
                subject = material,
            )
        }
    }

    private fun observeQuestions(quizId: Long) {
        questionsJob?.cancel()
        questionsJob = viewModelScope.launch {
            quizRepository.observeQuestions(quizId).collect { questions ->
                _state.update { it.copy(questions = questions) }
            }
        }
    }

    /** Tworzy quiz przy pierwszej potrzebie (dodanie pytania). */
    private suspend fun ensureQuiz(): Long {
        _state.value.quizId?.let { return it }
        val s = _state.value
        val id = quizRepository.createQuiz(
            title = s.title.trim().ifBlank { "Quiz" },
            subject = s.subject.trim(),
            reminderId = s.reminderId,
        )
        _state.update { it.copy(quizId = id, title = it.title.ifBlank { "Quiz" }) }
        observeQuestions(id)
        return id
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value) }

    fun onSubjectChange(value: String) = _state.update { it.copy(subject = value) }

    fun onNewQuestionChange(value: String) = _state.update { it.copy(newQuestion = value, inputError = null) }

    fun onNewAnswerChange(value: String) = _state.update { it.copy(newAnswer = value, inputError = null) }

    fun addQuestion() {
        val s = _state.value
        val question = s.newQuestion.trim()
        val answer = s.newAnswer.trim()
        if (question.isEmpty() || answer.isEmpty()) {
            _state.update { it.copy(inputError = "Wpisz pytanie i odpowiedź.") }
            return
        }
        viewModelScope.launch {
            val id = ensureQuiz()
            quizRepository.addQuestions(id, listOf(question to answer))
            _state.update { it.copy(newQuestion = "", newAnswer = "") }
        }
    }

    /** Wklejona lista: jedna linia = jedno pytanie. Zwraca liczbę rozpoznanych pytań. */
    fun addBulk(text: String): Int {
        val pairs = QuizRepository.parseBulk(text)
        if (pairs.isEmpty()) {
            _state.update {
                it.copy(message = "Nie rozpoznano pytań. Oddziel pytanie od odpowiedzi myślnikiem: „pytanie - odpowiedź”.")
            }
            return 0
        }
        viewModelScope.launch {
            val id = ensureQuiz()
            quizRepository.addQuestions(id, pairs)
            _state.update {
                it.copy(message = "Dodano ${pairs.size} ${pluralPl(pairs.size, "pytanie", "pytania", "pytań")}")
            }
        }
        return pairs.size
    }

    fun updateQuestion(question: QuizQuestion, newQuestion: String, newAnswer: String) {
        if (newQuestion.isBlank() || newAnswer.isBlank()) return
        viewModelScope.launch {
            quizRepository.updateQuestion(question.copy(question = newQuestion.trim(), answer = newAnswer.trim()))
        }
    }

    fun deleteQuestion(question: QuizQuestion) {
        viewModelScope.launch { quizRepository.deleteQuestion(question) }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    /** Zapis tytułu i tematu (przy wyjściu z ekranu i przed rozpoczęciem nauki). */
    fun saveMeta() {
        val s = _state.value
        val id = s.quizId ?: return
        viewModelScope.launch {
            val quiz = quizRepository.getQuiz(id) ?: return@launch
            quizRepository.updateQuiz(quiz.copy(title = s.title.trim().ifBlank { "Quiz" }, subject = s.subject.trim()))
        }
    }

    fun deleteQuiz() {
        val id = _state.value.quizId ?: run {
            _state.update { it.copy(deleted = true) }
            return
        }
        viewModelScope.launch {
            quizRepository.getQuiz(id)?.let { quizRepository.deleteQuiz(it) }
            _state.update { it.copy(deleted = true) }
        }
    }
}
