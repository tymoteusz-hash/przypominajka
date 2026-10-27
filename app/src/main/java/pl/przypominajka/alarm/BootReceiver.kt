package pl.przypominajka.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.przypominajka.appContainer

/**
 * Alarmy AlarmManagera nie przetrwają restartu telefonu – tutaj planujemy je od nowa.
 * Reagujemy też na aktualizację aplikacji, zmianę czasu/strefy oraz przyznanie zgody
 * na dokładne alarmy (wtedy zamieniamy alarmy niedokładne na dokładne).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_QUICKBOOT_POWERON,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> {
                val container = context.appContainer
                launchAsync {
                    container.repository.rescheduleAll()
                    container.checklistRepository.rescheduleAll()
                }
            }
        }
    }

    private companion object {
        /** Szybki restart na niektórych telefonach (HTC, Xiaomi itp.). */
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
