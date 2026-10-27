package pl.przypominajka.ui.kartka.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ikony Kartki – cienkie, zaokrąglone kreski jak w makietach (siatka 24×24). Kolor nadaje `Icon(tint = …)`.
 * Ścieżki przepisane z makiet; koła i prostokąty zamienione na łuki.
 */
object KartkaIcons {
    /** Strzałka „Wróć”. */
    val Back: ImageVector by lazy { icon("Back") { stroke("M15 5L8 12L15 19", 2f) } }

    /** Strzałka w prawo (np. „4 do zrobienia ›”). */
    val ChevronRight: ImageVector by lazy { icon("ChevronRight") { stroke("M9 5L16 12L9 19", 2f) } }

    /** Strzałka w dół (rozwijanie). */
    val ChevronDown: ImageVector by lazy { icon("ChevronDown") { stroke("M5 9L12 16L19 9", 2f) } }

    /** Konto (postać). */
    val Account: ImageVector by lazy {
        icon("Account") {
            stroke(circle(12f, 8f, 4f), 1.8f)
            stroke("M4 21C4 17 7.6 15 12 15S20 17 20 21", 1.8f)
        }
    }

    /** Ustawienia (suwaki). */
    val Settings: ImageVector by lazy {
        icon("Settings") {
            stroke("M4 6H13M17 6H20M4 12H7M11 12H20M4 18H15M19 18H20", 1.8f)
            stroke(circle(15f, 6f, 2f), 1.8f)
            stroke(circle(9f, 12f, 2f), 1.8f)
            stroke(circle(17f, 18f, 2f), 1.8f)
        }
    }

    /** Przypomnienia (budzik). */
    val Reminders: ImageVector by lazy {
        icon("Reminders") {
            stroke(circle(12f, 13f, 7f), 1.8f)
            stroke("M12 9.5V13L14.5 15M5 4.5L2.8 6.7M19 4.5L21.2 6.7", 1.8f)
        }
    }

    /** Plan / kalendarz. */
    val Plan: ImageVector by lazy {
        icon("Plan") {
            stroke(roundRect(3.5f, 5f, 17f, 15f, 3f), 1.8f)
            stroke("M3.5 10H20.5M8 3V7M16 3V7", 1.8f)
        }
    }

    /** Kalendarz (to samo co [Plan]) – do wyboru daty. */
    val Calendar: ImageVector get() = Plan

    /** Trening (hantla). */
    val Workout: ImageVector by lazy {
        icon("Workout") { stroke("M6.5 6.5V17.5M17.5 6.5V17.5M3.5 9V15M20.5 9V15M6.5 12H17.5", 1.8f) }
    }

    /** Quizy (karty ze znakiem zapytania). */
    val Quiz: ImageVector by lazy {
        icon("Quiz") {
            stroke(roundRect(4f, 6.5f, 12.5f, 14f, 2.5f), 1.8f)
            stroke("M8 3.5H17.5A2.5 2.5 0 0 1 20 6V17", 1.8f)
            stroke(
                "M8.5 11.5A1.8 1.8 0 1 1 11.1 13.1C10.5 13.4 10.2 13.8 10.2 14.4M10.2 17H10.21",
                1.8f,
            )
        }
    }

    /** Zegar (godzina przypomnienia). */
    val Clock: ImageVector by lazy {
        icon("Clock") {
            stroke(circle(12f, 12f, 8.5f), 2f)
            stroke("M12 7.5V12L15 14", 2f)
        }
    }

    /** Kosz. */
    val Trash: ImageVector by lazy { icon("Trash") { stroke("M4 7H20M9 7V4.5H15V7M6.5 7L7.5 20H16.5L17.5 7", 2f) } }

    /** Plus (dodaj). */
    val Plus: ImageVector by lazy { icon("Plus") { stroke("M12 5V19M5 12H19", 2.2f) } }

    /** Ptaszek w polu wyboru (gruby). */
    val Check: ImageVector by lazy { icon("Check") { stroke("M5 12.5L9.5 17L19 7.5", 3.2f) } }

