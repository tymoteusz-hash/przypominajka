package pl.przypominajka.ui.tablica.checklist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
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
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.tablica.components.BoardEmptyState
import pl.przypominajka.ui.tablica.components.DashedTile
import pl.przypominajka.ui.tablica.components.LocalDockPadding
import pl.przypominajka.ui.tablica.components.Note
import pl.przypominajka.ui.tablica.components.NoteCheckbox
import pl.przypominajka.ui.tablica.components.NoteTextField
import pl.przypominajka.ui.tablica.components.OutlineInkButton
import pl.przypominajka.ui.tablica.components.PinInk
import pl.przypominajka.ui.tablica.components.RoundIconButton
import pl.przypominajka.ui.tablica.components.RoundIconStyle
import pl.przypominajka.ui.tablica.components.RoundIconToggle
import pl.przypominajka.ui.tablica.components.TablicaDateTimePicker
import pl.przypominajka.ui.tablica.components.TablicaIcons
import pl.przypominajka.ui.tablica.components.TablicaPermissionNotes
import pl.przypominajka.ui.tablica.components.TablicaScreen
import pl.przypominajka.ui.tablica.components.TablicaType
import pl.przypominajka.ui.tablica.components.UndoBar
import pl.przypominajka.ui.tablica.components.noteFor
import pl.przypominajka.ui.tablica.components.noteTilt
import pl.przypominajka.ui.tablica.components.rememberMinuteClock
import pl.przypominajka.ui.tablica.components.rememberTablicaPermissions
import pl.przypominajka.ui.tablica.components.reminderTimeText
import pl.przypominajka.util.pluralPl
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import java.time.ZoneId

/**
 * Checklista w wyglądzie Tablicy („Moja lista”): każdy punkt to kolorowa karteczka w siatce 2 kolumn.
 * Odhaczanie okrągłym polem, przenoszenie przez przytrzymanie karteczki, dopisywanie w przerywanym
 * kafelku na końcu (z godziną), poprawianie po dotknięciu tekstu, „Usuń zrobione” z „Cofnij”.
 */
