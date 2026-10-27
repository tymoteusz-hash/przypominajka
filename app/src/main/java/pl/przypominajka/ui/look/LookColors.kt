package pl.przypominajka.ui.look

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =====================================================================================================
// KOLORY WYGLĄDÓW 4.0 – wartości przepisane wprost z zatwierdzonych makiet (renderVals() w plikach
// A_*, B_*, C_*.dc.html). Każdy wygląd ma wersję jasną i ciemną; ekrany biorą je z Local*Colors.
// =====================================================================================================

/** Kartka: biel/grafit i jeden kobaltowy akcent. */
@Immutable
data class KartkaColors(
    val bg: Color,
    /** Szare pola (karty, pole dodawania). */
    val surface: Color,
    /** Uniesiony wiersz przy przeciąganiu, okna. */
    val raised: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    /** „Cofnij” na ciemnym (jasnym w ciemnym motywie) pasku. */
    val toastAction: Color,
    /** Cień uniesionego wiersza. */
    val shadow: Color,
) {
    companion object {
        val Light = KartkaColors(
            bg = Color(0xFFFFFFFF),
            surface = Color(0xFFF3F4F7),
            raised = Color(0xFFFFFFFF),
            line = Color(0xFFE4E6EB),
            ink = Color(0xFF111318),
            ink2 = Color(0xFF5B616E),
            accent = Color(0xFF2B59FF),
            accentSoft = Color(0xFFE8EDFF),
            onAccent = Color(0xFFFFFFFF),
            toastAction = Color(0xFFA9BCFF),
            shadow = Color(0xFF111318).copy(alpha = 0.16f),
        )
        val Dark = KartkaColors(
            bg = Color(0xFF0F1115),
            surface = Color(0xFF191C22),
            raised = Color(0xFF232730),
            line = Color(0xFF2A2E37),
            ink = Color(0xFFF2F3F5),
            ink2 = Color(0xFFA3A9B6),
            accent = Color(0xFF8EA8FF),
            accentSoft = Color(0xFF1E2740),
            onAccent = Color(0xFF0F1115),
            toastAction = Color(0xFF2B59FF),
            shadow = Color.Black.copy(alpha = 0.55f),
        )

        fun of(dark: Boolean): KartkaColors = if (dark) Dark else Light
    }
}

/** Dzwon: granat nocy i złoto (domyślnie ciemny, ale ma też wersję jasną). */
@Immutable
data class DzwonColors(
    val bg: Color,
    /** Karty i okrągłe przyciski na tle. */
    val surface: Color,
    /** Arkusz wysuwany od dołu (ekrany działów). */
    val sheet: Color,
    /** Pola tekstowe. */
    val field: Color,
    /** Uniesiony wiersz przy przeciąganiu. */
    val lifted: Color,
    /** Poświata za dzwonem. */
    val halo: Color,
    /** Tor pierścienia postępu i uchwyt arkusza. */
    val track: Color,
    val line: Color,
    /** Obwódka przycisków-działów na pierścieniu. */
    val chipLine: Color,
    val ink: Color,
    val ink2: Color,
    val gold: Color,
    /** Krawędź i serce dzwonu. */
    val goldDeep: Color,
    /** Złoto jako tekst (w jasnym motywie przyciemnione dla czytelności). */
    val goldText: Color,
    val onGold: Color,
    val toastBg: Color,
    val toastInk: Color,
    val shadow: Color,
) {
    companion object {
        val Light = DzwonColors(
            bg = Color(0xFFF1F0F7),
            surface = Color(0xFFFFFFFF),
            sheet = Color(0xFFFFFFFF),
            field = Color(0xFFF1F0F7),
            lifted = Color(0xFFFFFFFF),
            halo = Color(0xFFE8E6F3),
            track = Color(0xFFDAD8EA),
            line = Color(0xFFE7E5F0),
            chipLine = Color(0xFFDAD8EA),
            ink = Color(0xFF1A1830),
            ink2 = Color(0xFF5D5A78),
            gold = Color(0xFFEBB635),
            goldDeep = Color(0xFFB9871A),
            goldText = Color(0xFF8A6400),
            onGold = Color(0xFF1A1530),
            toastBg = Color(0xFF1A1830),
            toastInk = Color(0xFFF5F3FF),
            shadow = Color(0xFF1A1830).copy(alpha = 0.18f),
        )
        val Dark = DzwonColors(
            bg = Color(0xFF12112A),
            surface = Color(0xFF1E1C3D),
            sheet = Color(0xFF1A1936),
            field = Color(0xFF25234A),
            lifted = Color(0xFF2A2852),
            halo = Color(0xFF191837),
            // Makiety: tor #2C2A52 (start) / #34325C (checklista, pulpit) – bierzemy częstszy.
            track = Color(0xFF34325C),
            line = Color(0xFF2F2D55),
            chipLine = Color(0xFF34325C),
            ink = Color(0xFFF5F3FF),
            ink2 = Color(0xFFB3B0CF),
            gold = Color(0xFFF5C64B),
            goldDeep = Color(0xFFC9971F),
            goldText = Color(0xFFF5C64B),
            onGold = Color(0xFF1A1530),
            toastBg = Color(0xFFF5F3FF),
            toastInk = Color(0xFF1A1830),
            shadow = Color.Black.copy(alpha = 0.5f),
        )

        fun of(dark: Boolean): DzwonColors = if (dark) Dark else Light
    }
}