    /** Ptaszek cienki (np. zaznaczona opcja na liście). */
    val CheckThin: ImageVector by lazy { icon("CheckThin") { stroke("M5 12.5L9.5 17L19 7.5", 2f) } }

    /** Zamknij / wyczyść. */
    val Close: ImageVector by lazy { icon("Close") { stroke("M6 6L18 18M18 6L6 18", 2f) } }

    /** Uchwyt do przeciągania (6 kropek). */
    val DragHandle: ImageVector by lazy {
        icon("DragHandle") {
            for (y in listOf(6f, 12f, 18f)) {
                fill(circle(9f, y, 1.6f))
                fill(circle(15f, y, 1.6f))
            }
        }
    }

    /** Więcej (3 kropki pionowo). */
    val More: ImageVector by lazy {
        icon("More") { for (y in listOf(5f, 12f, 19f)) fill(circle(12f, y, 1.6f)) }
    }

    /** Ołówek (edycja). */
    val Edit: ImageVector by lazy {
        icon("Edit") { stroke("M4 20L5 15.5L15.5 5A2.1 2.1 0 0 1 19 8.5L8.5 19L4 20ZM13.5 7L17 10.5", 1.8f) }
    }

    /** Dzwonek (powiadomienia). */
    val Bell: ImageVector by lazy {
        icon("Bell") {
            stroke("M6 16.5V11A6 6 0 0 1 18 11V16.5L19.5 18.5H4.5L6 16.5Z", 1.8f)
            stroke("M10 21.5H14", 1.8f)
        }
    }

    /** Informacja / ostrzeżenie (kółko z wykrzyknikiem). */
    val Info: ImageVector by lazy {
        icon("Info") {
            stroke(circle(12f, 12f, 9f), 1.8f)
            stroke("M12 7.5V13M12 16.5V16.51", 2f)
        }
    }

    /** Folder. */
    val Folder: ImageVector by lazy {
        icon("Folder") {
            stroke("M3.5 7A2 2 0 0 1 5.5 5H9.5L11.5 7.5H18.5A2 2 0 0 1 20.5 9.5V17A2 2 0 0 1 18.5 19H5.5A2 2 0 0 1 3.5 17Z", 1.8f)
        }
    }

    /** Odtwórz (np. rozpocznij quiz). */
    val Play: ImageVector by lazy { icon("Play") { stroke("M8 5.5V18.5L18.5 12Z", 1.8f) } }

    /** Otwórz na zewnątrz (strona, ustawienia systemu). */
    val External: ImageVector by lazy {
        icon("External") {
            stroke("M14 4H20V10M20 4L11 13", 1.8f)
            stroke("M18 14V18A2 2 0 0 1 16 20H6A2 2 0 0 1 4 18V8A2 2 0 0 1 6 6H10", 1.8f)
        }
    }

    /** Odśwież / synchronizuj. */
    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            stroke("M19.5 12A7.5 7.5 0 1 1 17.3 6.7", 1.8f)
            stroke("M19.5 4V8.5H15", 1.8f)
        }
    }

    // ------------------------------------------------------------------------- budowanie

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    private fun ImageVector.Builder.stroke(pathData: String, width: Float) {
        addPath(
            pathData = addPathNodes(pathData),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    private fun ImageVector.Builder.fill(pathData: String) {
        addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
    }

    /** Koło jako dwa łuki (ścieżki SVG nie mają „circle”). */
    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}A$r $r 0 1 0 ${cx + r} ${cy}A$r $r 0 1 0 ${cx - r} ${cy}Z"

    /** Prostokąt o zaokrąglonych rogach (jak `<rect rx>` w SVG). */
    private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val right = x + w
        val bottom = y + h
        return "M${x + r} ${y}H${right - r}A$r $r 0 0 1 $right ${y + r}" +
            "V${bottom - r}A$r $r 0 0 1 ${right - r} ${bottom}" +
            "H${x + r}A$r $r 0 0 1 $x ${bottom - r}" +
            "V${y + r}A$r $r 0 0 1 ${x + r} ${y}Z"
    }
}
