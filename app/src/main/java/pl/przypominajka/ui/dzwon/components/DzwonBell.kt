package pl.przypominajka.ui.dzwon.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookFonts
import pl.przypominajka.ui.look.LookTheme
import kotlin.math.min

// Dzwon z makiety (układ 120×128): gałka, korpus, krawędź, serce, odblask.
private const val BELL_W = 120f
private const val BELL_H = 128f
private const val BELL_BODY = "M60 18C37 18 25 36 25 60v20L13 98h94L95 80V60c0-24-12-42-35-42z"
private const val BELL_SHINE = "M41 52c2-12 9-20 18-23"

/** Ścieżki w układzie makiety – skalowane przy rysowaniu. */
private object BellPaths {
    val body = PathParser().parsePathString(BELL_BODY).toPath()
    val shine = PathParser().parsePathString(BELL_SHINE).toPath()
}

/** Wysokość dzwonu o szerokości [width] (proporcje makiety 120×128). */
fun dzwonBellHeight(width: Dp): Dp = width * (BELL_H / BELL_W)

/**
 * Złoty dzwon Dzwonu narysowany jak w makiecie. [size] to szerokość (wysokość = 128/120 szerokości).
 *
 * [number] – napis na korpusie (np. liczba dzisiejszych przypomnień); null = pusty dzwon.
 * [swingKey] – każda NOWA wartość (także pierwsza, różna od null) to jedno wychylenie dzwonu
 * (ok. ±10°, gasnące, 700 ms, obrót wokół gałki). Nigdy w pętli i bez dźwięku. null = dzwon stoi.
 * [contentDescription] – null oznacza dzwon dekoracyjny (pomijany przez czytnik ekranu).
 */
@Composable
fun DzwonBell(
    size: Dp,
    modifier: Modifier = Modifier,
    number: String? = null,
    swingKey: Any? = null,
    contentDescription: String? = null,
    color: Color = LookTheme.dzwon.gold,
    deepColor: Color = LookTheme.dzwon.goldDeep,
    numberColor: Color = LookTheme.dzwon.onGold,
    shine: Boolean = true,
) {
    val swing = remember { Animatable(0f) }
    LaunchedEffect(swingKey) {
        if (swingKey == null) return@LaunchedEffect
        swing.snapTo(0f)
        swing.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 700
                10f at 110
                -7f at 280
                4f at 440
                -2f at 580
            },
        )
    }

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val numberLayout = remember(number, size, density, measurer) {
        number?.let { text ->
            // Rozmiar cyfr związany z dzwonem (nie z ustawieniem czcionki) – liczba jest też w opisie dostępności.
            val units = when (text.length) {
                1 -> 40f
                2 -> 34f
                else -> 26f
            }
            val fontSize = with(density) { (size * (units / BELL_W)).toSp() }
            measurer.measure(
                text = text,
                style = TextStyle(
                    fontFamily = LookFonts.DzwonHeading,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                ),
            )
        }
    }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .then(semanticsModifier)
            .size(width = size, height = dzwonBellHeight(size))
            .graphicsLayer {
                rotationZ = swing.value
                transformOrigin = TransformOrigin(0.5f, 12f / BELL_H)
            },
    ) {
        val u = this.size.width / BELL_W
        scale(scale = u, pivot = Offset.Zero) {
            drawCircle(color = color, radius = 7f, center = Offset(60f, 12f))
            drawPath(BellPaths.body, color = color)
        }
        // Serce wychyla się lekko w drugą stronę niż dzwon.
        rotate(degrees = -swing.value * 0.6f, pivot = Offset(60f * u, 100f * u)) {
            drawCircle(color = deepColor, radius = 10f * u, center = Offset(60f * u, 116f * u))
        }
        drawRoundRect(
            color = deepColor,
            topLeft = Offset(9f * u, 95f * u),
            size = Size(102f * u, 11f * u),
            cornerRadius = CornerRadius(5.5f * u),
        )
        if (shine) {
            scale(scale = u, pivot = Offset.Zero) {
                drawPath(
                    BellPaths.shine,
                    color = Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round),
                )
            }
        }
        numberLayout?.let { layout ->
            drawText(
                textLayoutResult = layout,
                color = numberColor,
                topLeft = Offset(
                    x = 60f * u - layout.size.width / 2f,
                    y = 80f * u - layout.firstBaseline,
                ),
            )
        }
    }
}

/**
 * Pierścień postępu: tor i złoty łuk od góry zgodnie z ruchem wskazówek. [progress] 0–1;
 * zmiana wartości jest animowana (350 ms). Rysuje okrąg wpisany w pole (z odstępem na grubość linii).
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 6.dp,
    color: Color = LookTheme.dzwon.gold,
    trackColor: Color = LookTheme.dzwon.track,
    animate: Boolean = true,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated = animateFloatAsState(
        targetValue = target,
        animationSpec = if (animate) tween(350) else tween(0),
        label = "progressRing",
    )
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = min(this.size.width, this.size.height) - stroke
        if (diameter <= 0f) return@Canvas
        val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
        drawCircle(
            color = trackColor,
            radius = diameter / 2f,
            center = center,
            style = Stroke(width = stroke),
        )
        val p = animated.value
        if (p > 0.001f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Mały dzwon w pierścieniu postępu (nagłówek Checklisty) – układ 112×112 z makiety
 * (pierścień r = 48, dzwon w skali 0,47). [size] skaluje całość.
 */
@Composable
fun DzwonBellInRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    strokeWidth: Dp = 6.dp,
    swingKey: Any? = null,
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Box(modifier.then(semanticsModifier).size(size)) {
        ProgressRing(
            progress = progress,
            strokeWidth = strokeWidth,
            modifier = Modifier
                .size(size)
                .padding(size * (5f / 112f)),
        )
        DzwonBell(
            size = size * (0.47f * BELL_W / 112f),
            swingKey = swingKey,
            shine = false,
            modifier = Modifier.offset(x = size * (28f / 112f), y = size * (26f / 112f)),
        )
    }
}
