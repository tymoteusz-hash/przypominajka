package pl.przypominajka.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.przypominajka.alarm.NotificationHelper
import pl.przypominajka.alarm.ReminderSchedule
import pl.przypominajka.alarm.ReminderScheduler
import pl.przypominajka.alarm.TrainingReminder
import pl.przypominajka.util.SchoolPrefs
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Wynik scalenia wpisów z zewnętrznego źródła (Librus) z przypomnieniami w bazie. */
data class ExternalSyncResult(
    val added: Int,
    val updated: Int,
    val removed: Int,
)

/**
 * Jedyne miejsce, które zmienia dane przypomnień. Każda zmiana kończy się
 * przeliczeniem alarmu, więc stare alarmy są zawsze anulowane, a nowe planowane.
 *
 * Zasada: dla każdego przypomnienia w AlarmManagerze jest najwyżej JEDEN alarm –
 * najbliższe wystąpienie (lub odłożenie "za 10 min", jeśli jest wcześniej).
 * Po jego wywołaniu planowany jest kolejny.
 */
class ReminderRepository(
    private val db: AppDatabase,
    private val scheduler: ReminderScheduler,
    private val notifications: NotificationHelper,
    private val dismissedExternalIds: DismissedExternalIds,
    private val schoolPrefs: SchoolPrefs,
    private val trainingReminder: TrainingReminder,
) {
    private val folderDao = db.folderDao()
    private val reminderDao = db.reminderDao()
    private val timetableDao = db.timetableDao()

    fun observeFolders(): Flow<List<Folder>> = folderDao.observeAll()

    fun observeReminders(): Flow<List<Reminder>> = reminderDao.observeAll()

    suspend fun getReminder(id: Long): Reminder? = reminderDao.getById(id)

    // ---------------------------------------------------------------- plan lekcji

    /** Godziny końca lekcji (plan z Librusa + ustawienia) – aktualizowane na bieżąco. */
    fun observeSchoolTimes(): Flow<SchoolTimes> = combine(
        timetableDao.observeLessonEnds(LocalDate.now().minusDays(7)),
        schoolPrefs.observeChanges(),
    ) { ends, _ ->
        SchoolTimes(
            timetableEnds = ends.associate { it.date to it.endTime },
            weekdayEnds = schoolPrefs.weekdayEnds(),
            defaultEnd = schoolPrefs.endTime,
        )
    }

    suspend fun currentSchoolTimes(): SchoolTimes = SchoolTimes(
        timetableEnds = timetableDao.getLessonEnds(LocalDate.now().minusDays(1)).associate { it.date to it.endTime },
        weekdayEnds = schoolPrefs.weekdayEnds(),
        defaultEnd = schoolPrefs.endTime,
    )

    fun observeTimetable(from: LocalDate, to: LocalDate): Flow<List<TimetableLesson>> =
        timetableDao.observeRange(from, to)

    fun observeTimetableSize(): Flow<Int> = timetableDao.observeCount()

    /** Podmienia plan dla pobranych tygodni (poniedziałek → lekcje) i przelicza przypomnienia "po lekcjach". */
    suspend fun replaceTimetable(weeks: Map<LocalDate, List<TimetableLesson>>) {
        db.withTransaction {
            weeks.forEach { (monday, lessons) ->
                timetableDao.deleteRange(monday, monday.plusDays(6))
                timetableDao.insertAll(lessons)
            }
        }
        rescheduleAfterSchool()
    }

    suspend fun clearTimetable() {
        timetableDao.deleteAll()
        rescheduleAfterSchool()
    }

    /** Po zmianie planu lub godzin lekcji – nowe godziny dla przypomnień "po lekcjach". */
    suspend fun rescheduleAfterSchool() {
        val schoolTimes = currentSchoolTimes()
        val now = System.currentTimeMillis()
        reminderDao.getActive().filter { it.afterSchool }.forEach { scheduleNext(it, now, schoolTimes) }
        trainingReminder.reschedule(schoolTimes)
    }

    /** Przypomnienie o treningu na koniec lekcji – po zmianie dni albo po jego wywołaniu. */
    suspend fun rescheduleTraining() = trainingReminder.reschedule(currentSchoolTimes())

    // ---------------------------------------------------------------- przypomnienia

    /** Zapisuje nowe lub edytowane przypomnienie i planuje je od nowa. */
    suspend fun saveReminder(reminder: Reminder): Long {
        val toSave = reminder.copy(
            times = reminder.times.distinct().sorted(),
            finished = false,
            snoozedUntil = null,
        )
        val id = if (toSave.id == 0L) {
            reminderDao.insert(toSave)
        } else {
            reminderDao.update(toSave)
            toSave.id
        }
        notifications.cancel(id)
        refreshAlarm(id)
        return id
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        reminderDao.setEnabled(id, enabled)
        if (!enabled) notifications.cancel(id)
        refreshAlarm(id)
    }

    /** Usunięcie przez użytkownika. Wpis z Librusa nie wróci przy następnej synchronizacji. */
    suspend fun deleteReminder(reminder: Reminder) {
        removeReminder(reminder.id)
        reminder.externalId?.let { dismissedExternalIds.add(it) }
    }

    private suspend fun removeReminder(id: Long) {
        scheduler.cancel(id)
        notifications.cancel(id)
        reminderDao.deleteById(id)
    }

    // ---------------------------------------------------------------- wpisy z Librusa

    /**
     * Scala listę przypomnień wyliczonych z zewnętrznego źródła z tym, co jest w bazie:
     * - nowe wpisy dodaje (do folderu "Szkoła", a jeśli go usunięto – do "Inne"),
     * - istniejące aktualizuje (treść, termin), zachowując folder, przełącznik i własne godziny,
     * - nadchodzące wpisy, których źródło już nie zwraca (np. nauczyciel usunął sprawdzian), usuwa,
     * - pomija wpisy, które użytkownik sam usunął.
     */
    suspend fun applyExternalReminders(
        prefix: String,
        desired: List<Reminder>,
        today: LocalDate,
    ): ExternalSyncResult {
        val existing = reminderDao.getByExternalPrefix(prefix).associateBy { it.externalId }
        val dismissed = dismissedExternalIds.all()
        val targetFolderExists = folderDao.getById(Folder.SCHOOL_FOLDER_ID) != null
        val schoolTimes = currentSchoolTimes()
        var added = 0
        var updated = 0
        var removed = 0

        for (wanted in desired) {
            val externalId = wanted.externalId ?: continue
            if (externalId in dismissed) continue
            val old = existing[externalId]

            if (old == null) {
                val toInsert = withCatchUpTime(wanted, today, schoolTimes).copy(
                    id = 0,
                    folderId = if (targetFolderExists) wanted.folderId else Folder.OTHER_FOLDER_ID,
                )
                val id = reminderDao.insert(toInsert)
                refreshAlarm(id)
                added++
                continue
            }

            // Już się odbyło (np. przypomnienie "na teraz" dla wpisu dodanego późno) – nie wskrzeszamy go.
            if (old.finished && old.endDate == wanted.endDate) continue

            // Wpisy sprzed wersji z planem lekcji (bez eventDate) przejmują nowe ustawienia w całości.
            val keepUserTimes = old.eventDate != null && !old.afterSchool
            val merged = old.copy(
                title = wanted.title,
                note = wanted.note,
                startDate = wanted.startDate,
                durationDays = wanted.durationDays,
                schoolType = wanted.schoolType,
                eventDate = wanted.eventDate,
                afterSchool = if (keepUserTimes) false else wanted.afterSchool,
                times = if (keepUserTimes) old.times else wanted.times,
            )
            if (merged != old) {
                reminderDao.update(merged.copy(finished = false))
                refreshAlarm(old.id)
                updated++
            }
        }

        val wantedIds = desired.mapNotNull { it.externalId }.toSet()
        for (old in existing.values) {
            val stillUpcoming = old.endDate?.let { !it.isBefore(today) } ?: true
            if (old.externalId !in wantedIds && stillUpcoming) {
                removeReminder(old.id)
                removed++
            }
        }
        return ExternalSyncResult(added, updated, removed)
    }

    /** Po rozłączeniu konta: usuwa wszystkie przypomnienia z danego źródła. */
    suspend fun deleteExternalReminders(prefix: String): Int {
        val reminders = reminderDao.getByExternalPrefix(prefix)
        reminders.forEach { removeReminder(it.id) }
        dismissedExternalIds.removeWithPrefix(prefix)
        return reminders.size
    }

    /**
     * Wpis wykryty dopiero wtedy, gdy godzina przypomnienia w ostatnim możliwym dniu już minęła
     * (np. nauczyciel dodał sprawdzian na jutro wieczorem) – przypominamy za 2 minuty.
     */
    private fun withCatchUpTime(reminder: Reminder, today: LocalDate, schoolTimes: SchoolTimes): Reminder {
        if (reminder.startDate != today || reminder.durationDays != 1) return reminder
        val now = LocalTime.now()
        if (ReminderSchedule.timesOn(reminder, today, schoolTimes).any { it.isAfter(now) }) return reminder
        val soon = now.plusMinutes(2).withSecond(0).withNano(0)
        if (soon.isBefore(now)) return reminder // tuż przed północą – nie przeskakujemy na kolejny dzień
        return reminder.copy(times = listOf(soon), afterSchool = false)
    }

    // ---------------------------------------------------------------- foldery

    suspend fun saveFolder(folder: Folder) {
        if (folder.id == 0L) folderDao.insert(folder) else folderDao.update(folder)
    }

    /**
     * Usuwa folder. Jego przypomnienia są przenoszone do "Inne"
     * albo usuwane (razem z alarmami i powiadomieniami).
     */
    suspend fun deleteFolder(folder: Folder, deleteReminders: Boolean) {
        if (folder.isSystem) return
        val reminderIds = reminderDao.getIdsInFolder(folder.id)
        db.withTransaction {
            if (deleteReminders) {
                reminderDao.deleteInFolder(folder.id)
            } else {
                reminderDao.moveToFolder(folder.id, Folder.OTHER_FOLDER_ID)
            }
            folderDao.delete(folder)
        }
        if (deleteReminders) {
            reminderIds.forEach { id ->
                scheduler.cancel(id)
                notifications.cancel(id)
            }
        }
    }

    // ---------------------------------------------------------------- alarmy

    /** Wywoływane przez AlarmReceiver o zaplanowanej godzinie. */
    suspend fun onAlarmFired(id: Long, triggerAtMillis: Long) {
        val reminder = reminderDao.getById(id) ?: return
        if (!reminder.enabled || reminder.finished) return

        // Odniesienie: później z "teraz" i planowanego czasu – dzięki temu ten sam termin
        // nie zostanie zaplanowany ponownie, nawet jeśli alarm przyszedł minimalnie wcześniej.
        val reference = maxOf(System.currentTimeMillis(), triggerAtMillis)

        var current = reminder
        val snoozedUntil = reminder.snoozedUntil
        if (snoozedUntil != null && snoozedUntil <= reference) {
            reminderDao.clearSnooze(id)
            current = reminder.copy(snoozedUntil = null)
        }

        val folderName = folderDao.getById(current.folderId)?.name ?: "Inne"
        notifications.show(current, folderName)
        scheduleNext(current, reference)
    }

    /** Odłożenie o wybraną liczbę minut. */
    suspend fun snooze(id: Long, minutes: Long) =
        snoozeUntil(id, System.currentTimeMillis() + minutes * 60_000L)

    /** Jutro o tej samej lokalnej godzinie, także przy zmianie czasu. */
    suspend fun snoozeTomorrow(id: Long) =
        snoozeUntil(id, ZonedDateTime.now().plusDays(1).toInstant().toEpochMilli())

    private suspend fun snoozeUntil(id: Long, triggerAt: Long) {
        val reminder = reminderDao.getById(id) ?: return
        if (!reminder.enabled) return
        reminderDao.snooze(id, triggerAt)
        refreshAlarm(id)
    }

    /** "Zrobione" – zamyka powiadomienie i kasuje ewentualne odłożenie. */
    suspend fun markDone(id: Long) {
        notifications.cancel(id)
        val reminder = reminderDao.getById(id) ?: return
        if (reminder.snoozedUntil != null) {
            reminderDao.clearSnooze(id)
            refreshAlarm(id)
        }
    }

    /** Anuluje stary alarm i – jeśli przypomnienie jest aktywne – planuje najbliższy. */
    suspend fun refreshAlarm(id: Long) {
        scheduler.cancel(id)
        val reminder = reminderDao.getById(id) ?: return
        if (!reminder.enabled || reminder.finished) return
        scheduleNext(reminder, System.currentTimeMillis())
    }

    /** Po restarcie telefonu, zmianie czasu, aktualizacji aplikacji itp. */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        val schoolTimes = currentSchoolTimes()
        reminderDao.getActive().forEach { reminder ->
            scheduleNext(reminder, now, schoolTimes)
        }
        trainingReminder.reschedule(schoolTimes)
    }

    private suspend fun scheduleNext(reminder: Reminder, afterMillis: Long, schoolTimes: SchoolTimes? = null) {
        val times = if (reminder.afterSchool) schoolTimes ?: currentSchoolTimes() else null
        val next = ReminderSchedule.nextTrigger(reminder, afterMillis, times)
        if (next == null) {
            // Nie ma już kolejnych terminów – przypomnienie się zakończyło.
            scheduler.cancel(reminder.id)
            reminderDao.setFinished(reminder.id, true)
        } else {
            scheduler.schedule(reminder.id, next)
        }
    }
}
