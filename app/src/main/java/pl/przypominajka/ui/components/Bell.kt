package pl.przypominajka.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.BellGold
import pl.przypominajka.util.BellSound
import kotlin.math.hypot
import kotlin.math.max

// =====================================================================================================
// DZWON – centrum aplikacji. Ten sam kształt co na ikonie (res/drawable/ic_launcher_foreground.xml).
// Przy każdym przejściu między ekranami dzwon "bije" (kołysze się, rozchodzą się fale i słychać cichy
// dźwięk), a nowy ekran rozchodzi się od niego kołem; przy powrocie zwija się z powrotem do dzwonu.
// =====================================================================================================

/** Wspólny stan dzwonów: uderzenia, położenie widocznego dzwonu i kierunek ostatniego przejścia. */
@Stable
class BellController(private val sound: BellSound) {
    /** Licznik uderzeń – każdy widoczny dzwon kołysze się przy jego zmianie. */
    var ringCount by mutableIntStateOf(0)
        private set

    /** Środek dzwonu widocznego teraz na ekranie (współrzędne całego okna). */
    var bellCenter: Offset = Offset.Unspecified

    /** Skąd ma się rozejść następny ekran – ustawiane tuż przed nawigacją. */
    var pendingOrigin: Offset = Offset.Unspecified
        private set

    /** Ostatnie przejście było powrotem – wtedy ekran zwija się do dzwonu zamiast rozwijać. */
    var lastNavigationWasPop: Boolean = false

    /** Uderzenie: kołysanie wszystkich widocznych dzwonów i dźwięk (zgodnie z Ustawieniami). */
    fun ring() {
        ringCount++
        sound.play()
    }

    /** Zapamiętuje dzwon bieżącego ekranu jako początek przejścia. */
    fun prepareNavigation() {
        pendingOrigin = bellCenter
    }
}

val LocalBell = staticCompositionLocalOf<BellController?> { null }

// ---------------------------------------------------------------- kształt dzwonu

// Złoty dzwon jak emoji: kopuła, rozchylona krawędź z ciemnym otworem, w którym widać serce.
// Układ 200 × 200 – te same ścieżki są w ikonie aplikacji (res/drawable/ic_launcher_foreground.xml).

/** Korpus: kopuła i boki aż do krawędzi (dół domyka elipsa krawędzi). */
private fun bellBodyPath(): Path = Path().apply {
    moveTo(100f, 34f)
    cubicTo(128f, 34f, 148f, 52f, 152f, 84f)
    cubicTo(156f, 112f, 156f, 132f, 166f, 148f)
    cubicTo(172f, 158f, 186f, 164f, 192f, 170f)
    lineTo(8f, 170f)
    cubicTo(14f, 164f, 28f, 158f, 34f, 148f)
    cubicTo(44f, 132f, 44f, 112f, 48f, 84f)
    cubicTo(52f, 52f, 72f, 34f, 100f, 34f)
    close()
}

/** Ciemniejszy pas po prawej stronie (przycinany do korpusu). */
private fun bellShadePath(): Path = Path().apply {
    moveTo(118f, 36f)
    cubicTo(134f, 46f, 138f, 70f, 136f, 100f)
    cubicTo(134f, 128f, 140f, 146f, 160f, 158f)
    lineTo(200f, 168f)
    lineTo(200f, 0f)
    close()
}

/** Jasny połysk po lewej. */
private fun bellHighlightPath(): Path = Path().apply {
    moveTo(72f, 58f)
    cubicTo(62f, 74f, 58f, 100f, 54f, 128f)
    cubicTo(54f, 134f, 60f, 134f, 62f, 128f)
    cubicTo(66f, 104f, 70f, 82f, 80f, 62f)
    cubicTo(82f, 56f, 76f, 52f, 72f, 58f)
    close()
}

/** Połysk na rozchylonej krawędzi. */
private fun bellRimHighlightPath(): Path = Path().apply {
    moveTo(30f, 156f)
    quadraticTo(52f, 146f, 94f, 148f)
}

private val RimCenter = Offset(100f, 172f)
private val RimSize = Size(186f, 34f)
private val MouthSize = Size(156f, 22f)
private val MouthCenter = Offset(100f, 170f)

