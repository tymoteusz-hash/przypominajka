package pl.przypominajka.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Punkt na liście „Checklista” – do odhaczenia, opcjonalnie z godziną przypomnienia. */
@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val done: Boolean = false,
    val position: Int = 0,
    /** Data i godzina przypomnienia (epoch millis); null = bez przypomnienia. */
    val remindAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ChecklistDao {

    @Query("SELECT * FROM checklist_items ORDER BY position, id")
    fun observeAll(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items ORDER BY position, id")
    suspend fun getAll(): List<ChecklistItem>

    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun getById(id: Long): ChecklistItem?

    @Insert
    suspend fun insert(item: ChecklistItem): Long

    /** REPLACE pozwala przywrócić usunięte punkty z tymi samymi id (Cofnij). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItem>)

    @Update
    suspend fun update(item: ChecklistItem)

    @Query("UPDATE checklist_items SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM checklist_items WHERE done = 1")
    suspend fun getDone(): List<ChecklistItem>

    @Query("DELETE FROM checklist_items WHERE done = 1")
    suspend fun deleteDone()

    @Query("SELECT COALESCE(MAX(position), -1) FROM checklist_items")
    suspend fun maxPosition(): Int

    @Query("UPDATE checklist_items SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    /** Nieodhaczone punkty z przypomnieniem w przyszłości – do planowania alarmów. */
    @Query("SELECT * FROM checklist_items WHERE done = 0 AND remindAt IS NOT NULL AND remindAt > :now")
    suspend fun getPendingReminders(now: Long): List<ChecklistItem>
}
