package pl.przypominajka.ui.tablica.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor

// ---------------------------------------------------------------- rama ekranu

/**
 * Tło ekranu Tablicy: kolor tablicy z kropkami, LocalContentColor = atrament, [LocalPaper] = tablica.
 * Nie dodaje odstępów – treść sama bierze pasek stanu ([statusBarsPadding] / [dockContentPadding])
 * i miejsce na dok ([LocalDockPadding]).
 */
@Composable
fun TablicaScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LookTheme.tablica
    Box(
        modifier = modifier
            .fillMaxSize()
            .boardBackground(colors),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.ink, LocalPaper provides colors.board) {
            content()
        }
    }
}

/**
 * Gotowy przewijany ekran działu z dokiem: tablica, nagłówek [BoardHeader] ([title], [subtitle],
 * [trailing] po prawej) i treść w kolumnie (odstępy 18 dp, boki 16 dp). Na dole miejsce na dok.
 */
@Composable
fun TablicaScrollScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    TablicaScreen(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = LocalDockPadding.current + 16.dp),
        ) {
            BoardHeader(
                title = title,
                subtitle = subtitle,
                trailing = trailing,
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp),
            )
            Spacer(Modifier.height(22.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                content = content,
            )
        }
    }
}

/**
 * Nagłówek ekranu na tablicy: odręczny tytuł (Caveat 48) i pod nim [subtitle] (Nunito 15, ink2);
 * po prawej [trailing] (np. okrągły przycisk „Konto”). Bez odstępów zewnętrznych.
 */
@Composable
fun BoardHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = LookTheme.tablica
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = TablicaType.ScreenTitle, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(text = subtitle, style = TablicaType.Subtitle, color = colors.ink2)
        }
        trailing()
    }
}

/**
 * Pasek górny ekranów szczegółów (bez doku): okrągły przycisk „Wróć”, odręczny tytuł (Caveat 34,
 * zawija się) i [actions]. Sam dodaje odstęp paska stanu.
 */
@Composable
fun TablicaTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LookTheme.tablica
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(icon = TablicaIcons.Back, contentDescription = "Wróć", onClick = onBack)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = TablicaType.DialogTitle, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(text = subtitle, style = TablicaType.Hint, color = colors.ink2)
        }
        actions()
    }
}

/** Odręczny nagłówek sekcji (Caveat 28) z opcjonalnym dopiskiem [trailing] po prawej (Nunito, ink2). */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = TablicaType.Section,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (trailing != null) Text(text = trailing, style = TablicaType.Hint, color = LookTheme.tablica.ink2)
    }
}

// ---------------------------------------------------------------- wiersze

/**
 * Wiersz listy – na karteczce albo na tablicy: [leading] (ikona, znaczek), tytuł (Nunito 16 ExtraBold,
 * zawija się), [subtitle] (13 SemiBold, przygaszony) i [trailing] (przełącznik, odznaka, strzałka).
 * Wysokość ≥ 56 dp; z [onClick] cały wiersz jest przyciskiem.
 */
@Composable
fun NoteRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) leading()
        Column(Modifier.weight(1f)) {
            Text(text = title, style = TablicaType.Strong)
            if (subtitle != null) {
                Text(text = subtitle, style = TablicaType.Hint, modifier = Modifier.alpha(0.78f))
            }
        }
        if (trailing != null) trailing()
    }
}

/**
 * Wiersz z przełącznikiem – cały wiersz przełącza (Role.Switch), więc dotyk działa wszędzie.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = TablicaType.Strong)
            if (subtitle != null) {
                Text(text = subtitle, style = TablicaType.Hint, modifier = Modifier.alpha(0.78f))
            }
        }
        // Przełącznik tylko pokazuje stan – dotyk obsługuje cały wiersz.
        TablicaSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Przerywana kreska między wierszami na karteczce (kolor [NoteColor.rule] albo przygaszony atrament). */
@Composable
fun NoteDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current.copy(alpha = 0.22f),
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            cap = StrokeCap.Butt,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        )
    }
}

// ---------------------------------------------------------------- pusty stan

/**
 * Pusty stan: przekrzywiona karteczka z taśmą na środku tablicy – odręczny [title], wyjaśnienie [text]
 * i opcjonalnie [action] (np. [InkButton]). Szerokość do 340 dp.
 */
@Composable
fun BoardEmptyState(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    color: NoteColor = LookTheme.tablica.yellow,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Note(
            color = color,
            tilt = -1.5f,
            decoration = NoteDecoration.Tape,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 24.dp,
                end = 20.dp,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = TablicaType.NoteTitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(text = text, style = TablicaType.BodyLight, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            if (action != null) {
                Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) { action() }
            }
        }
    }
}
