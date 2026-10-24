package pl.przypominajka

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.przypominajka.ui.navigation.AppNavHost
import pl.przypominajka.ui.navigation.StartRequest
import pl.przypominajka.ui.theme.PrzypominajkaTheme
import pl.przypominajka.ui.update.UpdateDialogHost
import pl.przypominajka.util.ThemeMode

class MainActivity : ComponentActivity() {

    /** Ekran do otwarcia ze skrótu na ikonie albo z widżetu (np. "Nowe przypomnienie"). */
    private var startRequest by mutableStateOf<StartRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            // Np. po "Wymuś zatrzymanie" system kasuje alarmy aplikacji – odtwarzamy je przy starcie.
            lifecycleScope.launch { appContainer.repository.rescheduleAll() }
            // Synchronizacja z Librusem w tle (KEEP – nie dubluje już zaplanowanej pracy).
            if (appContainer.librusSettings.isConnected) appContainer.librusScheduler.schedulePeriodic()
            // Nowa wersja na GitHubie? (po cichu, najwyżej raz na kilka godzin)
            appContainer.updateManager.checkOnStart()
            startRequest = StartRequest.fromIntent(intent)
        }

        val appearancePrefs = appContainer.appearancePrefs
        setContent {
            val appearanceFlow = remember { appearancePrefs.observe() }
            val appearance by appearanceFlow.collectAsStateWithLifecycle(appearancePrefs.current())
            val darkTheme = when (appearance.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Ikony paska stanu muszą pasować do motywu aplikacji, a nie tylko do ustawień telefonu.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose {}
            }

            PrzypominajkaTheme(darkTheme = darkTheme) {
                AppNavHost(
                    startRequest = startRequest,
                    onStartRequestHandled = { startRequest = null },
                )
                UpdateDialogHost(appContainer.updateManager)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        StartRequest.fromIntent(intent)?.let { startRequest = it }
    }

    private companion object {
        // Te same półprzezroczyste tła paska nawigacji co domyślnie w enableEdgeToEdge().
        val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
