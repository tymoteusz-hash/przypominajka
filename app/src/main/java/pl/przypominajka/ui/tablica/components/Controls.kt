package pl.przypominajka.ui.tablica.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor

/** Bieżący „papier” albo – gdy go brak – kolor tablicy. */
@Composable
internal fun currentPaper(): Color = LocalPaper.current.takeOrElse { LookTheme.tablica.board }

// ---------------------------------------------------------------- pola wyboru

/**
 * Okrągłe pole wyboru „pisane ręcznie”: obwódka 2,5 dp w kolorze atramentu; zaznaczone – wypełnione
 * atramentem, a ptaszek w kolorze papieru rysuje się kreską (180 ms). Cel dotyku 48 dp.
 * Domyślne kolory z bieżącej karteczki ([LocalContentColor] / [LocalPaper]).
 */
@Composable
fun NoteCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    ink: Color = LocalContentColor.current,
    paper: Color = currentPaper(),
    size: Dp = 24.dp,
) {
    val progress by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "noteCheck")
    Box(
        modifier = modifier
            .size(48.dp)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = 2.5.dp.toPx()
            val radius = this.size.minDimension / 2f
            if (progress > 0f) drawCircle(ink.copy(alpha = ink.alpha * progress), radius = radius)
            drawCircle(ink, radius = radius - stroke / 2f, style = Stroke(stroke))
            if (progress > 0f) {
                // Ptaszek z makiety (ikona 14/24 rozmiaru kółka, przesunięta o 5/24).
                val unit = this.size.width / 24f
                val k = unit * 14f / 24f
                val o = 5f * unit
                drawCheck(
                    points = listOf(Offset(o + 5f * k, o + 12.5f * k), Offset(o + 9.5f * k, o + 17f * k), Offset(o + 19f * k, o + 7.5f * k)),
                    color = paper,
                    width = 3.6f * k,
                    progress = progress,
                )
            }
        }
    }
}

/**
 * Mały kwadratowy „ręczny” znaczek (14 dp) – do podglądu listy na karteczce. Sam nie reaguje na dotyk:
 * klikalny ma być cały wiersz (toggleable z Role.Checkbox).
 */
@Composable
fun NoteSquareCheck(
    checked: Boolean,
    modifier: Modifier = Modifier,
    ink: Color = LocalContentColor.current,
    paper: Color = currentPaper(),
    size: Dp = 14.dp,
) {
    val progress by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "squareCheck")
    Canvas(modifier.size(size)) {
        val stroke = 2.dp.toPx()
        val corner = CornerRadius(3.dp.toPx())
        if (progress > 0f) drawRoundRect(ink.copy(alpha = ink.alpha * progress), cornerRadius = corner)
        drawRoundRect(
            color = ink,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            cornerRadius = corner,
            style = Stroke(stroke),
        )
        if (progress > 0f) {
            val k = this.size.width / 24f
            drawCheck(
                points = listOf(Offset(5f * k, 12.5f * k), Offset(9.5f * k, 17f * k), Offset(19f * k, 7.5f * k)),
                color = paper,
                width = 4f * k,
                progress = progress,
            )
        }
    }
}

/** Ptaszek rysowany kreską do [progress] (0–1) długości. */
private fun DrawScope.drawCheck(points: List<Offset>, color: Color, width: Float, progress: Float) {
    val full = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    val path = if (progress >= 1f) {
        full
    } else {
        val measure = PathMeasure().apply { setPath(full, false) }
        Path().also { measure.getSegment(0f, measure.length * progress, it, true) }
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * Przełącznik w stylu Tablicy: włączony – tor w kolorze atramentu, gałka w kolorze papieru;
 * wyłączony – obwódka atramentu, tor przezroczysty.
 */
@Composable
fun TablicaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val ink = LocalContentColor.current
    val paper = currentPaper()
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = paper,
            checkedTrackColor = ink,
            checkedBorderColor = ink,
            checkedIconColor = ink,
            uncheckedThumbColor = ink,
            uncheckedTrackColor = Color.Transparent,
            uncheckedBorderColor = ink,
            uncheckedIconColor = paper,
            disabledCheckedThumbColor = paper.copy(alpha = 0.6f),
            disabledCheckedTrackColor = ink.copy(alpha = 0.38f),
            disabledCheckedBorderColor = ink.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = ink.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = Color.Transparent,
            disabledUncheckedBorderColor = ink.copy(alpha = 0.38f),
        ),
    )
}

// ---------------------------------------------------------------- przyciski

private val PillShape = RoundedCornerShape(percent = 50)

