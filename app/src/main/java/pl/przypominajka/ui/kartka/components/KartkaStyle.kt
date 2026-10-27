package pl.przypominajka.ui.kartka.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.przypominajka.ui.look.LookFonts

// =====================================================================================================
// KARTKA – wymiary, kształty, typografia i ruch. Wartości z makiet Main / A_Checklista (1 px = 1 dp).
// Kolory zawsze z LookTheme.kartka (jasny i ciemny motyw).
// =====================================================================================================

/** Wymiary i kształty Kartki. */
object KartkaDefaults {
    /** Poziomy margines treści ekranu (tytuł, sekcje, karty). */
    val PagePadding = 20.dp

    /** Poziomy margines list z polem wyboru na początku wiersza (checklista). */
    val ListPadding = 12.dp

    /** Minimalny cel dotyku. */
    val TouchTarget = 48.dp

    /** Przyciski, pola tekstowe, przyciski-ikony. */
    val ButtonShape = RoundedCornerShape(12.dp)
    val FieldShape = RoundedCornerShape(12.dp)

    /** Kafelki działów, uniesiony wiersz, pasek „Cofnij”. */
    val TileShape = RoundedCornerShape(14.dp)

    /** Szare bloki (karta podglądu checklisty, ogłoszenia). */
    val CardShape = RoundedCornerShape(16.dp)

    /** Okna dialogowe i arkusz dolny. */
    val DialogShape = RoundedCornerShape(24.dp)
    val PillShape = RoundedCornerShape(percent = 50)

    /** Cienkie linie zamiast cieni. */
    val LineWidth = 1.dp
}

/**
 * Style tekstu Kartki: Inter Tight, ciasne półgrube nagłówki, nadtytuły WIELKIMI LITERAMI z rozstrzeleniem.
 * Kolor dobiera komponent (ink / ink2 / accent z [pl.przypominajka.ui.look.LookTheme.kartka]).
 */
object KartkaType {
    private val Family = LookFonts.KartkaBody

    /** Wielki nagłówek strony głównej („Dziś masz…”), 34 sp. */
    val Hero = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.85).sp,
    )

    /** Tytuł ekranu w treści (pod strzałką „Wróć”), 32 sp. */
    val PageTitle = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp,
    )

    /** Duży tytuł: pusty stan, liczba w nagłówku sekcji, 20 sp. */
    val TitleLarge = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp,
    )

    /** Tytuł karty i okna, 18 sp. */
    val Title = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp,
    )

    /** Tytuł wiersza listy, 16 sp Medium. */
    val RowTitle = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 21.sp,
    )

    /** Zwykły tekst (punkt checklisty, pole tekstowe), 16 sp. */
    val Body = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 21.sp,
    )

    /** Podtytuł ekranu, tekst w kartach, 15 sp. */
    val BodySmall = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 20.sp,
    )

    /** Druga linia wiersza, podpowiedzi, 13 sp. */
    val Caption = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp,
    )

    /** Godzina przy punkcie, „Przypomnę dziś o 18:00”, 13 sp SemiBold. */
    val CaptionStrong = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum",
    )

    /** Nadtytuł („PON · 26 PAŹDZIERNIKA”, „NAJBLIŻEJ”) – tekst zamieniany na wielkie litery w [KartkaEyebrow]. */
    val Eyebrow = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 1.04.sp,
    )

    /** Napis na przycisku, 15 sp SemiBold. */
    val Button = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp,
    )

    /** Przycisk tekstowy / link w kolorze akcentu, 14 sp SemiBold. */
    val Link = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    )

    /** Godzina w kolumnie listy „Najbliżej” (cyfry o stałej szerokości), 18 sp. */
    val Time = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 22.sp, fontFeatureSettings = "tnum",
    )

    /** Chip / odznaka, 13 sp SemiBold. */
    val Chip = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 16.sp, fontFeatureSettings = "tnum",
    )
}

/**
 * Ruch Kartki: zwykły stos. Nowy ekran wjeżdża z prawej o ~10% szerokości i się pojawia (250 ms),
 * poprzedni lekko odsuwa się w lewo i znika; powrót odwrotnie. Wszystko krótkie i przerywalne.
 */
object KartkaMotion {
    const val DURATION_MS = 250

    /** Drobne zmiany stanu (kolor pola wyboru, uniesienie wiersza). */
    const val SHORT_MS = 150

    private fun <T> spec(): TweenSpec<T> = tween(DURATION_MS, easing = FastOutSlowInEasing)

    val Enter: EnterTransition =
        slideInHorizontally(spec()) { width -> width / 10 } + fadeIn(spec())
    val Exit: ExitTransition =
        slideOutHorizontally(spec()) { width -> -width / 25 } + fadeOut(spec())
    val PopEnter: EnterTransition =
        slideInHorizontally(spec()) { width -> -width / 25 } + fadeIn(spec())
    val PopExit: ExitTransition =
        slideOutHorizontally(spec()) { width -> width / 10 } + fadeOut(spec())
}
