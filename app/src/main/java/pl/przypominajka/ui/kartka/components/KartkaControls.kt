package pl.przypominajka.ui.kartka.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme

/**
 * Pole wyboru Kartki: cel dotyku 48 dp, w środku zaokrąglony kwadrat [boxSize] (22 dp na liście, 18 dp
 * w podglądzie) – pusty z obwódką ink2, zaznaczony wypełniony akcentem z białym ptaszkiem.
 * @param onCheckedChange null = tylko wygląd (gdy cały wiersz jest `toggleable`).
 * @param contentDescription np. „Odhacz: Kupić zeszyt” – dla czytnika ekranu.
 */
@Composable
fun KartkaCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    boxSize: Dp = 22.dp,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val toggle = if (onCheckedChange != null) {
        Modifier
            .toggleable(
                value = checked,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 20.dp),
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(KartkaDefaults.TouchTarget)
            .then(toggle)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        KartkaCheckMark(checked = checked, size = boxSize)
    }
}

/** Sam kwadracik pola wyboru (bez celu dotyku) – do wierszy, które w całości są przełącznikiem. */
@Composable
fun KartkaCheckMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val c = LookTheme.kartka
    val shape = RoundedCornerShape(size * 0.32f)
    val fill by animateColorAsState(
        targetValue = if (checked) c.accent else Color.Transparent,
        animationSpec = tween(KartkaMotion.SHORT_MS),
        label = "checkFill",
    )
    val border by animateColorAsState(
        targetValue = if (checked) c.accent else c.ink2,
        animationSpec = tween(KartkaMotion.SHORT_MS),
        label = "checkBorder",
    )
    val tick by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(KartkaMotion.SHORT_MS),
        label = "checkTick",
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(fill)
            .border(2.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (tick > 0f) {
            Icon(
                imageVector = KartkaIcons.Check,
                contentDescription = null,
                tint = c.onAccent,
                modifier = Modifier
                    .size(size * 0.64f)
                    .scale(0.6f + 0.4f * tick)
                    .alpha(tick),
            )
        }
    }
}

/** Przełącznik w kolorach Kartki (włączony = kobaltowy tor). null w [onCheckedChange] = tylko wygląd. */
@Composable
fun KartkaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LookTheme.kartka
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onAccent,
            checkedTrackColor = c.accent,
            checkedBorderColor = c.accent,
            uncheckedThumbColor = c.ink2,
            uncheckedTrackColor = c.surface,
            uncheckedBorderColor = c.ink2,
        ),
    )
}

/**
 * Wiersz ustawienia z przełącznikiem: tytuł (+ opcjonalny opis) po lewej, [KartkaSwitch] po prawej,
 * cały wiersz przełącza; linia 1 dp pod spodem ([showDivider]).
 */
@Composable
fun KartkaSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    val c = LookTheme.kartka
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(vertical = 10.dp)
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = KartkaType.RowTitle, color = c.ink)
                if (subtitle != null) Text(text = subtitle, style = KartkaType.Caption, color = c.ink2)
            }
            KartkaSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
        if (showDivider) KartkaDivider()
    }
}

/**
 * Wiersz wyboru jednej opcji z listy (np. motyw, dźwięk powiadomień): tytuł (+ opis), przy wybranej
 * ptaszek w kolorze akcentu po prawej; linia 1 dp pod spodem ([showDivider]).
 */
@Composable
fun KartkaOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    val c = LookTheme.kartka
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 10.dp)
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = KartkaType.RowTitle, color = if (selected) c.accent else c.ink)
                if (subtitle != null) Text(text = subtitle, style = KartkaType.Caption, color = c.ink2)
            }
            if (selected) {
                Icon(
                    imageVector = KartkaIcons.CheckThin,
                    contentDescription = null,
                    tint = c.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (showDivider) KartkaDivider()
    }
}

/** Odcień chipa / odznaki. */
enum class KartkaChipTone { Accent, Neutral, Warning }

/** Mały chip-odznaka w kształcie pigułki (np. „za 2 dni”, „Z Librusa”) – tylko do odczytu. */
@Composable
fun KartkaChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: KartkaChipTone = KartkaChipTone.Accent,
) {
    val c = LookTheme.kartka
    val error = MaterialTheme.colorScheme.error
    val (bg, fg) = when (tone) {
        KartkaChipTone.Accent -> c.accentSoft to c.accent
        KartkaChipTone.Neutral -> c.surface to c.ink2
        KartkaChipTone.Warning -> error.copy(alpha = 0.12f) to error
    }
    Text(
        text = text,
        style = KartkaType.Chip,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(KartkaDefaults.PillShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/**
 * Chip do wyboru (filtr folderów, dni tygodnia…): wybrany = accentSoft z napisem w akcencie,
 * niewybrany = obwódka 1 dp. Cel dotyku ≥ 48 dp.
 */
@Composable
fun KartkaFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: Role? = null,
) {
    val c = LookTheme.kartka
    val bg by animateColorAsState(
        targetValue = if (selected) c.accentSoft else Color.Transparent,
        animationSpec = tween(KartkaMotion.SHORT_MS),
        label = "chipBg",
    )
    val borderColor = if (selected) c.accentSoft else c.line
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, enabled = enabled, role = role, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = KartkaType.Link,
            color = if (selected) c.accent else c.ink,
            maxLines = 1,
            modifier = Modifier
                .clip(KartkaDefaults.PillShape)
                .background(bg)
                .border(KartkaDefaults.LineWidth, borderColor, KartkaDefaults.PillShape)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .alpha(if (enabled) 1f else 0.4f),
        )
    }
}

/**
 * Pole tekstowe Kartki: szare pole (surface), zaokrąglenie 12 dp, min. 48 dp wysokości, obwódka w akcencie
 * po wybraniu (czerwona przy błędzie). Opcjonalnie etykieta nad polem i tekst pomocniczy pod nim.
 */
@Composable
fun KartkaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = KartkaType.Body,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val c = LookTheme.kartka
    val error = MaterialTheme.colorScheme.error
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> error
            focused -> c.accent
            else -> Color.Transparent
        },
        animationSpec = tween(KartkaMotion.SHORT_MS),
        label = "fieldBorder",
    )
    val description = label ?: placeholder
    Column(modifier = modifier.alpha(if (enabled) 1f else 0.5f)) {
        if (label != null) {
            Text(text = label, style = KartkaType.CaptionStrong, color = c.ink2)
            Spacer(Modifier.height(6.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (description != null) {
                        Modifier.semantics { contentDescription = description }
                    } else {
                        Modifier
                    },
                ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.copy(color = c.ink),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
            interactionSource = source,
            cursorBrush = SolidColor(c.accent),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KartkaDefaults.TouchTarget)
                        .clip(KartkaDefaults.FieldShape)
                        .background(c.surface)
                        .border(1.5.dp, borderColor, KartkaDefaults.FieldShape)
                        .padding(start = 14.dp, end = if (trailing != null) 4.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = textStyle, color = c.ink2, maxLines = maxLines)
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
        if (supportingText != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = supportingText,
                style = KartkaType.Caption,
                color = if (isError) error else c.ink2,
            )
        }
    }
}
