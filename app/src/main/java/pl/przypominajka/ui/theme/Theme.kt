package pl.przypominajka.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================================================
// WYGLĄD 3.1 "ZŁOTY DZWON" – paleta zbudowana wokół złotego dzwonu:
// ciepły krem (tło), espresso (tekst, przyciski), złoto (dzwon, akcenty) i jedna rodzina
// przygaszonych, ziemistych barw o podobnej jasności i nasyceniu – żaden kolor nie "odstaje".
// =====================================================================================================

/** Złoto dzwonu – wspólne dla obu motywów (jak na ikonie). */
object BellGold {
    val Body = Color(0xFFFFC62E)
    val Shade = Color(0xFFE9A80F)
    val Deep = Color(0xFFC98A0A)
    val Light = Color(0xFFFFF0A0)
    val Mouth = Color(0xFF4A3127)
}

private val Espresso = Color(0xFF3A2A20)
private val Cream = Color(0xFFF8F3EA)

private val LightColors = lightColorScheme(
    primary = Espresso,
    onPrimary = Color(0xFFFFF6E6),
    primaryContainer = Color(0xFFF7E6B5),
    onPrimaryContainer = Color(0xFF3D2C00),
    inversePrimary = Color(0xFFF2C14E),
    secondary = Color(0xFF5E8B5A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEAD6),
    onSecondaryContainer = Color(0xFF1C3A1B),
    // Złoto przyciemnione do czytelnego tekstu na kremowym tle.
    tertiary = Color(0xFF9C6F00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7E6B5),
    onTertiaryContainer = Color(0xFF3D2C00),
    error = Color(0xFFB9472F),
    onError = Color.White,
    errorContainer = Color(0xFFF6DCD3),
    onErrorContainer = Color(0xFF4A1408),
    background = Cream,
    onBackground = Color(0xFF2B211A),
    surface = Cream,
    onSurface = Color(0xFF2B211A),
    surfaceTint = Espresso,
    surfaceVariant = Color(0xFFEFE7DA),
    onSurfaceVariant = Color(0xFF6B5E52),
    outline = Color(0xFFC7BBAA),
    outlineVariant = Color(0xFFE8DECF),
    inverseSurface = Espresso,
    inverseOnSurface = Cream,
    surfaceContainerLowest = Color(0xFFFFFDF9),
    surfaceContainerLow = Color(0xFFFBF7F0),
    surfaceContainer = Color(0xFFF6F0E6),
    surfaceContainerHigh = Color(0xFFFFFDF9),
    surfaceContainerHighest = Color(0xFFEDE4D6),
)

private val DarkColors = darkColorScheme(
    // W ciemnym motywie przyciski są złote z ciemnym tekstem.
    primary = Color(0xFFF2C14E),
    onPrimary = Color(0xFF2B1F05),
    primaryContainer = Color(0xFF4A3A12),
    onPrimaryContainer = Color(0xFFFCE7A8),
    inversePrimary = Espresso,
    secondary = Color(0xFF9CC598),
    onSecondary = Color(0xFF13300F),
    secondaryContainer = Color(0xFF2C3F28),
    onSecondaryContainer = Color(0xFFD5EBCF),
    tertiary = Color(0xFFF2C14E),
    onTertiary = Color(0xFF2B1F05),
    tertiaryContainer = Color(0xFF4A3A12),
    onTertiaryContainer = Color(0xFFFCE7A8),
    error = Color(0xFFF0927A),
    onError = Color(0xFF4A1408),
    errorContainer = Color(0xFF5A2618),
    onErrorContainer = Color(0xFFFCDDD3),
    background = Color(0xFF15110D),
    onBackground = Color(0xFFF3EADF),
    surface = Color(0xFF15110D),
    onSurface = Color(0xFFF3EADF),
    surfaceTint = Color(0xFFF2C14E),
    surfaceVariant = Color(0xFF2E2620),
    onSurfaceVariant = Color(0xFFBFB2A3),
    outline = Color(0xFF6E6257),
    outlineVariant = Color(0xFF382E26),
    inverseSurface = Color(0xFFF3EADF),
    inverseOnSurface = Color(0xFF2B211A),
    surfaceContainerLowest = Color(0xFF0F0C09),
    surfaceContainerLow = Color(0xFF1A1511),
    surfaceContainer = Color(0xFF1C1712),
    surfaceContainerHigh = Color(0xFF251E18),
    surfaceContainerHighest = Color(0xFF30281F),
)

