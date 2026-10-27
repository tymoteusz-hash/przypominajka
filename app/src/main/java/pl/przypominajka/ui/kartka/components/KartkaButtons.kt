package pl.przypominajka.ui.kartka.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme

private const val DISABLED_ALPHA = 0.4f

/**
 * Główny przycisk Kartki: pełny kobaltowy akcent, biały napis, zaokrąglenie 12 dp, wysokość ≥ 48 dp.
 * @param destructive czerwony wariant (np. „Usuń” w oknie potwierdzenia).
 */
@Composable
fun KartkaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    val c = LookTheme.kartka
    val container = if (destructive) MaterialTheme.colorScheme.error else c.accent
    val content = if (destructive) MaterialTheme.colorScheme.onError else c.onAccent
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = KartkaDefaults.TouchTarget),
        enabled = enabled,
        shape = KartkaDefaults.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = content.copy(alpha = 0.85f),
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/** Drugorzędny przycisk: szare pole (surface) i grafitowy napis – np. „Anuluj” obok głównego albo „Ustaw godzinę”. */
@Composable
fun KartkaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = LookTheme.kartka
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = KartkaDefaults.TouchTarget),
        enabled = enabled,
        shape = KartkaDefaults.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.surface,
            contentColor = c.ink,
            disabledContainerColor = c.surface,
            disabledContentColor = c.ink.copy(alpha = DISABLED_ALPHA),
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/** Przycisk z obwódką 1 dp (styl kafelków działów) – płaski, bez tła. */
@Composable
fun KartkaOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = LookTheme.kartka
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = KartkaDefaults.TouchTarget),
        enabled = enabled,
        shape = KartkaDefaults.ButtonShape,
        border = BorderStroke(KartkaDefaults.LineWidth, c.line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = c.ink,
            disabledContentColor = c.ink.copy(alpha = DISABLED_ALPHA),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/**
 * Przycisk tekstowy (np. „Wszystkie”, „Usuń zrobione (3)”): napis 14 sp w kolorze akcentu, opcjonalna ikona
 * 18 dp przed napisem; nieaktywny = przygaszony. [color] pozwala dać grafit (np. „Anuluj”) albo czerwień.
 */
@Composable
fun KartkaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = Color.Unspecified,
) {
    val c = LookTheme.kartka
    val tint = if (color.isSpecified) color else c.accent
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = KartkaDefaults.TouchTarget),
        enabled = enabled,
        shape = KartkaDefaults.ButtonShape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = tint,
            disabledContentColor = tint.copy(alpha = DISABLED_ALPHA),
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = KartkaType.Link)
    }
}

/**
 * Przycisk-ikona bez tła (48×48 dp, zaokrąglenie 12 dp) – np. „Wróć”, „Konto”, „Ustawienia”.
 * [contentDescription] jest wymagany dla czytnika ekranu (null tylko, gdy obok jest opis).
 */
@Composable
fun KartkaIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    iconSize: Dp = 22.dp,
) {
    val c = LookTheme.kartka
    Box(
        modifier = modifier
            .size(KartkaDefaults.TouchTarget)
            .clip(KartkaDefaults.ButtonShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (tint.isSpecified) tint else c.ink,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Kwadratowy przycisk-ikona z tłem (48×48 dp) – domyślnie akcent z białą ikoną, jak „+” przy polu
 * „Dodaj punkt…”. Kolory można podmienić (np. `c.surface` + `c.ink`).
 */
@Composable
fun KartkaFilledIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    iconSize: Dp = 22.dp,
) {
    val c = LookTheme.kartka
    val bg = if (containerColor.isSpecified) containerColor else c.accent
    val fg = if (contentColor.isSpecified) contentColor else c.onAccent
    Box(
        modifier = modifier
            .size(KartkaDefaults.TouchTarget)
            .clip(KartkaDefaults.ButtonShape)
            .background(if (enabled) bg else bg.copy(alpha = DISABLED_ALPHA))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = fg, modifier = Modifier.size(iconSize))
    }
}

/**
 * Przełącznik-ikona (48×48 dp): wyłączony = szare pole i ikona ink2, włączony = accentSoft i ikona w akcencie
 * (jak przycisk zegara przy polu „Dodaj punkt…”).
 */
@Composable
fun KartkaToggleIconButton(
    icon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 22.dp,
) {
    val c = LookTheme.kartka
    val bg by animateColorAsState(
        targetValue = if (checked) c.accentSoft else c.surface,
        animationSpec = tween(KartkaMotion.SHORT_MS),
        label = "toggleBg",
    )
    val fg by animateColorAsState(
        targetValue = if (checked) c.accent else c.ink2,
        animationSpec = tween(KartkaMotion.SHORT_MS),
        label = "toggleFg",
    )
    Box(
        modifier = modifier
            .size(KartkaDefaults.TouchTarget)
            .clip(KartkaDefaults.ButtonShape)
            .background(bg)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription }
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text = text, style = KartkaType.Button, textAlign = TextAlign.Center)
}
