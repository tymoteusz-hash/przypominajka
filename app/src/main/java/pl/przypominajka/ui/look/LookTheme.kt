package pl.przypominajka.ui.look

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================================================
// MOTYW WYGLĄDU 4.0 – dla wybranego wyglądu i trybu (jasny/ciemny) dostarcza:
//  • tokeny makiety (LocalKartkaColors / LocalDzwonColors / LocalTablicaColors),
//  • pasujący ColorScheme, Typography i Shapes Material 3 – żeby zwykłe okna, wybór daty i godziny,
//    pola tekstowe i paski „Cofnij” z Material 3 wyglądały jak reszta danego wyglądu.
// =====================================================================================================

/** Wygląd, w którym rysujemy (ustawiany przez [LookTheme]). */
val LocalLook = staticCompositionLocalOf { Look.KARTKA }

/** Czy obowiązuje ciemny motyw (już po rozstrzygnięciu „Jak w telefonie”). */
val LocalIsDark = staticCompositionLocalOf { false }

@Composable
fun LookTheme(look: Look, dark: Boolean, content: @Composable () -> Unit) {
    val kartka = KartkaColors.of(dark)
    val dzwon = DzwonColors.of(dark)
    val tablica = TablicaColors.of(dark)
    val (colorScheme, typography, shapes) = remember(look, dark) {
        when (look) {
            Look.KARTKA -> Triple(kartkaScheme(kartka, dark), KartkaTypography, KartkaShapes)
            Look.DZWON -> Triple(dzwonScheme(dzwon, dark), DzwonTypography, DzwonShapes)
            Look.TABLICA -> Triple(tablicaScheme(tablica, dark), TablicaTypography, TablicaShapes)
        }
    }
    // Tokeny wszystkich wyglądów są zawsze dostępne – wspólne komponenty nie muszą sprawdzać, który jest wybrany.
    CompositionLocalProvider(
        LocalLook provides look,
        LocalIsDark provides dark,
        LocalKartkaColors provides kartka,
        LocalDzwonColors provides dzwon,
        LocalTablicaColors provides tablica,
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = shapes, content = content)
    }
}

/** Skróty jak MaterialTheme: `LookTheme.dzwon.gold`, `LookTheme.isDark`… */
object LookTheme {
    val look: Look
        @Composable @ReadOnlyComposable
        get() = LocalLook.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDark.current

    val kartka: KartkaColors
        @Composable @ReadOnlyComposable
        get() = LocalKartkaColors.current

    val dzwon: DzwonColors
        @Composable @ReadOnlyComposable
        get() = LocalDzwonColors.current

    val tablica: TablicaColors
        @Composable @ReadOnlyComposable
        get() = LocalTablicaColors.current
}

// ---------------------------------------------------------------- kolory Material 3

/** Kolory błędów – makiety ich nie mają; jedna spokojna czerwień dla Kartki i Dzwonu. */
private val ErrorLight = Color(0xFFC8372D)
private val ErrorDark = Color(0xFFFF8A7E)

private fun kartkaScheme(c: KartkaColors, dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            primaryContainer = c.accentSoft, onPrimaryContainer = c.ink,
            inversePrimary = c.toastAction,
            secondary = c.accent, onSecondary = c.onAccent,
            secondaryContainer = c.accentSoft, onSecondaryContainer = c.ink,
            tertiary = c.accent, onTertiary = c.onAccent,
            tertiaryContainer = c.accentSoft, onTertiaryContainer = c.ink,
            background = c.bg, onBackground = c.ink,
            surface = c.bg, onSurface = c.ink,
            surfaceVariant = c.surface, onSurfaceVariant = c.ink2,
            surfaceTint = Color.Transparent,
            inverseSurface = c.ink, inverseOnSurface = c.bg,
            error = ErrorDark, onError = c.bg,
            errorContainer = Color(0xFF5C1A14), onErrorContainer = Color(0xFFFFDAD5),
            outline = lerp(c.line, c.ink2, 0.6f), outlineVariant = c.line,
            surfaceBright = c.raised, surfaceDim = c.bg,
            surfaceContainerLowest = c.bg,
            surfaceContainerLow = lerp(c.bg, c.surface, 0.5f),
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.raised,
            surfaceContainerHighest = lerp(c.raised, c.line, 0.5f),
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            primaryContainer = c.accentSoft, onPrimaryContainer = c.accent,
            inversePrimary = c.toastAction,
            secondary = c.accent, onSecondary = c.onAccent,
            secondaryContainer = c.accentSoft, onSecondaryContainer = c.ink,
            tertiary = c.accent, onTertiary = c.onAccent,
            tertiaryContainer = c.accentSoft, onTertiaryContainer = c.ink,
            background = c.bg, onBackground = c.ink,
            surface = c.bg, onSurface = c.ink,
            surfaceVariant = c.surface, onSurfaceVariant = c.ink2,
            surfaceTint = Color.Transparent,
            inverseSurface = c.ink, inverseOnSurface = c.bg,
            error = ErrorLight, onError = Color.White,
            errorContainer = Color(0xFFFCE4E1), onErrorContainer = Color(0xFF5C130D),
            outline = lerp(c.line, c.ink2, 0.6f), outlineVariant = c.line,
            surfaceBright = c.bg, surfaceDim = c.surface,
            surfaceContainerLowest = c.raised,
            surfaceContainerLow = lerp(c.bg, c.surface, 0.5f),
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.raised,
            surfaceContainerHighest = lerp(c.surface, c.line, 0.5f),
        )
    }

