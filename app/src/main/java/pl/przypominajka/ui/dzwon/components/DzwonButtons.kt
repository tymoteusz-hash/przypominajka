package pl.przypominajka.ui.dzwon.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme

/** Przezroczystość wyłączonych przycisków (jak w makiecie „Usuń zrobione”). */
private const val DISABLED_ALPHA = 0.4f

private val ButtonShape = RoundedCornerShape(16.dp)

/** Główny przycisk: złoty, ciemny napis (np. „Dodaj”, „Zapisz”). Wysokość ≥ 48 dp. */
@Composable
fun DzwonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = LookTheme.dzwon
    DzwonButtonBase(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        container = c.gold,
        content = c.onGold,
    )
}

/** Drugorzędny przycisk: pole w kolorze `field`, napis w kolorze tekstu (np. „Dodaj godzinę”). */
@Composable
fun DzwonSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = LookTheme.dzwon
    DzwonButtonBase(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        container = c.field,
        content = c.ink,
    )
}

/**
 * Przycisk tekstowy bez tła – domyślnie złoty napis (np. „Usuń zrobione (3)”).
 * Dla „Anuluj” podaj `color = LookTheme.dzwon.ink`, dla usuwania `MaterialTheme.colorScheme.error`.
 */
@Composable
fun DzwonTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = LookTheme.dzwon.goldText,
) {
    DzwonButtonBase(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        container = Color.Transparent,
        content = color,
        shape = RoundedCornerShape(12.dp),
        horizontalPadding = 12.dp,
        textStyle = DzwonType.Chip,
    )
}

@Composable
private fun DzwonButtonBase(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    icon: ImageVector?,
    container: Color,
    content: Color,
    shape: RoundedCornerShape = ButtonShape,
    horizontalPadding: Dp = 20.dp,
    textStyle: TextStyle = DzwonType.Button,
) {
    // Kolor treści także dla fali dotyku (ripple bierze LocalContentColor).
    CompositionLocalProvider(LocalContentColor provides content) {
        Row(
            modifier = modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(container)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = horizontalPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            }
            Text(text = text, style = textStyle, color = content, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Okrągły przycisk-ikona (Konto, Ustawienia, „Zwiń”). [size] to widoczne koło (makieta: 44 dp);
 * pole dotyku ma zawsze co najmniej 48 dp.
 */
@Composable
fun DzwonRoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 21.dp,
    containerColor: Color = LookTheme.dzwon.surface,
    contentColor: Color = LookTheme.dzwon.ink,
    enabled: Boolean = true,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
            }
        }
    }
}

/**
 * Kwadratowy przycisk-ikona 50 dp z zaokrągleniem 16 dp (pasek dodawania: zegar i „+”).
 * Domyślnie złoty. Gdy podasz [selected], działa jak przełącznik: włączony – złoty,
 * wyłączony – w kolorze pola (`field`) z szarą ikoną.
 */
@Composable
fun DzwonSquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    enabled: Boolean = true,
    size: Dp = 50.dp,
) {
    val c = LookTheme.dzwon
    val on = selected ?: true
    val container = if (on) c.gold else c.field
    val content = if (on) c.onGold else c.ink2
    val shape = RoundedCornerShape(16.dp)
    val interaction = if (selected != null) {
        Modifier.toggleable(value = selected, enabled = enabled, role = Role.Switch, onValueChange = { onClick() })
    } else {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    }
    CompositionLocalProvider(LocalContentColor provides content) {
        Box(
            modifier = modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .size(size)
                .clip(shape)
                .background(container)
                .then(interaction)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * Chip w kształcie pigułki (działy wokół dzwonu, filtry, wybór opcji). Wybrany – złoty z ciemnym
 * napisem, zwykły – tło `surface` z cienką obwódką `chipLine`. Wysokość ≥ 48 dp; szerokość rośnie z napisem.
 * [onClick] = null – chip tylko do odczytu.
 */
@Composable
fun DzwonChip(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = LookTheme.dzwon.surface,
    textStyle: TextStyle = DzwonType.Chip,
) {
    val c = LookTheme.dzwon
    val container = if (selected) c.gold else containerColor
    val content = if (selected) c.onGold else c.ink
    // Makieta: wybrany chip grubszy (800), zwykły 700.
    val style = if (selected) textStyle else textStyle.copy(fontWeight = FontWeight.Bold)
    val shape = CircleShape
    CompositionLocalProvider(LocalContentColor provides content) {
        Row(
            modifier = modifier
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(container)
                .then(if (selected) Modifier else Modifier.border(1.dp, c.chipLine, shape))
                .then(
                    if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            }
            Text(text = text, style = style, color = content, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** Mała odznaka-pigułka z liczbą lub krótkim słowem (np. liczba przypomnień w folderze). */
@Composable
fun DzwonBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LookTheme.dzwon.gold,
    contentColor: Color = LookTheme.dzwon.onGold,
) {
    Box(
        modifier = modifier
            .heightIn(min = 22.dp)
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = DzwonType.Badge, color = contentColor)
    }
}
