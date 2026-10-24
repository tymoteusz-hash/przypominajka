package pl.przypominajka.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.TimetableLesson
import pl.przypominajka.librus.LibrusSettings
import pl.przypominajka.librus.LibrusSyncer
import java.time.DayOfWeek
import java.time.LocalDate

data class DayEvent(val reminderId: Long, val type: SchoolType, val title: String)

data class PlanUiState(
    val weekStart: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val selectedDay: LocalDate = LocalDate.now(),
    val lessons: List<TimetableLesson> = emptyList(),
    val events: Map<LocalDate, List<DayEvent>> = emptyMap(),
    val schoolTimes: SchoolTimes = SchoolTimes(),
    val librusConnected: Boolean = false,
    val hasAnyTimetable: Boolean = false,
    val isSyncing: Boolean = false,
) {
    /** Pn–pt, a sobota/niedziela tylko gdy są w nich lekcje. */
    val days: List<LocalDate>
        get() = (0L..6L).map { weekStart.plusDays(it) }.filter { day ->
            day.dayOfWeek.value <= 5 || lessons.any { it.date == day }
        }

    val lessonsOfSelectedDay: List<TimetableLesson>
        get() = lessons.filter { it.date == selectedDay }.sortedWith(compareBy({ it.lessonNo }, { it.startTime }))
}

private data class Selection(val weekStart: LocalDate, val selectedDay: LocalDate, val isSyncing: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(
    private val repository: ReminderRepository,
    private val librusSettings: LibrusSettings,
    private val librusSyncer: LibrusSyncer,
) : ViewModel() {

    private val selection = MutableStateFlow(initialSelection())

    private val lessons = selection.flatMapLatest { repository.observeTimetable(it.weekStart, it.weekStart.plusDays(6)) }

    val state: StateFlow<PlanUiState> = combine(
        selection,
        lessons,
        repository.observeReminders(),
        repository.observeSchoolTimes(),
        combine(librusSettings.observe(), repository.observeTimetableSize()) { status, size -> status.connected to (size > 0) },
    ) { selection, lessons, reminders, schoolTimes, (connected, hasTimetable) ->
        val weekEnd = selection.weekStart.plusDays(6)
        val events = reminders
            .mapNotNull { reminder ->
                val type = SchoolType.fromName(reminder.schoolType) ?: return@mapNotNull null
                val date = reminder.eventDate ?: return@mapNotNull null
                if (date.isBefore(selection.weekStart) || date.isAfter(weekEnd)) return@mapNotNull null
                date to DayEvent(reminder.id, type, reminder.title)
            }
            .groupBy({ it.first }, { it.second })
        PlanUiState(
            weekStart = selection.weekStart,
            selectedDay = selection.selectedDay,
            lessons = lessons,
            events = events,
            schoolTimes = schoolTimes,
            librusConnected = connected,
            hasAnyTimetable = hasTimetable,
            isSyncing = selection.isSyncing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun selectDay(day: LocalDate) = selection.update { it.copy(selectedDay = day) }

    fun changeWeek(delta: Long) = selection.update {
        val newStart = it.weekStart.plusWeeks(delta)
        val today = LocalDate.now()
        val day = if (today.with(DayOfWeek.MONDAY) == newStart) today else newStart
        it.copy(weekStart = newStart, selectedDay = day)
    }

    fun sync() {
        if (selection.value.isSyncing) return
        selection.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            librusSyncer.sync()
            selection.update { it.copy(isSyncing = false) }
        }
    }

    private fun initialSelection(): Selection {
        val today = LocalDate.now()
        // W weekend od razu pokazujemy najbliższy poniedziałek.
        val day = when (today.dayOfWeek) {
            DayOfWeek.SATURDAY -> today.plusDays(2)
            DayOfWeek.SUNDAY -> today.plusDays(1)
            else -> today
        }
        return Selection(weekStart = day.with(DayOfWeek.MONDAY), selectedDay = day, isSyncing = false)
    }
}
