package pl.przypominajka.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.przypominajka.alarm.NotificationHelper
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolType
import pl.przypominajka.librus.LibrusSettings
import pl.przypominajka.librus.LibrusSyncer
import pl.przypominajka.update.UpdateManager
import pl.przypominajka.util.Appearance
import pl.przypominajka.util.AppearancePrefs
import pl.przypominajka.util.BellSound
import pl.przypominajka.util.BellTone
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.ThemeMode
import java.time.DayOfWeek
import java.time.LocalTime

data class SettingsUiState(
    val appearance: Appearance = Appearance(),
    val leadDays: Map<SchoolType, Int> = emptyMap(),
    val weekdayEnds: Map<DayOfWeek, LocalTime> = emptyMap(),
    val librusConnected: Boolean = false,
    val includeTrips: Boolean = true,
    val isSyncing: Boolean = false,
)

/** Zakładka Ustawienia: wygląd, dźwięki dzwonu i powiadomień, ustawienia szkolne. */
class SettingsViewModel(
    private val appearancePrefs: AppearancePrefs,
    private val schoolPrefs: SchoolPrefs,
    private val librusSettings: LibrusSettings,
    private val librusSyncer: LibrusSyncer,
    private val repository: ReminderRepository,
    private val notificationHelper: NotificationHelper,
    private val bellSound: BellSound,
    val updates: UpdateManager,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)

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

    fun setBellSound(enabled: Boolean) {
        appearancePrefs.setBellSound(enabled)
        if (enabled) bellSound.preview(appearancePrefs.transitionTone)
    }

    fun setTransitionTone(tone: BellTone) {
        appearancePrefs.setTransitionTone(tone)
        bellSound.preview(tone)
    }

    fun setBellVolume(volume: Float) {
        appearancePrefs.setBellVolume(volume)
        bellSound.preview(appearancePrefs.transitionTone, volume)
    }

    fun setQuietWhenSilent(enabled: Boolean) = appearancePrefs.setQuietWhenSilent(enabled)

    fun setNotificationTone(tone: BellTone) {
        appearancePrefs.setNotificationTone(tone)
        notificationHelper.ensureReminderChannel(tone)
        bellSound.preview(tone, volume = 0.8f)
    }

    fun preview(tone: BellTone) = bellSound.preview(tone, volume = 0.8f)

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
        bellSound.stopPreview()
    }
}
