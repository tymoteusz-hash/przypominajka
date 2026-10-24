package pl.przypominajka.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.BellGold
import pl.przypominajka.ui.theme.Pastel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------- rodzaje wpisów szkolnych

val SchoolType.icon: ImageVector
    get() = when (this) {
        SchoolType.HOMEWORK -> Icons.Rounded.AutoStories
        SchoolType.QUIZ -> Icons.Rounded.EditNote
        SchoolType.TEST -> Icons.Rounded.School
        SchoolType.TRIP -> Icons.Rounded.DirectionsBus
    }

/** Kolor rodzaju: sprawdzian – terakota, kartkówka – złoto, zadanie – szałwia, wyjście – zmierzch. */
val SchoolType.pastel: Pastel
    get() = when (this) {
        SchoolType.TEST -> Pastel.TERRACOTTA
        SchoolType.QUIZ -> Pastel.GOLD
        SchoolType.HOMEWORK -> Pastel.SAGE
        SchoolType.TRIP -> Pastel.DUSK
    }

/** Mocny kolor rodzaju (ikony, liczniki, suwaki). */
val SchoolType.accent: Color
    @Composable get() = pastel.content

// ---------------------------------------------------------------- nagłówki

/** Duży nagłówek ekranu z koralową kropką i "nadtytułem" – zamiast klasycznego paska aplikacji. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .appear()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppSurfaces.accent),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Nagłówek grupy elementów na liście, z licznikiem w kapsułce. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 1.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- ikony i przyciski

/** Zaokrąglony kafelek z ikoną na delikatnym tle w danym kolorze. */
@Composable
fun IconBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

/** Kafelek z ikoną w kolorach pastelowych. */
@Composable
fun PastelBadge(
    icon: ImageVector,
    pastel: Pastel,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(pastel.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = pastel.content, modifier = Modifier.size(size * 0.5f))
    }
}

/** Główny przycisk – ciemna (w ciemnym motywie kremowa) kapsuła, sprężyście wciskana. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction)
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.35f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = contentColor),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** Okrągły przycisk z ikoną na karcie (akcje w nagłówkach). */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .padding(4.dp)
            .pressScale(interaction, pressedScale = 0.88f)
            .size(44.dp)
            .clip(CircleShape)
            .background(AppSurfaces.card)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

/**
 * Pływający przycisk "Nowe" – ciemna kapsuła z cieniem. Przy przewijaniu w dół napis się chowa
 * ([expanded] = false) i zostaje sama ikona.
 */
@Composable
fun AppFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.9f)
            .shadow(14.dp, CircleShape, ambientColor = colors.onSurface, spotColor = colors.onSurface)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(interactionSource = interaction, indication = ripple(color = colors.onPrimary), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = text, tint = colors.onPrimary, modifier = Modifier.size(24.dp))
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimary,
                modifier = Modifier.padding(start = 10.dp, end = 4.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- karta "hero"

/**
 * Wyróżniona karta: jednolity fiolet (bez gradientów) i duży, ledwo widoczny dzwon w rogu –
 * ten sam co na ikonie i na stronie głównej.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = AppSurfaces.hero
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .drawBehind {
                drawRect(base)
                // Dzwon w prawym dolnym rogu, lekko wychodzący poza kartę.
                val bell = minOf(size.height * 0.95f, size.width * 0.45f)
                drawBellSilhouette(
                    topLeft = Offset(size.width - bell * 0.82f, size.height - bell * 0.9f),
                    size = bell,
                    color = BellGold.Body.copy(alpha = 0.14f),
                )
            }
            .padding(contentPadding),
        content = content,
    )
}

// ---------------------------------------------------------------- odstęp na dole

/**
 * Ile miejsca na dole ekranu zajmuje pasek systemowy – ekrany dodają tyle odstępu na końcu list
 * i podnoszą przyciski, żeby nic nie wchodziło pod gesty systemu.
 */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }
