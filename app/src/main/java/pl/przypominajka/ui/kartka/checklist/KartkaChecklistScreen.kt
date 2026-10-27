@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.checklist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.ChecklistItem
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.checklist.ChecklistSummary
import pl.przypominajka.ui.checklist.ChecklistViewModel
import pl.przypominajka.ui.checklist.ChecklistQuickTime
import pl.przypominajka.ui.kartka.components.KartkaBottomBar
import pl.przypominajka.ui.kartka.components.KartkaBottomSheet
import pl.przypominajka.ui.kartka.components.KartkaButton
import pl.przypominajka.ui.kartka.components.KartkaCheckbox
import pl.przypominajka.ui.kartka.components.KartkaDateTimePickerDialog
import pl.przypominajka.ui.kartka.components.KartkaDefaults
import pl.przypominajka.ui.kartka.components.KartkaDivider
import pl.przypominajka.ui.kartka.components.KartkaEmptyState
import pl.przypominajka.ui.kartka.components.KartkaFilledIconButton
import pl.przypominajka.ui.kartka.components.KartkaFilterChip
import pl.przypominajka.ui.kartka.components.KartkaIcons
import pl.przypominajka.ui.kartka.components.KartkaMotion
import pl.przypominajka.ui.kartka.components.KartkaPageTitle
import pl.przypominajka.ui.kartka.components.KartkaPermissionNotices
import pl.przypominajka.ui.kartka.components.KartkaScaffold
import pl.przypominajka.ui.kartka.components.KartkaTextButton
import pl.przypominajka.ui.kartka.components.KartkaTextField
import pl.przypominajka.ui.kartka.components.KartkaToggleIconButton
import pl.przypominajka.ui.kartka.components.KartkaType
import pl.przypominajka.ui.kartka.components.KartkaUndoHost
import pl.przypominajka.ui.kartka.components.kartkaNextFullHour
import pl.przypominajka.ui.kartka.components.kartkaWhenPhrase
import pl.przypominajka.ui.kartka.components.kartkaWhenText
import pl.przypominajka.ui.kartka.components.rememberMinuteClock
import pl.przypominajka.ui.kartka.components.toEpochMillis
import pl.przypominajka.ui.kartka.components.toLocalDateTime
import pl.przypominajka.ui.kartka.components.withExtraBottom
import pl.przypominajka.ui.look.LookTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Checklista Kartki (makieta A_Checklista): odhaczanie, przeciąganie za uchwyt, „Usuń zrobione” z „Cofnij”,
 * dodawanie z dolnego paska (z godziną przypomnienia), edycja punktu w arkuszu dolnym.
 */
