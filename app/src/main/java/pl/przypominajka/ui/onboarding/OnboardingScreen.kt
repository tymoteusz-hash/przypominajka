package pl.przypominajka.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.przypominajka.R
import pl.przypominajka.ui.look.DzwonColors
import pl.przypominajka.ui.look.KartkaColors
import pl.przypominajka.ui.look.Look
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.TablicaColors
import pl.przypominajka.util.ThemeMode
import kotlin.math.max

/**
 * Przewodnik przy pierwszym uruchomieniu: krok 1 – wygląd, krok 2 – motyw.
 * Celowo neutralny (bez barw żadnego wyglądu), żeby niczego nie podpowiadać.
 *
 * Wybór zapisuje się od razu ([onLookChange], [onThemeChange]), więc w kroku 2 cały ekran
 * przełącza się na wybrany motyw na żywo. [onFinish] dostaje wybrany wygląd
 * (zapis „przewodnik zakończony” + zmiana ikony aplikacji).
 */
@Composable
fun OnboardingScreen(
    look: Look?,
    theme: ThemeMode,
    onLookChange: (Look) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onFinish: (Look) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    BackHandler(enabled = step == 2) { step = 1 }

    val dark = LookTheme.isDark
    val c = if (dark) WelcomeDark else WelcomeLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        TopRow(step = step, c = c, onBack = { step = 1 })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(14.dp))
            if (step == 1) {
                Heading(
                    title = "Jak ma wyglądać Twoja Przypominajka?",
                    text = "Każdy wygląd ma inny układ ekranów i swoją ikonę. Zmienisz go później w Ustawieniach.",
                    c = c,
                )
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Look.entries.forEach { option ->
                        LookCard(
                            look = option,
                            selected = option == look,
                            dark = dark,
                            c = c,
                            onClick = { onLookChange(option) },
                        )
                    }
                }
            } else {
                val chosen = look ?: Look.KARTKA
                Heading(
                    title = "Jasny czy ciemny?",
                    text = "Podgląd pokazuje wygląd ${chosen.label}. Widżety na pulpicie też się dopasują.",
                    c = c,
                )
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach { mode ->
                        ThemeCard(
                            look = chosen,
                            mode = mode,
                            selected = mode == theme,
                            c = c,
                            onClick = { onThemeChange(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "„Jak w telefonie” przełącza się samo, gdy telefon zmienia tryb. " +
                        "Oba wybory zmienisz później w Ustawieniach → Wygląd.",
                    style = body(14.sp),
                    color = c.ink2,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        val enabled = look != null
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(if (enabled) 1f else 0.35f)
                .clip(RoundedCornerShape(16.dp))
                .background(c.ink)
                .clickable(enabled = enabled, role = Role.Button) {
                    val chosen = look ?: return@clickable
                    if (step == 1) step = 2 else onFinish(chosen)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (step == 1) "Dalej" else "Gotowe",
                style = body(16.sp, FontWeight.Bold),
                color = c.bg,
            )
        }
    }
}

// ---------------------------------------------------------------- kolory i teksty

/** Neutralne kolory przewodnika (grafit na bieli / jasny na grafitowym). */
private data class WelcomeColors(
    val bg: Color,
    val surface: Color,
    val selBg: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val off: Color,
)

private val WelcomeLight = WelcomeColors(
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFF4F5F7),
    selBg = Color(0xFFFFFFFF),
    line = Color(0xFFE3E5E9),
    ink = Color(0xFF15171C),
    ink2 = Color(0xFF5C6270),
    off = Color(0xFFD9DCE1),
)

private val WelcomeDark = WelcomeColors(
    bg = Color(0xFF101114),
    surface = Color(0xFF1B1D22),
    selBg = Color(0xFF22252B),
    line = Color(0xFF2C2F36),
    ink = Color(0xFFF3F4F6),
    ink2 = Color(0xFFA5ABB6),
    off = Color(0xFF3A3E46),
)

/** Zwykła czcionka telefonu – przewodnik nie używa czcionek żadnego wyglądu. */
private fun body(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size,
    fontWeight = weight,
    lineHeight = size * 1.4f,
)

private val Look.pitch: String
    get() = when (this) {
        Look.KARTKA -> "Jasno i spokojnie: lista, cienkie linie, jeden kolor."
        Look.DZWON -> "Złoty dzwon na środku, działy na okręgu wokół niego."
        Look.TABLICA -> "Kolorowe karteczki przypięte do tablicy."
    }

// ---------------------------------------------------------------- części ekranu

@Composable
private fun TopRow(step: Int, c: WelcomeColors, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step == 2) {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = c.ink)
                Spacer(Modifier.width(6.dp))
                Text("Wstecz", style = body(15.sp, FontWeight.SemiBold), color = c.ink)
            }
        } else {
            Text("Przypominajka", style = body(15.sp, FontWeight.Bold), color = c.ink)
        }
        Spacer(Modifier.weight(1f))
        Text("Krok $step z 2", style = body(13.sp, FontWeight.SemiBold), color = c.ink2)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(22.dp, 4.dp).clip(CircleShape).background(c.ink))
        Spacer(Modifier.width(4.dp))
        Box(Modifier.size(22.dp, 4.dp).clip(CircleShape).background(if (step == 2) c.ink else c.off))
    }
}

