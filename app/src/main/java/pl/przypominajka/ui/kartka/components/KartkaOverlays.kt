package pl.przypominajka.ui.kartka.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import pl.przypominajka.ui.look.LookTheme

/** Przyciemnienie pod oknami i arkuszem (jak w makiecie: rgba(8,10,14,0.45)). */
internal val KartkaScrim = Color(0xFF080A0E).copy(alpha = 0.45f)

/**
 * Ciemny (w ciemnym motywie jasny) zaokrąglony pasek z komunikatem i opcjonalną akcją w kolorze `toastAction`.
 * Podstawa [KartkaUndoBar] i [KartkaSnackbarHost].
 */
@Composable
fun KartkaToast(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = LookTheme.kartka
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(KartkaDefaults.TileShape)
            .background(c.ink)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            .heightIn(min = KartkaDefaults.TouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = KartkaType.BodySmall,
            color = c.bg,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        )
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = KartkaDefaults.TouchTarget)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    style = KartkaType.Button.copy(fontWeight = FontWeight.Bold),
                    color = c.toastAction,
                )
            }
        }
    }
}

/** Pasek „Cofnij” po usunięciu: komunikat (np. „Usunięto 3 punkty”) i przycisk „Cofnij”. */
@Composable
fun KartkaUndoBar(
    message: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "Cofnij",
) {
    KartkaToast(message = message, modifier = modifier, actionLabel = actionLabel, onAction = onUndo)
}

/**
 * Miejsce na pasek „Cofnij” (do `snackbar` w [KartkaScaffold]): pokazuje [KartkaUndoBar], gdy [message] nie jest
 * null (np. `undoState?.message` z ViewModelu), i chowa go z krótką animacją. Czas wyświetlania liczy ViewModel.
 */
@Composable
fun KartkaUndoHost(
    message: String?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ostatni komunikat zostaje na czas animacji chowania.
    val last = remember { mutableStateOf(message.orEmpty()) }
    if (message != null && message != last.value) SideEffect { last.value = message }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(KartkaMotion.SHORT_MS)) +
            slideInVertically(tween(KartkaMotion.DURATION_MS)) { it / 2 },
        exit = fadeOut(tween(KartkaMotion.SHORT_MS)) +
            slideOutVertically(tween(KartkaMotion.SHORT_MS)) { it / 2 },
    ) {
        KartkaUndoBar(
            message = message ?: last.value,
            onUndo = onUndo,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** [SnackbarHost] w stylu Kartki – dla ekranów, które pokazują komunikaty przez [SnackbarHostState]. */
@Composable
fun KartkaSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        KartkaToast(
            message = data.visuals.message,
            actionLabel = data.visuals.actionLabel,
            onAction = data::performAction,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/**
 * Okno dialogowe Kartki: jasna/uniesiona plansza (raised), zaokrąglenie 24 dp, tytuł 18 sp, opcjonalny tekst
 * i własna treść (np. [KartkaTextField]), przyciski po prawej: [dismissLabel] (grafitowy) i [confirmLabel]
 * ([KartkaButton], czerwony przy [destructive]). Treść przewija się, gdy jest długa.
 */
@Composable
fun KartkaDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    confirmLabel: String? = null,
    onConfirm: () -> Unit = {},
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissLabel: String? = "Anuluj",
    onDismiss: () -> Unit = onDismissRequest,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = LookTheme.kartka
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .padding(16.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(KartkaDefaults.DialogShape)
                .background(c.raised)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = title,
                    style = KartkaType.Title,
                    color = c.ink,
                    modifier = Modifier.semantics { heading() },
                )
                if (text != null) {
                    Text(text = text, style = KartkaType.BodySmall, color = c.ink2)
                }
                content?.invoke(this)
            }
            if (confirmLabel != null || dismissLabel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissLabel != null) {
                        KartkaTextButton(text = dismissLabel, onClick = onDismiss, color = c.ink)
                    }
                    if (confirmLabel != null) {
                        KartkaButton(
                            text = confirmLabel,
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                            destructive = destructive,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Arkusz dolny Kartki (Material 3 ModalBottomSheet w kolorach Kartki): uniesiona plansza, zaokrąglenie 24 dp,
 * cienki uchwyt, opcjonalny tytuł 18 sp, przewijana treść z marginesami 20 dp. Sam omija klawiaturę.
 * Treść dostaje `close()` – chowa arkusz z animacją, a potem woła [onDismissRequest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KartkaBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val c = LookTheme.kartka
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismissRequest()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = c.raised,
        contentColor = c.ink,
        tonalElevation = 0.dp,
        scrimColor = KartkaScrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(KartkaDefaults.PillShape)
                    .background(c.line),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = KartkaType.Title,
                    color = c.ink,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content(close)
        }
    }
}
