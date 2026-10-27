@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.przypominajka.ui.tablica.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.Folder
import pl.przypominajka.data.FolderColors
import pl.przypominajka.data.isSystem
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.folders.FoldersViewModel
import pl.przypominajka.ui.folders.FolderWithCount
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.util.remindersText

private data class FolderEditRequest(val folder: Folder?)

@Composable
fun TablicaFoldersScreen(
    onBack: () -> Unit,
    viewModel: FoldersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FolderEditRequest?>(null) }
    var deleting by remember { mutableStateOf<FolderWithCount?>(null) }
    TablicaScreen {
        Column(Modifier.fillMaxSize()) {
            TablicaTopBar("Foldery", onBack, subtitle = "Porządek na tablicy")
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = LocalDockPadding.current + 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "add") { InkButton("Nowy folder", { editing = FolderEditRequest(null) }, icon = TablicaIcons.Plus) }
                if (folders.isEmpty()) {
                    item(key = "empty") { BoardEmptyState("Foldery", "Tutaj uporządkujesz swoje przypomnienia.") }
                }
                items(folders, key = { it.folder.id }) { item ->
                    val folder = item.folder
                    Note(
                        color = LookTheme.tablica.noteFor(folder.id),
                        tilt = noteTilt((folder.id % 8).toInt()),
                        decoration = NoteDecoration.Tape,
                        onClick = { editing = FolderEditRequest(folder) },
                        onClickLabel = "Edytuj folder ${folder.name}",
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        NoteRow(
                            title = folder.name,
                            subtitle = (if (folder.isSystem) "Folder domyślny · " else "") + remindersText(item.reminderCount),
                            leading = { Icon(Icons.Rounded.Folder, null, tint = Color(folder.colorArgb)) },
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            RoundIconButton(Icons.Rounded.Edit, "Edytuj folder ${folder.name}", { editing = FolderEditRequest(folder) })
                            if (!folder.isSystem) RoundIconButton(Icons.Rounded.Delete, "Usuń folder ${folder.name}", { deleting = item })
                        }
                    }
                }
            }
        }
    }
    editing?.let { request ->
        FolderEditor(request.folder, { editing = null }) { name, color ->
            viewModel.saveFolder(request.folder, name, color)
            editing = null
        }
    }
    deleting?.let { item ->
        val close = { deleting = null }
        if (item.reminderCount == 0) {
            ConfirmNoteDialog(
                "Usunąć folder?", "Folder „${item.folder.name}” jest pusty i zostanie usunięty.", "Usuń",
                { viewModel.deleteFolder(item.folder, false); deleting = null }, close,
            )
        } else {
            NoteDialog(onDismissRequest = close, title = "Usuń folder „${item.folder.name}”", color = LookTheme.tablica.pink,
                buttons = { InkTextButton("Anuluj", close) }) {
                Text("Ten folder zawiera ${remindersText(item.reminderCount)}. Co chcesz z nimi zrobić?", style = TablicaType.BodyLight)
                Spacer(Modifier.height(12.dp))
                InkButton("Przenieś do „Inne”", { viewModel.deleteFolder(item.folder, false); deleting = null }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlineInkButton("Usuń razem z przypomnieniami", { viewModel.deleteFolder(item.folder, true); deleting = null }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FolderEditor(folder: Folder?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name ?: "") }
    var color by rememberSaveable(folder?.id) { mutableIntStateOf(folder?.colorArgb ?: FolderColors.palette.first()) }
    val system = folder?.isSystem == true
    val colorNames = listOf("Niebieski", "Zielony", "Pomarańczowy", "Czerwony", "Fioletowy", "Turkusowy", "Żółty", "Brązowy", "Różowy", "Indygo", "Limonkowy", "Szary")
    NoteDialog(
        onDismissRequest = onDismiss,
        title = if (folder == null) "Nowy folder" else "Edytuj folder",
        modifier = Modifier.imePadding(),
        buttons = { NoteDialogButtons("Zapisz", { onSave(name, color) }, onDismiss, confirmEnabled = name.isNotBlank()) },
    ) {
        Text("Nazwa", style = TablicaType.Strong)
        NoteTextField(name, { name = it.take(30) }, Modifier.fillMaxWidth().heightIn(min = 48.dp),
            label = "Nazwa folderu", placeholder = "Nazwa", singleLine = true, enabled = !system)
        if (system) Text("Nazwy folderu domyślnego nie można zmienić", style = TablicaType.Hint)
        Spacer(Modifier.height(16.dp))
        Text("Kolor", style = TablicaType.Strong)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderColors.palette.forEachIndexed { index, option ->
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color(option))
                        .border(if (option == color) 3.dp else 0.dp, LookTheme.tablica.ink, CircleShape)
                        .selectable(option == color, role = Role.RadioButton, onClick = { color = option })
                        .semantics { contentDescription = colorNames[index] },
                    contentAlignment = Alignment.Center,
                ) {
                    if (option == color) Icon(Icons.Rounded.Check, "Wybrany kolor", tint = Color.White)
                }
            }
        }
    }
}
