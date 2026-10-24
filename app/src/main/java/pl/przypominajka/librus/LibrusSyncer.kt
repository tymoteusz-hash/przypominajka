package pl.przypominajka.librus

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.przypominajka.data.ExternalSyncResult
import pl.przypominajka.data.Folder
import pl.przypominajka.data.LIBRUS_EXTERNAL_PREFIX
import pl.przypominajka.data.Reminder
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.TimetableLesson
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.ShortDateFormatter
import pl.przypominajka.util.dayName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface LibrusSyncOutcome {
    data class Success(val result: ExternalSyncResult, val upcomingEvents: Int) : LibrusSyncOutcome
    data class Failure(val message: String, val retryable: Boolean) : LibrusSyncOutcome
    data object NotConnected : LibrusSyncOutcome
}

/**
 * Pobiera z Librusa plan lekcji i nadchodzące sprawdziany, kartkówki (i wyjścia), a następnie:
 * - zapisuje plan w bazie (zakładka Plan + godziny "po lekcjach" dla wszystkich przypomnień szkolnych),
 * - zamienia wpisy z terminarza w przypomnienia: od N dni przed terminem (osobno dla sprawdzianu,
 *   kartkówki…), każdego dnia godzinę po ostatniej lekcji.
 */
class LibrusSyncer(
    private val repository: ReminderRepository,
    private val settings: LibrusSettings,
    private val schoolPrefs: SchoolPrefs,
) {
    private val mutex = Mutex()

    /** Pierwsze połączenie: login i hasło zapisujemy dopiero po udanym logowaniu. */
    suspend fun connect(login: String, password: String): LibrusSyncOutcome = mutex.withLock {
        withContext(Dispatchers.IO) {
            val log = SyncLog()
            val client = LibrusClient(log)
            try {
                client.login(login, password)
                settings.saveCredentials(login, password)
                synchronize(client, log)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.asLibrusException()
                log.add("Błąd: ${error.userMessage}")
                LibrusSyncOutcome.Failure(error.userMessage, error.retryable)
            }
        }
    }

    /** Synchronizacja zapisanym kontem (z ekranu albo w tle). */
    suspend fun sync(): LibrusSyncOutcome = mutex.withLock {
        withContext(Dispatchers.IO) {
            val credentials = settings.credentials()
            if (credentials == null) {
                LibrusSyncOutcome.NotConnected
            } else {
                val log = SyncLog()
                val client = LibrusClient(log)
                try {
                    client.login(credentials.login, credentials.password)
                    synchronize(client, log)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val error = e.asLibrusException()
                    log.add("Błąd: ${error.userMessage}")
                    settings.recordSync(ok = false, message = error.userMessage, log = log.text())
                    LibrusSyncOutcome.Failure(error.userMessage, error.retryable)
                }
            }
        }
    }

    private suspend fun synchronize(client: LibrusClient, log: SyncLog): LibrusSyncOutcome {
        return try {
            val today = LocalDate.now()

            val categories = client.getList("HomeWorks/Categories", "Categories")
                .associate { it.optLong("Id") to it.string("Name") }
            log.add("Kategorie terminarza: ${categories.values.joinToString().ifBlank { "brak" }}")

            val subjects = try {
                client.getList("Subjects", "Subjects").associate { it.optLong("Id") to it.string("Name") }
            } catch (e: LibrusException) {
                log.add("Bez nazw przedmiotów: ${e.userMessage}")
                emptyMap()
            }

            val rawEvents = client.getList("HomeWorks", "HomeWorks")
            val includeTrips = settings.includeTrips
            val events = rawEvents
                .mapNotNull { LibrusEvent.parse(it, categories, subjects) }
                .filter { it.date.isAfter(today) && !it.date.isAfter(today.plusDays(HORIZON_DAYS)) }
                .filter { event ->
                    val type = event.type
                    type != null && (includeTrips || type != SchoolType.TRIP)
                }
            log.add("Wpisy w terminarzu: ${rawEvents.size}, nadchodzące do przypomnienia: ${events.size}")

            // Plan lekcji: bieżący i dwa kolejne tygodnie + tygodnie, w których wypadają przypomnienia.
            val thisMonday = today.with(DayOfWeek.MONDAY)
            val weeks = (
                listOf(thisMonday, thisMonday.plusWeeks(1), thisMonday.plusWeeks(2)) +
                    events.map { it.date.minusDays(1).with(DayOfWeek.MONDAY) }
                ).distinct().sorted().take(MAX_TIMETABLE_WEEKS)
            val timetable = loadTimetable(client, weeks, subjects, log)
            if (timetable.isNotEmpty()) repository.replaceTimetable(timetable)

            val schoolTimes = repository.currentSchoolTimes()
            val desired = events.map { toReminder(it, today, schoolTimes) }

            val result = repository.applyExternalReminders(LIBRUS_EXTERNAL_PREFIX, desired, today)
            val message = buildString {
                append(
                    if (events.isEmpty()) {
                        "Brak nadchodzących sprawdzianów i kartkówek."
                    } else {
                        "Nadchodzące: ${events.size}. Dodano ${result.added}, zaktualizowano ${result.updated}, " +
                            "usunięto ${result.removed}."
                    },
                )
                if (timetable.isNotEmpty()) append(" Plan lekcji: ${timetable.size} tyg.")
            }
            log.add(message)
            settings.recordSync(ok = true, message = message, log = log.text())
            LibrusSyncOutcome.Success(result, events.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = e.asLibrusException()
            log.add("Błąd: ${error.userMessage}")
            settings.recordSync(ok = false, message = error.userMessage, log = log.text())
            LibrusSyncOutcome.Failure(error.userMessage, error.retryable)
        }
    }

    /** Plan dla podanych tygodni; brak planu nie przerywa synchronizacji. */
    private fun loadTimetable(
        client: LibrusClient,
        mondays: List<LocalDate>,
        subjects: Map<Long, String>,
        log: SyncLog,
    ): Map<LocalDate, List<TimetableLesson>> {
        val result = mutableMapOf<LocalDate, List<TimetableLesson>>()
        for (monday in mondays) {
            try {
                val lessons = parseTimetable(client.getJson("Timetables?weekStart=$monday"), subjects)
                    .filter { !it.date.isBefore(monday) && !it.date.isAfter(monday.plusDays(6)) }
                result[monday] = lessons
                log.add("Plan $monday: ${lessons.size} lekcji")
            } catch (e: LibrusException) {
                log.add("Plan lekcji ($monday) niedostępny – użyję godzin z ustawień.")
            }
        }
        return result
    }

    private fun toReminder(event: LibrusEvent, today: LocalDate, schoolTimes: SchoolTimes): Reminder {
        val type = event.type ?: SchoolType.TEST
        val leadDays = schoolPrefs.leadDays(type)
        val lastDay = event.date.minusDays(1)
        val start = maxOf(event.date.minusDays(leadDays.toLong()), today)
        val duration = ChronoUnit.DAYS.between(start, lastDay).toInt() + 1

        val title = event.subjectName
            ?: event.content.lineSequence().firstOrNull()?.take(60)?.takeIf { it.isNotBlank() }
            ?: event.categoryName.replaceFirstChar { it.uppercase() }

        val whenText = buildString {
            append("Termin: ${dayName(event.date)} ${event.date.format(ShortDateFormatter)}")
            event.lessonNo?.let { append(", lekcja $it") }
        }
        val note = listOf(whenText, event.content).filter { it.isNotBlank() }.joinToString("\n")

        return Reminder(
            title = title,
            note = note,
            folderId = Folder.SCHOOL_FOLDER_ID,
            startDate = start,
            durationDays = duration.coerceAtLeast(1),
            times = listOf(schoolTimes.afterSchoolTime(start)),
            schoolType = type.name,
            externalId = LIBRUS_EXTERNAL_PREFIX + event.id,
            eventDate = event.date,
            afterSchool = true,
        )
    }

    /** Nieznane wyjątki (np. nietypowa odpowiedź Librusa) zamieniamy na czytelny komunikat. */
    private fun Exception.asLibrusException(): LibrusException =
        this as? LibrusException ?: LibrusException(
            "Nieoczekiwany błąd synchronizacji (${javaClass.simpleName}).",
            retryable = true,
            cause = this,
        )

    private companion object {
        /** Jak daleko w przód patrzymy w terminarz. */
        const val HORIZON_DAYS = 60L
        const val MAX_TIMETABLE_WEEKS = 6
    }
}