/** Sam kształt dzwonu jednym kolorem – np. jako znak wodny na kartach. */
fun DrawScope.drawBellSilhouette(topLeft: Offset, size: Float, color: Color) {
    val s = size / 200f
    translate(topLeft.x, topLeft.y) {
        scale(s, s, pivot = Offset.Zero) {
            drawCircle(color, radius = 12f, center = Offset(100f, 24f), style = Stroke(width = 9f))
            drawPath(bellBodyPath(), color)
            drawOval(color, RimCenter - Offset(RimSize.width / 2, RimSize.height / 2), RimSize)
        }
    }
}

/**
 * Dzwon rysowany w Compose. Przy pojawieniu się na ekranie i przy każdym uderzeniu
 * ([BellController.ring] albo dotknięcie) buja się, serce odbija w drugą stronę
 * i rozchodzą się fale. Na korpusie może być napis [label] (np. liczba dzisiejszych powiadomień).
 */
@Composable
fun Bell(
    size: Dp,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelColor: Color = BellGold.Mouth,
    showWaves: Boolean = true,
    reportPosition: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val controller = LocalBell.current
    val haptics = LocalHapticFeedback.current
    val swing = remember { Animatable(0f) }
    val waves = remember { Animatable(1f) }
    val ringCount = controller?.ringCount ?: 0

    // Uderzenie: przy pojawieniu się dzwonu na ekranie i przy każdym kolejnym ring().
    LaunchedEffect(ringCount) {
        coroutineScope {
            launch {
                waves.snapTo(0f)
                waves.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing))
            }
            swing.snapTo(0f)
            swing.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 1400
                    18f at 120
                    -14f at 330
                    10f at 550
                    -6f at 780
                    3f at 1010
                    -1f at 1220
                },
            )
        }
    }
    // Bez ciągłego "bujania w miejscu" – dzwon rusza się tylko, gdy bije (oszczędza baterię i płynność).

    val body = remember { bellBodyPath() }
    val shade = remember { bellShadePath() }
    val highlight = remember { bellHighlightPath() }
    val rimHighlight = remember { bellRimHighlightPath() }
    val waveColor = BellGold.Shade
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (reportPosition && controller != null) {
                    Modifier.onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        controller.bellCenter = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height * 0.48f)
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        controller?.ring()
                        onClick()
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Fale dźwięku (nie kołyszą się razem z dzwonem).
        if (showWaves) {
            Canvas(Modifier.fillMaxSize()) {
                val p = waves.value
                val s = this.size.minDimension / 200f
                if (p < 1f) {
                    val center = Offset(100f * s, 92f * s)
                    for (i in 0..2) {
                        val alpha = (1f - p) * (0.55f - i * 0.15f)
                        if (alpha <= 0f) continue
                        val radius = (84f + p * 38f + i * 15f) * s
                        val arcSize = Size(radius * 2, radius * 2)
                        val topLeft = Offset(center.x - radius, center.y - radius)
                        val stroke = Stroke(width = 6f * s, cap = StrokeCap.Round)
                        drawArc(waveColor.copy(alpha = alpha), -32f, 64f, false, topLeft, arcSize, style = stroke)
                        drawArc(waveColor.copy(alpha = alpha), 148f, 64f, false, topLeft, arcSize, style = stroke)
                    }
                }
                // Cień pod dzwonem.
                drawOval(
                    color = Color.Black.copy(alpha = 0.08f),
                    topLeft = Offset(64f * s, 190f * s),
                    size = Size(72f * s, 8f * s),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = swing.value
                    transformOrigin = TransformOrigin(0.5f, 0.12f)
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val s = this.size.minDimension / 200f
                scale(s, s, pivot = Offset.Zero) {
                    // Uszko.
                    drawCircle(BellGold.Deep, radius = 12f, center = Offset(100f, 24f), style = Stroke(width = 9f))
                    drawArc(
                        color = BellGold.Shade,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(88f, 12f),
                        size = Size(24f, 24f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round),
                    )
                    // Korpus z cieniem po prawej i połyskiem po lewej.
                    drawPath(body, BellGold.Body)
                    clipPath(body) { drawPath(shade, BellGold.Shade) }
                    drawPath(highlight, BellGold.Light)
                    // Krawędź i ciemny otwór.
                    drawOval(BellGold.Body, RimCenter - Offset(RimSize.width / 2, RimSize.height / 2), RimSize)
                    drawPath(rimHighlight, BellGold.Light, style = Stroke(width = 6f, cap = StrokeCap.Round))
                    drawOval(BellGold.Mouth, MouthCenter - Offset(MouthSize.width / 2, MouthSize.height / 2), MouthSize)
                    // Serce dzwonu – widać je w otworze, odbija w przeciwną stronę niż korpus.
                    rotate(degrees = -swing.value * 0.9f, pivot = Offset(100f, 140f)) {
                        drawCircle(BellGold.Shade, radius = 11f, center = Offset(100f, 174f))
                        drawCircle(BellGold.Light, radius = 4f, center = Offset(96f, 170f))
                    }
                }
            }
            if (label != null) {
                Text(
                    text = label,
                    color = labelColor,
                    fontSize = (size.value * 0.2f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    modifier = Modifier.padding(top = size * 0.33f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- górny pasek z dzwonkiem

/** Przycisk powrotu: strzałka + mały dzwonek. Dzwonek wskazuje też, skąd rozejdzie się przejście. */
@Composable
fun BellBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(CircleShape)
            .background(AppSurfaces.card)
            .border(1.dp, AppSurfaces.cardBorder, CircleShape)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Wróć" }
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.ChevronLeft, contentDescription = null, modifier = Modifier.size(22.dp))
        Bell(size = 30.dp, showWaves = false)
    }
}

/** Górny pasek każdego ekranu: dzwonek-powrót, tytuł (i nadtytuł), akcje po prawej. */
@Composable
fun BellTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppSurfaces.page)
            .statusBarsPadding()
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BellBackButton(onClick = onBack)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppSurfaces.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

// ---------------------------------------------------------------- przejście "od dzwonu"

private val OffsetSaver = Saver<MutableState<Offset>, List<Float>>(
    save = { listOf(it.value.x, it.value.y) },
    restore = { mutableStateOf(Offset(it[0], it[1])) },
)

/** Koło o promieniu zależnym od [progress] (0 – punkt, 1 – cały ekran) ze środkiem w [origin]. */
private class CircleRevealShape(private val progress: Float, private val origin: Offset) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val center = if (origin.isSpecified) origin else Offset(size.width / 2f, size.height * 0.3f)
        val maxRadius = max(
            max(hypot(center.x, center.y), hypot(size.width - center.x, center.y)),
            max(hypot(center.x, size.height - center.y), hypot(size.width - center.x, size.height - center.y)),
        )
        return Outline.Generic(Path().apply { addOval(Rect(center, maxRadius * progress)) })
    }
}

