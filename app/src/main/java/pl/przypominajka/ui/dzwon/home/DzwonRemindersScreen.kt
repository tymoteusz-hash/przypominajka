@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.dzwon.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.*
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.home.HomeViewModel
import pl.przypominajka.ui.home.ReminderItem
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.*
import java.time.LocalDate

@Composable
fun DzwonRemindersScreen(
    onBack: () -> Unit,
    onAddReminder: (Long?) -> Unit,
    onEditReminder: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    DzwonSheetScaffold(
        title = "Przypomnienia",
        subtitle = "${state.today.activeCount} " + pluralPl(state.today.activeCount, "aktywne", "aktywne", "aktywnych"),
        onClose = onBack,
        bottomBar = {
            DzwonPrimaryButton("Nowe przypomnienie", { onAddReminder(state.selectedFolderId) },
                Modifier.fillMaxWidth().padding(16.dp), icon = DzwonIcons.Plus)
        },
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.gold) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "folders") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { DzwonChip("Wszystkie", { viewModel.selectFolder(null) }, selected = state.selectedFolderId == null) }
                        items(state.folders, key = { it.id }) { folder ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.padding(end = 6.dp).size(9.dp).clip(CircleShape).background(Color(folder.colorArgb)))
                                DzwonChip(folder.name, { viewModel.selectFolder(folder.id) }, selected = state.selectedFolderId == folder.id)
                            }
                        }
                    }
                }
                item(key = "permissions") {
                    DzwonPermissionPrompts(containerColor = c.field, itemPadding = PaddingValues(horizontal = 16.dp))
                }
                if (state.isEmpty) item(key = "empty") {
                    DzwonEmptyState(
                        if (state.selectedFolderId != null) "Pusto w tym folderze" else "Zacznij od pierwszego przypomnienia",
                        "Dotknij „Nowe przypomnienie” na dole arkusza.",
                    )
                }
                state.sections.forEach { section ->
                    item(key = "header_${section.title}") {
                        DzwonSectionHeader(section.title, Modifier.padding(horizontal = 20.dp), trailing = { DzwonBadge(section.items.size.toString()) })
                    }
                    items(section.items, key = { it.reminder.id }) { item ->
                        // Gest otwiera potwierdzenie; wiersz zostaje aż do usunięcia w bazie.
                        val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) pendingDelete = item.reminder
                            false
                        })
                        SwipeToDismissBox(state = dismiss, modifier = Modifier.padding(horizontal = 16.dp), backgroundContent = {
                            Box(Modifier.fillMaxSize().clip(DzwonCardShape).background(c.field).padding(20.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(DzwonIcons.Trash, "Usuń przypomnienie", tint = MaterialTheme.colorScheme.error)
                            }
                        }) {
                            ReminderCard(item, { onEditReminder(item.reminder.id) }, { viewModel.setEnabled(item.reminder, it) }, { pendingDelete = item.reminder })
                        }
                    }
                }
            }
        }
    }
    pendingDelete?.let { reminder ->
        DzwonDialog(onDismissRequest = { pendingDelete = null }, title = "Usunąć przypomnienie?",
            text = "„${reminder.title}” zostanie usunięte, a zaplanowane powiadomienia anulowane.",
            confirmText = "Usuń", destructive = true, onConfirm = { viewModel.delete(reminder); pendingDelete = null })
    }
}

@Composable
private fun ReminderCard(item: ReminderItem, onEdit: () -> Unit, onEnabled: (Boolean) -> Unit, onDelete: () -> Unit) {
    val c = LookTheme.dzwon
    val reminder = item.reminder
    val today = LocalDate.now()
    val finished = reminder.isFinishedOn(today)
    val active = reminder.enabled && !finished
    val type = SchoolType.fromName(reminder.schoolType)
    DzwonCard(Modifier.fillMaxWidth(), color = c.field, contentPadding = PaddingValues(16.dp), onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(when {
                    finished -> "Zakończone"
                    !active -> "Wyłączone"
                    item.nextTriggerAt != null -> formatDzwonWhen(item.nextTriggerAt, today)
                    else -> "Brak najbliższego powiadomienia"
                }, style = DzwonType.Time, color = if (active) c.goldText else c.ink2)
                Text(reminder.title, style = DzwonType.SmallTitle, color = if (active) c.ink else c.ink2,
                    textDecoration = if (finished) TextDecoration.LineThrough else null)
            }
            DzwonSwitch(active, onEnabled, enabled = !finished, modifier = Modifier.semantics { contentDescription = "Włącz przypomnienie: ${reminder.title}" })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(item.folder?.let { Color(it.colorArgb) } ?: c.ink2))
            Text(listOfNotNull(item.folder?.name, type?.label,
                reminder.eventDate?.takeUnless { finished }?.let { "${countdownText(it, today)} (${formatShortDay(it)})" },
                weekdaysText(reminder.daysOfWeek).takeIf { reminder.daysOfWeek != 0 }).joinToString(" · "), style = DzwonType.Caption, color = c.ink2)
        }
        if (!reminder.afterSchool && reminder.times.size > 1) {
            Text(reminder.times.joinToString(" · ") { it.format(TimeFormatter) }, style = DzwonType.Time, color = c.goldText, modifier = Modifier.padding(top = 8.dp))
        }
        reminder.snoozedUntil?.takeIf { active && it > System.currentTimeMillis() }?.let {
            Text("Odłożone do ${formatMillisAsTime(it)}", style = DzwonType.Caption, color = c.goldText)
        }
        if (reminder.isFromLibrus) DzwonBadge("Librus", Modifier.padding(top = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!finished) Text(reminderStatusText(reminder, today), style = DzwonType.Caption, color = c.ink2, modifier = Modifier.weight(1f))
            else Spacer(Modifier.weight(1f))
            DzwonRoundIconButton(DzwonIcons.Trash, "Usuń: ${reminder.title}", onDelete, containerColor = c.field)
        }
    }
}
