package pl.przypominajka.ui.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

private const val KEY_RATIONALE_SHOWN = "notification_rationale_shown"
private const val KEY_SYSTEM_DIALOG_SHOWN = "notification_system_dialog_shown"

/**
 * Obsługa uprawnień z jasnym wyjaśnieniem:
 * - POST_NOTIFICATIONS (Android 13+): przy pierwszym uruchomieniu okno z wyjaśnieniem, potem systemowa prośba;
 *   jeśli odmówiono – baner z przyciskiem (ponowna prośba lub przejście do ustawień).
 * - Dokładne alarmy (Android 12+): baner z wyjaśnieniem i przejściem do ekranu "Alarmy i przypomnienia".
 * Stan jest odświeżany przy każdym powrocie do aplikacji.
 */
@Composable
fun PermissionsSection() {
    val context = LocalContext.current
    val container = context.appContainer
    val prefs = container.preferences
    val scope = rememberCoroutineScope()

    var notificationsAllowed by remember { mutableStateOf(container.notificationHelper.canPostNotifications()) }
    var exactAlarmsAllowed by remember { mutableStateOf(container.scheduler.canScheduleExactAlarms()) }
    var showRationale by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        prefs.edit { putBoolean(KEY_SYSTEM_DIALOG_SHOWN, true) }
        notificationsAllowed = container.notificationHelper.canPostNotifications()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = container.notificationHelper.canPostNotifications()
        val exactNow = container.scheduler.canScheduleExactAlarms()
        if (exactNow && !exactAlarmsAllowed) {
            // Użytkownik właśnie zezwolił – zamieniamy alarmy niedokładne na dokładne.
            scope.launch { container.repository.rescheduleAll() }
        }
        exactAlarmsAllowed = exactNow
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationsAllowed &&
            !prefs.getBoolean(KEY_RATIONALE_SHOWN, false)
        ) {
            showRationale = true
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
                prefs.edit { putBoolean(KEY_RATIONALE_SHOWN, true) }
            },
            icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
            title = { Text("Zezwól na powiadomienia") },
            text = {
                Text(
                    "Przypominajka pokazuje powiadomienia o godzinach, które ustawisz. " +
                        "Bez tej zgody przypomnienia nie będą się pojawiać.\n\n" +
                        "Na następnym ekranie wybierz „Zezwól”.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    prefs.edit { putBoolean(KEY_RATIONALE_SHOWN, true) }
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Dalej") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    prefs.edit { putBoolean(KEY_RATIONALE_SHOWN, true) }
                }) { Text("Nie teraz") }
            },
        )
    }

    if (!notificationsAllowed) {
        PermissionBanner(
            title = "Powiadomienia są wyłączone",
            text = "Bez zgody na powiadomienia nie zobaczysz żadnego przypomnienia.",
            buttonText = "Włącz powiadomienia",
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
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Odmowa na stałe lub powiadomienia wyłączone ręcznie – tylko ustawienia systemowe.
                    openAppNotificationSettings(context)
                }
            },
        )
    }

    if (!exactAlarmsAllowed) {
        PermissionBanner(
            title = "Przypomnienia mogą się spóźniać",
            text = "Android pozwala na punktualne powiadomienia tylko aplikacjom ze zgodą " +
                "„Alarmy i przypomnienia”. Bez niej przypomnienie może przyjść kilka–kilkanaście minut później.",
            buttonText = "Zezwól w ustawieniach",
            onClick = { openExactAlarmSettings(context) },
        )
    }
}

@Composable
private fun PermissionBanner(
    title: String,
    text: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    NoticeCard(
        title = title,
        body = text,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        icon = Icons.Filled.Warning,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .appear(),
        actions = { PrimaryButton(text = buttonText, onClick = onClick) },
    )
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
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
        )
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
