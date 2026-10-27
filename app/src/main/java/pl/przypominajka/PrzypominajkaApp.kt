package pl.przypominajka

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pl.przypominajka.widget.ChecklistWidget
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

        // Każda zmiana checklisty (w aplikacji, z powiadomienia, z widżetu) odświeża widżet "Checklista".
        appScope.launch {
            container.checklistRepository.observeItems().collectLatest {
                delay(WIDGET_REFRESH_DELAY_MS)
                runCatching { ChecklistWidget().updateAll(this@PrzypominajkaApp) }
            }
        }

        // Zmiana wyglądu albo motywu w Ustawieniach – oba widżety w nowym stylu
        // (pierwsza wartość to stan przy starcie, więc ją pomijamy).
        appScope.launch {
            container.appearancePrefs.observe()
                .map { it.look to it.theme }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest {
                    delay(WIDGET_REFRESH_DELAY_MS)
                    runCatching { TodayWidget().updateAll(this@PrzypominajkaApp) }
                    runCatching { ChecklistWidget().updateAll(this@PrzypominajkaApp) }
                }
        }
    }

    private companion object {
        const val WIDGET_REFRESH_DELAY_MS = 600L
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as PrzypominajkaApp).container