@Composable
private fun Heading(title: String, text: String, c: WelcomeColors) {
    Text(
        text = title,
        style = body(28.sp, FontWeight.Bold).copy(lineHeight = 32.sp, letterSpacing = (-0.4).sp),
        color = c.ink,
    )
    Spacer(Modifier.height(8.dp))
    Text(text = text, style = body(15.sp), color = c.ink2)
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun LookCard(look: Look, selected: Boolean, dark: Boolean, c: WelcomeColors, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(2.dp, if (selected) c.ink else c.line, shape)
            .background(if (selected) c.selBg else c.surface)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp, 132.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, c.line, RoundedCornerShape(12.dp)),
        ) {
            LookMiniature(look, dark, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(look.label, style = body(18.sp, FontWeight.Bold), color = c.ink)
            Text(look.pitch, style = body(14.sp), color = c.ink2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LookIcon(look)
                Spacer(Modifier.width(8.dp))
                Text("taka ikona", style = body(13.sp), color = c.ink2)
            }
        }
        Spacer(Modifier.width(8.dp))
        RadioMark(selected, c, 24.dp)
    }
}

@Composable
private fun ThemeCard(
    look: Look,
    mode: ThemeMode,
    selected: Boolean,
    c: WelcomeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(2.dp, if (selected) c.ink else c.line, shape)
            .background(if (selected) c.selBg else c.surface)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, c.line, RoundedCornerShape(10.dp)),
        ) {
            when (mode) {
                ThemeMode.LIGHT -> LookMiniature(look, dark = false, Modifier.fillMaxSize())
                ThemeMode.DARK -> LookMiniature(look, dark = true, Modifier.fillMaxSize())
                ThemeMode.SYSTEM -> {
                    // Pół na pół: lewa połowa jasna, prawa ciemna.
                    LookMiniature(look, dark = false, Modifier.fillMaxSize())
                    LookMiniature(
                        look,
                        dark = true,
                        Modifier
                            .fillMaxSize()
                            .drawWithContent { clipRect(left = size.width / 2f) { this@drawWithContent.drawContent() } },
                    )
                }
            }
        }
        Box(Modifier.heightIn(min = 34.dp), contentAlignment = Alignment.Center) {
            Text(
                text = mode.label,
                style = body(14.sp, FontWeight.Bold).copy(lineHeight = 17.sp),
                color = c.ink,
                textAlign = TextAlign.Center,
            )
        }
        RadioMark(selected, c, 22.dp)
    }
}

@Composable
private fun RadioMark(selected: Boolean, c: WelcomeColors, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) c.ink else Color.Transparent)
            .border(2.dp, if (selected) c.ink else c.ink2, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = c.bg, modifier = Modifier.size(size * 0.62f))
        }
    }
}

/** Ikona aplikacji danego wyglądu złożona z warstw ikony adaptacyjnej (widoczne środkowe 72 z 108 dp). */
@Composable
private fun LookIcon(look: Look, size: Dp = 26.dp) {
    val (background, foreground) = when (look) {
        Look.KARTKA -> R.drawable.ic_launcher_kartka_background to R.drawable.ic_launcher_kartka_foreground
        Look.DZWON -> R.drawable.ic_launcher_dzwon_background to R.drawable.ic_launcher_dzwon_foreground
        Look.TABLICA -> R.drawable.ic_launcher_tablica_background to R.drawable.ic_launcher_tablica_foreground
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(30)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(background), contentDescription = null, modifier = Modifier.requiredSize(size * 1.5f))
        Image(painterResource(foreground), contentDescription = null, modifier = Modifier.requiredSize(size * 1.5f))
    }
}

// ---------------------------------------------------------------- miniatury wyglądów

private const val MINI_W = 84f
private const val MINI_H = 132f
private const val BELL_BODY = "M60 18C37 18 25 36 25 60v20L13 98h94L95 80V60c0-24-12-42-35-42z"

/** Uproszczony ekran startowy wyglądu (układ 84×132 jak w makiecie), skalowany „na wypełnienie”. */
@Composable
private fun LookMiniature(look: Look, dark: Boolean, modifier: Modifier = Modifier) {
    val bell = remember { PathParser().parsePathString(BELL_BODY).toPath() }
    Canvas(modifier) {
        val s = max(size.width / MINI_W, size.height / MINI_H)
        val dx = (size.width - MINI_W * s) / 2f
        val dy = (size.height - MINI_H * s) / 2f
        withTransform({
            translate(dx, dy)
            scale(s, s, pivot = Offset.Zero)
        }) {
            when (look) {
                Look.KARTKA -> drawKartka(KartkaColors.of(dark))
                Look.DZWON -> drawDzwon(DzwonColors.of(dark), bell)
                Look.TABLICA -> drawTablica(TablicaColors.of(dark))
            }
        }
    }
}

