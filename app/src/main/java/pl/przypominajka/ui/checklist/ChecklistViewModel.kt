package pl.przypominajka.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.przypominajka.data.ChecklistItem
import pl.przypominajka.data.ChecklistRepository
import pl.przypominajka.util.pointsText

/** Ile punktów zostało do odhaczenia. */
data class ChecklistSummary(
    val left: Int = 0,
    val total: Int = 0,
) {
    val done: Int get() = total - left

    /** Lista nie jest pusta i wszystko odhaczone. */
    val allDone: Boolean get() = total > 0 && left == 0

    /** 0–1 – np. do złotego pierścienia w Dzwonie. */
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total

    companion object {
        fun of(items: List<ChecklistItem>): ChecklistSummary =
            ChecklistSummary(left = items.count { !it.done }, total = items.size)
    }
}

/**
 * Informacja do paska „Cofnij” po usunięciu punktów. [stamp] jest inny przy każdym usunięciu
 * (np. jako klucz LaunchedEffect), [items] wracają na listę po [ChecklistViewModel.undo].
 */
data class UndoInfo(
    val stamp: Long,
    val message: String,
    val items: List<ChecklistItem>,
)

/**
 * Checklista – wspólna logika dla wszystkich wyglądów (Kartka, Dzwon, Tablica mają tylko własny układ).
 * Kolejność [items] jak w bazie (pozycja, potem id), razem z odhaczonymi.
 */
class ChecklistViewModel(private val repository: ChecklistRepository) : ViewModel() {

    /** null = jeszcze nie wczytano (żeby nie mignął pusty stan). */
    private val loaded: StateFlow<List<ChecklistItem>?> = repository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<List<ChecklistItem>> = loaded
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoaded: StateFlow<Boolean> = loaded
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val summary: StateFlow<ChecklistSummary> = items
        .map { ChecklistSummary.of(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistSummary())

    private val _undoState = MutableStateFlow<UndoInfo?>(null)

    /** Ostatnie usunięcie, które można cofnąć; samo znika po [UNDO_TIMEOUT_MS]. */
    val undoState: StateFlow<UndoInfo?> = _undoState.asStateFlow()

    private var undoTimeout: Job? = null
    private var undoCounter = 0L

    /** Dodaje punkt na końcu listy; pusty tekst jest pomijany. [remindAt] – epoch millis albo null. */
    fun add(text: String, remindAt: Long? = null) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.add(text, remindAt) }
    }

    /** Zapis zmienionego punktu (tekst, godzina, odhaczenie). */
    fun edit(item: ChecklistItem) {
        viewModelScope.launch { repository.edit(item) }
    }

    fun toggle(id: Long) {
        viewModelScope.launch { repository.toggle(id) }
    }

    fun setDone(id: Long, done: Boolean) {
        viewModelScope.launch { repository.setDone(id, done) }
    }

    /** Usuwa punkt z możliwością „Cofnij”. */
    fun delete(id: Long) {
        viewModelScope.launch {
            val item = repository.getItem(id) ?: return@launch
            repository.delete(id)
            offerUndo(listOf(item))
        }
    }

    /** Usuwa wszystkie odhaczone punkty z możliwością „Cofnij”. */
    fun clearDone() {
        viewModelScope.launch {
            val removed = repository.clearDone()
            if (removed.isNotEmpty()) offerUndo(removed)
        }
    }

    /** Przywraca ostatnio usunięte punkty (z tymi samymi pozycjami i przypomnieniami). */
    fun undo() {
        val info = _undoState.value ?: return
        hideUndo()
        viewModelScope.launch { repository.restore(info.items) }
    }

    /** Pasek „Cofnij” zamknięty bez cofania. */
    fun dismissUndo() = hideUndo()

    /** Nowa kolejność po przeciągnięciu (id w nowym porządku; może to być tylko część listy). */
    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorder(orderedIds) }
    }

    private fun offerUndo(items: List<ChecklistItem>) {
        undoTimeout?.cancel()
        val info = UndoInfo(stamp = ++undoCounter, message = "Usunięto ${pointsText(items.size)}", items = items)
        _undoState.value = info
        undoTimeout = viewModelScope.launch {
            delay(UNDO_TIMEOUT_MS)
            if (_undoState.value === info) _undoState.value = null
        }
    }

    private fun hideUndo() {
        undoTimeout?.cancel()
        undoTimeout = null
        _undoState.value = null
    }

    companion object {
        const val UNDO_TIMEOUT_MS = 6_000L
    }
}
