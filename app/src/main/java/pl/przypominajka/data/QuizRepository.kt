package pl.przypominajka.data

import kotlinx.coroutines.flow.Flow

/** Quizy do nauki: zestawy pytań z odpowiedziami i wyniki. */
class QuizRepository(db: AppDatabase) {

    private val quizDao = db.quizDao()

    fun observeSummaries(): Flow<List<QuizSummary>> = quizDao.observeSummaries()

    fun observeQuiz(id: Long): Flow<Quiz?> = quizDao.observeQuiz(id)

    fun observeQuestions(quizId: Long): Flow<List<QuizQuestion>> = quizDao.observeQuestions(quizId)

    suspend fun getQuiz(id: Long): Quiz? = quizDao.getQuiz(id)

    suspend fun getQuestions(quizId: Long): List<QuizQuestion> = quizDao.getQuestions(quizId)

    suspend fun findByReminder(reminderId: Long): Quiz? = quizDao.getByReminder(reminderId)

    suspend fun createQuiz(title: String, subject: String, reminderId: Long?): Long =
        quizDao.insertQuiz(Quiz(title = title, subject = subject, reminderId = reminderId))

    suspend fun updateQuiz(quiz: Quiz) = quizDao.updateQuiz(quiz)

    suspend fun deleteQuiz(quiz: Quiz) = quizDao.deleteQuiz(quiz)

    /** Dodaje pytania na końcu zestawu. */
    suspend fun addQuestions(quizId: Long, pairs: List<Pair<String, String>>) {
        if (pairs.isEmpty()) return
        val start = quizDao.maxPosition(quizId) + 1
        quizDao.insertQuestions(
            pairs.mapIndexed { index, (question, answer) ->
                QuizQuestion(quizId = quizId, question = question, answer = answer, position = start + index)
            },
        )
    }

    suspend fun updateQuestion(question: QuizQuestion) = quizDao.updateQuestion(question)

    suspend fun deleteQuestion(question: QuizQuestion) = quizDao.deleteQuestion(question)

    suspend fun recordResult(quizId: Long, scorePercent: Int) =
        quizDao.recordResult(quizId, scorePercent.coerceIn(0, 100), System.currentTimeMillis())

    companion object {
        private val SEPARATORS = listOf(" - ", " – ", " — ", " = ", "\t", " ; ", ";")

        /**
         * Zamienia wklejony tekst na pary pytanie–odpowiedź. Każda linia to jedno pytanie, np.:
         * "Stolica Francji - Paryż", "2+2 = 4", "photosynthesis;fotosynteza".
         */
        fun parseBulk(text: String): List<Pair<String, String>> = text.lines().mapNotNull { line ->
            val trimmed = line.trim().trimStart('•', '*', '-', ' ').trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val separator = SEPARATORS.firstOrNull { trimmed.contains(it) } ?: return@mapNotNull null
            val question = trimmed.substringBefore(separator).trim()
            val answer = trimmed.substringAfter(separator).trim()
            if (question.isEmpty() || answer.isEmpty()) null else question to answer
        }
    }
}
