package pl.przypominajka.ui.dzwon.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme

/**
 * Okrągły „węzeł” checklisty (sam wygląd, bez obsługi dotyku): odhaczony – złote koło
 * z ciemnym ptaszkiem, nieodhaczony – obwódka 2,5 dp w kolorze `ink2` na tle [uncheckedFill].
 */
@Composable
fun DzwonCheckNode(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    uncheckedFill: Color = LookTheme.dzwon.sheet,
) {
    val c = LookTheme.dzwon
    val progress = animateFloatAsState(if (checked) 1f else 0f, tween(160), label = "checkNode")
    val fill by animateColorAsState(if (checked) c.gold else uncheckedFill, tween(160), label = "checkFill")
    val border by animateColorAsState(if (checked) c.gold else c.ink2, tween(160), label = "checkBorder")
    val checkColor = c.onGold
    val check = remember { Path() }
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val borderPx = 2.5.dp.toPx()
        drawCircle(fill, radius = r)
        drawCircle(border, radius = r - borderPx / 2f, style = Stroke(borderPx))
        val p = progress.value
        if (p > 0.01f) {
            // Ptaszek z makiety (siatka 24×24) w polu 14/26 węzła.
            val box = this.size.minDimension * (14f / 26f)
            val unit = box / 24f
            val origin = Offset((this.size.width - box) / 2f, (this.size.height - box) / 2f)
            check.reset()
            check.moveTo(origin.x + 5f * unit, origin.y + 12.5f * unit)
            check.lineTo(origin.x + 9.5f * unit, origin.y + 17f * unit)
            check.lineTo(origin.x + 19f * unit, origin.y + 7.5f * unit)
            drawPath(
                path = check,
                color = checkColor,
                alpha = p,
                style = Stroke(width = 3.4f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * Pole wyboru Dzwonu (okrągły węzeł) z polem dotyku 48 dp. [contentDescription] – np. „Odhacz: Kupić zeszyt”.
 */
@Composable
fun DzwonCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    size: Dp = 26.dp,
    uncheckedFill: Color = LookTheme.dzwon.sheet,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        DzwonCheckNode(checked = checked, size = size, uncheckedFill = uncheckedFill)
    }
}

/** Przełącznik w kolorach Dzwonu: włączony – złoty tor i ciemna gałka, wyłączony – tło pola. */
@Composable
fun DzwonSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LookTheme.dzwon
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onGold,
            checkedTrackColor = c.gold,
            checkedBorderColor = c.gold,
            uncheckedThumbColor = c.ink2,
            uncheckedTrackColor = c.field,
            uncheckedBorderColor = c.ink2,
        ),
    )
}

/**
 * Pole tekstowe Dzwonu: tło `field`, zaokrąglenie 16 dp, wysokość ≥ 50 dp, złota obwódka przy fokusie.
 * [label] – podpis nad polem (opcjonalny), [placeholder] – podpowiedź w pustym polu.
 * Przy `singleLine = false` pole rośnie do [maxLines] wierszy.
 */
@Composable
fun DzwonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    containerColor: Color = LookTheme.dzwon.field,
    accessibilityLabel: String? = null,
) {
    val c = LookTheme.dzwon
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = DzwonType.CaptionStrong,
                color = c.ink2,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            textStyle = DzwonType.Body.copy(color = c.ink),
            cursorBrush = SolidColor(c.goldText),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (accessibilityLabel != null) {
                        Modifier.semantics { contentDescription = accessibilityLabel }
                    } else {
                        Modifier
                    },
                ),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .clip(shape)
                        .background(containerColor)
                        .border(2.dp, if (focused) c.gold else Color.Transparent, shape)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(text = placeholder, style = DzwonType.Body, color = c.ink2, maxLines = 1)
                    }
                    inner()
                }
            },
        )
    }
}