private fun dzwonScheme(c: DzwonColors, dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = c.gold, onPrimary = c.onGold,
            primaryContainer = c.gold, onPrimaryContainer = c.onGold,
            // Akcja na jasnym pasku „Cofnij” – złoto przyciemnione z jasnego motywu.
            inversePrimary = DzwonColors.Light.goldText,
            secondary = c.ink2, onSecondary = c.bg,
            secondaryContainer = c.track, onSecondaryContainer = c.ink,
            tertiary = c.goldText, onTertiary = c.onGold,
            tertiaryContainer = c.gold, onTertiaryContainer = c.onGold,
            background = c.bg, onBackground = c.ink,
            surface = c.bg, onSurface = c.ink,
            surfaceVariant = c.field, onSurfaceVariant = c.ink2,
            surfaceTint = Color.Transparent,
            inverseSurface = c.toastBg, inverseOnSurface = c.toastInk,
            error = ErrorDark, onError = c.onGold,
            errorContainer = Color(0xFF5C1A14), onErrorContainer = Color(0xFFFFDAD5),
            outline = lerp(c.track, c.ink2, 0.5f), outlineVariant = c.line,
            surfaceBright = c.lifted, surfaceDim = c.bg,
            surfaceContainerLowest = c.bg,
            surfaceContainerLow = c.halo,
            surfaceContainer = c.sheet,
            surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.field,
        )
    } else {
        lightColorScheme(
            // W jasnym motywie złoto jako tekst przycisków byłoby nieczytelne – „primary” to złoto przyciemnione.
            primary = c.goldText, onPrimary = Color.White,
            primaryContainer = c.gold, onPrimaryContainer = c.onGold,
            inversePrimary = DzwonColors.Dark.gold,
            secondary = c.ink2, onSecondary = Color.White,
            secondaryContainer = c.track, onSecondaryContainer = c.ink,
            tertiary = c.goldText, onTertiary = Color.White,
            tertiaryContainer = c.gold, onTertiaryContainer = c.onGold,
            background = c.bg, onBackground = c.ink,
            surface = c.bg, onSurface = c.ink,
            surfaceVariant = c.field, onSurfaceVariant = c.ink2,
            surfaceTint = Color.Transparent,
            inverseSurface = c.toastBg, inverseOnSurface = c.toastInk,
            error = ErrorLight, onError = Color.White,
            errorContainer = Color(0xFFFCE4E1), onErrorContainer = Color(0xFF5C130D),
            outline = lerp(c.track, c.ink2, 0.5f), outlineVariant = c.line,
            surfaceBright = c.surface, surfaceDim = c.halo,
            surfaceContainerLowest = c.surface,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.sheet,
            surfaceContainerHigh = c.sheet,
            surfaceContainerHighest = c.field,
        )
    }

