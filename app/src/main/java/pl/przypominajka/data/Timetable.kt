package pl.przypominajka.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/** Lekcja z planu (pobrana z Librusa). */
@Entity(tableName = "timetable_lessons", indices = [Index(value = ["date"])])
data class TimetableLesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val lessonNo: Int,
    val subject: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val canceled: Boolean,
    val teacher: String,
    val classroom: String,
    val substitution: Boolean,
)

/** Koniec ostatniej nieodwołanej lekcji danego dnia. */
data class LessonEnd(
    val date: LocalDate,
    val endTime: LocalTime,
)

@Dao
interface TimetableDao {

    @Query("SELECT * FROM timetable_lessons WHERE date BETWEEN :from AND :to ORDER BY date, lessonNo, startTime")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<TimetableLesson>>

    @Query(
        "SELECT date, MAX(endTime) AS endTime FROM timetable_lessons " +
            "WHERE canceled = 0 AND date >= :from GROUP BY date",
    )
    suspend fun getLessonEnds(from: LocalDate): List<LessonEnd>

    @Query(
        "SELECT date, MAX(endTime) AS endTime FROM timetable_lessons " +
            "WHERE canceled = 0 AND date >= :from GROUP BY date",
    )
    fun observeLessonEnds(from: LocalDate): Flow<List<LessonEnd>>

    @Query("SELECT COUNT(*) FROM timetable_lessons")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM timetable_lessons WHERE date BETWEEN :from AND :to")
    suspend fun deleteRange(from: LocalDate, to: LocalDate)

    @Query("DELETE FROM timetable_lessons")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(lessons: List<TimetableLesson>)
}
