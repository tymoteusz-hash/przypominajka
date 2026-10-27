@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.*
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.home.HomeViewModel
import pl.przypominajka.ui.home.ReminderItem
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.util.*
import java.time.LocalDate

/** Przypomnienia jako przypięte karteczki, pogrupowane według stanu. */
@Composable
fun TablicaRemindersScreen(
    onBack: () -> Unit,
    onAddReminder: (Long?) -> Unit,
    onEditReminder: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    val now by rememberMinuteClock()
    val listState = rememberLazyListState()
    val expanded by remember { derivedStateOf { listState.firstVisibleItemIndex < 2 } }
    TablicaScreen {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = dockContentPadding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "header") {
                BoardHeader(
                    title = "Przypomnienia",
                    subtitle = "${state.today.activeCount} " + pluralPl(state.today.activeCount, "aktywne", "aktywne", "aktywnych"),
                    trailing = { RoundIconButton(TablicaIcons.Back, "Wróć", onBack) },
                )
            }
            if (state.isLoading) {
                item(key = "loading") { Text("Wczytywanie przypomnień…", style = TablicaType.Body) }
            } else {
                item(key = "folders") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { TablicaChip("Wszystkie", state.selectedFolderId == null, { viewModel.selectFolder(null) }) }
                        items(state.folders, key = { it.id }) { folder ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Folder, null, tint = Color(folder.colorArgb), modifier = Modifier.size(16.dp))
                                TablicaChip(folder.name, state.selectedFolderId == folder.id, { viewModel.selectFolder(folder.id) })
                            }
                        }
                    }
                }
                if (state.isEmpty) {
                    item(key = "empty") {
                        BoardEmptyState(
                            title = if (state.selectedFolderId != null) "Pusto w tym folderze" else "Pierwsze przypomnienie",
                            text = "Dotknij „Nowe”, aby przypiąć przypomnienie do tablicy.",
                        )
                    }
                }
                state.sections.forEach { section ->
                    item(key = "section_${section.title}") { SectionLabel(section.title, trailing = section.items.size.toString()) }
                    items(section.items, key = { it.reminder.id }) { item ->
                        ReminderNote(
                            item = item,
                            today = now.toLocalDate(),
                            onEdit = { onEditReminder(item.reminder.id) },
                            onEnabledChange = { viewModel.setEnabled(item.reminder, it) },
                            onDelete = { pendingDelete = item.reminder },
                        )
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = LocalDockPadding.current + 12.dp)) {
            if (expanded) {
                InkButton("Nowe", { onAddReminder(state.selectedFolderId) }, icon = TablicaIcons.Plus)
            } else {
                RoundIconButton(TablicaIcons.Plus, "Nowe przypomnienie", { onAddReminder(state.selectedFolderId) }, style = RoundIconStyle.Filled)
            }
        }
    }
    pendingDelete?.let { reminder ->
        ConfirmNoteDialog(
            title = "Usunąć przypomnienie?",
            text = "„${reminder.title}” zostanie usunięte, a zaplanowane powiadomienia anulowane.",
            confirmText = "Usuń",
            onConfirm = { viewModel.delete(reminder); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ReminderNote(item: ReminderItem, today: LocalDate, onEdit: () -> Unit, onEnabledChange: (Boolean) -> Unit, onDelete: () -> Unit) {
    val reminder = item.reminder
    val finished = reminder.isFinishedOn(today)
    val active = reminder.enabled && !finished
    val type = SchoolType.fromName(reminder.schoolType)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val swipe = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it != SwipeToDismissBoxValue.Settled) currentOnDelete()
        false
    })
    SwipeToDismissBox(
        state = swipe,
        backgroundContent = {
            val direction = swipe.dismissDirection
            Box(
                Modifier.fillMaxSize().background(if (direction == SwipeToDismissBoxValue.Settled) Color.Transparent else LookTheme.tablica.pink.bg).padding(20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) Icon(Icons.Rounded.Delete, "Usuń przypomnienie", tint = LookTheme.tablica.pink.ink)
            }
        },
    ) {
        Note(
            color = LookTheme.tablica.noteFor(reminder.folderId),
            tilt = noteTilt((reminder.id % 8).toInt()) * 0.5f,
            decoration = NoteDecoration.Pin,
            onClick = onEdit,
            onClickLabel = "Edytuj przypomnienie",
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        reminder.title, style = TablicaType.NoteTitle,
                        textDecoration = if (finished) TextDecoration.LineThrough else null,
                    )
                    val next = item.nextTriggerAt
                    Text(
                        when {
                            finished -> "Zakończone"
                            !active -> "Wyłączone"
                            next != null -> "${formatMillisAsTime(next)} · ${relativeDayLabel(next, today)}"
                            else -> "Brak najbliższego powiadomienia"
                        }, style = TablicaType.Strong,
                    )
                }
                TablicaSwitch(active, onEnabledChange, enabled = !finished,
                    modifier = Modifier.semantics { contentDescription = "Włącz przypomnienie: ${reminder.title}" })
            }
            item.folder?.let { folder ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, null, tint = Color(folder.colorArgb), modifier = Modifier.size(16.dp))
                    Text(folder.name, style = TablicaType.Hint)
                }
            }
            val meta = listOfNotNull(
                type?.label,
                reminder.eventDate?.takeIf { !finished }?.let { "${countdownText(it, today)} (${formatShortDay(it)})" },
                if (reminder.daysOfWeek != 0) weekdaysText(reminder.daysOfWeek) else null,
            ).joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = TablicaType.Hint)
            if (!reminder.afterSchool && reminder.times.size > 1) {
                Text(reminder.times.joinToString(" · ") { it.format(TimeFormatter) }, style = TablicaType.Time)
            }
            val snoozed = reminder.snoozedUntil
            if (active && snoozed != null && snoozed > System.currentTimeMillis()) NoteBadge("Odłożone do ${formatMillisAsTime(snoozed)}")
            if (reminder.isFromLibrus) NoteBadge("Librus")
            if (!finished) Text(reminderStatusText(reminder, today), style = TablicaType.Hint)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                InkTextButton("Usuń", onDelete, icon = Icons.Rounded.Delete)
            }
        }
    }
}