@Composable
fun TablicaChecklistScreen(
    viewModel: ChecklistViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val colors = LookTheme.tablica
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val undo by viewModel.undoState.collectAsStateWithLifecycle()
    val now by rememberMinuteClock()
    val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Lokalnie tylko kolejność; treść i odhaczenie zawsze z bazy.
    val order = remember { mutableStateListOf<Long>().apply { addAll(items.map { it.id }) } }
    val byId = items.associateBy { it.id }
    val local = order.mapNotNull { byId[it] } + items.filter { it.id !in order }
    val gridState = rememberLazyGridState()
    val haptics = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        val ids = local.map { it.id }.toMutableList()
        val fromIndex = local.indexOfFirst { it.id == from.key }
        val toIndex = local.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
            ids.add(toIndex, ids.removeAt(fromIndex))
            order.clear()
            order.addAll(ids)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    LaunchedEffect(items) {
        if (!reorderState.isAnyItemDragging && order.toList() != items.map { it.id }) {
            order.clear()
            order.addAll(items.map { it.id })
        }
    }
    val saveOrder: () -> Unit = {
        val ids = order.filter { it in byId } + items.filter { it.id !in order }.map { it.id }
        if (ids != items.map { it.id }) viewModel.reorder(ids)
    }
    /** Przesunięcie o jedno miejsce – dla czytnika ekranu (przeciągania nie da się zrobić bez wzroku). */
    val moveBy: (Long, Int) -> Unit = { id, delta ->
        val from = local.indexOfFirst { it.id == id }
        val to = from + delta
        if (from >= 0 && to in local.indices) {
            val ids = local.map { it.id }.toMutableList()
            ids.add(to, ids.removeAt(from))
            order.clear()
            order.addAll(ids)
            viewModel.reorder(ids)
        }
    }

    // Dopisywanie w kafelku na końcu siatki.
    var draft by rememberSaveable { mutableStateOf("") }
    var draftTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingDraftTime by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Po dopisaniu przewijamy do kafelka, gdy nowa karteczka już się pojawi (rozmiar listy > zapamiętany).
    var scrollWhenSizeAbove by remember { mutableIntStateOf(-1) }
    var draftTimeError by rememberSaveable(draftTime) { mutableStateOf(false) }
    val submit: () -> Unit = submit@{
        draftTimeError = draftTime?.let { it <= System.currentTimeMillis() } == true
        if (draftTimeError) return@submit
        if (draft.isNotBlank()) {
            scrollWhenSizeAbove = local.size
            viewModel.add(draft.trim(), draftTime)
            draft = ""
            draftTime = null
        }
    }

    val permissions = rememberTablicaPermissions()
    val showPermissions = !permissions.allGranted && local.any { !it.done && it.remindAt != null }
    val showEmpty = loaded && local.isEmpty()
    val addTileIndex = 1 + (if (showPermissions) 1 else 0) + (if (showEmpty) 1 else 0) + local.size
    LaunchedEffect(local.size) {
        if (scrollWhenSizeAbove >= 0 && local.size > scrollWhenSizeAbove) {
            scrollWhenSizeAbove = -1
            gridState.animateScrollToItem(addTileIndex)
        }
    }

    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val dockPadding = LocalDockPadding.current

    TablicaScreen {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusTop + 18.dp,
                bottom = if (imeVisible) 24.dp else dockPadding + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                ChecklistHeader(summary = summary, showHint = local.size >= 2, onClearDone = viewModel::clearDone)
            }
            if (showPermissions) {
                item(key = "permissions", span = { GridItemSpan(maxLineSpan) }, contentType = "permissions") {
                    TablicaPermissionNotes(
                        permissions = permissions,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
            }
            if (showEmpty) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                    BoardEmptyState(
                        title = "Tablica jest pusta",
                        text = "Przyklej pierwszą karteczkę – wpisz ją w polu poniżej.",
                    )
                }
            }
            itemsIndexed(local, key = { _, item -> item.id }, contentType = { _, _ -> "note" }) { index, item ->
                ReorderableItem(reorderState, key = item.id) { isDragging ->
                    ChecklistNote(
                        item = item,
                        index = index,
                        dragging = isDragging,
                        nowMillis = nowMillis,
                        onToggle = { viewModel.toggle(item.id) },
                        onEdit = { editingId = item.id },
                        onMoveEarlier = if (index > 0) ({ moveBy(item.id, -1) }) else null,
                        onMoveLater = if (index < local.lastIndex) ({ moveBy(item.id, 1) }) else null,
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDragStopped = saveOrder,
                        ),
                    )
                }
            }
            item(key = "add", contentType = "add") {
                AddNoteTile(
                    timeError = draftTimeError,
                    draft = draft,
                    onDraftChange = { draft = it },
                    remindAt = draftTime,
                    onTimeToggle = { on -> if (on) pickingDraftTime = true else draftTime = null },
                    onPickTime = { pickingDraftTime = true },
                    onSubmit = submit,
                )
            }
        }

        val undoInfo = undo
        UndoBar(
            message = undoInfo?.let {
                val n = it.items.size
                "Zdjęto $n ${pluralPl(n, "karteczkę", "karteczki", "karteczek")}"
            },
            onUndo = viewModel::undo,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = dockPadding),
        )
    }

    if (pickingDraftTime) {
        TablicaDateTimePicker(
            initialMillis = draftTime,
            onDismiss = { pickingDraftTime = false },
            onConfirm = {
                draftTime = it
                pickingDraftTime = false
            },
        )
    }

    val editing = editingId?.let { id -> local.firstOrNull { it.id == id } }
    if (editing != null) {
        EditNoteDialog(
            item = editing,
            onDismiss = { editingId = null },
            onSave = {
                viewModel.edit(it)
                editingId = null
            },
            onDelete = {
                viewModel.delete(editing.id)
                editingId = null
            },
        )
    }
}

@Composable
private fun ChecklistHeader(summary: ChecklistSummary, showHint: Boolean, onClearDone: () -> Unit) {
    val colors = LookTheme.tablica
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
    ) {
        Text(text = "Moja lista", style = TablicaType.ScreenTitle, modifier = Modifier.semantics { heading() })
        Text(
            text = when {
                summary.total == 0 -> "Tablica jest pusta"
                summary.allDone -> "Wszystko zrobione!"
                else -> "${summary.left} z ${summary.total} do zrobienia"
            },
            style = TablicaType.Subtitle,
            color = colors.ink2,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (showHint) "Przytrzymaj karteczkę, żeby ją przenieść" else "",
                style = TablicaType.Hint,
                color = colors.ink2,
                modifier = Modifier.weight(1f),
            )
            OutlineInkButton(
                text = if (summary.done > 0) "Usuń zrobione (${summary.done})" else "Usuń zrobione",
                onClick = onClearDone,
                enabled = summary.done > 0,
            )
        }
    }
}

