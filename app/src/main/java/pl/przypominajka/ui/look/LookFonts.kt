@file:OptIn(ExperimentalTextApi::class)

package pl.przypominajka.ui.look

import androidx.annotation.FontRes
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import pl.przypominajka.R

/**
 * Fonty wszystkich wyglądów – jedyne miejsce, które wie, z jakich plików korzystamy.
 * Pliki w res/font to fonty zmienne (oś wght) na licencji OFL (teksty licencji: assets/licenses).
 * Każda grubość to ten sam plik z inną wartością osi wght.
 */
object LookFonts {

    /** Kartka – Inter Tight (nagłówki i tekst). */
    val KartkaBody: FontFamily = variableFamily(R.font.inter_tight, 400, 500, 600, 700)
    val KartkaHeading: FontFamily = KartkaBody

    /** Dzwon – Fraunces (nagłówki, szeryfowy). */
    val DzwonHeading: FontFamily = variableFamily(R.font.fraunces, 400, 500, 600, 700)

    /** Dzwon – Manrope (tekst). */
    val DzwonBody: FontFamily = variableFamily(R.font.manrope, 400, 500, 600, 700, 800)

    /** Tablica – Caveat (odręczne nagłówki i karteczki; font ma tylko 400–700). */
    val TablicaHeading: FontFamily = variableFamily(R.font.caveat, 400, 500, 600, 700)

    /** Tablica – Nunito (tekst). */
    val TablicaBody: FontFamily = variableFamily(R.font.nunito, 400, 500, 600, 700, 800, 900)

    /** Rodzina z jednego pliku zmiennego: każda grubość ustawia oś wght. */
    private fun variableFamily(@FontRes res: Int, vararg weights: Int): FontFamily =
        FontFamily(
            weights.map { weight ->
                Font(
                    resId = res,
                    weight = FontWeight(weight),
                    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
                )
            },
        )
}
