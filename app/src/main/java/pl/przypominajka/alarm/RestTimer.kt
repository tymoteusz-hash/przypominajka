package pl.przypominajka.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.przypominajka.appContainer

/** Trwająca przerwa: koniec (epoch millis) i pełna długość – do pierścienia postępu. */
data class RestState(val endsAt: Long, val totalSeconds: Int)

/**
 * Licznik przerwy między seriami w Treningu. Koniec przerwy planujemy dokładnym alarmem, więc
 * powiadomienie z wibracją przyjdzie także przy zablokowanym ekranie (zwykłe opóźnienie w aplikacji
 * mogłoby się wtedy spóźnić). Stan jest zapisywany, żeby przetrwał zamknięcie aplikacji.
 */
class RestTimer(private val context: Context, private val prefs: SharedPreferences) {

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<RestState?> = _state.asStateFlow()

    fun start(seconds: Int) {
        val rest = RestState(System.currentTimeMillis() + seconds * 1000L, seconds)
        save(rest)
        schedule(rest.endsAt)
    }

    /** Wydłuża trwającą przerwę (np. "+30 s"). */
    fun extend(seconds: Int) {
        val current = _state.value ?: return
        val base = maxOf(current.endsAt, System.currentTimeMillis())
        val rest = RestState(base + seconds * 1000L, current.totalSeconds + seconds)
        save(rest)
        schedule(rest.endsAt)
    }

    fun stop() {
        alarmManager.cancel(pendingIntent())
        save(null)
    }

    /** Wywoływane przez [RestTimerReceiver], gdy przerwa się skończyła. */
    fun onFinished() = save(null)

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    private fun schedule(at: Long) {
        val pi = pendingIntent()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, RestTimerReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun readState(): RestState? {
        val endsAt = prefs.getLong(KEY_ENDS_AT, 0L)
        if (endsAt <= System.currentTimeMillis()) return null
        return RestState(endsAt, prefs.getInt(KEY_TOTAL, 0))
    }

    private fun save(rest: RestState?) {
        prefs.edit {
            if (rest == null) {
                remove(KEY_ENDS_AT)
                remove(KEY_TOTAL)
            } else {
                putLong(KEY_ENDS_AT, rest.endsAt)
                putInt(KEY_TOTAL, rest.totalSeconds)
            }
        }
        _state.value = rest
    }

    private companion object {
        const val REQUEST_CODE = 7_001
        const val KEY_ENDS_AT = "rest_ends_at"
        const val KEY_TOTAL = "rest_total_seconds"
    }
}

/** Koniec przerwy: powiadomienie z wibracją "Czas na kolejną serię". */
class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = context.appContainer
        container.restTimer.onFinished()
        container.notificationHelper.showRestFinished()
    }
}
