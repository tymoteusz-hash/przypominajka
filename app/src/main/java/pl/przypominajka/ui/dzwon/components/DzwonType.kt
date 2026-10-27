package pl.przypominajka.ui.dzwon.components

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pl.przypominajka.ui.look.LookFonts

/**
 * Style tekstu Dzwonu wprost z makiety (bez kolorów – kolor podaje ekran z `LookTheme.dzwon`).
 * Nagłówki: Fraunces (szeryfowy), reszta: Manrope.
 */
object DzwonType {
    /** Tytuł ekranu-arkusza („Checklista”), 30 sp. */
    val ScreenTitle = TextStyle(
        fontFamily = LookFonts.DzwonHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
    )

    /** Powitanie na stronie głównej („Cześć!”), 28 sp. */
    val Greeting = TextStyle(
        fontFamily = LookFonts.DzwonHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    )

    /** Tytuł arkusza dolnego, okna i pustego stanu, 22 sp. */
    val SheetTitle = TextStyle(
        fontFamily = LookFonts.DzwonHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    )

    /** Nagłówek karty / sekcji („Następne”), 19 sp. */
    val SectionTitle = TextStyle(
        fontFamily = LookFonts.DzwonHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )

    /** Mniejszy nagłówek (np. w komunikacie), 17 sp. */
    val SmallTitle = TextStyle(
        fontFamily = LookFonts.DzwonHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    )

    /** Tekst punktu listy i pól, 16 sp. */
    val Body = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    )

    /** Tytuł wiersza listy, 15 sp półgruby. */
    val RowTitle = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )

    /** Tekst pomocniczy (podtytuły, opisy), 13 sp. */
    val Caption = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    /** Pogrubiony tekst pomocniczy (data, podpisy pod dzwonem), 13 sp. */
    val CaptionStrong = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    /** Godzina przy punkcie („dziś 19:00”), 13 sp. */
    val Time = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
    )

    /** Duża godzina w karcie „Następne”, 16 sp. */
    val TimeLarge = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum",
    )

    /** Napis na przycisku, 15 sp. */
    val Button = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )

    /** Przycisk tekstowy i chip, 14 sp. */
    val Chip = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )

    /** Odznaka (liczba), 12 sp. */
    val Badge = TextStyle(
        fontFamily = LookFonts.DzwonBody,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    )
}
