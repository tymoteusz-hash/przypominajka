package pl.przypominajka

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.przypominajka.ui.AppRoot
import pl.przypominajka.ui.navigation.StartRequest

class MainActivity : ComponentActivity() {

    /** Ekran do otwarcia ze skrótu na ikonie, z widżetu albo z powiadomienia (np. "Nowe przypomnienie"). */
    private var startRequest by mutableStateOf<StartRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            // Np. po "Wymuś zatrzymanie" system kasuje alarmy aplikacji – odtwarzamy je przy starcie.
            lifecycleScope.launch {
                appContainer.repository.rescheduleAll()
                appContainer.checklistRepository.rescheduleAll()
            }
            // Synchronizacja z Librusem w tle (KEEP – nie dubluje już zaplanowanej pracy).
            if (appContainer.librusSettings.isConnected) appContainer.librusScheduler.schedulePeriodic()
            // Nowa wersja na GitHubie? (po cichu, najwyżej raz na kilka godzin)
            appContainer.updateManager.checkOnStart()
            startRequest = StartRequest.fromIntent(intent)
        } else {
            val key = savedInstanceState.getString(STATE_START_REQUEST)
            startRequest = StartRequest.entries.firstOrNull { it.key == key }
        }

        setContent {
            AppRoot(
                container = appContainer,
                startRequest = startRequest,
                onStartRequestHandled = { startRequest = null },
                onDarkThemeChange = { dark -> applySystemBarStyle(dark) },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_START_REQUEST, startRequest?.key)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        StartRequest.fromIntent(intent)?.let { startRequest = it }
    }

    /** Ikony paska stanu muszą pasować do motywu aplikacji, a nie tylko do ustawień telefonu. */
    private fun applySystemBarStyle(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
        )
    }

    private companion object {
        const val STATE_START_REQUEST = "start_request"
        // Te same półprzezroczyste tła paska nawigacji co domyślnie w enableEdgeToEdge().
        val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
