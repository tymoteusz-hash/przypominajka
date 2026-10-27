@file:OptIn(ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.look.TablicaColors

// ---------------------------------------------------------------- pasek „Cofnij”

/**
 * Pasek „Cofnij” w kolorach doku: komunikat (Nunito 15 Bold) i żółty przycisk [actionLabel].
 * Widoczny, gdy [message] != null (wjeżdża od dołu, 200 ms). Ustaw go nad dokiem:
 * `Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = LocalDockPadding.current)`.
 */
@Composable
fun UndoBar(
    message: String?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "Cofnij",
) {
    val colors = LookTheme.tablica
    // Ostatni komunikat zostaje na czas znikania paska (zwykły schowek, nie stan – nic nie przelicza).
    val holder = remember { MessageHolder() }
    if (message != null) holder.text = message
    val lastMessage = holder.text
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 2 },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.dock)
                .padding(start = 18.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = lastMessage,
                style = TablicaType.Subtitle,
                color = colors.dockText,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.dockActive)
                    .clickable(role = Role.Button, onClick = onUndo)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = actionLabel, style = TablicaType.Button, color = PinInk)
            }
        }
    }
}

private class MessageHolder {
    var text: String = ""
}

// ---------------------------------------------------------------- okna

/**
 * Okno w kształcie karteczki z taśmą (przekrzywione o [tilt]): odręczny [title] (Caveat 34),
 * przewijana treść [content] i rząd przycisków [buttons] (do prawej, np. [NoteDialogButtons]).
 * Kolor domyślnie żółty; w środku LocalContentColor/[LocalPaper] = kolory karteczki.
 */
@Composable
fun NoteDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    color: NoteColor = LookTheme.tablica.yellow,
    tilt: Float = -1.5f,
    buttons: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Note(
                color = color,
                tilt = tilt,
                decoration = NoteDecoration.Tape,
                modifier = modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 18.dp, top = 24.dp, end = 18.dp, bottom = 14.dp),
            ) {
                Text(text = title, style = TablicaType.DialogTitle, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    content = content,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = buttons,
                )
            }
        }
    }
}

/** Typowe przyciski okna: tekstowy [dismissText] i wypełniony [confirmText] (nieaktywny bez [confirmEnabled]). */
@Composable
fun RowScope.NoteDialogButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Anuluj",
    confirmEnabled: Boolean = true,
) {
    InkTextButton(text = dismissText, onClick = onDismiss)
    InkButton(text = confirmText, onClick = onConfirm, enabled = confirmEnabled)
}

/** Pytanie z potwierdzeniem (np. „Usunąć folder?”) jako karteczka; [color] domyślnie różowa. */
@Composable
fun ConfirmNoteDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Anuluj",
    color: NoteColor = LookTheme.tablica.pink,
) {
    NoteDialog(
        onDismissRequest = onDismiss,
        title = title,
        color = color,
        buttons = {
            NoteDialogButtons(
                confirmText = confirmText,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                dismissText = dismissText,
            )
        },
    ) {
        Text(text = text, style = TablicaType.BodyLight)
    }
}

// ---------------------------------------------------------------- arkusz dolny

/**
 * Arkusz od dołu w kolorze tablicy (rogi 24 dp) z odręcznym [title]. Treść dostaje `close(then)`:
 * wykonuje akcję `then` (np. nawigację) i chowa arkusz z animacją – używaj go w przyciskach arkusza.
 */
@Composable
fun TablicaSheet(
    onDismissRequest: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.(close: (then: () -> Unit) -> Unit) -> Unit,
) {
    val colors = LookTheme.tablica
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { then ->
        then()
        scope.launch { state.hide() }.invokeOnCompletion { onDismissRequest() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colors.board,
        contentColor = colors.ink,
        dragHandle = { SheetHandle(colors) },
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.ink, LocalPaper provides colors.board) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = TablicaType.DialogTitle,
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 2.dp)
                            .semantics { heading() },
                    )
                }
                content(close)
            }
        }
    }
}

@Composable
private fun SheetHandle(colors: TablicaColors) {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 12.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.ink2.copy(alpha = 0.5f)),
    )
}

/**
 * Wiersz-karteczka w arkuszu (np. „Więcej”, „Co dodać?”): kolorowa karteczka z ikoną i napisem
 * (Nunito 17 ExtraBold), lekko przekrzywiona [tilt]; cała jest przyciskiem, wysokość ≥ 56 dp.
 */
@Composable
fun SheetNoteRow(
    text: String,
    icon: ImageVector,
    color: NoteColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tilt: Float = 0f,
) {
    Note(
        color = color,
        tilt = tilt,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(text = text, style = TablicaType.Strong.copy(fontSize = TablicaType.Strong.fontSize * 1.06f))
                if (subtitle != null) Text(text = subtitle, style = TablicaType.Hint)
            }
        }
    }
}