/**
 * Główny przycisk: pigułka wypełniona atramentem, napis w kolorze papieru (Nunito 15 ExtraBold),
 * wysokość ≥ 48 dp. Na karteczce automatycznie w jej kolorach.
 */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val ink = LocalContentColor.current
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        decoration = Modifier
            .clip(PillShape)
            .background(ink),
        enabled = enabled,
        icon = icon,
        contentColor = currentPaper(),
        horizontalPadding = 20.dp,
    )
}

/** Drugorzędny przycisk: pigułka z obwódką 2 dp w kolorze atramentu, przezroczysta („Usuń zrobione”). */
@Composable
fun OutlineInkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val ink = LocalContentColor.current
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        decoration = Modifier
            .clip(PillShape)
            .border(2.dp, ink, PillShape),
        enabled = enabled,
        icon = icon,
        contentColor = ink,
        horizontalPadding = 14.dp,
    )
}

/** Przycisk tekstowy (bez tła i ramki), np. „Anuluj”. */
@Composable
fun InkTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = LocalContentColor.current,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        decoration = Modifier.clip(PillShape),
        enabled = enabled,
        icon = icon,
        contentColor = color,
        horizontalPadding = 14.dp,
    )
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    decoration: Modifier,
    enabled: Boolean,
    icon: ImageVector?,
    contentColor: Color,
    horizontalPadding: Dp,
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .then(decoration)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Text(text = text, style = TablicaType.Button, color = contentColor, textAlign = TextAlign.Center)
    }
}

/** Wygląd okrągłego przycisku z ikoną. */
enum class RoundIconStyle {
    /** Obwódka 2 dp w kolorze atramentu (np. „Konto” w nagłówku). */
    Outlined,

    /** Wypełniony atramentem, ikona w kolorze papieru (np. „Przyklej karteczkę”). */
    Filled,

    /** Sama ikona. */
    Plain,
}

/**
 * Okrągły przycisk z ikoną: kółko [size] (domyślnie 44 dp jak w makiecie) w celu dotyku 48 dp.
 * [contentDescription] jest obowiązkowy – czytnik ekranu nie ma innego opisu.
 */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RoundIconStyle = RoundIconStyle.Outlined,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
) {
    val ink = LocalContentColor.current
    val paper = currentPaper()
    Box(
        modifier = modifier
            .size(maxOf(size, 48.dp))
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        val circle = Modifier
            .size(size)
            .clip(CircleShape)
        val visual = when (style) {
            RoundIconStyle.Outlined -> circle.border(2.dp, ink, CircleShape)
            RoundIconStyle.Filled -> circle.background(ink)
            RoundIconStyle.Plain -> circle
        }
        Box(visual, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (style == RoundIconStyle.Filled) paper else ink,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/**
 * Okrągły przełącznik z ikoną (np. „Godzina przypomnienia” przy nowej karteczce): włączony –
 * wypełniony atramentem, wyłączony – przezroczysty z ikoną w kolorze ink2.
 */
@Composable
fun RoundIconToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val ink = LocalContentColor.current
    val paper = currentPaper()
    val background by animateColorAsState(if (checked) ink else Color.Transparent, tween(150), label = "toggleBg")
    val tint by animateColorAsState(
        if (checked) paper else ink.copy(alpha = 0.72f),
        tween(150),
        label = "toggleTint",
    )
    Box(
        modifier = modifier
            .size(maxOf(size, 48.dp))
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
        }
    }
}

// ---------------------------------------------------------------- chipy i odznaki

/**
 * Chip-pigułka (wybór folderu, dnia, godziny…): wybrany – atrament z napisem w kolorze papieru,
 * niewybrany – obwódka 2 dp. Wizualnie 36 dp, cel dotyku 48 dp.
 */
@Composable
fun TablicaChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val ink = LocalContentColor.current
    val paper = currentPaper()
    val background by animateColorAsState(if (selected) ink else Color.Transparent, tween(150), label = "chipBg")
    val content = if (selected) paper else ink
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(PillShape)
            .background(background)
            .border(2.dp, ink, PillShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Text(text = text, style = TablicaType.Button.copy(fontSize = TablicaType.Hint.fontSize), color = content)
    }
}

/**
 * Mała odznaka (np. „Librus”, „za 3 dni”, licznik): prostokącik 4 dp w kolorze karteczki [color];
 * bez koloru – atrament z napisem w kolorze papieru.
 */
@Composable
fun NoteBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: NoteColor? = null,
) {
    val background = color?.bg ?: LocalContentColor.current
    val content = color?.ink ?: currentPaper()
    Text(
        text = text,
        style = TablicaType.Tag,
        color = content,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
