package pl.przypominajka.ui.tablica.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.look.TablicaColors

/** Ozdoba u góry karteczki. */
enum class NoteDecoration {
    /** Półprzezroczysty pasek taśmy klejącej. */
    Tape,

    /** Okrągła czerwona pinezka. */
    Pin,
    None,
}

/** Domyślne wartości karteczek. */
object NoteDefaults {
    /** Karteczki są prawie kanciaste. */
    val Shape: Shape = RoundedCornerShape(4.dp)

    /** Wnętrze karteczki jak w makiecie (u góry więcej miejsca na taśmę/pinezkę). */
    val ContentPadding = PaddingValues(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 12.dp)
}

/**
 * Karteczka przyklejona do tablicy: kolor [color] (papier + atrament), lekkie przechylenie [tilt]
 * (stopnie), miękki cień i ozdoba [decoration] (taśma/pinezka) wystająca nad górną krawędź.
 * Gdy [onClick] != null – cała karteczka jest przyciskiem. [lifted] = podniesiona (przeciąganie):
 * większy, mocniejszy cień. Wewnątrz LocalContentColor = atrament karteczki, [LocalPaper] = jej tło,
 * domyślny styl tekstu [TablicaType.Body]. Rozmiar (np. fillMaxWidth, heightIn) ustawia [modifier].
 */
@Composable
fun Note(
    color: NoteColor,
    modifier: Modifier = Modifier,
    tilt: Float = 0f,
    decoration: NoteDecoration = NoteDecoration.None,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    lifted: Boolean = false,
    shape: Shape = NoteDefaults.Shape,
    contentPadding: PaddingValues = NoteDefaults.ContentPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LookTheme.tablica
    val elevation by animateDpAsState(if (lifted) 18.dp else 6.dp, tween(200), label = "noteElevation")
    // Cień systemowy sam mnoży kolor przez swoją przezroczystość – podajemy więc kolor bez alfy.
    val shadowColor = (if (lifted) colors.liftShadow else colors.noteShadow).copy(alpha = 1f)
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = tilt }
            .drawWithContent {
                drawContent()
                when (decoration) {
                    NoteDecoration.Tape -> drawTape(colors, tilt)
                    NoteDecoration.Pin -> drawPin(colors)
                    NoteDecoration.None -> Unit
                }
            },
        // Minimalny rozmiar z [modifier] (np. fillMaxWidth, heightIn) przechodzi na papier.
        propagateMinConstraints = true,
    ) {
        Column(
            modifier = Modifier
                .shadow(elevation, shape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
                .clip(shape)
                .background(color.bg)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides color.ink,
                LocalPaper provides color.bg,
                LocalTextStyle provides TablicaType.Body,
            ) {
                content()
            }
        }
    }
}

/** Taśma: pasek 56×18 dp na środku górnej krawędzi, przekrzywiony mocniej niż karteczka. */
private fun DrawScope.drawTape(colors: TablicaColors, tilt: Float) {
    val width = minOf(56.dp.toPx(), size.width * 0.45f)
    val height = 18.dp.toPx()
    val topLeft = Offset((size.width - width) / 2f, -height / 2f)
    val angle = if (tilt == 0f) -3f else (tilt * 2f).coerceIn(-5f, 5f)
    rotate(angle, pivot = Offset(size.width / 2f, 0f)) {
        drawRect(colors.tape, topLeft = topLeft, size = Size(width, height))
    }
}

/** Pinezka: czerwona kulka 16 dp z cieniem i odblaskiem, wbita w środek górnej krawędzi. */
private fun DrawScope.drawPin(colors: TablicaColors) {
    val radius = 8.dp.toPx()
    val center = Offset(size.width / 2f, 1.dp.toPx())
    drawCircle(Color.Black.copy(alpha = 0.22f), radius = radius, center = center + Offset(0f, 2.dp.toPx()))
    drawCircle(colors.pin, radius = radius, center = center)
    drawCircle(
        Color.White.copy(alpha = 0.35f),
        radius = radius * 0.32f,
        center = center + Offset(-radius * 0.32f, -radius * 0.32f),
    )
}

/**
 * Pole w przerywanej ramce (kolor ink2), bez tła – miejsce na nową karteczkę.
 * Do własnej treści (np. pole wpisywania); gotowy przycisk to [NewNoteTile].
 */
@Composable
fun DashedTile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LookTheme.tablica
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .dashedBorder(colors.ink2)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.ink, LocalPaper provides colors.board) {
            content()
        }
    }
}

/** Przerywany kafelek „+ Nowa karteczka” (Caveat 24, kolor ink2) – cały jest przyciskiem. */
@Composable
fun NewNoteTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Nowa karteczka",
) {
    val colors = LookTheme.tablica
    DashedTile(
        modifier = modifier.heightIn(min = 100.dp),
        onClick = onClick,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(TablicaIcons.Plus, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = text,
            style = TablicaType.NoteText,
            color = colors.ink2,
            textAlign = TextAlign.Center,
        )
    }
}