/**
 * Opakowanie ekranu w nawigacji: nowy ekran rozchodzi się kołem od dzwonu poprzedniego ekranu,
 * a przy powrocie zwija się z powrotem do niego. Na krawędzi koła biegnie fala w kolorze dzwonu.
 */
@Composable
fun AnimatedVisibilityScope.BellReveal(content: @Composable () -> Unit) {
    val controller = LocalBell.current
    val origin = rememberSaveable(saver = OffsetSaver) {
        mutableStateOf(controller?.pendingOrigin ?: Offset.Unspecified)
    }
    val progress = transition.animateFloat(
        transitionSpec = { tween(durationMillis = 560, easing = FastOutSlowInEasing) },
        label = "bellReveal",
    ) { state ->
        val pop = controller?.lastNavigationWasPop == true
        when (state) {
            EnterExitState.Visible -> 1f
            EnterExitState.PreEnter -> if (pop) 1f else 0f
            EnterExitState.PostExit -> if (pop) 0f else 1f
        }
    }
    val waveColor = BellGold.Shade
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                if (p < 1f) {
                    clip = true
                    shape = CircleRevealShape(p, origin.value)
                } else {
                    clip = false
                }
            },
    ) {
        content()
        Canvas(Modifier.fillMaxSize()) {
            val p = progress.value
            if (p > 0f && p < 1f) {
                val o = origin.value
                val center = if (o.isSpecified) o else Offset(size.width / 2f, size.height * 0.3f)
                val maxRadius = max(
                    max(hypot(center.x, center.y), hypot(size.width - center.x, center.y)),
                    max(hypot(center.x, size.height - center.y), hypot(size.width - center.x, size.height - center.y)),
                )
                val width = 10.dp.toPx()
                drawCircle(
                    color = waveColor.copy(alpha = 0.45f * (1f - p)),
                    radius = (maxRadius * p - width / 2).coerceAtLeast(0f),
                    center = center,
                    style = Stroke(width = width),
                )
            }
        }
    }
}
