package pl.przypominajka.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/** Cienka warstwa nad AlarmManagerem: jeden alarm (PendingIntent) na przypomnienie. */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Android 12+ wymaga zgody na dokładne alarmy; niżej są zawsze dostępne. */
    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(reminderId: Long, triggerAtMillis: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            alarmIntent(reminderId)
                .putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
                .putExtra(AlarmReceiver.EXTRA_TRIGGER_AT, triggerAtMillis),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Ten sam PendingIntent => AlarmManager zastępuje poprzedni alarm tego przypomnienia.
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

    fun cancel(reminderId: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            alarmIntent(reminderId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /** Unikalne data URI sprawia, że PendingIntenty różnych przypomnień nigdy się nie mylą. */
    private fun alarmIntent(reminderId: Long): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            data = Uri.parse("przypominajka://reminder/$reminderId")
        }
}
