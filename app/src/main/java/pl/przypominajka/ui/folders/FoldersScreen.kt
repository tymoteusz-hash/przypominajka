package pl.przypominajka.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.Folder
import pl.przypominajka.data.FolderColors
import pl.przypominajka.data.isSystem
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.AppFab
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.FolderDot
import pl.przypominajka.ui.components.RoundIconButton
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.util.remindersText

/** Okno edycji: folder == null oznacza tworzenie nowego. */
private data class FolderEditRequest(val folder: Folder?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onBack: () -> Unit,
    viewModel: FoldersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var editRequest by remember { mutableStateOf<FolderEditRequest?>(null) }
    var deleteRequest by remember { mutableStateOf<FolderWithCount?>(null) }

    Scaffold(
        containerColor = AppSurfaces.page,
        topBar = {
            BellTopBar(title = "Foldery", eyebrow = "Przypomnienia", onBack = onBack)
        },
        floatingActionButton = {
            AppFab(
                text = "Nowy folder",
                icon = Icons.Filled.Add,
                onClick = { editRequest = FolderEditRequest(folder = null) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(items = folders, key = { _, it -> it.folder.id }) { index, item ->
                val folder = item.folder
                val color = Color(folder.colorArgb)
                AppCard(
                    onClick = { editRequest = FolderEditRequest(folder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .appear(index),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            FolderDot(color = color, size = 18.dp)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                        ) {
                            Text(folder.name, style = MaterialTheme.typography.titleMedium)
                            val count = remindersText(item.reminderCount)
                            Text(
                                text = if (folder.isSystem) "Folder domyślny · $count" else count,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editRequest = FolderEditRequest(folder) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edytuj folder ${folder.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!folder.isSystem) {
                            IconButton(onClick = { deleteRequest = item }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Usuń folder ${folder.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editRequest?.let { request ->
        FolderEditDialog(
            folder = request.folder,
            onDismiss = { editRequest = null },
            onSave = { name, color ->
                viewModel.saveFolder(request.folder, name, color)
                editRequest = null
            },
        )
    }

    deleteRequest?.let { item ->
        DeleteFolderDialog(
            item = item,
            onDismiss = { deleteRequest = null },
            onMoveToOther = {
                viewModel.deleteFolder(item.folder, deleteReminders = false)
                deleteRequest = null
            },
            onDeleteWithReminders = {
                viewModel.deleteFolder(item.folder, deleteReminders = true)
                deleteRequest = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderEditDialog(
    folder: Folder?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorArgb: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(folder?.name ?: "") }
    var color by rememberSaveable { mutableIntStateOf(folder?.colorArgb ?: FolderColors.palette.first()) }
    val isSystem = folder?.isSystem == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "Nowy folder" else "Edytuj folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Nazwa") },
                    singleLine = true,
                    enabled = !isSystem,
                    supportingText = if (isSystem) {
                        { Text("Nazwy folderu domyślnego nie można zmienić") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Kolor", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FolderColors.palette.forEach { option ->
                        ColorSwatch(
                            color = Color(option),
                            selected = option == color,
                            onClick = { color = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, color) }, enabled = name.isNotBlank()) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
    )
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val selectionBorder = if (selected) {
        Modifier.border(width = 3.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(selectionBorder)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Wybrany kolor", tint = Color.White)
        }
    }
}

@Composable
private fun DeleteFolderDialog(
    item: FolderWithCount,
    onDismiss: () -> Unit,
    onMoveToOther: () -> Unit,
    onDeleteWithReminders: () -> Unit,
) {
    val name = item.folder.name
    if (item.reminderCount == 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Usunąć folder?") },
            text = { Text("Folder „$name” jest pusty i zostanie usunięty.") },
            confirmButton = { TextButton(onClick = onMoveToOther) { Text("Usuń") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Usuń folder „$name”") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ten folder zawiera ${remindersText(item.reminderCount)}. Co chcesz z nimi zrobić?")
                    Button(onClick = onMoveToOther, modifier = Modifier.fillMaxWidth()) {
                        Text("Przenieś do „Inne”")
                    }
                    OutlinedButton(
                        onClick = onDeleteWithReminders,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Usuń razem z przypomnieniami")
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
        )
    }
}
