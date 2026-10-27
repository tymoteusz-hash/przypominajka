package pl.przypominajka.ui.tablica.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import pl.przypominajka.ui.look.LookTheme

// Te same klucze co w starym PermissionsSection – okno z wyjaśnieniem pokazuje się tylko raz na całą aplikację.
private const val KEY_RATIONALE_SHOWN = "notification_rationale_shown"
private const val KEY_SYSTEM_DIALOG_SHOWN = "notification_system_dialog_shown"

/** Stan zgód potrzebnych do przypomnień (odświeżany przy każdym powrocie do aplikacji). */
@Stable
class TablicaPermissions internal constructor(notifications: Boolean, exactAlarms: Boolean) {
    var notificationsAllowed by mutableStateOf(notifications)
        internal set
    var exactAlarmsAllowed by mutableStateOf(exactAlarms)
        internal set

    /** Wszystko w porządku – nie trzeba nic pokazywać. */
    val allGranted: Boolean get() = notificationsAllowed && exactAlarmsAllowed
}

/**
 * Zgody na powiadomienia i dokładne alarmy. Gdy użytkownik właśnie zezwolił na dokładne alarmy,
 * przeplanowuje przypomnienia i checklistę (alarmy niedokładne → dokładne).
 */
@Composable
fun rememberTablicaPermissions(): TablicaPermissions {
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()
    val permissions = remember {
        TablicaPermissions(
            notifications = container.notificationHelper.canPostNotifications(),
            exactAlarms = container.scheduler.canScheduleExactAlarms(),
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions.notificationsAllowed = container.notificationHelper.canPostNotifications()
        val exactNow = container.scheduler.canScheduleExactAlarms()
        if (exactNow && !permissions.exactAlarmsAllowed) {
            scope.launch {
                container.repository.rescheduleAll()
                container.checklistRepository.rescheduleAll()
            }
        }
        permissions.exactAlarmsAllowed = exactNow
    }
    return permissions
}

/**
 * Prośby o zgody jako różowe karteczki z czerwoną pinezką: „Powiadomienia są wyłączone” i „Przypomnienia
 * mogą się spóźniać” – każda z przyciskiem do zgody / ustawień. Gdy wszystko jest w porządku, nic nie
 * rysuje. [askOnFirstRun] = przy pierwszym uruchomieniu (Android 13+) okno z wyjaśnieniem i prośba systemowa.
 */
@Composable
fun TablicaPermissionNotes(
    modifier: Modifier = Modifier,
    permissions: TablicaPermissions = rememberTablicaPermissions(),
    askOnFirstRun: Boolean = false,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val prefs = container.preferences
    var showRationale by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        prefs.edit { putBoolean(KEY_SYSTEM_DIALOG_SHOWN, true) }
        permissions.notificationsAllowed = container.notificationHelper.canPostNotifications()
    }

    LaunchedEffect(askOnFirstRun) {
        if (askOnFirstRun &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !permissions.notificationsAllowed &&
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
        NoteDialog(
            onDismissRequest = close,
            title = "Zezwól na powiadomienia",
            buttons = {
                NoteDialogButtons(
                    confirmText = "Dalej",
                    dismissText = "Nie teraz",
                    onDismiss = close,
                    onConfirm = {
                        close()
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            },
        ) {
            Text(
                text = "Przypominajka pokazuje powiadomienia o godzinach, które ustawisz. " +
                    "Bez tej zgody przypomnienia nie będą się pojawiać.\n\nNa następnym ekranie wybierz „Zezwól”.",
                style = TablicaType.BodyLight,
            )
        }
    }

    if (permissions.allGranted) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        if (!permissions.notificationsAllowed) {
            PermissionNote(
                title = "Powiadomienia są wyłączone",
                text = "Bez zgody na powiadomienia nie zobaczysz żadnego przypomnienia.",
                buttonText = "Włącz powiadomienia",
                tilt = -0.8f,
                onClick = {
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
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Odmowa na stałe lub powiadomienia wyłączone ręcznie – tylko ustawienia systemowe.
                        context.startActivitySafely(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                },
            )
        }
        if (!permissions.exactAlarmsAllowed) {
            PermissionNote(
                title = "Przypomnienia mogą się spóźniać",
                text = "Android pozwala na punktualne powiadomienia tylko aplikacjom ze zgodą " +
                    "„Alarmy i przypomnienia”. Bez niej przypomnienie może przyjść kilka–kilkanaście minut później.",
                buttonText = "Zezwól w ustawieniach",
                tilt = 0.7f,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivitySafely(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun PermissionNote(title: String, text: String, buttonText: String, tilt: Float, onClick: () -> Unit) {
    Note(
        color = LookTheme.tablica.pink,
        tilt = tilt,
        decoration = NoteDecoration.Pin,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = TablicaType.NoteTitle.copy(fontSize = TablicaType.Section.fontSize))
        Text(text = text, style = TablicaType.BodyLight)
        InkButton(text = buttonText, onClick = onClick, modifier = Modifier.padding(top = 4.dp))
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
