package pl.przypominajka.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pl.przypominajka.MainActivity
import pl.przypominajka.R
import pl.przypominajka.data.Reminder
import pl.przypominajka.data.SchoolType
import pl.przypominajka.util.AppearancePrefs
import pl.przypominajka.util.BellTone
import android.media.AudioAttributes

class NotificationHelper(
    private val context: Context,
    private val appearancePrefs: AppearancePrefs,
) {

    private val manager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    /** Tworzy kanały powiadomień (wywoływane przy starcie aplikacji; wielokrotne wywołanie jest bezpieczne). */
    fun createChannel() {
        ensureReminderChannel(appearancePrefs.notificationTone)
        val workout = NotificationChannel(
            WORKOUT_CHANNEL_ID,
            "Przerwa w treningu",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Sygnał końca przerwy między seriami"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        manager.createNotificationChannel(workout)
    }

    /**
     * Kanał przypomnień z wybranym dźwiękiem. Dźwięk kanału można ustawić tylko przy jego tworzeniu,
     * dlatego każdy dźwięk ma własny kanał ("reminders_<dźwięk>"), a pozostałe są usuwane.
     */
    fun ensureReminderChannel(tone: BellTone) {
        val id = reminderChannelId(tone)
        if (manager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(id, "Przypomnienia", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Powiadomienia o zaplanowanych przypomnieniach – dźwięk: ${tone.label}"
                enableVibration(true)
                val raw = tone.raw
                when {
                    tone == BellTone.SILENT -> setSound(null, null)
                    raw != null -> setSound(
                        Uri.parse("android.resource://${context.packageName}/$raw"),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    // SYSTEM – zostaje domyślny dźwięk powiadomień telefonu.
                }
            }
            manager.createNotificationChannel(channel)
        }
        manager.notificationChannels
            .filter { (it.id == LEGACY_CHANNEL_ID || it.id.startsWith(REMINDER_CHANNEL_PREFIX)) && it.id != id }
            .forEach { manager.deleteNotificationChannel(it.id) }
    }

    /** Identyfikator kanału przypomnień dla bieżącego dźwięku (np. do ustawień systemowych). */
    val currentReminderChannelId: String
        get() = reminderChannelId(appearancePrefs.notificationTone)

    private fun reminderChannelId(tone: BellTone) = REMINDER_CHANNEL_PREFIX + tone.key

    /** Koniec przerwy w treningu – dotknięcie otwiera zakładkę Trening. */
    @SuppressLint("MissingPermission") // uprawnienie sprawdzane w canPostNotifications()
    fun showRestFinished() {
        if (!canPostNotifications()) return
        val openWorkout = PendingIntent.getActivity(
            context,
            REST_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("przypominajka://open/workout")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, WORKOUT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Koniec przerwy")
            .setContentText("Czas na kolejną serię!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setTimeoutAfter(2 * 60 * 1000L)
            .setContentIntent(openWorkout)
            .build()
        NotificationManagerCompat.from(context).notify(REST_NOTIFICATION_ID, notification)
    }

    /** Koniec lekcji w dzień treningowy – dźwięk jak przy przypomnieniach, dotknięcie otwiera Trening. */
    @SuppressLint("MissingPermission") // uprawnienie sprawdzane w canPostNotifications()
    fun showTrainingReminder() {
        if (!canPostNotifications()) return
        val openWorkout = PendingIntent.getActivity(
            context,
            TRAINING_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("przypominajka://open/workout")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val tone = appearancePrefs.notificationTone
        ensureReminderChannel(tone)
        val notification = NotificationCompat.Builder(context, reminderChannelId(tone))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Czas na trening")
            .setContentText("Pora poćwiczyć – dotknij, żeby otworzyć plan treningowy")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openWorkout)
            .build()
        NotificationManagerCompat.from(context).notify(TRAINING_NOTIFICATION_ID, notification)
    }

    fun canPostNotifications(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // uprawnienie sprawdzane w canPostNotifications()
    fun show(reminder: Reminder, folderName: String) {
        if (!canPostNotifications()) return
        val notificationId = reminder.id.toInt()

        val openAppIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val typeLabel = SchoolType.fromName(reminder.schoolType)?.label
        val folderText = if (typeLabel != null) "Folder: $folderName · $typeLabel" else "Folder: $folderName"
        val bigText = if (reminder.note.isBlank()) folderText else "$folderText\n${reminder.note}"

        val tone = appearancePrefs.notificationTone
        ensureReminderChannel(tone)
        val notification = NotificationCompat.Builder(context, reminderChannelId(tone))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(folderText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(
                0,
                "Zrobione",
                actionIntent(NotificationActionReceiver.ACTION_DONE, reminder.id),
            )
            .addAction(
                0,
                "Za ${NotificationActionReceiver.SNOOZE_MINUTES} min",
                actionIntent(NotificationActionReceiver.ACTION_SNOOZE, reminder.id),
            )
            .addAction(
                0,
                "Za godzinę",
                actionIntent(NotificationActionReceiver.ACTION_SNOOZE_HOUR, reminder.id),
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun cancel(reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    private fun actionIntent(action: String, reminderId: Long): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            // Inny adres dla każdej akcji – inaczej system połączyłby PendingIntenty w jeden.
            data = Uri.parse("przypominajka://action/$reminderId/${action.substringAfterLast('.')}")
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Kanał z wersji sprzed wyboru dźwięku – usuwany przy pierwszym uruchomieniu. */
        private const val LEGACY_CHANNEL_ID = "reminders"
        private const val REMINDER_CHANNEL_PREFIX = "reminders_"
        const val WORKOUT_CHANNEL_ID = "workout_rest"
        private const val REST_NOTIFICATION_ID = 7_001_001
        private const val TRAINING_NOTIFICATION_ID = 7_002_001
    }
}
