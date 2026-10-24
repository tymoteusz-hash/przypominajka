package pl.przypominajka.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND finished = 0")
    suspend fun getActive(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE externalId LIKE :prefix || '%'")
    suspend fun getByExternalPrefix(prefix: String): List<Reminder>

    @Query("SELECT id FROM reminders WHERE folderId = :folderId")
    suspend fun getIdsInFolder(folderId: Long): List<Long>

    /** REPLACE pozwala też przywrócić usunięte przypomnienie z tym samym id (Cofnij). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reminders SET enabled = :enabled, snoozedUntil = NULL WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE reminders SET finished = :finished WHERE id = :id")
    suspend fun setFinished(id: Long, finished: Boolean)

    /** Odłożenie przywraca też przypomnienie, które właśnie zakończyło się ostatnim powiadomieniem. */
    @Query("UPDATE reminders SET snoozedUntil = :until, finished = 0 WHERE id = :id")
    suspend fun snooze(id: Long, until: Long)

    @Query("UPDATE reminders SET snoozedUntil = NULL WHERE id = :id")
    suspend fun clearSnooze(id: Long)

    @Query("UPDATE reminders SET folderId = :toFolderId WHERE folderId = :fromFolderId")
    suspend fun moveToFolder(fromFolderId: Long, toFolderId: Long)

    @Query("DELETE FROM reminders WHERE folderId = :folderId")
    suspend fun deleteInFolder(folderId: Long)
}
