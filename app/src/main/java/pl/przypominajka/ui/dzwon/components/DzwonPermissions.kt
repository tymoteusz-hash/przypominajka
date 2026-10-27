package pl.przypominajka.ui.dzwon.components

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

// Te same klucze co w starym PermissionsSection – stan „już pytaliśmy” jest wspólny dla wszystkich wyglądów.
private const val KEY_RATIONALE_SHOWN = "notification_rationale_shown"
private const val KEY_SYSTEM_DIALOG_SHOWN = "notification_system_dialog_shown"

/**
 * Prośby o uprawnienia w stylu Dzwonu (karty [DzwonNotice] z przyciskiem):
 * - powiadomienia (Android 13+) – przy [askOnFirstRun] najpierw jednorazowe okno z wyjaśnieniem i systemowa
 *   prośba; potem, jeśli odmówiono, karta z ponowną prośbą albo przejściem do ustawień,
 * - dokładne alarmy (Android 12+) – karta z przejściem do „Alarmy i przypomnienia”.
 * Gdy wszystko jest dozwolone, nic nie rysuje. Stan odświeża się przy każdym powrocie do aplikacji.
 * [containerColor] – tło kart (na arkuszu podaj `LookTheme.dzwon.field`), [itemPadding] – odstęp wokół
 * każdej karty (ukryte karty nie zostawiają pustego miejsca).
 */
@Composable
fun DzwonPermissionPrompts(
    modifier: Modifier = Modifier,
    askOnFirstRun: Boolean = false,
    containerColor: Color = LookTheme.dzwon.surface,
    itemPadding: PaddingValues = PaddingValues(0.dp),
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
            // Właśnie zezwolono – alarmy niedokładne zamieniamy na dokładne (przypomnienia i checklista).
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
        DzwonDialog(
            onDismissRequest = close,
            title = "Zezwól na powiadomienia",
            text = "Przypominajka pokazuje powiadomienia o godzinach, które ustawisz. " +
                "Bez tej zgody dzwon nie zadzwoni.\n\nNa następnym ekranie wybierz „Zezwól”.",
            confirmText = "Dalej",
            onConfirm = {
                close()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            dismissText = "Nie teraz",
        )
    }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = !notificationsAllowed,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            DzwonNotice(
                title = "Powiadomienia są wyłączone",
                text = "Bez zgody na powiadomienia dzwon nie zadzwoni – nie zobaczysz żadnego przypomnienia.",
                icon = Icons.Rounded.NotificationsOff,
                containerColor = containerColor,
                modifier = Modifier.padding(itemPadding),
                actions = {
                    DzwonPrimaryButton(
                        text = "Włącz powiadomienia",
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
                                // Odmowa na stałe albo wyłączone ręcznie – zostają ustawienia systemowe.
                                openAppNotificationSettings(context)
                            }
                        },
                    )
                },
            )
        }
        AnimatedVisibility(
            visible = !exactAlarmsAllowed,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        ) {
            DzwonNotice(
                title = "Dzwon może się spóźniać",
                text = "Android pozwala na punktualne powiadomienia tylko aplikacjom ze zgodą " +
                    "„Alarmy i przypomnienia”. Bez niej przypomnienie może przyjść kilka–kilkanaście minut później.",
                icon = Icons.Rounded.AlarmOn,
                containerColor = containerColor,
                modifier = Modifier.padding(itemPadding),
                actions = {
                    DzwonPrimaryButton(text = "Zezwól w ustawieniach", onClick = { openExactAlarmSettings(context) })
                },
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