@Composable
fun KartkaChecklistScreen(
    onBack: () -> Unit,
    viewModel: ChecklistViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val allItems by viewModel.items.collectAsStateWithLifecycle()
    val loaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val undo by viewModel.undoState.collectAsStateWithLifecycle()
    val addedMessage by viewModel.addedMessage.collectAsStateWithLifecycle()
    val now by rememberMinuteClock()
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    val haptic = LocalHapticFeedback.current

    var draft by rememberSaveable { mutableStateOf("") }
    var draftRemindAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingDraftTime by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Kolejność w trakcie przeciągania (null = jak w bazie). Po upuszczeniu zapisujemy ją w bazie
    // i czekamy, aż wróci z bazy – dopiero wtedy wracamy do listy z ViewModelu (bez mignięcia).
    var dragOrder by remember { mutableStateOf<List<Long>?>(null) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(allItems) {
        if (!dragging) dragOrder = null
    }
    val shown = remember(allItems, dragOrder) { ordered(allItems, dragOrder) }

    val listState = rememberLazyListState()
    var scrollToEnd by remember { mutableStateOf(false) }
    val needsPermissions = draftRemindAt != null || allItems.any { !it.done && it.remindAt != null }
    // Elementy listy przed punktami: tytuł, wiersz narzędzi, prośby o uprawnienia.
    val headerCount = 1 + (if (shown.isNotEmpty()) 1 else 0) + (if (needsPermissions) 1 else 0)

    // Po dodaniu punktu przewijamy do niego (dodaje się na końcu listy).
    LaunchedEffect(shown.size) {
        if (scrollToEnd && shown.isNotEmpty()) {
            scrollToEnd = false
            listState.animateScrollToItem(headerCount + shown.lastIndex)
        }
    }

    var draftTimeError by rememberSaveable(draftRemindAt) { mutableStateOf(false) }
    val submit: () -> Unit = submit@{
        draftTimeError = draftRemindAt?.let { it <= System.currentTimeMillis() } == true
        if (draftTimeError) return@submit
        val text = draft.trim()
        if (text.isNotEmpty()) {
            viewModel.add(text, draftRemindAt)
            draft = ""
            draftRemindAt = null
            scrollToEnd = true
        }
    }

    /** Przesunięcie o jedno miejsce (akcje czytnika ekranu zamiast przeciągania). */
    val moveBy: (Long, Int) -> Unit = { id, delta ->
        val ids = shown.map { it.id }.toMutableList()
        val from = ids.indexOf(id)
        val to = from + delta
        if (from >= 0 && to in ids.indices) {
            ids.add(to, ids.removeAt(from))
            viewModel.reorder(ids)
        }
    }

    KartkaScaffold(
        onBack = onBack,
        bottomBar = {
            ChecklistInputBar(
                addedMessage = addedMessage,
                onQuickTime = { draftRemindAt = it.millis() },
                timeError = draftTimeError,
                draft = draft,
                onDraftChange = { draft = it },
                remindAt = draftRemindAt,
                today = today,
                now = now,
                onTimeToggle = {
                    if (draftRemindAt == null) {
                        pickingDraftTime = true
                    } else {
                        draftRemindAt = null
                    }
                },
                onEditTime = { pickingDraftTime = true },
                onSubmit = submit,
            )
        },
        snackbar = { KartkaUndoHost(message = undo?.message, onUndo = viewModel::undo) },
    ) { padding ->
        val reorderState = rememberReorderableLazyListState(listState, scrollThresholdPadding = padding) { from, to ->
            val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
            val toId = to.key as? Long ?: return@rememberReorderableLazyListState
            val ids = ordered(allItems, dragOrder).map { it.id }.toMutableList()
            val fromIndex = ids.indexOf(fromId)
            val toIndex = ids.indexOf(toId)
            if (fromIndex < 0 || toIndex < 0) return@rememberReorderableLazyListState
            ids.add(toIndex, ids.removeAt(fromIndex))
            dragOrder = ids
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding.withExtraBottom(16.dp),
        ) {
            item(key = "title") {
                KartkaPageTitle(
                    title = "Checklista",
                    subtitle = if (loaded) summaryText(summary) else null,
                )
            }
            if (shown.isNotEmpty()) {
                item(key = "tools") {
                    ToolsRow(doneCount = summary.done, onClearDone = viewModel::clearDone)
                }
            }
            if (needsPermissions) {
                item(key = "permissions") {
                    KartkaPermissionNotices(
                        modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding, vertical = 8.dp),
                    )
                }
            }
            items(shown, key = { it.id }) { item ->
                ReorderableItem(reorderState, key = item.id) { isDragging ->
                    val index = shown.indexOfFirst { it.id == item.id }
                    ChecklistRow(
                        item = item,
                        isDragging = isDragging,
                        now = now,
                        today = today,
                        handleModifier = Modifier.draggableHandle(
                            onDragStarted = {
                                dragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                dragging = false
                                val order = dragOrder
                                if (order != null && order != allItems.map { it.id }) {
                                    viewModel.reorder(order)
                                } else {
                                    dragOrder = null
                                }
                            },
                        ),
                        canMoveUp = index > 0,
                        canMoveDown = index in 0 until shown.lastIndex,
                        onToggle = { viewModel.toggle(item.id) },
                        onEdit = { editingId = item.id },
                        onMove = { delta -> moveBy(item.id, delta) },
                    )
                }
            }
            if (loaded && shown.isEmpty()) {
                item(key = "empty") {
                    KartkaEmptyState(title = "Pusto i czysto", text = "Wpisz pierwszy punkt na dole ekranu.")
                }
            }
        }
    }

    // ------------------------------------------------ okna

    if (pickingDraftTime) {
        KartkaDateTimePickerDialog(
            initial = draftRemindAt?.toLocalDateTime() ?: kartkaNextFullHour(),
            onDismiss = { pickingDraftTime = false },
            onConfirm = { picked ->
                draftRemindAt = picked.toEpochMillis()
                pickingDraftTime = false
            },
        )
    }

    val editing = editingId?.let { id -> allItems.firstOrNull { it.id == id } }
    // Punkt zniknął (np. usunięty w innym miejscu) – zamykamy arkusz.
    LaunchedEffect(editingId, editing == null, loaded) {
        if (editingId != null && editing == null && loaded) editingId = null
    }
    if (editing != null) {
        ChecklistEditSheet(
            item = editing,
            today = today,
            now = now,
            onDismiss = { editingId = null },
            onSave = viewModel::edit,
            onDelete = { viewModel.delete(editing.id) },
        )
    }
}

