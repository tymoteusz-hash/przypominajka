package pl.przypominajka.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.przypominajka.alarm.ReminderSchedule
import pl.przypominajka.data.ChecklistItem
import pl.przypominajka.data.ChecklistRepository
import pl.przypominajka.data.Folder
import pl.przypominajka.data.Reminder
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.endDate
import pl.przypominajka.data.isFinishedOn
import pl.przypominajka.librus.LibrusSettings
import pl.przypominajka.ui.checklist.ChecklistSummary
import java.time.LocalDate
import java.time.LocalTime

data class ReminderItem(
    val reminder: Reminder,
    val folder: Folder?,
    /** Najbliższe powiadomienie (epoch millis) – tylko dla aktywnych. */
    val nextTriggerAt: Long?,
)

data class HomeSection(
    val title: String,
    val items: List<ReminderItem>,
)

/** Sprawdzian, kartkówka… z datą – do poziomej listy "W szkole wkrótce". */
data class UpcomingEvent(
    val reminder: Reminder,
    val type: SchoolType,
    val date: LocalDate,
)

/** Jedna pozycja listy "Dziś": godzina i tytuł. */
data class AgendaEntry(
    val reminderId: Long,
    val time: LocalTime,
    val title: String,
    val type: SchoolType?,
)

data class TodaySummary(
    val remainingToday: Int = 0,
    /** Wszystkie dzisiejsze powiadomienia (z tymi, które już były) – do pierścienia postępu. */
    val totalToday: Int = 0,
    val nextTitle: String? = null,
    val nextAt: Long? = null,
    /** Najbliższe dzisiejsze powiadomienia (najwyżej [AGENDA_SIZE]). */
    val agenda: List<AgendaEntry> = emptyList(),
    /** Liczba aktywnych przypomnień (wszystkie foldery). */
    val activeCount: Int = 0,
) {
    val passedToday: Int get() = (totalToday - remainingToday).coerceAtLeast(0)
}

const val AGENDA_SIZE = 4

/** Ile nieodhaczonych punktów checklisty pokazać na stronie głównej. */
const val CHECKLIST_PREVIEW_SIZE = 3

