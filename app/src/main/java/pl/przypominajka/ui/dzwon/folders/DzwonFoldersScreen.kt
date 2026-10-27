@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.przypominajka.ui.dzwon.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.folders.FoldersViewModel
import pl.przypominajka.ui.folders.FolderWithCount
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.remindersText

@Composable
fun DzwonFoldersScreen(
    onBack: () -> Unit,
    viewModel: FoldersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<FolderWithCount?>(null) }
    DzwonSheetScaffold(
        title = "Foldery", subtitle = "Porządek w przypomnieniach", onClose = onBack,
        bottomBar = {
            DzwonPrimaryButton("Nowy folder", { editingId = null; editing = true },
                Modifier.fillMaxWidth().padding(16.dp), icon = DzwonIcons.Plus)
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(folders, key = { it.folder.id }) { item ->
                val folder = item.folder
                DzwonCard(Modifier.fillMaxWidth(), color = c.field, onClick = { editingId = folder.id; editing = true }) {
                    DzwonListRow(
                        title = folder.name,
                        subtitle = (if (folder.isSystem) "Folder domyślny · " else "") + remindersText(item.reminderCount),
                        leading = { Box(Modifier.size(16.dp).clip(CircleShape).background(Color(folder.colorArgb))) },
                        trailing = {
                            DzwonRoundIconButton(DzwonIcons.Pencil, "Edytuj folder ${folder.name}", { editingId = folder.id; editing = true }, containerColor = c.field)
                        },
                    )
                    if (!folder.isSystem) DzwonTextButton("Usuń folder", { deleting = item }, icon = DzwonIcons.Trash, color = c.ink2)
                }
            }
        }
    }
    if (editing) {
        val folder = folders.firstOrNull { it.folder.id == editingId }?.folder
        // Przy odtwarzaniu ekranu czekamy na folder zamiast otwierać pusty formularz.
        if (editingId == null || folder != null) EditFolderSheet(folder, { editing = false }) { name, color ->
            viewModel.saveFolder(folder, name, color)
            editing = false
        }
    }
    deleting?.let { item ->
        if (item.reminderCount == 0) DzwonDialog(
            onDismissRequest = { deleting = null }, title = "Usunąć folder?",
            text = "Folder „${item.folder.name}” jest pusty i zostanie usunięty.",
            confirmText = "Usuń", destructive = true,
            onConfirm = { viewModel.deleteFolder(item.folder, false); deleting = null },
        ) else DzwonBottomSheet(onDismissRequest = { deleting = null }, title = "Usuń folder „${item.folder.name}”") {
            Text("Ten folder zawiera ${remindersText(item.reminderCount)}. Co chcesz z nimi zrobić?", style = DzwonType.Body, color = c.ink)
            DzwonPrimaryButton("Przenieś do „Inne”", { viewModel.deleteFolder(item.folder, false); deleting = null }, Modifier.fillMaxWidth())
            DzwonTextButton("Usuń razem z przypomnieniami", { viewModel.deleteFolder(item.folder, true); deleting = null },
                Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error)
            DzwonTextButton("Anuluj", { deleting = null }, Modifier.fillMaxWidth(), color = c.ink)
        }
    }
}

@Composable
private fun EditFolderSheet(folder: Folder?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    val c = LookTheme.dzwon
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var color by rememberSaveable(folder?.id) { mutableIntStateOf(folder?.colorArgb ?: FolderColors.palette.first()) }
    val isSystem = folder?.isSystem == true
    val sheetState = rememberDzwonSheetState()
    val scope = rememberCoroutineScope()
    DzwonBottomSheet(onDismissRequest = onDismiss, title = if (folder == null) "Nowy folder" else "Edytuj folder", sheetState = sheetState) {
        DzwonTextField(name, { name = it.take(30) }, label = "Nazwa", enabled = !isSystem)
        if (isSystem) Text("Nazwy folderu domyślnego nie można zmienić", style = DzwonType.Caption, color = c.ink2)
        else Text("${name.length}/30 znaków", style = DzwonType.Caption, color = c.ink2)
        DzwonSectionHeader("Kolor")
        val colorNames = listOf("Niebieski", "Zielony", "Pomarańczowy", "Czerwony", "Fioletowy", "Turkusowy", "Żółty", "Brązowy", "Różowy", "Indygo", "Limonkowy", "Szary")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderColors.palette.forEachIndexed { index, option ->
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .selectable(selected = color == option, role = Role.RadioButton, onClick = { color = option })
                        .semantics { contentDescription = "Kolor: ${colorNames[index]}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(option))
                        .then(if (color == option) Modifier.border(3.dp, c.ink, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                        if (color == option) Icon(DzwonIcons.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            DzwonTextButton("Anuluj", { sheetState.hideThen(scope, onDismiss) }, color = c.ink)
            DzwonPrimaryButton("Zapisz", { sheetState.hideThen(scope) { onSave(name, color) } }, enabled = name.isNotBlank())
        }
    }
}
