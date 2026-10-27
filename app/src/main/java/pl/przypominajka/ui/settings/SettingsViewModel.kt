package pl.przypominajka.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.przypominajka.alarm.NotificationHelper
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolType
import pl.przypominajka.librus.LibrusSettings
import pl.przypominajka.librus.LibrusSyncer
import pl.przypominajka.ui.look.Look
import pl.przypominajka.update.UpdateManager
import pl.przypominajka.util.Appearance
import pl.przypominajka.util.AppearancePrefs
import pl.przypominajka.util.BellTone
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.ThemeMode
import pl.przypominajka.util.TonePreview
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ReminderReliabilityUiState(
    val notificationsAllowed: Boolean = false,
    val exactAlarmsAllowed: Boolean = false,
    val channelBlocked: Boolean = false,
    val testMessage: String? = null,
)

data class SettingsUiState(
    val appearance: Appearance = Appearance(),
    val leadDays: Map<SchoolType, Int> = emptyMap(),
    val weekdayEnds: Map<DayOfWeek, LocalTime> = emptyMap(),
    val librusConnected: Boolean = false,
    val includeTrips: Boolean = true,
    val isSyncing: Boolean = false,
)

/** Zakładka Ustawienia: wygląd, motyw, dźwięk powiadomień, ustawienia szkolne. */
class SettingsViewModel(
    private val appearancePrefs: AppearancePrefs,
    private val schoolPrefs: SchoolPrefs,
    private val librusSettings: LibrusSettings,
    private val librusSyncer: LibrusSyncer,
    private val repository: ReminderRepository,
    private val notificationHelper: NotificationHelper,
    private val tonePreview: TonePreview,
    /** Zapis wyglądu + zmiana ikony aplikacji (AppContainer.changeLook). */
    private val changeLook: (Look) -> Unit,
    val updates: UpdateManager,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)
    private val reminderReliability = MutableStateFlow(ReminderReliabilityUiState())
    val reliability = reminderReliability.asStateFlow()

    fun refreshReminderReliability() {
        reminderReliability.value = reminderReliability.value.copy(
            notificationsAllowed = notificationHelper.canPostNotifications(),
            exactAlarmsAllowed = notificationHelper.canScheduleExactAlarms(),
            channelBlocked = notificationHelper.isReminderChannelBlocked(),
        )
    }

    fun scheduleTestNotification() {
        val triggerAt = notificationHelper.scheduleTestNotification()
        refreshReminderReliability()
        reminderReliability.value = reminderReliability.value.copy(
            testMessage = if (triggerAt != null) {
                val time = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                "Próbne powiadomienie przyjdzie o $time"
            } else {
                "Włącz zgodę na dokładne alarmy i spróbuj ponownie."
            },
        )
    }

    fun openNotificationSettings(context: Context) = openSystemSettings(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )

    fun openExactAlarmSettings(context: Context) = openSystemSettings(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        },
    )

    fun openReminderChannelSettings(context: Context) = openSystemSettings(
        context,
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, reminderChannelId),
    )

    private fun openSystemSettings(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    val state: StateFlow<SettingsUiState> = combine(
        appearancePrefs.observe(),
        schoolPrefs.observeChanges(),
        librusSettings.observe(),
        syncing,
    ) { appearance, _, librus, isSyncing ->
        SettingsUiState(
            appearance = appearance,
            leadDays = SchoolType.entries.associateWith { schoolPrefs.leadDays(it) },
            weekdayEnds = schoolPrefs.weekdayEnds(),
            librusConnected = librus.connected,
            includeTrips = librus.includeTrips,
            isSyncing = isSyncing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            appearance = appearancePrefs.current(),
            leadDays = SchoolType.entries.associateWith { schoolPrefs.leadDays(it) },
            weekdayEnds = schoolPrefs.weekdayEnds(),
        ),
    )

    // ---------------------------------------------------------------- wygląd i dźwięki

    fun setTheme(mode: ThemeMode) = appearancePrefs.setTheme(mode)

    /** Nowy wygląd – razem z nim zmienia się ikona aplikacji. */
    fun setLook(look: Look) = changeLook(look)

    fun setNotificationTone(tone: BellTone) {
        appearancePrefs.setNotificationTone(tone)
        notificationHelper.ensureReminderChannel(tone)
        refreshReminderReliability()
        tonePreview.play(tone)
    }

    fun preview(tone: BellTone) = tonePreview.play(tone)

    val reminderChannelId: String get() = notificationHelper.currentReminderChannelId

    // ---------------------------------------------------------------- szkoła

    /** Nowa domyślna liczba dni; wpisy z Librusa przeliczamy od razu. */
    fun setLeadDays(type: SchoolType, days: Int) {
        if (schoolPrefs.leadDays(type) == days) return
        schoolPrefs.setLeadDays(type, days)
        if (librusSettings.isConnected) sync()
    }

    fun setIncludeTrips(include: Boolean) {
        librusSettings.includeTrips = include
        if (librusSettings.isConnected) sync()
    }

    fun setWeekdayEnd(day: DayOfWeek, time: LocalTime) {
        schoolPrefs.setWeekdayEnd(day, time)
        viewModelScope.launch { repository.rescheduleAfterSchool() }
    }

    private fun sync() {
        if (syncing.value) return
        syncing.value = true
        viewModelScope.launch {
            librusSyncer.sync()
            syncing.value = false
        }
    }

    override fun onCleared() {
        tonePreview.stop()
    }
}
