package pl.przypominajka

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pl.przypominajka.widget.TodayWidget

class PrzypominajkaApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Kanały powiadomień tworzone przy każdym starcie (operacja idempotentna).
        container.notificationHelper.createChannel()

        // Każda zmiana przypomnień odświeża widżet "Dziś" (z małym opóźnieniem, żeby nie migał).
        appScope.launch {
            container.repository.observeReminders().collectLatest {
                delay(WIDGET_REFRESH_DELAY_MS)
                runCatching { TodayWidget().updateAll(this@PrzypominajkaApp) }
            }
        }
    }

    private companion object {
        const val WIDGET_REFRESH_DELAY_MS = 600L
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as PrzypominajkaApp).container