@Composable
private fun ChecklistNote(
    item: ChecklistItem,
    index: Int,
    dragging: Boolean,
    nowMillis: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onMoveEarlier: (() -> Unit)?,
    onMoveLater: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LookTheme.tablica
    val tilt by animateFloatAsState(if (dragging) 5f else noteTilt(index), tween(220), label = "noteTilt")
    val lift by animateFloatAsState(if (dragging) 1f else 0f, tween(200), label = "noteLift")
    Note(
        color = colors.noteFor(item.id),
        tilt = tilt,
        lifted = dragging,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .graphicsLayer {
                val scale = 1f + 0.06f * lift
                scaleX = scale
                scaleY = scale
                translationY = -6.dp.toPx() * lift
            },
        contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 10.dp, bottom = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            NoteCheckbox(
                checked = item.done,
                onCheckedChange = { onToggle() },
                contentDescription = (if (item.done) "Odznacz: " else "Odhacz: ") + item.text,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = TablicaIcons.Grip,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(16.dp)
                    .alpha(0.5f),
            )
        }
        Text(
            text = item.text,
            style = TablicaType.NoteText,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (item.done) 0.55f else 1f)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClickLabel = "Popraw karteczkę", onClick = onEdit)
                .semantics {
                    // Przeciągania nie da się zrobić czytnikiem ekranu – zamiast tego akcje „przenieś”.
                    customActions = listOfNotNull(
                        onMoveEarlier?.let { move -> CustomAccessibilityAction("Przenieś wcześniej") { move(); true } },
                        onMoveLater?.let { move -> CustomAccessibilityAction("Przenieś dalej") { move(); true } },
                    )
                }
                .padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        )
        val remindAt = item.remindAt
        if (remindAt != null) TimeTag(remindAt = remindAt, done = item.done, nowMillis = nowMillis)
    }
}

/** Godzina przy karteczce: zegarek + „dziś 19:00”; minione (i nieodhaczone) – na czerwonym tle. */
@Composable
private fun TimeTag(remindAt: Long, done: Boolean, nowMillis: Long) {
    val past = !done && remindAt < nowMillis
    val text = reminderTimeText(remindAt)
    val tint = if (past) PinInk else LocalContentColor.current
    Row(
        modifier = Modifier
            .padding(start = 8.dp, top = 8.dp)
            .alpha(if (done) 0.55f else 1f)
            .then(
                if (past) {
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(LookTheme.tablica.pin)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (past) "Termin minął: $text" else "Przypomnienie: $text"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(TablicaIcons.Clock, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Text(text = text, style = TablicaType.Tag, color = tint)
    }
}

/** Ostatnia komórka siatki: pole „Nowa karteczka…”, przełącznik godziny i okrągłe „+” (Przyklej). */
@Composable
private fun AddNoteTile(
    draft: String,
    onDraftChange: (String) -> Unit,
    remindAt: Long?,
    onTimeToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    onSubmit: () -> Unit,
    timeError: Boolean,
) {
    val colors = LookTheme.tablica
    DashedTile(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp),
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (timeError) Text(text = "Wybierz przyszłą datę i godzinę.", style = TablicaType.Hint, color = PinInk)
        NoteTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = "Nowa karteczka…",
            label = "Nowa karteczka",
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        if (remindAt != null) {
            Text(
                text = reminderTimeText(remindAt),
                style = TablicaType.Tag,
                color = colors.ink2,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClickLabel = "Zmień godzinę", onClick = onPickTime)
                    .padding(horizontal = 2.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconToggle(
                checked = remindAt != null,
                onCheckedChange = onTimeToggle,
                icon = TablicaIcons.Clock,
                contentDescription = "Godzina przypomnienia",
            )
            RoundIconButton(
                icon = TablicaIcons.Plus,
                contentDescription = "Przyklej karteczkę",
                onClick = onSubmit,
                style = RoundIconStyle.Filled,
                enabled = draft.isNotBlank(),
                iconSize = 22.dp,
            )
        }
    }
}
