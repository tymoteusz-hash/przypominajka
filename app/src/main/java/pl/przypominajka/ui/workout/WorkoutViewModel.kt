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
import pl.przypominajka.data.WorkoutExercise
import pl.przypominajka.data.WorkoutFolder
import pl.przypominajka.data.WorkoutPlan
import pl.przypominajka.data.WorkoutRepository

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

/** Długości przerwy do szybkiego wyboru (sekundy). */
val REST_PRESETS: List<Int> = listOf(60, 90, 120, 180)

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val restTimer: RestTimer,
) : ViewModel() {

    /** Trwająca przerwa między seriami (null = brak). */
    val rest: StateFlow<RestState?> = restTimer.state

    fun startRest(seconds: Int) = restTimer.start(seconds)

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
