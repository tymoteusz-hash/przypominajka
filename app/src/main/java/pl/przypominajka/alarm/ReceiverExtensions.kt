package pl.przypominajka.alarm

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Uruchamia pracę w tle (dostęp do bazy) z BroadcastReceivera.
 * goAsync() utrzymuje proces przy życiu, dopóki nie wywołamy finish().
 */
fun BroadcastReceiver.launchAsync(block: suspend CoroutineScope.() -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } catch (e: Exception) {
            Log.e("Przypominajka", "Błąd w ${this@launchAsync.javaClass.simpleName}", e)
        } finally {
            pendingResult.finish()
        }
    }
}
