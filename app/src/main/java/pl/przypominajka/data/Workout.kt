package pl.przypominajka.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Folder treningowy, np. „Siłownia”, „Bieganie”. */
@Entity(tableName = "workout_folders")
data class WorkoutFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val position: Int = 0,
)

/** Plan w folderze, np. „Push”, „Pull”, „Legs” albo „Upper”, „Lower”. */
@Entity(
    tableName = "workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutFolder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["folderId"])],
)
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val name: String,
    val position: Int = 0,
)

/** Ćwiczenie w planie – z polem „odhaczone” (checkbox). */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["planId"])],
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    /** Dowolny opis, np. „4 × 8, 60 kg”. */
    val details: String = "",
    val done: Boolean = false,
    val position: Int = 0,
)

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workout_folders ORDER BY position, id")
    fun observeFolders(): Flow<List<WorkoutFolder>>

    @Query("SELECT * FROM workout_plans ORDER BY position, id")
    fun observePlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_exercises ORDER BY position, id")
    fun observeExercises(): Flow<List<WorkoutExercise>>

    @Insert
    suspend fun insertFolder(folder: WorkoutFolder): Long

    @Update
    suspend fun updateFolder(folder: WorkoutFolder)

    @Delete
    suspend fun deleteFolder(folder: WorkoutFolder)

    @Insert
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlan)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlan)

    @Insert
    suspend fun insertExercise(exercise: WorkoutExercise): Long

    @Update
    suspend fun updateExercise(exercise: WorkoutExercise)

    @Delete
    suspend fun deleteExercise(exercise: WorkoutExercise)

    @Query("SELECT COALESCE(MAX(position), -1) FROM workout_folders")
    suspend fun maxFolderPosition(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM workout_plans WHERE folderId = :folderId")
    suspend fun maxPlanPosition(folderId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM workout_exercises WHERE planId = :planId")
    suspend fun maxExercisePosition(planId: Long): Int

    @Query("UPDATE workout_exercises SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("UPDATE workout_exercises SET done = 0 WHERE planId = :planId")
    suspend fun resetPlan(planId: Long)
}

class WorkoutRepository(db: AppDatabase) {

    private val dao = db.workoutDao()

    fun observeFolders(): Flow<List<WorkoutFolder>> = dao.observeFolders()

    fun observePlans(): Flow<List<WorkoutPlan>> = dao.observePlans()

    fun observeExercises(): Flow<List<WorkoutExercise>> = dao.observeExercises()

    suspend fun addFolder(name: String, colorArgb: Int): Long =
        dao.insertFolder(WorkoutFolder(name = name, colorArgb = colorArgb, position = dao.maxFolderPosition() + 1))

    suspend fun updateFolder(folder: WorkoutFolder) = dao.updateFolder(folder)

    /** Usuwa folder razem z planami i ćwiczeniami (kaskadowo). */
    suspend fun deleteFolder(folder: WorkoutFolder) = dao.deleteFolder(folder)

    suspend fun addPlans(folderId: Long, names: List<String>) {
        var position = dao.maxPlanPosition(folderId)
        names.forEach { name -> dao.insertPlan(WorkoutPlan(folderId = folderId, name = name, position = ++position)) }
    }

    suspend fun updatePlan(plan: WorkoutPlan) = dao.updatePlan(plan)

    suspend fun deletePlan(plan: WorkoutPlan) = dao.deletePlan(plan)

    suspend fun addExercise(planId: Long, name: String, details: String) {
        dao.insertExercise(
            WorkoutExercise(planId = planId, name = name, details = details, position = dao.maxExercisePosition(planId) + 1),
        )
    }

    suspend fun updateExercise(exercise: WorkoutExercise) = dao.updateExercise(exercise)

    suspend fun deleteExercise(exercise: WorkoutExercise) = dao.deleteExercise(exercise)

    suspend fun setDone(exerciseId: Long, done: Boolean) = dao.setDone(exerciseId, done)

    suspend fun resetPlan(planId: Long) = dao.resetPlan(planId)
}
