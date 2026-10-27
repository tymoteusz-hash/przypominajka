@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.dzwon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pl.przypominajka.data.ChecklistItem
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.checklist.ChecklistViewModel
import pl.przypominajka.ui.checklist.ChecklistQuickTime
import pl.przypominajka.ui.dzwon.components.DzwonBellInRing
import pl.przypominajka.ui.dzwon.components.DzwonBottomSheet
import pl.przypominajka.ui.dzwon.components.DzwonCheckbox
import pl.przypominajka.ui.dzwon.components.DzwonChip
import pl.przypominajka.ui.dzwon.components.DzwonDateTimePickerDialog
import pl.przypominajka.ui.dzwon.components.DzwonEmptyState
import pl.przypominajka.ui.dzwon.components.DzwonIcons
import pl.przypominajka.ui.dzwon.components.DzwonPermissionPrompts
import pl.przypominajka.ui.dzwon.components.DzwonPrimaryButton
import pl.przypominajka.ui.dzwon.components.DzwonSecondaryButton
import pl.przypominajka.ui.dzwon.components.DzwonSheetScaffold
import pl.przypominajka.ui.dzwon.components.DzwonSquareIconButton
import pl.przypominajka.ui.dzwon.components.DzwonTextButton
import pl.przypominajka.ui.dzwon.components.DzwonTextField
import pl.przypominajka.ui.dzwon.components.DzwonType
import pl.przypominajka.ui.dzwon.components.DzwonUndoHost
import pl.przypominajka.ui.dzwon.components.epochMillisToLocalDateTime
import pl.przypominajka.ui.dzwon.components.formatDzwonAt
import pl.przypominajka.ui.dzwon.components.formatDzwonWhen
import pl.przypominajka.ui.dzwon.components.hideThen
import pl.przypominajka.ui.dzwon.components.nextFullHour
import pl.przypominajka.ui.dzwon.components.rememberDzwonSheetState
import pl.przypominajka.ui.dzwon.components.toEpochMillis
import pl.przypominajka.ui.look.LookTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Środek węzła (i lina) od lewej krawędzi wiersza – jak w makiecie (przycisk 52 dp). */
private val RopeX = 26.dp

/**
 * Checklista Dzwonu (makieta B_Checklista): w nagłówku mały dzwon w pierścieniu postępu, w arkuszu
 * lista jako „lina” – pionowa linia przez okrągłe węzły-pola wyboru (złota przy zrobionych).
 * Kropki po prawej służą do przeciągania, dotknięcie tekstu otwiera edycję, na dole pasek dodawania
 * (pole, zegar z wyborem daty i godziny, złoty „+”), nad nim pasek „Cofnij”.
 */