private fun tablicaScheme(c: TablicaColors, dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            // Przyciski w kolorze „atramentu”, zaznaczenia jak żółta karteczka.
            primary = c.ink, onPrimary = c.board,
            primaryContainer = c.yellow.bg, onPrimaryContainer = c.yellow.ink,
            inversePrimary = c.dockActive,
            secondary = c.ink2, onSecondary = c.board,
            secondaryContainer = c.blue.bg, onSecondaryContainer = c.blue.ink,
            tertiary = c.pin, onTertiary = Color.White,
            tertiaryContainer = c.pink.bg, onTertiaryContainer = c.pink.ink,
            background = c.board, onBackground = c.ink,
            surface = c.board, onSurface = c.ink,
            surfaceVariant = lerp(c.board, c.dot, 0.5f), onSurfaceVariant = c.ink2,
            surfaceTint = Color.Transparent,
            inverseSurface = c.dock, inverseOnSurface = c.dockText,
            error = c.pin, onError = Color.White,
            errorContainer = c.pink.bg, onErrorContainer = c.pink.ink,
            outline = lerp(c.dot, c.ink2, 0.5f), outlineVariant = c.dot,
            surfaceBright = lerp(c.board, Color.White, 0.08f), surfaceDim = c.board,
            surfaceContainerLowest = lerp(c.board, Color.Black, 0.3f),
            surfaceContainerLow = lerp(c.board, Color.White, 0.03f),
            surfaceContainer = lerp(c.board, Color.White, 0.05f),
            surfaceContainerHigh = lerp(c.board, Color.White, 0.08f),
            surfaceContainerHighest = lerp(c.board, c.dot, 0.7f),
        )
    } else {
        lightColorScheme(
            primary = c.ink, onPrimary = c.board,
            primaryContainer = c.yellow.bg, onPrimaryContainer = c.yellow.ink,
            inversePrimary = c.dockActive,
            secondary = c.ink2, onSecondary = Color.White,
            secondaryContainer = c.blue.bg, onSecondaryContainer = c.blue.ink,
            tertiary = c.pin, onTertiary = Color.White,
            tertiaryContainer = c.pink.bg, onTertiaryContainer = c.pink.ink,
            background = c.board, onBackground = c.ink,
            surface = c.board, onSurface = c.ink,
            surfaceVariant = lerp(c.board, c.dot, 0.5f), onSurfaceVariant = c.ink2,
            surfaceTint = Color.Transparent,
            inverseSurface = c.dock, inverseOnSurface = c.dockText,
            error = c.pin, onError = Color.White,
            errorContainer = c.pink.bg, onErrorContainer = c.pink.ink,
            outline = lerp(c.dot, c.ink2, 0.5f), outlineVariant = c.dot,
            surfaceBright = lerp(c.board, Color.White, 0.75f), surfaceDim = c.board,
            // Okna i karty jak jasny papier na tablicy.
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = lerp(c.board, Color.White, 0.4f),
            surfaceContainer = lerp(c.board, Color.White, 0.55f),
            surfaceContainerHigh = lerp(c.board, Color.White, 0.75f),
            surfaceContainerHighest = lerp(c.board, c.dot, 0.6f),
        )
    }

// ---------------------------------------------------------------- typografia

private val Base = Typography()

