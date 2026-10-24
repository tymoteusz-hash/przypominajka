package pl.przypominajka.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.przypominajka.appContainer

/** Wywoływany przez AlarmManager: pokazuje powiadomienie i planuje następny alarm. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis())

        val repository = context.appContainer.repository
        launchAsync { repository.onAlarmFired(reminderId, triggerAt) }
    }

    companion object {
        const val ACTION_FIRE = "pl.przypominajka.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
