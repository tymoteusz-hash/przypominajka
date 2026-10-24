package pl.przypominajka.util

import androidx.annotation.RawRes
import pl.przypominajka.R

/**
 * Dźwięki dzwonów (syntezowane, pliki w res/raw). Używane jako dźwięk przejść w aplikacji
 * i do wyboru jako dźwięk powiadomień o przypomnieniach.
 */
enum class BellTone(val key: String, val label: String, @RawRes val raw: Int?, val forTransitions: Boolean) {
    BELL_MELODY("dzwon_melodia", "Dzwon – melodia", R.raw.dzwon_melodia, forTransitions = false),
    BELL("dzwon", "Dzwon", R.raw.dzwon, forTransitions = true),
    DING("ding", "Dzwonek", R.raw.dzwonek, forTransitions = true),
    CRYSTAL("crystal", "Kryształ", R.raw.krysztal, forTransitions = true),
    BICYCLE("bicycle", "Rowerowy", R.raw.rowerowy, forTransitions = true),
    GONG("gong", "Gong", R.raw.gong, forTransitions = false),
    SYSTEM("system", "Dźwięk telefonu", null, forTransitions = false),
    SILENT("silent", "Bez dźwięku (tylko wibracja)", null, forTransitions = false),
    ;

    companion object {
        val DEFAULT_TRANSITION = BELL
        val DEFAULT_NOTIFICATION = BELL_MELODY

        fun fromKey(key: String?, default: BellTone): BellTone = entries.firstOrNull { it.key == key } ?: default
    }
}