private val BaseTypography = Typography()

/** Grube, ciasne nagłówki i spokojny, dobrze rozstawiony tekst. */
private val AppTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-2).sp),
    displaySmall = BaseTypography.displaySmall.copy(
        fontWeight = FontWeight.Black, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1.4).sp,
    ),
    headlineLarge = BaseTypography.headlineLarge.copy(
        fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-1.1).sp,
    ),
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.9).sp,
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(
        fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.6).sp,
    ),
    titleLarge = BaseTypography.titleLarge.copy(
        fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.4).sp,
    ),
    titleMedium = BaseTypography.titleMedium.copy(
        fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.1).sp,
    ),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp),
    bodyLarge = BaseTypography.bodyLarge.copy(lineHeight = 25.sp, letterSpacing = 0.15.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    bodySmall = BaseTypography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.15.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp),
    labelMedium = BaseTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = BaseTypography.labelSmall.copy(
        fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Motyw aplikacji – jasny/ciemny według ustawień systemu albo wyboru w Ustawieniach.
 * Kolory dynamiczne (z tapety) są celowo wyłączone, żeby aplikacja miała własny charakter.
 */
@Composable
fun PrzypominajkaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** Warstwy powierzchni – płaskie kolory: kremowe tło strony, jaśniejsze karty, espresso dla kart "hero". */
object AppSurfaces {
    val isLight: Boolean
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.isLight()

    val page: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.let { if (it.isLight()) it.surfaceContainer else it.surface }

    val card: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.let {
            if (it.isLight()) it.surfaceContainerLowest else it.surfaceContainerHigh
        }

    /** Delikatna ramka kart. */
    val cardBorder: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant

    /** Karta wyróżniona (profil, trening, fiszki) – ciemne espresso ze złotym dzwonem w tle. */
    val hero: Color
        @Composable @ReadOnlyComposable
        get() = if (MaterialTheme.colorScheme.isLight()) Espresso else Color(0xFF2B221B)

    /** Złoty akcent (nadtytuły, kropki, "teraz"). */
    val accent: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiary
}

/**
 * Jedna rodzina przygaszonych, ziemistych barw o podobnej jasności – kafelki działów, rodzaje wpisów,
 * wykresy. Tło ([container]) i mocny kolor ikon/tekstu ([content]) w obu motywach.
 */
enum class Pastel(
    private val lightContainer: Long,
    private val lightContent: Long,
    private val darkContainer: Long,
    private val darkContent: Long,
) {
    GOLD(0xFFF7E6B5, 0xFF9C6F00, 0xFF3F3214, 0xFFF2C14E),
    TERRACOTTA(0xFFF6DCD3, 0xFFB5523A, 0xFF46261C, 0xFFF0A08A),
    SAGE(0xFFDCEAD6, 0xFF4E7A4B, 0xFF263623, 0xFFA8CFA3),
    DUSK(0xFFDCE5EF, 0xFF46698C, 0xFF1F2A36, 0xFFA3BEDA),
    PLUM(0xFFEBDDEA, 0xFF7E5481, 0xFF332434, 0xFFD2AFD2),
    SAND(0xFFEDE4D6, 0xFF7A6A58, 0xFF2E2620, 0xFFCDBDA9),
    ;

    val container: Color
        @Composable @ReadOnlyComposable
        get() = Color(if (MaterialTheme.colorScheme.isLight()) lightContainer else darkContainer)

    val content: Color
        @Composable @ReadOnlyComposable
        get() = Color(if (MaterialTheme.colorScheme.isLight()) lightContent else darkContent)
}

private fun ColorScheme.isLight(): Boolean = surface.luminance() > 0.5f
