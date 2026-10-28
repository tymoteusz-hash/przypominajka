package pl.przypominajka.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.przypominajka.alarm.RestState
import pl.przypominajka.alarm.RestTimer
import pl.przypominajka.alarm.TrainingReminder
import pl.przypominajka.alarm.TrainingSettings
import pl.przypominajka.alarm.TrainingTrigger
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.WorkoutExercise
import pl.przypominajka.data.WorkoutFolder
import pl.przypominajka.data.WorkoutPlan
import pl.przypominajka.data.WorkoutRepository
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

data class WorkoutPlanUi(
    val plan: WorkoutPlan,
    val exercises: List<WorkoutExercise>,
    val expanded: Boolean,
) {
    val doneCount: Int get() = exercises.count { it.done }
    val total: Int get() = exercises.size
}

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val folders: List<WorkoutFolder> = emptyList(),
    val selectedFolder: WorkoutFolder? = null,
    val plans: List<WorkoutPlanUi> = emptyList(),
) {
    val doneCount: Int get() = plans.sumOf { it.doneCount }
    val total: Int get() = plans.sumOf { it.total }
}

/** Gotowe podziały treningowe do szybkiego dodania. */
val WORKOUT_PRESETS: List<Pair<String, List<String>>> = listOf(
    "Push / Pull / Legs" to listOf("Push", "Pull", "Legs"),
    "Upper / Lower" to listOf("Upper", "Lower"),
    "Full body A / B" to listOf("Full body A", "Full body B"),
)

/** Najkrótsza i najdłuższa przerwa do ustawienia w okienku „Własny czas” (sekundy). */
const val REST_MIN_SECONDS = 5
const val REST_MAX_SECONDS = 60 * 60

/** Czas z okienka „Własny czas” (puste pole = 0) albo null, gdy poza zakresem. */
fun parseRestTime(minutes: String, seconds: String): Int? {
    val m = if (minutes.isBlank()) 0 else minutes.toIntOrNull() ?: return null
    val s = if (seconds.isBlank()) 0 else seconds.toIntOrNull() ?: return null
    if (s > 59) return null
    return (m * 60 + s).takeIf { it in REST_MIN_SECONDS..REST_MAX_SECONDS }
}

/** Podpowiedź pod polami okienka „Własny czas”. */
fun restTimeHint(total: Int?): String =
    if (total == null) "Od 5 s do 60 min, sekundy 0–59" else "Ten czas zostanie też w szybkim wyborze"

/** Karta "Przypomnienie o treningu": ustawienia i najbliższy termin. */
data class TrainingUi(
    val settings: TrainingSettings = TrainingSettings(),
    val next: TrainingTrigger? = null,
)

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val restTimer: RestTimer,
    private val trainingReminder: TrainingReminder,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    val training: StateFlow<TrainingUi> = combine(
        trainingReminder.settings,
        reminderRepository.observeSchoolTimes(),
    ) { settings, schoolTimes ->
        TrainingUi(settings, TrainingReminder.nextTrigger(settings, schoolTimes, LocalDateTime.now()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingUi(trainingReminder.settings.value))

    fun setTrainingEnabled(enabled: Boolean) {
        trainingReminder.setEnabled(enabled)
        viewModelScope.launch { reminderRepository.rescheduleTraining() }
    }

    fun toggleTrainingDay(day: DayOfWeek) {
        trainingReminder.toggleDay(day)
        viewModelScope.launch { reminderRepository.rescheduleTraining() }
    }

    fun setTrainingWeekendTime(day: DayOfWeek, time: LocalTime) {
        trainingReminder.setWeekendTime(day, time)
        viewModelScope.launch { reminderRepository.rescheduleTraining() }
    }

    /** Trwająca przerwa między seriami (null = brak). */
    val rest: StateFlow<RestState?> = restTimer.state

    fun startRest(seconds: Int) = restTimer.start(seconds)

    /** Czasy przerwy do szybkiego wyboru (sekundy, rosnąco). */
    val restPresets: StateFlow<List<Int>> = restTimer.presets

    /** Start przerwy o własnej długości – ten czas zostaje też w szybkim wyborze. */
    fun startCustomRest(seconds: Int) {
        restTimer.addPreset(seconds)
        restTimer.start(seconds)
    }

    fun removeRestPreset(seconds: Int) = restTimer.removePreset(seconds)

    fun extendRest(seconds: Int) = restTimer.extend(seconds)

    fun stopRest() = restTimer.stop()

    private val selectedFolderId = MutableStateFlow<Long?>(null)

    /** Zwinięte plany (domyślnie wszystkie są rozwinięte). */
    private val collapsed = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<WorkoutUiState> = combine(
        repository.observeFolders(),
        repository.observePlans(),
        repository.observeExercises(),
        selectedFolderId,
        collapsed,
    ) { folders, plans, exercises, selectedId, collapsedIds ->
        val selected = folders.firstOrNull { it.id == selectedId } ?: folders.firstOrNull()
        val exercisesByPlan = exercises.groupBy { it.planId }
        WorkoutUiState(
            isLoading = false,
            folders = folders,
            selectedFolder = selected,
            plans = plans
                .filter { it.folderId == selected?.id }
                .map { WorkoutPlanUi(it, exercisesByPlan[it.id].orEmpty(), expanded = it.id !in collapsedIds) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    fun selectFolder(id: Long) {
        selectedFolderId.value = id
    }

    fun toggleExpanded(planId: Long) = collapsed.update { if (planId in it) it - planId else it + planId }

    // ---------------------------------------------------------------- foldery

    fun addFolder(name: String, colorArgb: Int) {
        viewModelScope.launch { selectedFolderId.value = repository.addFolder(name.trim(), colorArgb) }
    }

    fun updateFolder(folder: WorkoutFolder, name: String, colorArgb: Int) {
        viewModelScope.launch { repository.updateFolder(folder.copy(name = name.trim(), colorArgb = colorArgb)) }
    }

    fun deleteFolder(folder: WorkoutFolder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            selectedFolderId.value = null
        }
    }

    // ---------------------------------------------------------------- plany

    fun addPlans(names: List<String>) {
        val folderId = state.value.selectedFolder?.id ?: return
        viewModelScope.launch { repository.addPlans(folderId, names.map { it.trim() }.filter { it.isNotEmpty() }) }
    }

    fun renamePlan(plan: WorkoutPlan, name: String) {
        viewModelScope.launch { repository.updatePlan(plan.copy(name = name.trim())) }
    }

    fun deletePlan(plan: WorkoutPlan) {
        viewModelScope.launch { repository.deletePlan(plan) }
    }

    fun resetPlan(planId: Long) {
        viewModelScope.launch { repository.resetPlan(planId) }
    }

    // ---------------------------------------------------------------- ćwiczenia

    fun addExercise(planId: Long, name: String, details: String) {
        viewModelScope.launch { repository.addExercise(planId, name.trim(), details.trim()) }
    }

    fun updateExercise(exercise: WorkoutExercise, name: String, details: String) {
        viewModelScope.launch { repository.updateExercise(exercise.copy(name = name.trim(), details = details.trim())) }
    }

    fun deleteExercise(exercise: WorkoutExercise) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    fun setDone(exercise: WorkoutExercise, done: Boolean) {
        viewModelScope.launch { repository.setDone(exercise.id, done) }
    }
}
