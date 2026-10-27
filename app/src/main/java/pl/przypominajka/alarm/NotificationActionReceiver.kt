package pl.przypominajka.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.przypominajka.appContainer

/** Przyciski przypomnienia i osobny alarm próbny. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TEST) {
            context.appContainer.notificationHelper.showTestNotification()
            return
        }
        if (intent.action !in setOf(ACTION_DONE, ACTION_SNOOZE, ACTION_SNOOZE_HOUR, ACTION_SNOOZE_TOMORROW)) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val container = context.appContainer
        container.notificationHelper.cancel(reminderId)

        when (intent.action) {
            ACTION_DONE -> launchAsync { container.repository.markDone(reminderId) }
            ACTION_SNOOZE -> launchAsync { container.repository.snooze(reminderId, SNOOZE_MINUTES) }
            ACTION_SNOOZE_HOUR -> launchAsync { container.repository.snooze(reminderId, SNOOZE_HOUR_MINUTES) }
            ACTION_SNOOZE_TOMORROW -> launchAsync { container.repository.snoozeTomorrow(reminderId) }
        }
    }

    companion object {
        const val ACTION_DONE = "pl.przypominajka.action.DONE"
        const val ACTION_SNOOZE = "pl.przypominajka.action.SNOOZE"
        const val ACTION_SNOOZE_HOUR = "pl.przypominajka.action.SNOOZE_HOUR"
        const val ACTION_SNOOZE_TOMORROW = "pl.przypominajka.action.SNOOZE_TOMORROW"
        const val ACTION_TEST = "pl.przypominajka.action.TEST_NOTIFICATION"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val SNOOZE_MINUTES = 10L
        const val SNOOZE_HOUR_MINUTES = 60L
    }
}