@Composable
fun DzwonChecklistScreen(
    onClose: () -> Unit,
    viewModel: ChecklistViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val c = LookTheme.dzwon
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val undo by viewModel.undoState.collectAsStateWithLifecycle()
    val addedMessage by viewModel.addedMessage.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    // Co pół minuty odświeżamy „teraz” – godziny, które minęły, zmieniają kolor.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val today = remember(now) { Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate() }

    // Lokalna kopia listy: przestawiana na żywo przy przeciąganiu, z bazą synchronizowana tylko wtedy,
    // gdy nic nie jest przeciągane (a po upuszczeniu – dopiero gdy baza zapisze nową kolejność).
    val local = remember { mutableStateListOf<ChecklistItem>().apply { addAll(items) } }
    var dragging by remember { mutableStateOf(false) }
    var pendingOrder by remember { mutableStateOf<List<Long>?>(null) }

    LaunchedEffect(items, dragging, pendingOrder) {
        if (dragging) return@LaunchedEffect
        val pending = pendingOrder
        if (pending != null) {
            val ids = items.map { it.id }
            val idSet = ids.toHashSet()
            val pendingSet = pending.toHashSet()
            if (ids.filter { it in pendingSet } != pending.filter { it in idSet }) {
                // Baza jeszcze nie ma nowej kolejności – czekamy na nią (z bezpiecznikiem czasowym).
                delay(1_500)
                pendingOrder = null
                return@LaunchedEffect
            }
            pendingOrder = null
        }
        if (local.toList() != items) {
            local.clear()
            local.addAll(items)
        }
    }

    fun commitOrder() {
        val order = local.map { it.id }
        if (order != items.map { it.id }) {
            pendingOrder = order
            viewModel.reorder(order)
        }
    }

    /** Przesunięcie o jedno miejsce – dla czytnika ekranu (zamiast przeciągania). */
    fun moveBy(id: Long, delta: Int) {
        val from = local.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in local.indices) return
        local.add(to, local.removeAt(from))
        commitOrder()
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = local.indexOfFirst { it.id == from.key }
        val toIndex = local.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            local.add(toIndex, local.removeAt(fromIndex))
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    var draft by rememberSaveable { mutableStateOf("") }
    var draftRemindAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var showNewPicker by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var scrollToEnd by remember { mutableStateOf(false) }

    val showPermissions = local.any { !it.done && it.remindAt != null }
    val headerItems = if (showPermissions) 1 else 0

    // Po dodaniu punktu przewijamy do końca listy, żeby nowy punkt było widać.
    LaunchedEffect(local.size) {
        if (scrollToEnd && local.isNotEmpty()) {
            listState.animateScrollToItem(headerItems + local.lastIndex)
        }
        scrollToEnd = false
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

    DzwonSheetScaffold(
        title = "Checklista",
        subtitle = when {
            summary.total == 0 -> "Lista jest pusta"
            summary.left == 0 -> "Wszystko zrobione"
            else -> "${summary.left} z ${summary.total} do zrobienia"
        },
        onClose = onClose,
        headerExtra = {
            DzwonBellInRing(
                progress = summary.progress,
                // Gdy wszystko odhaczone, dzwonek w pierścieniu raz się kołysze.
                swingKey = if (summary.allDone) summary.total else null,
                contentDescription = "Odhaczone ${summary.done} z ${summary.total}",
            )
        },
        bottomBar = {
            AddBar(
                addedMessage = addedMessage,
                onQuickTime = { draftRemindAt = it.millis() },
                timeError = draftTimeError,
                draft = draft,
                onDraftChange = { draft = it },
                remindAt = draftRemindAt,
                today = today,
                onTimeToggle = {
                    if (draftRemindAt == null) showNewPicker = true else draftRemindAt = null
                },
                onEditTime = { showNewPicker = true },
                onSubmit = submit,
            )
        },
        overlay = {
            DzwonUndoHost(
                message = undo?.message,
                onUndo = viewModel::undo,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        },
    ) {
        if (local.isNotEmpty()) {
            ListToolbar(
                doneCount = summary.done,
                showHint = local.size > 1,
                onClearDone = viewModel::clearDone,
            )
        }
        when {
            // Jeszcze nie wczytano – nic nie pokazujemy, żeby nie mignął pusty stan.
            local.isEmpty() && (!loaded || items.isNotEmpty()) -> Spacer(Modifier.weight(1f))
            local.isEmpty() -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                DzwonEmptyState(title = "Dzwon odpoczywa", text = "Na liście nic nie zostało.")
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 4.dp,
                    top = 2.dp,
                    bottom = if (undo != null) 84.dp else 16.dp,
                ),
            ) {
                if (showPermissions) {
                    item(key = "permissions", contentType = "permissions") {
                        DzwonPermissionPrompts(
                            containerColor = c.field,
                            itemPadding = PaddingValues(start = 6.dp, end = 10.dp, bottom = 10.dp),
                        )
                    }
                }
                itemsIndexed(local, key = { _, item -> item.id }, contentType = { _, _ -> "item" }) { index, item ->
                    ReorderableItem(reorderState, key = item.id) { isDragging ->
                        RopeRow(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == local.lastIndex,
                            isDragging = isDragging,
                            now = now,
                            today = today,
                            onToggle = { viewModel.toggle(item.id) },
                            onEdit = { editingId = item.id },
                            onDragStarted = {
                                dragging = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                dragging = false
                                commitOrder()
                            },
                            onMoveUp = if (index > 0) ({ moveBy(item.id, -1) }) else null,
                            onMoveDown = if (index < local.lastIndex) ({ moveBy(item.id, 1) }) else null,
                        )
                    }
                }
            }
        }
    }

    if (showNewPicker) {
        DzwonDateTimePickerDialog(
            initial = draftRemindAt?.let(::epochMillisToLocalDateTime) ?: nextFullHour(),
            onDismiss = { showNewPicker = false },
            onConfirm = { picked ->
                draftRemindAt = picked.toEpochMillis()
                showNewPicker = false
            },
        )
    }

    val editing = editingId?.let { id -> local.firstOrNull { it.id == id } ?: items.firstOrNull { it.id == id } }
    // Punkt zniknął (np. usunięty) – zamykamy edycję.
    LaunchedEffect(editingId, editing == null, loaded) {
        if (editingId != null && editing == null && loaded) editingId = null
    }
    if (editing != null) {
        EditItemSheet(
            item = editing,
            onDismiss = { editingId = null },
            onSave = viewModel::edit,
            onDelete = { viewModel.delete(editing.id) },
        )
    }
}

