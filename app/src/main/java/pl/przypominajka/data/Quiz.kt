package pl.przypominajka.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Zestaw pytań do nauki na kartkówkę / sprawdzian. */
@Entity(tableName = "quizzes")
data class Quiz(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Temat / zakres materiału, np. "Funkcja liniowa, rozdz. 3". */
    val subject: String = "",
    /** Przypomnienie (sprawdzian, kartkówka), do którego powstał quiz – opcjonalnie. */
    val reminderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Wynik w procentach. */
    val lastScore: Int? = null,
    val bestScore: Int? = null,
    val lastPlayedAt: Long? = null,
)

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = Quiz::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["quizId"])],
)
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quizId: Long,
    val question: String,
    val answer: String,
    val position: Int = 0,
)

data class QuizSummary(
    @Embedded val quiz: Quiz,
    val questionCount: Int,
)

@Dao
interface QuizDao {

    @Query(
        "SELECT quizzes.*, (SELECT COUNT(*) FROM quiz_questions WHERE quiz_questions.quizId = quizzes.id) " +
            "AS questionCount FROM quizzes ORDER BY createdAt DESC",
    )
    fun observeSummaries(): Flow<List<QuizSummary>>

    @Query("SELECT * FROM quizzes WHERE id = :id")
    fun observeQuiz(id: Long): Flow<Quiz?>

    @Query("SELECT * FROM quizzes WHERE id = :id")
    suspend fun getQuiz(id: Long): Quiz?

    @Query("SELECT * FROM quizzes WHERE reminderId = :reminderId LIMIT 1")
    suspend fun getByReminder(reminderId: Long): Quiz?

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY position, id")
    fun observeQuestions(quizId: Long): Flow<List<QuizQuestion>>

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY position, id")
    suspend fun getQuestions(quizId: Long): List<QuizQuestion>

    @Query("SELECT COALESCE(MAX(position), -1) FROM quiz_questions WHERE quizId = :quizId")
    suspend fun maxPosition(quizId: Long): Int

    @Insert
    suspend fun insertQuiz(quiz: Quiz): Long

    @Update
    suspend fun updateQuiz(quiz: Quiz)

    @Delete
    suspend fun deleteQuiz(quiz: Quiz)

    @Insert
    suspend fun insertQuestions(questions: List<QuizQuestion>)

    @Update
    suspend fun updateQuestion(question: QuizQuestion)

    @Delete
    suspend fun deleteQuestion(question: QuizQuestion)

    @Query(
        "UPDATE quizzes SET lastScore = :score, bestScore = MAX(COALESCE(bestScore, 0), :score), " +
            "lastPlayedAt = :playedAt WHERE id = :id",
    )
    suspend fun recordResult(id: Long, score: Int, playedAt: Long)
}
