@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.Reminder
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.isFinishedOn
import pl.przypominajka.data.isFromLibrus
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.home.HomeViewModel
import pl.przypominajka.ui.home.ReminderItem
import pl.przypominajka.ui.kartka.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.formatShortDay
import pl.przypominajka.util.pluralPl
import pl.przypominajka.util.reminderStatusText
import pl.przypominajka.util.weekdaysText
import java.time.LocalDate

/** Przypomnienia Kartki: filtry, płaska lista z godzinami i sekcje stanu. */
@Composable
fun KartkaRemindersScreen(
    onBack: () -> Unit,
    onAddReminder: (folderId: Long?) -> Unit,
    onEditReminder: (reminderId: Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    val now by rememberMinuteClock()
    val today = now.toLocalDateTime().toLocalDate()
    val c = LookTheme.kartka

    KartkaScaffold(
        onBack = onBack,
        bottomBar = {
            KartkaBottomBar {
                KartkaButton(
                    text = "Nowe przypomnienie",
                    icon = KartkaIcons.Plus,
                    onClick = { onAddReminder(state.selectedFolderId) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding.withExtraBottom(24.dp),
            ) {
                item(key = "title") {
                    KartkaPageTitle(
                        title = "Przypomnienia",
                        subtitle = "${state.today.activeCount} " +
                            pluralPl(state.today.activeCount, "aktywne", "aktywne", "aktywnych"),
                    )
                }
                item(key = "folders") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = KartkaDefaults.PagePadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "all") {
                            KartkaFilterChip("Wszystkie", state.selectedFolderId == null, { viewModel.selectFolder(null) })
                        }
                        items(state.folders, key = { it.id }) { folder ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(folder.colorArgb)))
                                KartkaFilterChip(folder.name, state.selectedFolderId == folder.id, { viewModel.selectFolder(folder.id) })
                            }
                        }
                    }
                }
                if (state.isEmpty) {
                    item(key = "empty") {
                        KartkaEmptyState(
                            title = if (state.selectedFolderId != null) "Pusto w tym folderze" else "Zacznij od pierwszego przypomnienia",
                            text = "Dotknij „Nowe przypomnienie” na dole ekranu.",
                        )
                    }
                }
                state.sections.forEach { section ->
                    item(key = "header_${section.title}") {
                        KartkaSectionHeader(
                            title = "${section.title} · ${section.items.size}",
                            modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding, vertical = 8.dp),
                        )
                    }
                    items(section.items, key = { it.reminder.id }) { item ->
                        ReminderRow(
                            item = item,
                            today = today,
                            now = now,
                            onEdit = { onEditReminder(item.reminder.id) },
                            onDelete = { pendingDelete = item.reminder },
                            onEnabledChange = { viewModel.setEnabled(item.reminder, it) },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { reminder ->
        KartkaDialog(
            onDismissRequest = { pendingDelete = null },
            title = "Usunąć przypomnienie?",
            text = "„${reminder.title}” zostanie usunięte, a zaplanowane powiadomienia anulowane.",
            confirmLabel = "Usuń",
            destructive = true,
            onConfirm = { viewModel.delete(reminder); pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderRow(
    item: ReminderItem,
    today: LocalDate,
    now: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val c = LookTheme.kartka
    val reminder = item.reminder
    val finished = reminder.isFinishedOn(today)
    val active = reminder.enabled && !finished
    val type = SchoolType.fromName(reminder.schoolType)
    val currentDelete by rememberUpdatedState(onDelete)
    val swipe = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value != SwipeToDismissBoxValue.Settled) currentDelete()
        false
    })
    SwipeToDismissBox(
        state = swipe,
        modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding),
        backgroundContent = {
            val error = MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier.fillMaxSize().background(error.copy(alpha = 0.12f)),
                contentAlignment = if (swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(KartkaIcons.Trash, contentDescription = "Usuń przypomnienie", tint = error, modifier = Modifier.padding(16.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(c.bg).semantics {
                customActions = listOf(CustomAccessibilityAction("Usuń przypomnienie") { onDelete(); true })
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Edytuj przypomnienie", onClick = onEdit)
                    .heightIn(min = 72.dp).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(58.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val next = item.nextTriggerAt
                    when {
                        finished -> Icon(KartkaIcons.CheckThin, "Zakończone", tint = c.ink2)
                        !active -> Text("Wył.", style = KartkaType.CaptionStrong, color = c.ink2)
                        next != null -> {
                            val dateTime = next.toLocalDateTime()
                            KartkaTimeLabel(dateTime.toLocalTime().format(KartkaHourFormatter), color = c.accent)
                            Text(kartkaDayLead(dateTime.toLocalDate(), today), style = KartkaType.Caption, color = c.ink2)
                        }
                        else -> Icon(KartkaIcons.Folder, null, tint = c.ink2)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        reminder.title,
                        style = KartkaType.RowTitle,
                        color = if (active) c.ink else c.ink2,
                        textDecoration = if (finished) TextDecoration.LineThrough else null,
                    )
                    val meta = listOfNotNull(
                        item.folder?.name,
                        type?.label,
                        reminder.eventDate?.takeIf { !finished }?.let { "${countdownText(it, today)} (${formatShortDay(it)})" },
                        if (reminder.daysOfWeek != 0) weekdaysText(reminder.daysOfWeek) else null,
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(item.folder?.let { Color(it.colorArgb) } ?: c.ink2))
                            Text(meta, style = KartkaType.Caption, color = c.ink2)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!reminder.afterSchool && reminder.times.size > 1) {
                            reminder.times.forEach { KartkaChip(it.format(KartkaHourFormatter), tone = KartkaChipTone.Neutral) }
                        }
                        reminder.snoozedUntil?.takeIf { active && it > now }?.let {
                            KartkaChip("odłożone do ${it.toLocalDateTime().toLocalTime().format(KartkaHourFormatter)}")
                        }
                        if (reminder.isFromLibrus) KartkaChip("Librus", tone = KartkaChipTone.Neutral)
                    }
                    if (!finished) Text(reminderStatusText(reminder, today), style = KartkaType.Caption, color = c.ink2)
                }
                KartkaSwitch(
                    checked = active,
                    onCheckedChange = onEnabledChange,
                    enabled = !finished,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Włącz przypomnienie: ${reminder.title}" },
                )
            }
            KartkaDivider()
        }
    }
}
