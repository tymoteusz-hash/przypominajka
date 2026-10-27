@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.Folder
import pl.przypominajka.data.FolderColors
import pl.przypominajka.data.isSystem
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.folders.FolderWithCount
import pl.przypominajka.ui.folders.FoldersViewModel
import pl.przypominajka.ui.kartka.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.remindersText

private data class FolderEditRequest(val folder: Folder?)

/** Foldery jako płaskie wiersze z kolorową kropką, licznikiem i akcjami. */
@Composable
fun KartkaFoldersScreen(
    onBack: () -> Unit,
    viewModel: FoldersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FolderEditRequest?>(null) }
    var deleting by remember { mutableStateOf<FolderWithCount?>(null) }
    val c = LookTheme.kartka

    KartkaScaffold(
        onBack = onBack,
        bottomBar = {
            KartkaBottomBar {
                KartkaButton(
                    text = "Nowy folder",
                    icon = KartkaIcons.Plus,
                    onClick = { editing = FolderEditRequest(null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding.withExtraBottom(24.dp)) {
            item(key = "title") {
                KartkaPageTitle("Foldery", subtitle = "Uporządkuj swoje przypomnienia")
                Spacer(Modifier.height(16.dp))
            }
            items(folders, key = { it.folder.id }) { item ->
                val folder = item.folder
                KartkaListRow(
                    title = folder.name,
                    subtitle = (if (folder.isSystem) "Folder domyślny · " else "") + remindersText(item.reminderCount),
                    modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding),
                    leading = {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(folder.colorArgb)))
                    },
                    trailing = {
                        Row {
                            KartkaIconButton(KartkaIcons.Edit, "Edytuj folder ${folder.name}", { editing = FolderEditRequest(folder) })
                            if (!folder.isSystem) {
                                KartkaIconButton(KartkaIcons.Trash, "Usuń folder ${folder.name}", { deleting = item }, tint = c.ink2)
                            }
                        }
                    },
                    onClick = { editing = FolderEditRequest(folder) },
                    onClickLabel = "Edytuj folder",
                )
            }
            if (folders.isEmpty()) {
                item(key = "empty") {
                    KartkaEmptyState("Brak folderów", "Dodaj folder przyciskiem na dole ekranu.")
                }
            }
        }
    }

    editing?.let { request ->
        FolderEditor(
            folder = request.folder,
            onDismiss = { editing = null },
            onSave = { name, color -> viewModel.saveFolder(request.folder, name, color); editing = null },
        )
    }
    deleting?.let { item ->
        val close = { deleting = null }
        KartkaDialog(
            onDismissRequest = close,
            title = if (item.reminderCount == 0) "Usunąć folder?" else "Usuń folder „${item.folder.name}”",
            text = if (item.reminderCount == 0) "Folder „${item.folder.name}” jest pusty i zostanie usunięty."
                else "Ten folder zawiera ${remindersText(item.reminderCount)}. Co chcesz z nimi zrobić?",
            confirmLabel = if (item.reminderCount == 0) "Usuń" else null,
            destructive = true,
            onConfirm = { viewModel.deleteFolder(item.folder, deleteReminders = false); close() },
            content = if (item.reminderCount > 0) {
                {
                    KartkaButton(
                        "Przenieś do „Inne”",
                        { viewModel.deleteFolder(item.folder, deleteReminders = false); close() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    KartkaButton(
                        "Usuń razem z przypomnieniami",
                        { viewModel.deleteFolder(item.folder, deleteReminders = true); close() },
                        modifier = Modifier.fillMaxWidth(),
                        destructive = true,
                    )
                }
            } else null,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderEditor(folder: Folder?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var color by rememberSaveable(folder?.id) { mutableIntStateOf(folder?.colorArgb ?: FolderColors.palette.first()) }
    val c = LookTheme.kartka
    val system = folder?.isSystem == true
    val colorNames = listOf("Niebieski", "Zielony", "Pomarańczowy", "Czerwony", "Fioletowy", "Turkusowy",
        "Żółty", "Brązowy", "Różowy", "Indygo", "Limonkowy", "Szary")

    KartkaDialog(
        onDismissRequest = onDismiss,
        title = if (folder == null) "Nowy folder" else "Edytuj folder",
        confirmLabel = "Zapisz",
        confirmEnabled = name.isNotBlank(),
        onConfirm = { onSave(name, color) },
    ) {
        KartkaTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            label = "Nazwa",
            enabled = !system,
            supportingText = if (system) "Nazwy folderu domyślnego nie można zmienić" else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        KartkaEyebrow("Kolor")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderColors.palette.forEachIndexed { index, option ->
                val selected = color == option
                Box(
                    modifier = Modifier.size(48.dp).clip(KartkaDefaults.ButtonShape)
                        .border(2.dp, if (selected) c.accent else Color.Transparent, KartkaDefaults.ButtonShape)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { color = option })
                        .semantics { contentDescription = "Kolor: ${colorNames[index]}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(option)), contentAlignment = Alignment.Center) {
                        if (selected) Icon(KartkaIcons.CheckThin, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