private fun summaryText(summary: ChecklistSummary): String = when {
    summary.total == 0 -> "Lista jest pusta"
    summary.left == 0 -> "Wszystko zrobione"
    else -> "${summary.left} z ${summary.total} do zrobienia"
}

/** Punkty w kolejności z przeciągania; nowe (dodane w trakcie) na końcu, usunięte pominięte. */
private fun ordered(items: List<ChecklistItem>, order: List<Long>?): List<ChecklistItem> {
    if (order == null) return items
    val byId = items.associateBy { it.id }
    val known = order.toHashSet()
    return order.mapNotNull { byId[it] } + items.filter { it.id !in known }
}

// ---------------------------------------------------------------- części ekranu

@Composable
private fun ToolsRow(doneCount: Int, onClearDone: () -> Unit) {
    val c = LookTheme.kartka
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = KartkaDefaults.PagePadding, end = KartkaDefaults.ListPadding, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Przytrzymaj uchwyt, żeby przesunąć",
            style = KartkaType.Caption,
            color = c.ink2,
            modifier = Modifier.weight(1f),
        )
        KartkaTextButton(
            text = if (doneCount > 0) "Usuń zrobione ($doneCount)" else "Usuń zrobione",
            onClick = onClearDone,
            enabled = doneCount > 0,
            icon = KartkaIcons.Trash,
        )
    }
}

/**
 * Wiersz punktu: pole wyboru (48 dp), tekst (dotknięcie = edycja) z godziną pod spodem, uchwyt do przeciągania.
 * Przeciągany wiersz unosi się: jaśniejsze tło, zaokrąglenie, cień, lekki obrót i powiększenie.
 */
@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    isDragging: Boolean,
    now: Long,
    today: LocalDate,
    handleModifier: Modifier,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val c = LookTheme.kartka
    val error = MaterialTheme.colorScheme.error
    val lift by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(KartkaMotion.SHORT_MS),
        label = "lift",
    )
    val shape = KartkaDefaults.TileShape
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KartkaDefaults.ListPadding)
            .graphicsLayer {
                rotationZ = -1f * lift
                scaleX = 1f + 0.02f * lift
                scaleY = 1f + 0.02f * lift
                shadowElevation = 12.dp.toPx() * lift
                this.shape = shape
                clip = false
            }
            .background(c.raised.copy(alpha = c.raised.alpha * lift), shape)
            .semantics {
                customActions = buildList {
                    if (canMoveUp) add(CustomAccessibilityAction("Przesuń wyżej") { onMove(-1); true })
                    if (canMoveDown) add(CustomAccessibilityAction("Przesuń niżej") { onMove(1); true })
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KartkaCheckbox(
                checked = item.done,
                onCheckedChange = { onToggle() },
                contentDescription = (if (item.done) "Odznacz: " else "Odhacz: ") + item.text,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = "Edytuj punkt", onClick = onEdit)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.text,
                    style = KartkaType.Body,
                    color = if (item.done) c.ink2 else c.ink,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                )
                val remindAt = item.remindAt
                if (remindAt != null) {
                    val overdue = !item.done && remindAt < now
                    val tint = when {
                        item.done -> c.ink2
                        overdue -> error
                        else -> c.accent
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = KartkaIcons.Clock,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = kartkaWhenText(remindAt, today) + if (overdue) " · minęło" else "",
                            style = KartkaType.CaptionStrong,
                            color = tint,
                        )
                    }
                }
            }
            Box(
                modifier = handleModifier
                    .size(KartkaDefaults.TouchTarget)
                    .semantics { contentDescription = "Uchwyt – przeciągnij, żeby zmienić kolejność" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = KartkaIcons.DragHandle,
                    contentDescription = null,
                    tint = c.ink2,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        KartkaDivider(modifier = Modifier.alpha(1f - lift))
    }
}

