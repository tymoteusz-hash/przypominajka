package pl.przypominajka.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import pl.przypominajka.ui.look.Look

/**
 * Ikona aplikacji pasująca do wyglądu. W manifeście są trzy aliasy MainActivity z własnymi ikonami
 * (.LauncherKartka, .LauncherDzwon, .LauncherTablica) – włączony jest zawsze dokładnie jeden.
 * Domyślnie (przed wyborem) włączony jest Dzwon.
 *
 * Uwaga: launcher Samsunga (One UI) po zmianie potrafi usunąć ikonę z ekranu głównego – zostaje
 * w szufladzie aplikacji i trzeba ją przeciągnąć na ekran jeszcze raz. Sama aplikacja działa dalej.
 */
object AppIconSwitcher {

    private const val ALIAS_KARTKA = "pl.przypominajka.LauncherKartka"
    private const val ALIAS_DZWON = "pl.przypominajka.LauncherDzwon"
    private const val ALIAS_TABLICA = "pl.przypominajka.LauncherTablica"

    /** Alias włączony w manifeście (android:enabled="true"), gdy nikt jeszcze niczego nie zmieniał. */
    private const val DEFAULT_ALIAS = ALIAS_DZWON

    private fun aliasOf(look: Look): String = when (look) {
        Look.KARTKA -> ALIAS_KARTKA
        Look.DZWON -> ALIAS_DZWON
        Look.TABLICA -> ALIAS_TABLICA
    }

    /** Włącza alias wybranego wyglądu i wyłącza pozostałe; nic nie robi, gdy ikona już się zgadza. */
    fun apply(context: Context, look: Look) {
        val pm = context.packageManager
        val wanted = aliasOf(look)
        // Najpierw włączamy nowy alias, potem wyłączamy stare – żeby ani na chwilę nie zniknęła ikona z listy aplikacji.
        val ordered = listOf(wanted) + listOf(ALIAS_KARTKA, ALIAS_DZWON, ALIAS_TABLICA).filter { it != wanted }
        ordered.forEach { alias ->
            val component = ComponentName(context.packageName, alias)
            val enable = alias == wanted
            if (isEnabled(pm, component, alias) == enable) return@forEach
            pm.setComponentEnabledSetting(
                component,
                if (enable) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    private fun isEnabled(pm: PackageManager, component: ComponentName, alias: String): Boolean =
        when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> alias == DEFAULT_ALIAS
            else -> false
        }
}
