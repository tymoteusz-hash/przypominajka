package pl.przypominajka.ui.tablica.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.przypominajka.ui.look.LookFonts
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.look.TablicaColors

// =====================================================================================================
// TABLICA – podstawy: „papier” pod treścią, odstęp na dok, style tekstu, tło w kropki i przerywana ramka.
// =====================================================================================================

/**
 * Kolor „papieru” pod treścią: tło karteczki ([Note]) albo tablicy ([TablicaScreen]).
 * Przyciski wypełnione atramentem biorą go jako kolor napisu. Unspecified = brak (wtedy kolor tablicy).
 */
val LocalPaper = compositionLocalOf { Color.Unspecified }

/**
 * Ile miejsca na dole zajmuje pływający dok (razem z paskiem systemowym). Ekrany z dokiem dodają tyle
 * odstępu na końcu treści, a pasek „Cofnij” stoi dokładnie nad dokiem. Na ekranach szczegółów (bez doku)
 * to tylko wysokość paska systemowego.
 */
val LocalDockPadding = compositionLocalOf { 0.dp }

/** Kąty karteczek na liście (jak w makiecie checklisty) – n-ta karteczka dostaje [noteTilt] (n). */
val NoteTilts: List<Float> = listOf(-1.5f, 1.2f, 0.8f, -1.1f, 1.6f, -0.7f, 1.0f, -1.3f)

/** Lekkie przechylenie n-tej karteczki na liście (kąty się powtarzają). */
fun noteTilt(index: Int): Float = NoteTilts[Math.floorMod(index, NoteTilts.size)]

/** Stały kolor karteczki dla elementu o danym id (żółta, różowa, miętowa, niebieska, liliowa). */
fun TablicaColors.noteFor(id: Long): NoteColor = notes[Math.floorMod(id, notes.size.toLong()).toInt()]

/** Atrament na czerwonej pinezce (ostrzeżenia) – ciemny w obu motywach, dla kontrastu. */
val PinInk: Color = TablicaColors.Light.ink

/**
 * Style tekstu Tablicy: odręczny Caveat do nagłówków i treści karteczek, Nunito do reszty.
 * Kolor nie jest ustawiony – tekst bierze LocalContentColor (atrament tablicy albo karteczki).
 */
object TablicaType {
    private val hand = LookFonts.TablicaHeading
    private val body = LookFonts.TablicaBody

    /** Nagłówek ekranu: „Poniedziałek”, „Moja lista” (Caveat 48). */
    val ScreenTitle = TextStyle(fontFamily = hand, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 50.sp)

    /** Tytuł okna i paska szczegółów (Caveat 34). */
    val DialogTitle = TextStyle(fontFamily = hand, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 36.sp)

    /** Tytuł karteczki: „Dziś”, „Lista” (Caveat 32). */
    val NoteTitle = TextStyle(fontFamily = hand, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 32.sp)

    /** Nagłówek sekcji na tablicy (Caveat 28). */
    val Section = TextStyle(fontFamily = hand, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 30.sp)

    /** Treść karteczki na liście (Caveat 24). */
    val NoteText = TextStyle(fontFamily = hand, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 25.sp)

    /** Podtytuł pod nagłówkiem ekranu (Nunito 15 Bold). */
    val Subtitle = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp)

    /** Wyróżniony wiersz na karteczce: „teraz: Fizyka” (Nunito 16 ExtraBold). */
    val Strong = TextStyle(fontFamily = body, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 21.sp)

    /** Zwykły tekst na karteczce (Nunito 14 Bold). */
    val Body = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)

    /** Dłuższy opis (Nunito 14 SemiBold). */
    val BodyLight = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)

    /** Drobny opis, podpowiedź (Nunito 13 SemiBold). */
    val Hint = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)

    /** Podkreślony „link” na dole karteczki (Nunito 13 ExtraBold). */
    val Link = TextStyle(
        fontFamily = body,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        textDecoration = TextDecoration.Underline,
    )

    /** Napis na przyciskach (Nunito 15 ExtraBold). */
    val Button = TextStyle(fontFamily = body, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp)

    /** Godzina przy karteczce, odznaki (Nunito 12 ExtraBold, cyfry równej szerokości). */
    val Tag = TextStyle(
        fontFamily = body,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    )

    /** Godziny na liście „Dziś” (Nunito 14 Bold, cyfry równej szerokości). */
    val Time = Body.copy(fontFeatureSettings = "tnum")
}

/**
 * Tło tablicy: kolor [TablicaColors.board] i kropki co 22 dp (promień 1,6 dp) – rysowane, nie z obrazka.
 * Ścieżka kropek liczona raz na rozmiar (drawWithCache), więc przewijanie jej nie przelicza.
 */
fun Modifier.boardBackground(colors: TablicaColors): Modifier = drawWithCache {
    val step = 22.dp.toPx()
    val radius = 1.6.dp.toPx()
    val start = 3.dp.toPx()
    val dots = Path()
    var y = start
    while (y < size.height + radius) {
        var x = start
        while (x < size.width + radius) {
            dots.addOval(Rect(center = Offset(x, y), radius = radius))
            x += step
        }
        y += step
    }
    onDrawBehind {
        drawRect(colors.board)
        drawPath(dots, colors.dot)
    }
}

/** Przerywana ramka (np. kafelek „Nowa karteczka”) – 2,5 dp, zaokrąglenie 6 dp jak w makiecie. */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 2.5.dp,
    radius: Dp = 6.dp,
    dash: Dp = 7.dp,
    gap: Dp = 5.dp,
): Modifier = drawWithCache {
    val strokeWidth = width.toPx()
    val stroke = Stroke(
        width = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
    )
    val inset = strokeWidth / 2
    val corner = CornerRadius(radius.toPx())
    onDrawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = corner,
            style = stroke,
        )
    }
}

/**
 * Odstępy treści ekranu z dokiem: z boku [horizontal], u góry pasek stanu + [top],
 * na dole miejsce na dok ([LocalDockPadding]) + [bottom]. Do LazyColumn/LazyVerticalGrid.
 */
@Composable
fun dockContentPadding(horizontal: Dp = 16.dp, top: Dp = 18.dp, bottom: Dp = 16.dp): PaddingValues {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return PaddingValues(
        start = horizontal,
        end = horizontal,
        top = statusBar + top,
        bottom = LocalDockPadding.current + bottom,
    )
}
