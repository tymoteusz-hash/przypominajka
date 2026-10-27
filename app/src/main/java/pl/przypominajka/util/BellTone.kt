package pl.przypominajka.util

import androidx.annotation.RawRes
import pl.przypominajka.R

/**
 * Dźwięki do wyboru jako dźwięk powiadomień o przypomnieniach (syntezowane dzwony w res/raw,
 * dźwięk telefonu albo cisza). Każdy ma własny kanał powiadomień (zob. NotificationHelper).
 */
enum class BellTone(val key: String, val label: String, @RawRes val raw: Int?) {
    BELL_MELODY("dzwon_melodia", "Dzwon – melodia", R.raw.dzwon_melodia),
    BELL("dzwon", "Dzwon", R.raw.dzwon),
    DING("ding", "Dzwonek", R.raw.dzwonek),
    CRYSTAL("crystal", "Kryształ", R.raw.krysztal),
    BICYCLE("bicycle", "Rowerowy", R.raw.rowerowy),
    GONG("gong", "Gong", R.raw.gong),
    SYSTEM("system", "Dźwięk telefonu", null),
    SILENT("silent", "Bez dźwięku (tylko wibracja)", null),
    ;

    companion object {
        val DEFAULT_NOTIFICATION = BELL_MELODY

        fun fromKey(key: String?, default: BellTone): BellTone = entries.firstOrNull { it.key == key } ?: default
    }
}
