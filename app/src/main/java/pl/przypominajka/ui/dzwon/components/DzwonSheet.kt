package pl.przypominajka.ui.dzwon.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.przypominajka.ui.look.LookTheme

/** Zaokrąglenie arkuszy Dzwonu (górne rogi 32 dp). */
val DzwonSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/**
 * Rama każdego ekranu Dzwonu poza stroną główną – ekran „wysuwa się spod dzwonu”.
 *
 * Na górze (tło `bg`): okrągły przycisk „Zwiń” (lewy górny róg), na środku [headerExtra]
 * (domyślnie mały dzwon; np. [DzwonBellInRing] z postępem), tytuł Fraunces 30 sp i [subtitle].
 * [actions] – opcjonalne okrągłe przyciski w prawym górnym rogu.
 * Niżej arkusz (`sheet`, górne rogi 32 dp, uchwyt) z treścią [content] (ColumnScope – lista
 * z `Modifier.weight(1f)` albo przewijana kolumna) i opcjonalnym paskiem [bottomBar] na dole.
 * [overlay] rysuje się nad treścią arkusza, tuż nad paskiem (np. [DzwonUndoHost] z `Alignment.BottomCenter`).
 *
 * Rama sama obsługuje paski systemowe i klawiaturę: [bottomBar] (albo dół treści) jest podnoszony
 * nad pasek nawigacji i klawiaturę, a przy otwartej klawiaturze nagłówek chowa dzwon i podtytuł.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DzwonSheetScaffold(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    headerExtra: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LookTheme.dzwon
    val compact = WindowInsets.isImeVisible
    CompositionLocalProvider(LocalContentColor provides c.ink) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(c.bg),
        ) {
            // ------------------------------------------------ nagłówek nad arkuszem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedVisibility(
                        visible = !compact,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (headerExtra != null) headerExtra() else DzwonBell(size = 52.dp)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Text(
                        text = title,
                        style = DzwonType.ScreenTitle,
                        color = c.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 60.dp)
                            .semantics { heading() },
                    )
                    AnimatedVisibility(
                        visible = subtitle != null && !compact,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
                    ) {
                        Text(
                            text = subtitle.orEmpty(),
                            style = DzwonType.RowTitle,
                            color = c.ink2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp),
                        )
                    }
                }
                DzwonRoundIconButton(
                    icon = DzwonIcons.ChevronDown,
                    contentDescription = "Zwiń",
                    onClick = onClose,
                    iconSize = 22.dp,
                    modifier = Modifier.padding(start = 10.dp, top = 12.dp),
                )
                if (actions != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 10.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }

            // ------------------------------------------------ arkusz
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(DzwonSheetShape)
                    .background(c.sheet),
            ) {
                DzwonSheetHandle(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 4.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Column(modifier = Modifier.fillMaxSize(), content = content)
                    if (overlay != null) overlay()
                }
                if (bottomBar != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.sheet)
                            .navigationBarsPadding()
                            .imePadding(),
                    ) {
                        bottomBar()
                    }
                } else {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars.union(WindowInsets.ime)))
                }
            }
        }
    }
}

/** Stan arkusza dolnego Dzwonu – od razu w pełni rozwinięty. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberDzwonSheetState(): SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

/** Chowa arkusz z animacją, a potem wykonuje [action] (np. wyzerowanie stanu „co edytuję”). */
@OptIn(ExperimentalMaterial3Api::class)
fun SheetState.hideThen(scope: CoroutineScope, action: () -> Unit) {
    scope.launch { hide() }.invokeOnCompletion { if (!isVisible) action() }
}

/**
 * Arkusz dolny (okno od dołu) w stylu Dzwonu: tło `surface`, górne rogi 32 dp, uchwyt, przyciemnienie
 * jak w makiecie, opcjonalny tytuł Fraunces 22 sp. Treść się przewija i unosi nad klawiaturę.
 * Żeby zamknąć z animacją z przycisku: `sheetState.hideThen(scope) { pokazany = false }`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DzwonBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberDzwonSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LookTheme.dzwon
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = DzwonSheetShape,
        containerColor = c.surface,
        contentColor = c.ink,
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0A091E).copy(alpha = 0.5f),
        dragHandle = { DzwonSheetHandle(Modifier.padding(top = 10.dp, bottom = 6.dp)) },
    ) {
        CompositionLocalProvider(LocalContentColor provides c.ink) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = DzwonType.SheetTitle,
                        color = c.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                content()
            }
        }
    }
}