private fun TextStyle.styled(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: Double = 0.0,
): TextStyle = copy(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

/** Kartka: Inter Tight – półgrube, ciasne nagłówki, spokojny tekst. */
private val KartkaTypography: Typography = LookFonts.KartkaHeading.let { h ->
    val b = LookFonts.KartkaBody
    Typography(
        displayLarge = Base.displayLarge.styled(h, FontWeight.SemiBold, 56, 60, -1.4),
        displayMedium = Base.displayMedium.styled(h, FontWeight.SemiBold, 44, 48, -1.1),
        displaySmall = Base.displaySmall.styled(h, FontWeight.SemiBold, 36, 40, -0.9),
        headlineLarge = Base.headlineLarge.styled(h, FontWeight.SemiBold, 34, 38, -0.85),
        headlineMedium = Base.headlineMedium.styled(h, FontWeight.SemiBold, 28, 32, -0.6),
        headlineSmall = Base.headlineSmall.styled(h, FontWeight.SemiBold, 24, 30, -0.4),
        titleLarge = Base.titleLarge.styled(h, FontWeight.SemiBold, 20, 26, -0.2),
        titleMedium = Base.titleMedium.styled(h, FontWeight.SemiBold, 18, 24, -0.1),
        titleSmall = Base.titleSmall.styled(h, FontWeight.SemiBold, 15, 20),
        bodyLarge = Base.bodyLarge.styled(b, FontWeight.Normal, 16, 24, 0.1),
        bodyMedium = Base.bodyMedium.styled(b, FontWeight.Normal, 15, 21, 0.1),
        bodySmall = Base.bodySmall.styled(b, FontWeight.Normal, 13, 18, 0.1),
        labelLarge = Base.labelLarge.styled(b, FontWeight.SemiBold, 15, 20),
        labelMedium = Base.labelMedium.styled(b, FontWeight.SemiBold, 13, 18, 0.1),
        // Nadtytuły WIELKIMI LITERAMI z rozstrzeleniem jak w makiecie.
        labelSmall = Base.labelSmall.styled(b, FontWeight.SemiBold, 12, 16, 1.0),
    )
}

/** Dzwon: Fraunces (szeryfowe nagłówki) + Manrope (tekst). */
private val DzwonTypography: Typography = LookFonts.DzwonHeading.let { h ->
    val b = LookFonts.DzwonBody
    Typography(
        displayLarge = Base.displayLarge.styled(h, FontWeight.Bold, 56, 60, -0.8),
        displayMedium = Base.displayMedium.styled(h, FontWeight.Bold, 44, 48, -0.6),
        displaySmall = Base.displaySmall.styled(h, FontWeight.Bold, 36, 40, -0.4),
        headlineLarge = Base.headlineLarge.styled(h, FontWeight.Bold, 32, 36, -0.3),
        headlineMedium = Base.headlineMedium.styled(h, FontWeight.Bold, 28, 32, -0.2),
        headlineSmall = Base.headlineSmall.styled(h, FontWeight.Bold, 22, 28),
        titleLarge = Base.titleLarge.styled(h, FontWeight.Bold, 19, 24),
        titleMedium = Base.titleMedium.styled(h, FontWeight.Bold, 17, 22),
        titleSmall = Base.titleSmall.styled(h, FontWeight.Bold, 15, 20),
        bodyLarge = Base.bodyLarge.styled(b, FontWeight.Medium, 16, 24, 0.1),
        bodyMedium = Base.bodyMedium.styled(b, FontWeight.Medium, 15, 21, 0.1),
        bodySmall = Base.bodySmall.styled(b, FontWeight.Medium, 13, 18, 0.1),
        labelLarge = Base.labelLarge.styled(b, FontWeight.ExtraBold, 15, 20),
        labelMedium = Base.labelMedium.styled(b, FontWeight.Bold, 13, 18, 0.1),
        labelSmall = Base.labelSmall.styled(b, FontWeight.Bold, 12, 16, 0.5),
    )
}

/**
 * Tablica: Caveat (pismo odręczne) + Nunito. Caveat jest drobny, więc nagłówki są większe niż zwykle;
 * titleMedium i titleSmall zostają w Nunito – odręczne pismo w rozmiarze 15–17 sp jest nieczytelne.
 */
private val TablicaTypography: Typography = LookFonts.TablicaHeading.let { h ->
    val b = LookFonts.TablicaBody
    Typography(
        displayLarge = Base.displayLarge.styled(h, FontWeight.Bold, 64, 64),
        displayMedium = Base.displayMedium.styled(h, FontWeight.Bold, 52, 54),
        displaySmall = Base.displaySmall.styled(h, FontWeight.Bold, 44, 46),
        headlineLarge = Base.headlineLarge.styled(h, FontWeight.Bold, 42, 44),
        headlineMedium = Base.headlineMedium.styled(h, FontWeight.Bold, 36, 38),
        headlineSmall = Base.headlineSmall.styled(h, FontWeight.Bold, 32, 34),
        titleLarge = Base.titleLarge.styled(h, FontWeight.Bold, 28, 30),
        titleMedium = Base.titleMedium.styled(b, FontWeight.ExtraBold, 17, 22),
        titleSmall = Base.titleSmall.styled(b, FontWeight.ExtraBold, 15, 20),
        bodyLarge = Base.bodyLarge.styled(b, FontWeight.SemiBold, 16, 24, 0.1),
        bodyMedium = Base.bodyMedium.styled(b, FontWeight.SemiBold, 14, 20, 0.1),
        bodySmall = Base.bodySmall.styled(b, FontWeight.SemiBold, 13, 18, 0.1),
        labelLarge = Base.labelLarge.styled(b, FontWeight.ExtraBold, 15, 20),
        labelMedium = Base.labelMedium.styled(b, FontWeight.Bold, 13, 18, 0.1),
        labelSmall = Base.labelSmall.styled(b, FontWeight.Bold, 12, 16, 0.5),
    )
}

// ---------------------------------------------------------------- kształty

/** Kartka: umiarkowanie zaokrąglone (12–16 dp), okna 24 dp. */
private val KartkaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Dzwon: miękko, „pigułkowo” (16–32 dp) – arkusze od dołu mają 32 dp. */
private val DzwonShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Tablica: karteczki prawie kanciaste (4–6 dp); przyciski i tak są okrągłe (domyślnie w Material 3). */
private val TablicaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)
