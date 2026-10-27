package pl.przypominajka.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import pl.przypominajka.alarm.ChecklistAlarms
import pl.przypominajka.alarm.NotificationHelper

/**
 * Jedyne miejsce, które zmienia checklistę. Zmiana terminu lub odhaczenia przelicza alarm punktu:
 * stary jest anulowany, a nowy planowany tylko dla nieodhaczonego punktu z godziną w przyszłości.
 */
class ChecklistRepository(
    private val db: AppDatabase,
    private val alarms: ChecklistAlarms,
    private val notifications: NotificationHelper,
) {
    private val dao = db.checklistDao()

    fun observeItems(): Flow<List<ChecklistItem>> = dao.observeAll()

    suspend fun getItem(id: Long): ChecklistItem? = dao.getById(id)

    /** Dodaje punkt na końcu listy. Pusty tekst jest ignorowany (zwraca -1). */
    suspend fun add(text: String, remindAt: Long? = null): Long {
        val clean = text.trim()
        if (clean.isEmpty()) return -1L
        val item = db.withTransaction {
            val toInsert = ChecklistItem(text = clean, position = dao.maxPosition() + 1, remindAt = remindAt)
            toInsert.copy(id = dao.insert(toInsert))
        }
        syncAlarm(item)
        return item.id
    }

    /** Zapisuje zmieniony punkt (tekst, godzina, odhaczenie) i w razie zmiany terminu lub odhaczenia przelicza alarm. */
    suspend fun edit(item: ChecklistItem) {
        val clean = item.text.trim()
        if (clean.isEmpty()) return
        val toSave = item.copy(text = clean)
        val previous = db.withTransaction {
            val current = dao.getById(item.id) ?: return@withTransaction null
            dao.update(toSave)
            current
        } ?: return
        if (previous.remindAt != toSave.remindAt || previous.done != toSave.done) {
            notifications.cancelChecklistItem(toSave.id)
            syncAlarm(toSave)
        }
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val item = db.withTransaction {
            dao.setDone(id, done)
            dao.getById(id)
        } ?: return
        afterDoneChanged(item)
    }

    /** Przełącza odhaczenie punktu (np. z widżetu na ekranie głównym). */
    suspend fun toggle(id: Long) {
        val item = db.withTransaction {
            dao.getById(id)?.let { current ->
                val flipped = current.copy(done = !current.done)
                dao.setDone(id, flipped.done)
                flipped
            }
        } ?: return
        afterDoneChanged(item)
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
        alarms.cancel(id)
        notifications.cancelChecklistItem(id)
    }

    /**
     * Przenosi punkt na liście (kolejność jak w [observeItems], razem z odhaczonymi)
     * i zapisuje pozycje od nowa jako 0..n-1.
     */
    suspend fun move(fromIndex: Int, toIndex: Int) {
        db.withTransaction {
            val list = dao.getAll().toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                list.add(toIndex, list.removeAt(fromIndex))
                list.forEachIndexed { index, item ->
                    if (item.position != index) dao.updatePosition(item.id, index)
                }
            }
        }
    }

    /**
     * Nowa kolejność po przeciągnięciu: [orderedIds] w podanym porządku (może to być tylko część listy,
     * np. same nieodhaczone). Pozostałe punkty zostają na swoich pozycjach; zapisujemy 0..n-1
     * tylko tam, gdzie pozycja się zmieniła.
     */
    suspend fun reorder(orderedIds: List<Long>) {
        if (orderedIds.isEmpty()) return
        db.withTransaction {
            val rank = HashMap<Long, Int>()
            orderedIds.forEachIndexed { index, id -> rank.getOrPut(id) { index } }
            dao.getAll()
                .sortedWith(compareBy<ChecklistItem>({ rank[it.id] ?: it.position }, { it.id }))
                .forEachIndexed { index, item ->
                    if (item.position != index) dao.updatePosition(item.id, index)
                }
        }
    }

    /** Usuwa odhaczone punkty i zwraca je, żeby ekran mógł zaproponować „Cofnij” ([restore]). */
    suspend fun clearDone(): List<ChecklistItem> {
        val removed = db.withTransaction {
            val done = dao.getDone()
            dao.deleteDone()
            done
        }
        removed.forEach {
            alarms.cancel(it.id)
            notifications.cancelChecklistItem(it.id)
        }
        return removed
    }

    /** Przywraca usunięte punkty z tymi samymi id (i pozycjami) oraz ich przyszłe przypomnienia. */
    suspend fun restore(items: List<ChecklistItem>) {
        if (items.isEmpty()) return
        dao.insertAll(items)
        items.forEach { scheduleIfPending(it) }
    }

    /** Po restarcie telefonu itp. – planuje od nowa alarmy wszystkich nieodhaczonych punktów z godziną w przyszłości. */
    suspend fun rescheduleAll() {
        dao.getPendingReminders(System.currentTimeMillis()).forEach { scheduleIfPending(it) }
    }

    /** Odhaczony punkt traci alarm i powiadomienie; odznaczony dostaje alarm z powrotem (jeśli godzina jeszcze nie minęła). */
    private fun afterDoneChanged(item: ChecklistItem) {
        if (item.done) notifications.cancelChecklistItem(item.id)
        syncAlarm(item)
    }

    /** Alarm istnieje tylko dla nieodhaczonego punktu z godziną w przyszłości – w pozostałych przypadkach jest anulowany. */
    private fun syncAlarm(item: ChecklistItem) {
        if (!scheduleIfPending(item)) alarms.cancel(item.id)
    }

    private fun scheduleIfPending(item: ChecklistItem): Boolean {
        val at = item.remindAt ?: return false
        if (item.done || at <= System.currentTimeMillis()) return false
        alarms.schedule(item.id, at)
        return true
    }
}