/** Dolny pasek: „Przypomnę dziś o 18:00” (gdy wybrano godzinę), pole „Dodaj punkt…”, zegar i „+”. */
@Composable
private fun ChecklistInputBar(
    addedMessage: String?,
    onQuickTime: (ChecklistQuickTime) -> Unit,
    draft: String,
    onDraftChange: (String) -> Unit,
    remindAt: Long?,
    today: LocalDate,
    now: Long,
    onTimeToggle: () -> Unit,
    onEditTime: () -> Unit,
    onSubmit: () -> Unit,
    timeError: Boolean,
) {
    val c = LookTheme.kartka
    KartkaBottomBar {
        if (addedMessage != null) Text(text = addedMessage, style = KartkaType.CaptionStrong, color = c.accent)
        if (timeError) Text(text = "Wybierz przyszłą datę i godzinę.", style = KartkaType.Caption, color = MaterialTheme.colorScheme.error)
        if (remindAt != null) {
            val past = remindAt < now
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (past) "Ta godzina już minęła" else "Przypomnę ${kartkaWhenPhrase(remindAt, today)}",
                    style = KartkaType.CaptionStrong,
                    color = if (past) MaterialTheme.colorScheme.error else c.accent,
                    modifier = Modifier.weight(1f),
                )
                KartkaTextButton(text = "Zmień", onClick = onEditTime, color = c.ink2)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KartkaTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = "Dodaj punkt…",
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                // Klawiatura zostaje otwarta – można od razu wpisać kolejny punkt.
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.weight(1f),
            )
            KartkaToggleIconButton(
                icon = KartkaIcons.Clock,
                contentDescription = "Godzina przypomnienia",
                checked = remindAt != null,
                onCheckedChange = { onTimeToggle() },
            )
            KartkaFilledIconButton(
                icon = KartkaIcons.Plus,
                contentDescription = "Dodaj punkt",
                onClick = onSubmit,
                enabled = draft.isNotBlank(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChecklistQuickTime.values().forEach { choice ->
                KartkaFilterChip(text = choice.label, selected = false, onClick = { onQuickTime(choice) })
            }
            KartkaFilterChip(text = "Wybierz…", selected = false, onClick = onEditTime)
        }
    }
}

/** Arkusz edycji punktu: tekst, data i godzina przypomnienia, „Usuń godzinę”, „Usuń punkt”, „Zapisz”. */
@Composable
private fun ChecklistEditSheet(
    item: ChecklistItem,
    today: LocalDate,
    now: Long,
    onDismiss: () -> Unit,
    onSave: (ChecklistItem) -> Unit,
    onDelete: () -> Unit,
) {
    val c = LookTheme.kartka
    val error = MaterialTheme.colorScheme.error
    var text by rememberSaveable(item.id) { mutableStateOf(item.text) }
    var remindAt by rememberSaveable(item.id) { mutableStateOf(item.remindAt) }
    var timeError by rememberSaveable(item.id, remindAt) { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    // Usuwamy dopiero po schowaniu arkusza – inaczej zniknąłby bez animacji.
    var deleteAfterClose by remember { mutableStateOf(false) }

    KartkaBottomSheet(
        onDismissRequest = {
            if (deleteAfterClose) onDelete()
            onDismiss()
        },
        title = "Punkt",
    ) { close ->
        KartkaTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "Treść punktu",
            singleLine = false,
            maxLines = 6,
            isError = text.isBlank(),
            supportingText = if (text.isBlank()) "Punkt musi mieć treść." else null,
        )

        val at = remindAt
        val overdue = at != null && !item.done && at < now
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = KartkaIcons.Clock,
                contentDescription = null,
                tint = if (at == null) c.ink2 else if (overdue) error else c.accent,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "Przypomnienie", style = KartkaType.RowTitle, color = c.ink)
                Text(
                    text = when {
                        at == null -> "Bez przypomnienia"
                        overdue -> kartkaWhenText(at, today) + " · minęło"
                        else -> kartkaWhenText(at, today)
                    },
                    style = KartkaType.Caption,
                    color = if (overdue) error else c.ink2,
                )
            }
            KartkaTextButton(text = if (at == null) "Ustaw" else "Zmień", onClick = { picking = true })
        }
        if (at != null) {
            KartkaTextButton(
                text = "Usuń godzinę",
                onClick = { remindAt = null },
                icon = KartkaIcons.Close,
                color = c.ink2,
            )
        }

        if (timeError) Text(text = "Wybierz przyszłą datę i godzinę.", style = KartkaType.Caption, color = MaterialTheme.colorScheme.error)

        KartkaDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KartkaTextButton(
                text = "Usuń punkt",
                onClick = {
                    deleteAfterClose = true
                    close()
                },
                icon = KartkaIcons.Trash,
                color = error,
            )
            Spacer(Modifier.weight(1f))
            KartkaButton(
                text = "Zapisz",
                onClick = {
                    timeError = remindAt != item.remindAt && remindAt?.let { it <= System.currentTimeMillis() } == true
                    if (!timeError) {
                        onSave(item.copy(text = text.trim(), remindAt = remindAt))
                        close()
                    }
                },
                enabled = text.isNotBlank(),
            )
        }
    }

    if (picking) {
        KartkaDateTimePickerDialog(
            initial = remindAt?.toLocalDateTime() ?: kartkaNextFullHour(),
            onDismiss = { picking = false },
            onConfirm = { picked ->
                remindAt = picked.toEpochMillis()
                picking = false
            },
        )
    }
}
