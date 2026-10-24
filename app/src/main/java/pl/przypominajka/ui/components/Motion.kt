package pl.przypominajka.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Wspólne animacje nowego wyglądu: sprężyste wciśnięcia, wjazd elementów, unoszenie, licznik, konfetti.

/** Element przy dotyku sprężyście się "wciska" (zmniejsza) i odbija po puszczeniu. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.95f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Pojawienie się elementu: wjazd z dołu i rozjaśnienie. [index] opóźnia start, więc listy
 * układają się "kaskadą". Animacja odtwarza się raz – po przewinięciu czy powrocie na ekran już nie.
 */
@Composable
fun Modifier.appear(index: Int = 0): Modifier {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index.coerceIn(0, 10) * 55L)
            progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 220f))
            played = true
        }
    }
    val offset = with(LocalDensity.current) { 32.dp.toPx() }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * offset
        val s = 0.96f + 0.04f * p.coerceIn(0f, 1f)
        scaleX = s
        scaleY = s
    }
}

/**
 * Delikatne podskoczenie przy pojawieniu się (np. ikona w pustym stanie) – dwa łagodne bujnięcia
 * i spoczynek. Celowo nie w nieskończoność: stała animacja zmuszałaby ekran do ciągłego rysowania.
 */
@Composable
fun Modifier.floating(amplitude: Dp = 6.dp, periodMillis: Int = 2600): Modifier {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = periodMillis
                -1f at periodMillis / 4
                0.8f at periodMillis / 2
                -0.4f at periodMillis * 3 / 4
            },
        )
    }
    val px = with(LocalDensity.current) { amplitude.toPx() }
    return graphicsLayer {
        translationY = t.value * px
        rotationZ = t.value * 2f
    }
}

/** Pierścień postępu, który dorysowuje się płynnie przy każdej zmianie. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animated.animateTo(progress.coerceIn(0f, 1f), tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(trackColor, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (animated.value > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** Zaokrąglony pasek postępu z miękką, sprężystą animacją. */
@Composable
fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 90f),
        label = "progressBar",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        if (animated > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Liczba, która przy zmianie wjeżdża z góry albo z dołu (jak licznik). */
@Composable
fun AnimatedNumber(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier, suffix: String = "") {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                (slideOutVertically { if (up) -it else it } + fadeOut()) using SizeTransform(clip = false)
        },
        modifier = modifier,
        label = "animatedNumber",
    ) { number ->
        Text(text = "$number$suffix", style = style, color = color)
    }
}

/** Tekst liczby "nabijany" od zera do [target] (np. wynik quizu). */
@Composable
fun CountUpText(target: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier, suffix: String = "") {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) {
        value.animateTo(target.toFloat(), tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }
    Text(text = "${value.value.roundToInt()}$suffix", style = style, color = color, modifier = modifier)
}

// ---------------------------------------------------------------- konfetti

private class ConfettiPiece(
    val startX: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val spin: Float,
    val wobble: Float,
)

private val ConfettiColors = listOf(
    Color(0xFFFFC62E), Color(0xFFE9A80F), Color(0xFFB5523A), Color(0xFF5E8B5A), Color(0xFF46698C), Color(0xFF7E5481),
)

/**
 * Wybuch konfetti – rysowany na wierzchu, odpala się przy każdej zmianie [trigger] (> 0).
 * Kilkadziesiąt kolorowych paseczków wylatuje do góry, wiruje i opada.
 */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val progress = remember(trigger) { Animatable(0f) }
    val pieces = remember(trigger) {
        val random = Random(trigger * 7919)
        List(110) {
            ConfettiPiece(
                startX = 0.5f + (random.nextFloat() - 0.5f) * 0.3f,
                vx = (random.nextFloat() - 0.5f) * 1.3f,
                vy = -(0.7f + random.nextFloat() * 0.9f),
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                width = 7f + random.nextFloat() * 7f,
                height = 11f + random.nextFloat() * 9f,
                rotation = random.nextFloat() * 360f,
                spin = (random.nextFloat() - 0.5f) * 900f,
                wobble = random.nextFloat() * 6f,
            )
        }
    }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(durationMillis = DURATION_MS, easing = LinearEasing))
    }
    val density = LocalDensity.current.density
    Canvas(modifier = modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val t = p * DURATION_MS / 1000f
        val fade = if (p > 0.7f) 1f - (p - 0.7f) / 0.3f else 1f
        pieces.forEach { piece ->
            val x = (piece.startX + piece.vx * t + sin(t * piece.wobble) * 0.02f) * size.width
            val y = (0.38f + piece.vy * t + 0.5f * GRAVITY * t * t) * size.height
            if (y > size.height + 40f) return@forEach
            val angle = piece.rotation + piece.spin * t
            // "Obracanie się" paseczka w 3D – zmienna szerokość.
            val w = piece.width * density * (0.35f + 0.65f * abs(cos(Math.toRadians(angle.toDouble() * 1.7)).toFloat()))
            val h = piece.height * density
            rotate(angle, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color.copy(alpha = fade),
                    topLeft = Offset(x - w / 2, y - h / 2),
                    size = Size(w, h),
                )
            }
        }
    }
}

private const val DURATION_MS = 2600
private const val GRAVITY = 1.7f
