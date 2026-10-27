package pl.przypominajka.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.przypominajka.AppContainer
import pl.przypominajka.ui.dzwon.DzwonApp
import pl.przypominajka.ui.kartka.KartkaApp
import pl.przypominajka.ui.look.Look
import pl.przypominajka.ui.tablica.TablicaApp
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.navigation.StartRequest
import pl.przypominajka.ui.onboarding.OnboardingScreen
import pl.przypominajka.ui.update.UpdateDialogHost
import pl.przypominajka.util.ThemeMode

/**
 * Korzeń interfejsu: motyw wybranego wyglądu, przewodnik na start (wygląd + motyw)
 * i „powłoka” wybranego wyglądu (każdy wygląd ma własny układ ekranów i nawigację).
 *
 * @param onDarkThemeChange wywoływane przy każdej zmianie jasny/ciemny – np. do kolorów ikon paska stanu.
 */
@Composable
fun AppRoot(
    container: AppContainer,
    startRequest: StartRequest?,
    onStartRequestHandled: () -> Unit,
    onDarkThemeChange: (Boolean) -> Unit = {},
) {
    val prefs = container.appearancePrefs
    val appearanceFlow = remember(prefs) { prefs.observe() }
    val appearance by appearanceFlow.collectAsStateWithLifecycle(prefs.current())
    val dark = when (appearance.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    DisposableEffect(dark) {
        onDarkThemeChange(dark)
        onDispose {}
    }

    val look = appearance.look ?: Look.KARTKA
    LookTheme(look = look, dark = dark) {
        if (!appearance.onboardingDone) {
            // Wybór od razu zmienia motyw przewodnika (podgląd na żywo); ikona zmienia się dopiero na końcu.
            OnboardingScreen(
                look = appearance.look,
                theme = appearance.theme,
                onLookChange = prefs::setLook,
                onThemeChange = prefs::setTheme,
                onFinish = { chosen ->
                    prefs.setOnboardingDone(true)
                    container.changeLook(chosen)
                },
            )
        } else {
            when (look) {
                Look.KARTKA -> KartkaApp(startRequest, onStartRequestHandled)
                Look.DZWON -> DzwonApp(startRequest, onStartRequestHandled)
                Look.TABLICA -> TablicaApp(startRequest, onStartRequestHandled)
            }
        }
        UpdateDialogHost(container.updateManager)
    }
}