/** Pasek nad listą: podpowiedź o przeciąganiu i złoty przycisk „Usuń zrobione (N)”. */
@Composable
private fun ListToolbar(doneCount: Int, showHint: Boolean, onClearDone: () -> Unit) {
    val c = LookTheme.dzwon
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 10.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (showHint) {
                Text(text = "Przytrzymaj kropki, żeby przesunąć", style = DzwonType.Caption, color = c.ink2)
            }
        }
        DzwonTextButton(
            text = if (doneCount > 0) "Usuń zrobione ($doneCount)" else "Usuń zrobione",
            onClick = onClearDone,
            enabled = doneCount > 0,
            icon = DzwonIcons.Trash,
        )
    }
}

/**
 * Jeden punkt na „linie”: lina (2 dp, złota przy zrobionych) przechodzi przez okrągły węzeł 26 dp,
 * obok tekst (zawijany) i godzina, po prawej sześć kropek do przeciągania. Przeciągany wiersz się
 * unosi: tło `lifted`, zaokrąglenie 16 dp, cień, obrót −1° i powiększenie 1,02.
 */
@Composable
private fun ReorderableCollectionItemScope.RopeRow(
    item: ChecklistItem,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean,
    now: Long,
    today: LocalDate,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    val c = LookTheme.dzwon
    val lift = animateFloatAsState(if (isDragging) 1f else 0f, tween(150), label = "ropeLift")
    val ropeColor by animateColorAsState(if (item.done) c.gold else c.track, tween(200), label = "ropeColor")
    val liftedColor = c.lifted
    val shadowColor = c.shadow
    val rowShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .graphicsLayer {
                val l = lift.value
                rotationZ = -1f * l
                scaleX = 1f + 0.02f * l
                scaleY = 1f + 0.02f * l
                shadowElevation = 14.dp.toPx() * l
                shape = rowShape
                clip = false
                ambientShadowColor = shadowColor
                spotShadowColor = shadowColor
            }
            .drawBehind {
                val l = lift.value
                if (l > 0f) {
                    drawRoundRect(
                        color = liftedColor.copy(alpha = liftedColor.alpha * l),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                    )
                }
                // Lina: u pierwszego punktu zaczyna się w węźle, u ostatniego w nim kończy.
                val x = RopeX.toPx()
                val top = if (isFirst) size.height / 2f else 0f
                val bottom = if (isLast) size.height / 2f else size.height
                if (bottom > top) {
                    drawLine(ropeColor, Offset(x, top), Offset(x, bottom), strokeWidth = 2.dp.toPx())
                }
            }
            .semantics {
                customActions = listOfNotNull(
                    CustomAccessibilityAction("Edytuj") {
                        onEdit()
                        true
                    },
                    onMoveUp?.let { move ->
                        CustomAccessibilityAction("Przesuń wyżej") {
                            move()
                            true
                        }
                    },
                    onMoveDown?.let { move ->
                        CustomAccessibilityAction("Przesuń niżej") {
                            move()
                            true
                        }
                    },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DzwonCheckbox(
            checked = item.done,
            onCheckedChange = { onToggle() },
            contentDescription = (if (item.done) "Odznacz: " else "Odhacz: ") + item.text,
            uncheckedFill = if (isDragging) c.lifted else c.sheet,
            modifier = Modifier.padding(2.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = "Edytuj punkt", onClick = onEdit)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.text,
                style = DzwonType.Body,
                color = if (item.done) c.ink2 else c.ink,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            )
            val remindAt = item.remindAt
            if (remindAt != null) {
                val past = !item.done && remindAt < now
                val color = when {
                    item.done -> c.ink2
                    past -> MaterialTheme.colorScheme.error
                    else -> c.goldText
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(DzwonIcons.Clock, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                    Text(
                        text = formatDzwonWhen(remindAt, today) + if (past) " · minęło" else "",
                        style = DzwonType.Time,
                        color = color,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 52.dp)
                .draggableHandle(
                    onDragStarted = { onDragStarted() },
                    onDragStopped = onDragStopped,
                )
                // Czytnik ekranu przesuwa punkty akcjami „Przesuń wyżej / niżej” na wierszu.
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(DzwonIcons.DragDots, contentDescription = null, tint = c.ink2, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Pasek dodawania na dole arkusza: (gdy wybrano godzinę) „Dzwon zadzwoni dziś o 18:00”,
 * pole „Dodaj punkt…”, przełącznik godziny (złoty, gdy włączony) i złoty „+”.
 */
@Composable
private fun AddBar(
    addedMessage: String?,
    onQuickTime: (ChecklistQuickTime) -> Unit,
    draft: String,
    onDraftChange: (String) -> Unit,
    remindAt: Long?,
    today: LocalDate,
    onTimeToggle: () -> Unit,
    onEditTime: () -> Unit,
    onSubmit: () -> Unit,
    timeError: Boolean,
) {
    val c = LookTheme.dzwon
    val lineColor = c.line
    // Ostatnia godzina zostaje na czas animacji chowania napisu.
    val lastAt = remember { longArrayOf(remindAt ?: 0L) }
    if (remindAt != null) lastAt[0] = remindAt
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(lineColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (addedMessage != null) Text(text = addedMessage, style = DzwonType.CaptionStrong, color = c.goldText)
        if (timeError) Text(text = "Wybierz przyszłą datę i godzinę.", style = DzwonType.Caption, color = MaterialTheme.colorScheme.error)
        AnimatedVisibility(visible = remindAt != null) {
            Text(
                text = "Dzwon zadzwoni ${formatDzwonAt(remindAt ?: lastAt[0], today)}",
                style = DzwonType.Time.copy(fontWeight = FontWeight.Bold),
                color = c.goldText,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = "Zmień godzinę", onClick = onEditTime)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            DzwonTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = "Dodaj punkt…",
                accessibilityLabel = "Nowy punkt",
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.weight(1f),
            )
            DzwonSquareIconButton(
                icon = DzwonIcons.Clock,
                contentDescription = "Godzina przypomnienia",
                onClick = onTimeToggle,
                selected = remindAt != null,
            )
            DzwonSquareIconButton(
                icon = DzwonIcons.Plus,
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
                DzwonChip(text = choice.label, onClick = { onQuickTime(choice) })
            }
            DzwonChip(text = "Wybierz…", onClick = onEditTime)
        }
    }
}

/**
 * Edycja punktu w arkuszu od dołu: tekst, data i godzina przypomnienia („Usuń godzinę”),
 * „Usuń punkt” (z „Cofnij” na liście), „Anuluj” i „Zapisz”.
 */
@Composable
private fun EditItemSheet(
    item: ChecklistItem,
    onDismiss: () -> Unit,
    onSave: (ChecklistItem) -> Unit,
    onDelete: () -> Unit,
) {
    val c = LookTheme.dzwon
    val sheetState = rememberDzwonSheetState()
    val scope = rememberCoroutineScope()
    var text by rememberSaveable(item.id) { mutableStateOf(item.text) }
    var remindAt by rememberSaveable(item.id) { mutableStateOf(item.remindAt) }
    var timeError by rememberSaveable(item.id, remindAt) { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    /** Chowa arkusz z animacją, potem wykonuje akcję i zamyka edycję. */
    fun closeThen(action: () -> Unit) {
        sheetState.hideThen(scope) {
            action()
            onDismiss()
        }
    }

    DzwonBottomSheet(onDismissRequest = onDismiss, title = "Edytuj punkt", sheetState = sheetState) {
        DzwonTextField(
            value = text,
            onValueChange = { text = it },
            label = "Treść",
            placeholder = "Co zrobić?",
            singleLine = false,
            maxLines = 6,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Przypomnienie",
                style = DzwonType.CaptionStrong,
                color = c.ink2,
                modifier = Modifier.padding(start = 6.dp),
            )
            val at = remindAt
            if (at == null) {
                DzwonSecondaryButton(text = "Dodaj godzinę", onClick = { showPicker = true }, icon = DzwonIcons.Clock)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DzwonChip(
                        text = formatDzwonWhen(at),
                        onClick = { showPicker = true },
                        selected = true,
                        icon = DzwonIcons.Clock,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    DzwonTextButton(
                        text = "Usuń godzinę",
                        onClick = { remindAt = null },
                        icon = DzwonIcons.Close,
                        color = c.ink2,
                    )
                }
            }
        }

        if (timeError) Text(text = "Wybierz przyszłą datę i godzinę.", style = DzwonType.Caption, color = MaterialTheme.colorScheme.error)

        DzwonTextButton(
            text = "Usuń punkt",
            onClick = { closeThen(onDelete) },
            icon = DzwonIcons.Trash,
            color = MaterialTheme.colorScheme.error,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DzwonTextButton(text = "Anuluj", onClick = { closeThen {} }, color = c.ink)
            DzwonPrimaryButton(
                text = "Zapisz",
                enabled = text.isNotBlank(),
                onClick = {
                    timeError = remindAt != item.remindAt && remindAt?.let { it <= System.currentTimeMillis() } == true
                    if (!timeError) closeThen { onSave(item.copy(text = text.trim(), remindAt = remindAt)) }
                },
            )
        }
    }

    if (showPicker) {
        DzwonDateTimePickerDialog(
            initial = remindAt?.let(::epochMillisToLocalDateTime) ?: nextFullHour(),
            onDismiss = { showPicker = false },
            onConfirm = { picked ->
                remindAt = picked.toEpochMillis()
                showPicker = false
            },
        )
    }
}
