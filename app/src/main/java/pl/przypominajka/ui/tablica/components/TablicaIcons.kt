package pl.przypominajka.ui.tablica.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ikony Tablicy przepisane z makiety (kreska 2–2,6 w siatce 24×24, zaokrąglone końce).
 * Kolor nadaje `Icon(tint = …)`. Pozostałe ikony – z `Icons.Rounded`.
 */
object TablicaIcons {
    /** Zakładka „Tablica” – cztery karteczki. */
    val Board: ImageVector by lazy {
        strokeIcon(
            "Board",
            rr(3.5f, 3.5f, 7.5f, 9f, 1.5f) + rr(13f, 3.5f, 7.5f, 5.5f, 1.5f) +
                rr(3.5f, 14.5f, 7.5f, 6f, 1.5f) + rr(13f, 11f, 7.5f, 9.5f, 1.5f),
        )
    }

    /** Zakładka „Lista” – trzy odhaczone punkty. */
    val Checklist: ImageVector by lazy {
        strokeIcon(
            "Checklist",
            "M4 6.5l1.8 1.8L9 5M4 12.5l1.8 1.8L9 11M4 18.5l1.8 1.8L9 17M12.5 7h7.5M12.5 13h7.5M12.5 19h7.5",
        )
    }

    /** Zakładka „Plan” – kalendarz. */
    val Calendar: ImageVector by lazy { strokeIcon("Calendar", rr(3.5f, 5f, 17f, 15f, 3f) + "M3.5 10h17M8 3v4M16 3v4") }

    /** Zakładka „Więcej” – trzy kropki. */
    val More: ImageVector by lazy { fillIcon("More", circle(5f, 12f, 2f) + circle(12f, 12f, 2f) + circle(19f, 12f, 2f)) }

    /** Plus (dodawanie), gruba kreska. */
    val Plus: ImageVector by lazy { strokeIcon("Plus", "M12 5v14M5 12h14", 2.6f) }

    /** Konto – głowa i ramiona. */
    val Account: ImageVector by lazy { strokeIcon("Account", circle(12f, 8f, 4f) + "M4 21c0-4 3.6-6 8-6s8 2 8 6", 2.1f) }

    /** Zegar (godzina przypomnienia). */
    val Clock: ImageVector by lazy { strokeIcon("Clock", circle(12f, 12f, 8.5f) + "M12 7.5V12l3 2", 2.2f) }

    /** Ptaszek. */
    val Check: ImageVector by lazy { strokeIcon("Check", "M5 12.5l4.5 4.5L19 7.5", 3f) }

    /** Sześć kropek – „karteczkę można przenieść”. */
    val Grip: ImageVector by lazy {
        fillIcon(
            "Grip",
            circle(9f, 6f, 1.7f) + circle(15f, 6f, 1.7f) + circle(9f, 12f, 1.7f) +
                circle(15f, 12f, 1.7f) + circle(9f, 18f, 1.7f) + circle(15f, 18f, 1.7f),
        )
    }

    /** Powrót (strzałka w lewo). */
    val Back: ImageVector by lazy { strokeIcon("Back", "M19 12H5.5M11.5 5.5L5 12l6.5 6.5", 2.3f) }

    /** Zamknięcie (krzyżyk). */
    val Close: ImageVector by lazy { strokeIcon("Close", "M6.5 6.5l11 11M17.5 6.5l-11 11", 2.3f) }

    private fun strokeIcon(name: String, path: String, width: Float = 2f): ImageVector =
        ImageVector.Builder(
            name = "Tablica.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    private fun fillIcon(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = "Tablica.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            fill = SolidColor(Color.Black),
        ).build()

    /** Zaokrąglony prostokąt jako ścieżka SVG (jak <rect rx>). */
    private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float): String =
        "M${x + r} ${y}H${x + w - r}A$r $r 0 0 1 ${x + w} ${y + r}V${y + h - r}" +
            "A$r $r 0 0 1 ${x + w - r} ${y + h}H${x + r}A$r $r 0 0 1 $x ${y + h - r}V${y + r}" +
            "A$r $r 0 0 1 ${x + r} ${y}Z"

    /** Okrąg jako ścieżka SVG (jak <circle>). */
    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}A$r $r 0 1 0 ${cx + r} ${cy}A$r $r 0 1 0 ${cx - r} ${cy}Z"
}
