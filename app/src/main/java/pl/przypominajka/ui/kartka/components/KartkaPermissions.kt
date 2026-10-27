package pl.przypominajka.ui.kartka.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import pl.przypominajka.appContainer

// Te same klucze co w starym PermissionsSection – okno z wyjaśnieniem pokazuje się raz, niezależnie od wyglądu.
private const val KEY_RATIONALE_SHOWN = "notification_rationale_shown"
private const val KEY_SYSTEM_DIALOG_SHOWN = "notification_system_dialog_shown"

/**
 * Prośby o uprawnienia w stylu Kartki (rysuje się TYLKO, gdy czegoś brakuje – inaczej nie zajmuje miejsca,
 * więc [modifier] z odstępem stosuje się tylko wtedy):
 * - powiadomienia (Android 13+): ogłoszenie z przyciskiem – systemowa prośba albo ustawienia aplikacji;
 * - dokładne alarmy (Android 12+): ogłoszenie z przejściem do „Alarmy i przypomnienia”.
 * Stan odświeża się przy każdym powrocie do aplikacji; po zgodzie na dokładne alarmy przelicza alarmy.
 *
 * @param askOnFirstRun przy pierwszym uruchomieniu okno z wyjaśnieniem i systemowa prośba (tylko strona główna).
 */
@Composable
fun KartkaPermissionNotices(
    modifier: Modifier = Modifier,
    askOnFirstRun: Boolean = false,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val prefs = container.preferences
    val scope = rememberCoroutineScope()

    var notificationsAllowed by remember { mutableStateOf(container.notificationHelper.canPostNotifications()) }
    var exactAlarmsAllowed by remember { mutableStateOf(container.scheduler.canScheduleExactAlarms()) }
    var showRationale by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        prefs.edit { putBoolean(KEY_SYSTEM_DIALOG_SHOWN, true) }
        notificationsAllowed = container.notificationHelper.canPostNotifications()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = container.notificationHelper.canPostNotifications()
        val exactNow = container.scheduler.canScheduleExactAlarms()
        if (exactNow && !exactAlarmsAllowed) {
            // Użytkownik właśnie zezwolił – alarmy niedokładne zamieniamy na dokładne.
            scope.launch {
                container.repository.rescheduleAll()
                container.checklistRepository.rescheduleAll()
            }
        }
        exactAlarmsAllowed = exactNow
    }

    LaunchedEffect(askOnFirstRun) {
        if (askOnFirstRun &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationsAllowed &&
            !prefs.getBoolean(KEY_RATIONALE_SHOWN, false)
        ) {
            showRationale = true
        }
    }

    if (showRationale) {
        val close = {
            showRationale = false
            prefs.edit { putBoolean(KEY_RATIONALE_SHOWN, true) }
        }
        KartkaDialog(
            onDismissRequest = close,
            title = "Zezwól na powiadomienia",
            text = "Przypominajka pokazuje powiadomienia o godzinach, które ustawisz. " +
                "Bez tej zgody przypomnienia nie będą się pojawiać.\n\nNa następnym ekranie wybierz „Zezwól”.",
            confirmLabel = "Dalej",
            onConfirm = {
                close()
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            dismissLabel = "Nie teraz",
        )
    }

    if (notificationsAllowed && exactAlarmsAllowed) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!notificationsAllowed) {
            KartkaNotice(
                title = "Powiadomienia są wyłączone",
                text = "Bez zgody na powiadomienia nie zobaczysz żadnego przypomnienia.",
                icon = KartkaIcons.Bell,
                actionLabel = "Włącz powiadomienia",
                onAction = {
                    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    val activity = context.findActivity()
                    val canAskAgain = !prefs.getBoolean(KEY_SYSTEM_DIALOG_SHOWN, false) ||
                        (
                            activity != null &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            )
                    if (needsRuntimePermission && canAskAgain) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Odmowa na stałe albo powiadomienia wyłączone ręcznie – zostają ustawienia systemu.
                        openAppNotificationSettings(context)
                    }
                },
            )
        }
        if (!exactAlarmsAllowed) {
            KartkaNotice(
                title = "Przypomnienia mogą się spóźniać",
                text = "Android pozwala na punktualne powiadomienia tylko aplikacjom ze zgodą " +
                    "„Alarmy i przypomnienia”. Bez niej przypomnienie może przyjść kilka–kilkanaście minut później.",
                icon = KartkaIcons.Clock,
                actionLabel = "Zezwól w ustawieniach",
                onAction = { openExactAlarmSettings(context) },
            )
        }
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startActivitySafely(intent)
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}"),
        )
        context.startActivitySafely(intent)
    }
}

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Awaryjnie: ogólny ekran informacji o aplikacji.
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