private fun DrawScope.bar(x: Float, y: Float, w: Float, h: Float, color: Color, r: Float = 1.5f) =
    drawRoundRect(color, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(r, r))

private fun DrawScope.frame(x: Float, y: Float, w: Float, h: Float, color: Color, r: Float) =
    drawRoundRect(color, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(r, r), style = Stroke(1.2f))

private fun DrawScope.drawKartka(c: KartkaColors) {
    val deco = c.ink2.copy(alpha = 0.7f)
    drawRect(c.bg, size = Size(MINI_W, MINI_H))
    bar(8f, 10f, 28f, 3f, deco)
    bar(8f, 19f, 60f, 6f, c.ink, 2f)
    bar(8f, 28f, 46f, 6f, c.accent, 2f)
    bar(8f, 37f, 54f, 6f, c.ink, 2f)
    bar(8f, 52f, 11f, 4f, c.ink, 1f)
    bar(23f, 52f, 44f, 4f, deco, 1f)
    bar(8f, 62f, 11f, 4f, c.ink, 1f)
    bar(23f, 62f, 38f, 4f, deco, 1f)
    bar(6f, 74f, 72f, 28f, c.surface, 5f)
    frame(11f, 80f, 6f, 6f, deco, 1.5f)
    bar(21f, 81.5f, 40f, 3f, deco, 1f)
    frame(11f, 90f, 6f, 6f, deco, 1.5f)
    bar(21f, 91.5f, 32f, 3f, deco, 1f)
    frame(6f, 108f, 34f, 16f, c.line, 4f)
    frame(44f, 108f, 34f, 16f, c.line, 4f)
}

private fun DrawScope.drawDzwon(c: DzwonColors, bell: Path) {
    drawRect(c.bg, size = Size(MINI_W, MINI_H))
    drawCircle(c.track, radius = 30f, center = Offset(42f, 58f), style = Stroke(3f))
    drawArc(
        color = c.gold,
        startAngle = -90f,
        sweepAngle = 152f,
        useCenter = false,
        topLeft = Offset(12f, 28f),
        size = Size(60f, 60f),
        style = Stroke(3f, cap = StrokeCap.Round),
    )
    bar(30f, 24f, 24f, 8f, c.gold, 4f)
    bar(60f, 45f, 21f, 8f, c.surface, 4f)
    bar(3f, 45f, 21f, 8f, c.surface, 4f)
    bar(50f, 80f, 21f, 8f, c.surface, 4f)
    bar(13f, 80f, 21f, 8f, c.surface, 4f)
    withTransform({
        translate(29f, 44f)
        scale(0.22f, 0.22f, pivot = Offset.Zero)
    }) {
        drawCircle(c.gold, radius = 7f, center = Offset(60f, 12f))
        drawPath(bell, c.gold)
        drawRoundRect(c.goldDeep, topLeft = Offset(9f, 95f), size = Size(102f, 11f), cornerRadius = CornerRadius(5.5f, 5.5f))
        drawCircle(c.goldDeep, radius = 10f, center = Offset(60f, 116f))
    }
    bar(8f, 100f, 68f, 24f, c.surface, 7f)
}

private fun DrawScope.drawTablica(c: TablicaColors) {
    drawRect(c.board, size = Size(MINI_W, MINI_H))
    var y = 4f
    while (y < MINI_H) {
        var x = 4f
        while (x < MINI_W) {
            drawCircle(c.dot, radius = 0.8f, center = Offset(x, y))
            x += 9f
        }
        y += 9f
    }
    note(7f, 10f, 34f, 38f, c.yellow.bg, -4f)
    note(45f, 8f, 32f, 50f, c.mint.bg, 3f)
    note(8f, 54f, 33f, 30f, c.blue.bg, 2f)
    note(46f, 64f, 31f, 30f, c.pink.bg, -3f)
    bar(17f, 7f, 14f, 5f, c.tape, 0f)
    bar(54f, 5f, 14f, 5f, c.tape, 0f)
    bar(6f, 110f, 72f, 15f, c.dock, 6f)
    drawCircle(c.dockActive, radius = 5f, center = Offset(42f, 117.5f))
}

private fun DrawScope.note(x: Float, y: Float, w: Float, h: Float, color: Color, degrees: Float) {
    rotate(degrees, pivot = Offset(x + w / 2f, y + h / 2f)) {
        bar(x, y, w, h, color)
    }
}
