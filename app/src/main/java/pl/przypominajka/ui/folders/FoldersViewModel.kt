package pl.przypominajka.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.przypominajka.data.Folder
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.isSystem

data class FolderWithCount(
    val folder: Folder,
    val reminderCount: Int,
)

class FoldersViewModel(private val repository: ReminderRepository) : ViewModel() {

    val folders: StateFlow<List<FolderWithCount>> = combine(
        repository.observeFolders(),
        repository.observeReminders(),
    ) { folders, reminders ->
        val counts = reminders.groupingBy { it.folderId }.eachCount()
        folders.map { FolderWithCount(it, counts[it.id] ?: 0) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** [existing] == null – nowy folder. Nazwy folderu systemowego "Inne" nie zmieniamy. */
    fun saveFolder(existing: Folder?, name: String, colorArgb: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val folder = if (existing == null) {
            Folder(name = trimmed, colorArgb = colorArgb)
        } else {
            existing.copy(
                name = if (existing.isSystem) existing.name else trimmed,
                colorArgb = colorArgb,
            )
        }
        viewModelScope.launch { repository.saveFolder(folder) }
    }

    fun deleteFolder(folder: Folder, deleteReminders: Boolean) {
        viewModelScope.launch { repository.deleteFolder(folder, deleteReminders) }
    }
}