data class HomeUiState(
    val folders: List<Folder> = emptyList(),
    /** null = "Wszystkie". */
    val selectedFolderId: Long? = null,
    val sections: List<HomeSection> = emptyList(),
    val today: TodaySummary = TodaySummary(),
    val upcoming: List<UpcomingEvent> = emptyList(),
    val showLibrusPrompt: Boolean = false,
    /** Checklista: ile zostało do odhaczenia (ze wszystkich punktów). */
    val checklist: ChecklistSummary = ChecklistSummary(),
    /** Pierwsze nieodhaczone punkty (najwyżej [CHECKLIST_PREVIEW_SIZE]) w kolejności z listy. */
    val checklistPreview: List<ChecklistItem> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

private data class HomeData(
    val folders: List<Folder>,
    val reminders: List<Reminder>,
    val schoolTimes: SchoolTimes,
    val checklist: List<ChecklistItem>,
)

class HomeViewModel(
    private val repository: ReminderRepository,
    private val librusSettings: LibrusSettings,
    private val checklistRepository: ChecklistRepository,
) : ViewModel() {

    private val selectedFolderId = MutableStateFlow<Long?>(null)

    /** Co minutę odświeżamy "Następne: …", liczniki i "Zostało X dni". */
    private val minuteTicker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }

    private val data: Flow<HomeData> = combine(
        repository.observeFolders(),
        repository.observeReminders(),
        repository.observeSchoolTimes(),
        checklistRepository.observeItems(),
    ) { folders, reminders, schoolTimes, checklist -> HomeData(folders, reminders, schoolTimes, checklist) }

    val uiState: StateFlow<HomeUiState> = combine(
        data,
        selectedFolderId,
        librusSettings.observe(),
        minuteTicker,
    ) { data, selected, librus, _ ->
        buildState(data, selected, showLibrusPrompt = !librus.connected && !librus.promptDismissed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private fun buildState(data: HomeData, selected: Long?, showLibrusPrompt: Boolean): HomeUiState {
        val folders = data.folders
        // Jeśli wybrany folder został usunięty, wracamy do "Wszystkie".
        val validSelected = selected?.takeIf { id -> folders.any { it.id == id } }
        val foldersById = folders.associateBy { it.id }
        val today = LocalDate.now()
        val now = System.currentTimeMillis()

        fun Reminder.isActive() = enabled && !isFinishedOn(today)

        val allItems = data.reminders.map { reminder ->
            ReminderItem(
                reminder = reminder,
                folder = foldersById[reminder.folderId],
                nextTriggerAt = if (reminder.isActive()) {
                    ReminderSchedule.nextTrigger(reminder, now, data.schoolTimes)
                } else {
                    null
                },
            )
        }

        // Podsumowanie dnia liczymy ze wszystkich folderów.
        val activeAll = allItems.filter { it.reminder.isActive() }
        val todayEntries = activeAll.flatMap { item ->
            ReminderSchedule.upcomingToday(item.reminder, now, data.schoolTimes).map { time ->
                AgendaEntry(
                    reminderId = item.reminder.id,
                    time = time,
                    title = item.reminder.title,
                    type = SchoolType.fromName(item.reminder.schoolType),
                )
            }
        }.sortedWith(compareBy({ it.time }, { it.title.lowercase() }))
        val remainingToday = todayEntries.size
        val totalToday = activeAll.sumOf { ReminderSchedule.allToday(it.reminder, now, data.schoolTimes).size }
        val next = activeAll.filter { it.nextTriggerAt != null }.minByOrNull { it.nextTriggerAt ?: Long.MAX_VALUE }

        val upcoming = data.reminders
            .mapNotNull { reminder ->
                val type = SchoolType.fromName(reminder.schoolType) ?: return@mapNotNull null
                val date = reminder.eventDate ?: return@mapNotNull null
                if (date.isBefore(today) || date.isAfter(today.plusDays(UPCOMING_DAYS))) return@mapNotNull null
                UpcomingEvent(reminder, type, date)
            }
            .sortedBy { it.date }
            .take(8)

        val items = allItems.filter { validSelected == null || it.reminder.folderId == validSelected }
        val active = items
            .filter { it.reminder.isActive() }
            .sortedWith(
                compareBy<ReminderItem>({ it.nextTriggerAt ?: Long.MAX_VALUE }, { it.reminder.title.lowercase() }),
            )
        val disabled = items
            .filter { !it.reminder.enabled && !it.reminder.isFinishedOn(today) }
            .sortedBy { it.reminder.title.lowercase() }
        val finished = items
            .filter { it.reminder.isFinishedOn(today) }
            .sortedByDescending { it.reminder.endDate ?: it.reminder.startDate }

        return HomeUiState(
            folders = folders,
            selectedFolderId = validSelected,
            sections = listOf(
                HomeSection("Aktywne", active),
                HomeSection("Wyłączone", disabled),
                HomeSection("Zakończone", finished),
            ).filter { it.items.isNotEmpty() },
            today = TodaySummary(
                remainingToday = remainingToday,
                totalToday = totalToday,
                nextTitle = next?.reminder?.title,
                nextAt = next?.nextTriggerAt,
                agenda = todayEntries.take(AGENDA_SIZE),
                activeCount = activeAll.size,
            ),
            upcoming = upcoming,
            showLibrusPrompt = showLibrusPrompt,
            checklist = ChecklistSummary.of(data.checklist),
            checklistPreview = data.checklist.filter { !it.done }.take(CHECKLIST_PREVIEW_SIZE),
            isLoading = false,
        )
    }

    fun selectFolder(folderId: Long?) {
        selectedFolderId.value = folderId
    }

    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(reminder.id, enabled) }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch { repository.deleteReminder(reminder) }
    }

    /** Odhaczenie punktu checklisty prosto ze strony głównej. */
    fun toggleChecklistItem(id: Long) {
        viewModelScope.launch { checklistRepository.toggle(id) }
    }

    /** Szybkie dopisanie punktu checklisty ze strony głównej. */
    fun addChecklistItem(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { checklistRepository.add(text) }
    }

    fun dismissLibrusPrompt() {
        librusSettings.promptDismissed = true
    }

    private companion object {
        const val UPCOMING_DAYS = 21L
    }
}
