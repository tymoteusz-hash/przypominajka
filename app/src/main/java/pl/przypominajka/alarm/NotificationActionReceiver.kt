package pl.przypominajka.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.przypominajka.appContainer

/** Obsługa przycisków w powiadomieniu: "Zrobione", "Za 10 min" i "Za godzinę". */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val container = context.appContainer
        container.notificationHelper.cancel(reminderId)

        when (intent.action) {
            ACTION_DONE -> launchAsync { container.repository.markDone(reminderId) }
            ACTION_SNOOZE -> launchAsync { container.repository.snooze(reminderId, SNOOZE_MINUTES) }
            ACTION_SNOOZE_HOUR -> launchAsync { container.repository.snooze(reminderId, SNOOZE_HOUR_MINUTES) }
        }
    }

    companion object {
        const val ACTION_DONE = "pl.przypominajka.action.DONE"
        const val ACTION_SNOOZE = "pl.przypominajka.action.SNOOZE"
        const val ACTION_SNOOZE_HOUR = "pl.przypominajka.action.SNOOZE_HOUR"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val SNOOZE_MINUTES = 10L
        const val SNOOZE_HOUR_MINUTES = 60L
    }
}