/** Kolor karteczki: papier, tekst i linie (np. linia pola wpisywania). */
@Immutable
data class NoteColor(
    val bg: Color,
    val ink: Color,
    val rule: Color,
)

/** Tablica: karteczki na tablicy w kropki, ciemny pasek zakładek na dole. */
@Immutable
data class TablicaColors(
    val board: Color,
    /** Kropki na tablicy. */
    val dot: Color,
    val ink: Color,
    val ink2: Color,
    /** Taśma klejąca na karteczkach (półprzezroczysta). */
    val tape: Color,
    /** Pinezka. */
    val pin: Color,
    val noteShadow: Color,
    /** Cień karteczki podniesionej przy przeciąganiu. */
    val liftShadow: Color,
    val dock: Color,
    val dockInk: Color,
    val dockText: Color,
    val dockActive: Color,
    val yellow: NoteColor,
    val pink: NoteColor,
    val mint: NoteColor,
    val blue: NoteColor,
    val lilac: NoteColor,
) {
    /** Kolejność kolorów karteczek na liście (jak w makiecie checklisty): żółta, różowa, miętowa, niebieska, liliowa. */
    val notes: List<NoteColor> = listOf(yellow, pink, mint, blue, lilac)

    /** Kolor karteczki dla n-tego elementu listy (kolory się powtarzają). */
    fun note(index: Int): NoteColor = notes[Math.floorMod(index, notes.size)]

    companion object {
        private val LightInk = Color(0xFF1F2328)

        val Light = TablicaColors(
            board = Color(0xFFE6EAF0),
            dot = Color(0xFFC9D0DA),
            ink = LightInk,
            ink2 = Color(0xFF555D69),
            tape = Color.White.copy(alpha = 0.6f),
            pin = Color(0xFFE8604C),
            noteShadow = LightInk.copy(alpha = 0.14f),
            liftShadow = LightInk.copy(alpha = 0.28f),
            dock = Color(0xFF1F2328),
            dockInk = Color(0xFFAEB5BF),
            dockText = Color(0xFFF4F5F7),
            dockActive = Color(0xFFFFE68A),
            yellow = NoteColor(Color(0xFFFFE68A), LightInk, LightInk.copy(alpha = 0.2f)),
            pink = NoteColor(Color(0xFFFFC9DA), LightInk, LightInk.copy(alpha = 0.2f)),
            mint = NoteColor(Color(0xFFBFEBD4), LightInk, LightInk.copy(alpha = 0.2f)),
            blue = NoteColor(Color(0xFFC7DCFF), LightInk, LightInk.copy(alpha = 0.2f)),
            lilac = NoteColor(Color(0xFFDCCFFF), LightInk, LightInk.copy(alpha = 0.2f)),
        )
        val Dark = TablicaColors(
            board = Color(0xFF1B1D22),
            dot = Color(0xFF2C3038),
            ink = Color(0xFFEDEFF2),
            ink2 = Color(0xFFA9B0BB),
            tape = Color.White.copy(alpha = 0.18f),
            pin = Color(0xFFE8604C),
            noteShadow = Color.Black.copy(alpha = 0.45f),
            liftShadow = Color.Black.copy(alpha = 0.6f),
            dock = Color(0xFF2A2E36),
            dockInk = Color(0xFFA9B0BB),
            dockText = Color(0xFFEDEFF2),
            dockActive = Color(0xFFFFE68A),
            yellow = darkNote(0xFF4D4220, 0xFFFFF1C2),
            pink = darkNote(0xFF4D2633, 0xFFFFD9E6),
            mint = darkNote(0xFF1E4233, 0xFFD2F5E2),
            blue = darkNote(0xFF1F3352, 0xFFD9E8FF),
            lilac = darkNote(0xFF352B52, 0xFFE8DFFF),
        )

        fun of(dark: Boolean): TablicaColors = if (dark) Dark else Light

        private fun darkNote(bg: Long, ink: Long): NoteColor =
            NoteColor(Color(bg), Color(ink), Color(ink).copy(alpha = 0.25f))
    }
}

/** Kolory Kartki – dostarczane przez [LookTheme] (zawsze, niezależnie od wybranego wyglądu). */
val LocalKartkaColors = staticCompositionLocalOf { KartkaColors.Light }

/** Kolory Dzwonu – dostarczane przez [LookTheme]. */
val LocalDzwonColors = staticCompositionLocalOf { DzwonColors.Dark }

/** Kolory Tablicy – dostarczane przez [LookTheme]. */
val LocalTablicaColors = staticCompositionLocalOf { TablicaColors.Light }
