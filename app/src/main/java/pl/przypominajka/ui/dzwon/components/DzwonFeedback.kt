package pl.przypominajka.ui.dzwon.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pl.przypominajka.ui.look.LookTheme

/**
 * Pasek „Cofnij” z makiety: tło `toastBg`, tekst `toastInk`, złoty przycisk akcji.
 * Zwykle używaj [DzwonUndoHost], który sam go pokazuje i chowa.
 */
@Composable
fun DzwonUndoBar(
    message: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "Cofnij",
) {
    val c = LookTheme.dzwon
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.toastBg)
            .padding(start = 18.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = DzwonType.RowTitle,
            color = c.toastInk,
            modifier = Modifier.weight(1f),
        )
        CompositionLocalProvider(LocalContentColor provides c.onGold) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.gold)
                    .clickable(role = Role.Button, onClick = onUndo)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = actionLabel, style = DzwonType.Button, color = c.onGold)
            }
        }
    }
}

/**
 * Pokazuje [DzwonUndoBar], gdy [message] nie jest null (wjeżdża od dołu, 200 ms), i chowa go,
 * gdy zmieni się na null – np. `message = undoState?.message` z ViewModelu.
 */
@Composable
fun DzwonUndoHost(
    message: String?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ostatni tekst zostaje na czas animacji chowania (zwykła pamięć, nie stan – bez zapisu stanu w kompozycji).
    val lastMessage = remember { arrayOf(message.orEmpty()) }
    if (message != null) lastMessage[0] = message
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(150)) { it / 2 } + fadeOut(tween(150)),
    ) {
        DzwonUndoBar(message = message ?: lastMessage[0], onUndo = onUndo)
    }
}

/**
 * Okno dialogowe Dzwonu: tło arkusza, zaokrąglenie 28 dp, tytuł Fraunces, [text] albo własna
 * treść [content], przyciski po prawej (tekstowe „Anuluj” + złoty [confirmText]).
 * [destructive] – przycisk potwierdzenia w kolorze ostrzeżenia (np. „Usuń”).
 * [dismissText] = null – bez przycisku anulowania.
 */
@Composable
fun DzwonDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissText: String? = "Anuluj",
    onDismiss: () -> Unit = onDismissRequest,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = LookTheme.dzwon
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CompositionLocalProvider(LocalContentColor provides c.ink) {
            Column(
                modifier = modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.sheet)
                    .padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = DzwonType.SheetTitle,
                    color = c.ink,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .semantics { heading() },
                )
                if (text != null) {
                    Text(
                        text = text,
                        style = DzwonType.Body,
                        color = c.ink2,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                if (content != null) {
                    Column(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissText != null) {
                        DzwonTextButton(text = dismissText, onClick = onDismiss, color = c.ink)
                    }
                    if (destructive) {
                        DzwonTextButton(
                            text = confirmText,
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        DzwonPrimaryButton(text = confirmText, onClick = onConfirm, enabled = confirmEnabled)
                    }
                }
            }
        }
    }
}
