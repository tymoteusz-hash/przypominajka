package pl.przypominajka.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.przypominajka.ai.AiSettings
import pl.przypominajka.data.LIBRUS_EXTERNAL_PREFIX
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolType
import pl.przypominajka.librus.LibrusSettings
import pl.przypominajka.librus.LibrusStatus
import pl.przypominajka.librus.LibrusSyncOutcome
import pl.przypominajka.librus.LibrusSyncScheduler
import pl.przypominajka.librus.LibrusSyncer
import pl.przypominajka.util.Appearance
import pl.przypominajka.util.AppearancePrefs
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.ThemeMode
import java.time.DayOfWeek
import java.time.LocalTime

data class AccountUiState(
    val status: LibrusStatus = LibrusStatus(),
    val login: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isWorking: Boolean = false,
    val formError: String? = null,
    val leadDays: Map<SchoolType, Int> = emptyMap(),
    val weekdayEnds: Map<DayOfWeek, LocalTime> = emptyMap(),
    val aiHasKey: Boolean = false,
    val aiKeyHint: String = "",
    val aiKeyInput: String = "",
    val appearance: Appearance = Appearance(),
)

class AccountViewModel(
    private val settings: LibrusSettings,
    private val syncer: LibrusSyncer,
    private val scheduler: LibrusSyncScheduler,
    private val repository: ReminderRepository,
    private val schoolPrefs: SchoolPrefs,
    private val aiSettings: AiSettings,
    private val appearancePrefs: AppearancePrefs,
) : ViewModel() {

    private val form = MutableStateFlow(AccountUiState())

    val state: StateFlow<AccountUiState> = combine(
        form,
        settings.observe(),
        schoolPrefs.observeChanges(),
        aiSettings.observeHasKey(),
        appearancePrefs.observe(),
    ) { form, status, _, hasKey, appearance ->
        form.copy(
            status = status,
            leadDays = SchoolType.entries.associateWith { schoolPrefs.leadDays(it) },
            weekdayEnds = schoolPrefs.weekdayEnds(),
            aiHasKey = hasKey,
            aiKeyHint = aiSettings.keyHint,
            appearance = appearance,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountUiState(
            status = settings.status(),
            leadDays = SchoolType.entries.associateWith { schoolPrefs.leadDays(it) },
            weekdayEnds = schoolPrefs.weekdayEnds(),
            appearance = appearancePrefs.current(),
        ),
    )

    // ---------------------------------------------------------------- wygląd

    fun setTheme(mode: ThemeMode) = appearancePrefs.setTheme(mode)

    fun setBellSound(enabled: Boolean) = appearancePrefs.setBellSound(enabled)

    // ---------------------------------------------------------------- Librus

    fun onLoginChange(value: String) = form.update { it.copy(login = value.trim(), formError = null) }

    fun onPasswordChange(value: String) = form.update { it.copy(password = value, formError = null) }

    fun toggleShowPassword() = form.update { it.copy(showPassword = !it.showPassword) }

    fun connect() {
        val current = form.value
        if (current.isWorking) return
        if (current.login.isBlank() || current.password.isEmpty()) {
            form.update { it.copy(formError = "Podaj login i hasło do Librusa Synergia.") }
            return
        }
        form.update { it.copy(isWorking = true, formError = null) }
        viewModelScope.launch {
            val outcome = syncer.connect(current.login, current.password)
            if (settings.isConnected) {
                // Logowanie się udało (nawet jeśli pobranie danych nie) – dalej działamy w tle.
                scheduler.schedulePeriodic()
                form.update { it.copy(isWorking = false, password = "", showPassword = false) }
            } else {
                val message = (outcome as? LibrusSyncOutcome.Failure)?.message ?: "Nie udało się połączyć."
                form.update { it.copy(isWorking = false, formError = message) }
            }
        }
    }

    fun syncNow() {
        if (form.value.isWorking) return
        form.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            syncer.sync()
            form.update { it.copy(isWorking = false) }
        }
    }

    fun setIncludeTrips(include: Boolean) {
        settings.includeTrips = include
        if (settings.isConnected) syncNow()
    }

    fun disconnect(deleteData: Boolean) {
        scheduler.cancel()
        settings.clearCredentials()
        if (deleteData) {
            viewModelScope.launch {
                repository.deleteExternalReminders(LIBRUS_EXTERNAL_PREFIX)
                repository.clearTimetable()
            }
        }
    }

    // ---------------------------------------------------------------- AI

    fun onAiKeyChange(value: String) = form.update { it.copy(aiKeyInput = value.trim()) }

    fun saveAiKey() {
        val key = form.value.aiKeyInput
        if (key.isBlank()) return
        aiSettings.saveApiKey(key)
        form.update { it.copy(aiKeyInput = "") }
    }

    fun clearAiKey() = aiSettings.clearApiKey()

    // ---------------------------------------------------------------- szkoła

    /** Nowa domyślna liczba dni; wpisy z Librusa przeliczamy od razu. */
    fun setLeadDays(type: SchoolType, days: Int) {
        if (schoolPrefs.leadDays(type) == days) return
        schoolPrefs.setLeadDays(type, days)
        if (settings.isConnected) syncNow()
    }

    fun setWeekdayEnd(day: DayOfWeek, time: LocalTime) {
        schoolPrefs.setWeekdayEnd(day, time)
        viewModelScope.launch { repository.rescheduleAfterSchool() }
    }
}
