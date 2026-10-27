package pl.przypominajka.ui.dzwon.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ikony Dzwonu – kreskowe, przepisane z makiety (siatka 24×24, zaokrąglone końce).
 * Kolor nadaje `Icon(tint = …)`; dla pozostałych potrzeb można użyć `Icons.Rounded.*`.
 */
object DzwonIcons {
    /** Konto (głowa i ramiona). */
    val Person: ImageVector by lazy { stroke("person", 1.9f, circle(12f, 8f, 4f) + "M4 21c0-4 3.6-6 8-6s8 2 8 6") }

    /** Ustawienia (suwaki). */
    val Sliders: ImageVector by lazy {
        stroke(
            "sliders", 1.9f,
            "M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1" +
                circle(15f, 6f, 2f) + circle(9f, 12f, 2f) + circle(17f, 18f, 2f),
        )
    }

    /** „Zwiń” – strzałka w dół. */
    val ChevronDown: ImageVector by lazy { stroke("chevronDown", 2f, "M6 9l6 6 6-6") }

    /** Strzałka w prawo (wiersz prowadzący dalej). */
    val ChevronRight: ImageVector by lazy { stroke("chevronRight", 2f, "M9 6l6 6-6 6") }

    /** Ptaszek. */
    val Check: ImageVector by lazy { stroke("check", 3f, "M5 12.5l4.5 4.5L19 7.5") }

    /** Zegar (godzina przypomnienia). */
    val Clock: ImageVector by lazy { stroke("clock", 1.9f, circle(12f, 12f, 8.5f) + "M12 7.5V12l3 2") }

    /** Plus (dodawanie). */
    val Plus: ImageVector by lazy { stroke("plus", 2.4f, "M12 5v14M5 12h14") }

    /** Kosz. */
    val Trash: ImageVector by lazy { stroke("trash", 2f, "M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13") }

    /** Krzyżyk (zamknij / usuń godzinę). */
    val Close: ImageVector by lazy { stroke("close", 2f, "M6 6l12 12M18 6L6 18") }

    /** Kalendarz. */
    val Calendar: ImageVector by lazy {
        stroke(
            "calendar", 1.9f,
            "M7 5h10a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2zM5 10h14M9 3v4M15 3v4",
        )
    }

    /** Ołówek (edycja). */
    val Pencil: ImageVector by lazy { stroke("pencil", 1.9f, "M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4") }

    /** Klawiatura (wpisanie godziny zamiast tarczy). */
    val Keyboard: ImageVector by lazy {
        stroke(
            "keyboard", 1.9f,
            "M5 6h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2zM7 10h.01M11 10h.01M15 10h.01M8 14h8",
        )
    }

    /** Uchwyt przeciągania – sześć kropek. */
    val DragDots: ImageVector by lazy {
        fill(
            "dragDots",
            circle(9f, 6f, 1.6f) + circle(15f, 6f, 1.6f) + circle(9f, 12f, 1.6f) +
                circle(15f, 12f, 1.6f) + circle(9f, 18f, 1.6f) + circle(15f, 18f, 1.6f),
        )
    }

    /** Okrąg jako ścieżka SVG (dwa łuki). */
    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun stroke(name: String, width: Float, path: String): ImageVector =
        ImageVector.Builder(
            name = "Dzwon.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    private fun fill(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = "Dzwon.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(path),
            fill = SolidColor(Color.Black),
        ).build()
}
