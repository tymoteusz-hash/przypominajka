package pl.przypominajka.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/** Alarmy checklisty: jeden alarm (PendingIntent) na punkt z godziną przypomnienia. */
class ChecklistAlarms(private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Android 12+ wymaga zgody na dokładne alarmy; niżej są zawsze dostępne. */
    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(itemId: Long, triggerAtMillis: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            alarmIntent(itemId).putExtra(ChecklistAlarmReceiver.EXTRA_ITEM_ID, itemId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Ten sam PendingIntent => AlarmManager zastępuje poprzedni alarm tego punktu.
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            } catch (e: SecurityException) {
                // Zgoda cofnięta w międzyczasie – używamy alarmu niedokładnego poniżej.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    fun cancel(itemId: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            alarmIntent(itemId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /** Unikalne data URI sprawia, że PendingIntenty różnych punktów nigdy się nie mylą. */
    private fun alarmIntent(itemId: Long): Intent =
        Intent(context, ChecklistAlarmReceiver::class.java).apply {
            action = ChecklistAlarmReceiver.ACTION_FIRE
            data = Uri.parse("przypominajka://checklist/$itemId")
        }
}
