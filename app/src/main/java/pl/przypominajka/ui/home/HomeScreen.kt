package pl.przypominajka.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.Folder
import pl.przypominajka.data.Reminder
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.isFinishedOn
import pl.przypominajka.data.isFromLibrus
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.AppFab
import pl.przypominajka.ui.components.Bell
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.LocalBottomBarPadding
import pl.przypominajka.ui.components.Pill
import pl.przypominajka.ui.components.SectionLabel
import pl.przypominajka.ui.components.SwipeToDeleteContainer
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.floating
import pl.przypominajka.ui.components.icon
import pl.przypominajka.ui.components.pastel
import pl.przypominajka.ui.components.pressScale
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.Pastel
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.formatMillisAsTime
import pl.przypominajka.util.formatShortDay
import pl.przypominajka.util.relativeDayLabel
import pl.przypominajka.util.pluralPl
import pl.przypominajka.util.reminderStatusText
import pl.przypominajka.util.weekdaysText
import java.time.LocalDate

/** Dział "Przypomnienia": filtr folderów i lista w sekcjach Aktywne / Wyłączone / Zakończone. */
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    onAddReminder: (folderId: Long?) -> Unit,
    onEditReminder: (reminderId: Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    val today = LocalDate.now()
    val bottomBar = LocalBottomBarPadding.current
    val listState = rememberLazyListState()
    // Napis "Nowe" chowa się, gdy lista jest przewinięta w dół.
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex < 2 } }

    Scaffold(
        containerColor = AppSurfaces.page,
        topBar = {
            BellTopBar(
                title = "Przypomnienia",
                eyebrow = "${state.today.activeCount} " +
                    pluralPl(state.today.activeCount, "aktywne", "aktywne", "aktywnych"),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            AppFab(
                text = "Nowe",
                icon = Icons.Rounded.Add,
                expanded = fabExpanded,
                onClick = { onAddReminder(state.selectedFolderId) },
                modifier = Modifier.padding(bottom = bottomBar),
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 4.dp, bottom = bottomBar + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "folders") {
                FolderFilter(
                    folders = state.folders,
                    selectedFolderId = state.selectedFolderId,
                    onSelect = viewModel::selectFolder,
                    modifier = Modifier.appear(0),
                )
            }

            if (state.isEmpty) {
                item(key = "empty") { EmptyState(folderSelected = state.selectedFolderId != null) }
            }

            var index = 1
            state.sections.forEach { section ->
                item(key = "header_${section.title}") {
                    SectionLabel(
                        text = section.title,
                        trailing = section.items.size.toString(),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
                section.items.forEach { reminderItem ->
                    val itemIndex = index++
                    item(key = reminderItem.reminder.id) {
                        SwipeToDeleteContainer(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                                .appear(itemIndex),
                            onDeleteRequest = { pendingDelete = reminderItem.reminder },
                        ) {
                            ReminderCard(
                                item = reminderItem,
                                today = today,
                                onClick = { onEditReminder(reminderItem.reminder.id) },
                                onEnabledChange = { enabled -> viewModel.setEnabled(reminderItem.reminder, enabled) },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Usunąć przypomnienie?") },
            text = { Text("„${reminder.title}” zostanie usunięte, a zaplanowane powiadomienia anulowane.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(reminder)
                    pendingDelete = null
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Anuluj") } },
        )
    }
}

// ---------------------------------------------------------------- "W szkole wkrótce"

/** Karta nadchodzącego sprawdzianu / kartkówki w kolorze rodzaju (używana na stronie głównej). */
@Composable
internal fun UpcomingEventCard(event: UpcomingEvent, today: LocalDate, onClick: () -> Unit) {
    val pastel = event.type.pastel
    AppCard(
        onClick = onClick,
        color = pastel.container,
        border = null,
        modifier = Modifier.width(180.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(event.type.icon, contentDescription = null, tint = pastel.content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(event.type.label, style = MaterialTheme.typography.labelMedium, color = pastel.content)
            }
            Text(
                text = countdownText(event.date, today).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = event.reminder.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatShortDay(event.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------- filtr folderów

@Composable
private fun FolderFilter(
    folders: List<Folder>,
    selectedFolderId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterPill(
                label = "Wszystkie",
                color = MaterialTheme.colorScheme.primary,
                selected = selectedFolderId == null,
                onClick = { onSelect(null) },
            )
        }
        items(items = folders, key = { it.id }) { folder ->
            FilterPill(
                label = folder.name,
                color = Color(folder.colorArgb),
                selected = selectedFolderId == folder.id,
                onClick = { onSelect(folder.id) },
            )
        }
    }
}

/** Kapsułka filtra – wybrana płynnie zmienia się w ciemną. */
@Composable
private fun FilterPill(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.onSurface else AppSurfaces.card, label = "pillBg")
    val content by animateColorAsState(if (selected) colors.surface else colors.onSurface, label = "pillFg")
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, AppSurfaces.cardBorder),
        interactionSource = interaction,
        modifier = Modifier.pressScale(interaction, pressedScale = 0.92f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ---------------------------------------------------------------- karta przypomnienia

/**
 * Karta przypomnienia "najpierw godzina": po lewej kolorowy blok z najbliższą godziną i dniem,
 * po prawej tytuł, szczegóły i przełącznik.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderCard(
    item: ReminderItem,
    today: LocalDate,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val reminder = item.reminder
    val colors = MaterialTheme.colorScheme
    val finished = reminder.isFinishedOn(today)
    val active = reminder.enabled && !finished
    val schoolType = SchoolType.fromName(reminder.schoolType)
    val folderColor = item.folder?.let { Color(it.colorArgb) } ?: colors.outline

    val (blockBg, blockFg) = when {
        !active -> colors.surfaceVariant to colors.onSurfaceVariant
        schoolType != null -> schoolType.pastel.container to schoolType.pastel.content
        else -> folderColor.copy(alpha = 0.14f) to folderColor
    }

    val snoozedUntil = reminder.snoozedUntil
    val snoozed = active && snoozedUntil != null && snoozedUntil > System.currentTimeMillis()
    val eventDate = reminder.eventDate
    val meta = listOfNotNull(
        item.folder?.name,
        schoolType?.label,
        if (eventDate != null && !finished) "${countdownText(eventDate, today)} (${formatShortDay(eventDate)})" else null,
        if (reminder.daysOfWeek != 0) weekdaysText(reminder.daysOfWeek) else null,
    ).joinToString(" · ")

    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeBlock(
                item = item,
                today = today,
                active = active,
                finished = finished,
                schoolTypeIcon = schoolType?.icon,
                background = blockBg,
                content = blockFg,
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (active) colors.onSurface else colors.onSurfaceVariant,
                    textDecoration = if (finished) TextDecoration.LineThrough else null,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(folderColor),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val extraTimes = !reminder.afterSchool && reminder.times.size > 1
                if (extraTimes || reminder.isFromLibrus || snoozed) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (extraTimes) {
                            reminder.times.forEach { time ->
                                Pill(
                                    text = time.format(TimeFormatter),
                                    containerColor = colors.surfaceVariant,
                                    contentColor = colors.onSurface,
                                )
                            }
                        }
                        if (snoozed && snoozedUntil != null) {
                            Pill(
                                text = "odłożone do ${formatMillisAsTime(snoozedUntil)}",
                                containerColor = colors.tertiaryContainer,
                                contentColor = colors.onTertiaryContainer,
                            )
                        }
                        if (reminder.isFromLibrus) {
                            Pill(text = "Librus", containerColor = Pastel.GOLD.container, contentColor = Pastel.GOLD.content)
                        }
                    }
                }
                if (!finished) {
                    Text(
                        text = reminderStatusText(reminder, today),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) colors.onSurfaceVariant else colors.outline,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Switch(
                checked = active,
                onCheckedChange = onEnabledChange,
                enabled = !finished,
            )
        }
    }
}

/** Kolorowy blok z najbliższą godziną ("19:00 / dziś") albo ikoną stanu. */
@Composable
private fun TimeBlock(
    item: ReminderItem,
    today: LocalDate,
    active: Boolean,
    finished: Boolean,
    schoolTypeIcon: ImageVector?,
    background: Color,
    content: Color,
) {
    val next = item.nextTriggerAt
    Column(
        modifier = Modifier
            .size(width = 66.dp, height = 70.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            finished -> {
                Icon(Icons.Rounded.Check, contentDescription = "Zakończone", tint = content, modifier = Modifier.size(26.dp))
            }
            !active -> {
                Icon(Icons.Rounded.NotificationsOff, contentDescription = "Wyłączone", tint = content, modifier = Modifier.size(24.dp))
            }
            next != null -> {
                if (schoolTypeIcon != null) {
                    Icon(schoolTypeIcon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = formatMillisAsTime(next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = content,
                )
                Text(
                    text = relativeDayLabel(next, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = content.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
            else -> {
                Icon(schoolTypeIcon ?: Icons.Rounded.Folder, contentDescription = null, tint = content)
            }
        }
    }
}

// ---------------------------------------------------------------- pusto

@Composable
private fun EmptyState(folderSelected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 36.dp)
            .appear(1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Bell(size = 96.dp, reportPosition = false, modifier = Modifier.floating())
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (folderSelected) "Pusto w tym folderze" else "Zacznij od pierwszego przypomnienia",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Dotknij „Nowe” w prawym dolnym rogu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
