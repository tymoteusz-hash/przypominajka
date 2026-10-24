package pl.przypominajka.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.przypominajka.data.Folder
import pl.przypominajka.data.Reminder
import pl.przypominajka.data.ReminderRepository
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.endDate
import pl.przypominajka.data.includes
import pl.przypominajka.data.isFromLibrus
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.TimeFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

const val MIN_DAYS = 1
const val MAX_DAYS = 365

private val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
/** Domyślnie przypomnienie jednorazowe (jeden dzień) – dłuższe ustawia się przyciskami. */
private const val DEFAULT_DAYS = 1
private const val DEFAULT_DAYS_TEXT = "$DEFAULT_DAYS"

/** Najbliższy dzień szkolny po dzisiejszym (pomija weekend). */
private fun nextSchoolDay(today: LocalDate): LocalDate {
    var day = today.plusDays(1)
    while (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) day = day.plusDays(1)
    return day
}

data class EditReminderUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val reminderId: Long = 0,
    /** Wpis pobrany z Librusa – synchronizacja może nadpisać treść i termin. */
    val isFromLibrus: Boolean = false,
    val title: String = "",
    val note: String = "",
    val folderId: Long = Folder.OTHER_FOLDER_ID,
    val folders: List<Folder> = emptyList(),
    /** Rodzaj wpisu szkolnego; ma sens tylko w folderze "Szkoła". */
    val schoolType: SchoolType? = null,
    // --- zwykłe przypomnienie: od dnia X przez N dni
    val startDate: LocalDate = LocalDate.now(),
    val daysText: String = DEFAULT_DAYS_TEXT,
    val endless: Boolean = false,
    /** Wybrane dni tygodnia (maska bitów, 0 = codziennie). */
    val daysOfWeek: Int = 0,
    // --- wpis szkolny: dzień wydarzenia + ile dni wcześniej przypominać
    val eventDate: LocalDate = nextSchoolDay(LocalDate.now()),
    val leadDays: Int = 1,
    val leadTouched: Boolean = false,
    // --- godziny
    val afterSchool: Boolean = false,
    val times: List<LocalTime> = listOf(DEFAULT_TIME),
    val schoolTimes: SchoolTimes = SchoolTimes(),
    // --- walidacja i stan
    val titleError: Boolean = false,
    val daysError: String? = null,
    val timesError: String? = null,
    val eventDateError: String? = null,
    val isSaving: Boolean = false,
    /** Ustawiane po zapisie/usunięciu – ekran wtedy się zamyka. */
    val done: Boolean = false,
) {
    val isSchoolFolder: Boolean get() = folderId == Folder.SCHOOL_FOLDER_ID

    /** Wpis szkolny z terminem (sprawdzian, kartkówka, zadanie, wyjście). */
    val isEventMode: Boolean get() = isSchoolFolder && schoolType != null

    /** Godziny liczone z planu lekcji – tylko w folderze "Szkoła". */
    val usesAfterSchool: Boolean get() = isSchoolFolder && afterSchool

    /** Pierwszy dzień powiadomień w trybie szkolnym (nie wcześniej niż dziś). */
    val eventFirstDay: LocalDate get() = maxOf(eventDate.minusDays(leadDays.toLong()), LocalDate.now())

    /** Ostatni dzień powiadomień w trybie szkolnym – dzień przed wydarzeniem. */
    val eventLastDay: LocalDate get() = eventDate.minusDays(1)

    /** Dni, w których przyjdą powiadomienia (do podglądu, maks. 7). */
    val previewDays: List<LocalDate>
        get() {
            val (first, last) = if (isEventMode) {
                eventFirstDay to eventLastDay
            } else {
                val days = if (endless) 7 else (daysText.toIntOrNull() ?: 1).coerceIn(1, 7)
                startDate to startDate.plusDays(days - 1L)
            }
            if (last.isBefore(first)) return emptyList()
            return generateSequence(first) { it.plusDays(1) }
                .takeWhile { !it.isAfter(last) }
                .filter { isEventMode || daysOfWeek.includes(it.dayOfWeek) }
                .take(7)
                .toList()
        }
}

class EditReminderViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository,
    private val schoolPrefs: SchoolPrefs,
) : ViewModel() {

    private val reminderId: Long = savedStateHandle.get<Long>(Routes.ARG_REMINDER_ID) ?: -1L
    private val initialFolderId: Long = savedStateHandle.get<Long>(Routes.ARG_FOLDER_ID) ?: -1L

    /** Oryginał edytowanego przypomnienia (zachowuje m.in. stan włączenia i datę utworzenia). */
    private var original: Reminder? = null

    private val _state = MutableStateFlow(EditReminderUiState(isNew = reminderId <= 0L))
    val state: StateFlow<EditReminderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFolders().collect { folders ->
                _state.update { s ->
                    val folderStillExists = folders.isEmpty() || folders.any { it.id == s.folderId }
                    s.copy(
                        folders = folders,
                        folderId = if (folderStillExists) s.folderId else Folder.OTHER_FOLDER_ID,
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeSchoolTimes().collect { times -> _state.update { it.copy(schoolTimes = times) } }
        }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val existing = if (reminderId > 0L) repository.getReminder(reminderId) else null
        if (existing != null) {
            original = existing
            val type = SchoolType.fromName(existing.schoolType)
            // Dla wpisów sprzed wersji z terminem odtwarzamy dzień wydarzenia z końca przypominania.
            val eventDate = existing.eventDate ?: existing.endDate?.plusDays(1)
            _state.update {
                it.copy(
                    isLoading = false,
                    isNew = false,
                    reminderId = existing.id,
                    isFromLibrus = existing.isFromLibrus,
                    title = existing.title,
                    note = existing.note,
                    startDate = existing.startDate,
                    endless = existing.durationDays == null,
                    daysText = (existing.durationDays ?: DEFAULT_DAYS).toString(),
                    times = existing.times,
                    daysOfWeek = existing.daysOfWeek,
                    afterSchool = existing.afterSchool,
                    folderId = existing.folderId,
                    schoolType = if (eventDate != null) type else null,
                    eventDate = eventDate ?: it.eventDate,
                    leadDays = if (eventDate != null) {
                        ChronoUnit.DAYS.between(existing.startDate, eventDate).toInt()
                            .coerceIn(SchoolPrefs.MIN_LEAD_DAYS, SchoolPrefs.MAX_LEAD_DAYS)
                    } else {
                        it.leadDays
                    },
                    leadTouched = true,
                )
            }
        } else {
            val folderId = if (initialFolderId > 0L) initialFolderId else Folder.OTHER_FOLDER_ID
            _state.update {
                it.copy(
                    isLoading = false,
                    isNew = true,
                    folderId = folderId,
                    afterSchool = folderId == Folder.SCHOOL_FOLDER_ID,
                )
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, titleError = false) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value) }

    fun onStartDateChange(date: LocalDate) = _state.update { it.copy(startDate = date) }

    fun onDaysTextChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(3)
        _state.update { it.copy(daysText = digits, daysError = null) }
    }

    fun changeDaysBy(delta: Int) = _state.update {
        val current = it.daysText.toIntOrNull() ?: 0
        it.copy(daysText = (current + delta).coerceIn(MIN_DAYS, MAX_DAYS).toString(), daysError = null)
    }

    /** Szybki wybór długości (1 dzień, tydzień, …). */
    fun setDays(days: Int) = _state.update {
        it.copy(daysText = days.coerceIn(MIN_DAYS, MAX_DAYS).toString(), endless = false, daysError = null)
    }

    fun onEndlessChange(endless: Boolean) = _state.update { it.copy(endless = endless, daysError = null) }

    /** Włącza/wyłącza dzień tygodnia (0 = pn … 6 = nd). Wszystkie albo żaden = codziennie. */
    fun toggleWeekday(index: Int) = _state.update {
        val toggled = it.daysOfWeek xor (1 shl index)
        it.copy(daysOfWeek = if (toggled == 0b1111111) 0 else toggled)
    }

    fun onFolderChange(folderId: Long) = _state.update {
        val school = folderId == Folder.SCHOOL_FOLDER_ID
        val untouchedTimes = it.isNew && it.times == listOf(DEFAULT_TIME)
        it.copy(
            folderId = folderId,
            // Rodzaj wpisu i godziny "po lekcjach" dotyczą tylko folderu "Szkoła".
            schoolType = if (school) it.schoolType else null,
            afterSchool = when {
                !school -> false
                untouchedTimes -> true
                else -> it.afterSchool
            },
        )
    }

    /** Wybór rodzaju wpisu szkolnego (ponowne dotknięcie zdejmuje wybór). */
    fun onSchoolTypeChange(type: SchoolType) = _state.update { s ->
        val newType = if (s.schoolType == type) null else type
        s.copy(
            schoolType = newType,
            leadDays = if (newType != null && !s.leadTouched) schoolPrefs.leadDays(newType) else s.leadDays,
            eventDateError = null,
        )
    }

    fun onEventDateChange(date: LocalDate) = _state.update { it.copy(eventDate = date, eventDateError = null) }

    fun onLeadDaysChange(days: Int) = _state.update {
        it.copy(leadDays = days.coerceIn(SchoolPrefs.MIN_LEAD_DAYS, SchoolPrefs.MAX_LEAD_DAYS), leadTouched = true)
    }

    fun onAfterSchoolChange(enabled: Boolean) = _state.update { it.copy(afterSchool = enabled, timesError = null) }

    /** Dodanie nowej godziny ([previous] == null) lub zmiana istniejącej. */
    fun onTimePicked(previous: LocalTime?, time: LocalTime) = _state.update { s ->
        if (time != previous && time in s.times) {
            s.copy(timesError = "Godzina ${time.format(TimeFormatter)} jest już na liście")
        } else {
            val updated = (s.times - setOfNotNull(previous) + time).distinct().sorted()
            s.copy(times = updated, timesError = null)
        }
    }

    fun removeTime(time: LocalTime) = _state.update { it.copy(times = it.times - time, timesError = null) }

    fun save() {
        val s = _state.value
        if (s.isSaving) return
        val today = LocalDate.now()

        val title = s.title.trim()
        val titleError = title.isEmpty()
        val days = s.daysText.toIntOrNull()
        val daysError = if (!s.isEventMode && !s.endless && (days == null || days !in MIN_DAYS..MAX_DAYS)) {
            "Podaj liczbę dni od $MIN_DAYS do $MAX_DAYS"
        } else {
            null
        }
        val eventDateError = if (s.isEventMode && !s.eventDate.isAfter(today)) {
            "Wybierz dzień po dzisiejszym – przypomnienia przychodzą przed terminem."
        } else {
            null
        }
        val timesError = if (!s.usesAfterSchool && s.times.isEmpty()) "Dodaj co najmniej jedną godzinę" else null

        if (titleError || daysError != null || timesError != null || eventDateError != null) {
            _state.update {
                it.copy(
                    titleError = titleError,
                    daysError = daysError,
                    timesError = timesError,
                    eventDateError = eventDateError,
                )
            }
            return
        }

        val startDate: LocalDate
        val durationDays: Int?
        if (s.isEventMode) {
            startDate = s.eventFirstDay
            durationDays = (ChronoUnit.DAYS.between(startDate, s.eventLastDay).toInt() + 1).coerceAtLeast(1)
        } else {
            startDate = s.startDate
            durationDays = if (s.endless) null else days
        }
        // Przy godzinach "po lekcjach" zapisujemy też godzinę pierwszego dnia – jako zapas.
        val times = if (s.usesAfterSchool) listOf(s.schoolTimes.afterSchoolTime(startDate)) else s.times.sorted()

        val base = original ?: Reminder(
            title = title,
            folderId = s.folderId,
            startDate = startDate,
            durationDays = durationDays,
            times = times,
        )
        val reminder = base.copy(
            title = title,
            note = s.note.trim(),
            folderId = s.folderId,
            startDate = startDate,
            durationDays = durationDays,
            times = times,
            schoolType = if (s.isSchoolFolder) s.schoolType?.name else null,
            eventDate = if (s.isEventMode) s.eventDate else null,
            afterSchool = s.usesAfterSchool,
            // Wpis szkolny z terminem przypomina codziennie przed wydarzeniem.
            daysOfWeek = if (s.isEventMode) 0 else s.daysOfWeek,
        )

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.saveReminder(reminder)
            _state.update { it.copy(isSaving = false, done = true) }
        }
    }

    fun delete() {
        val reminder = original ?: return
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            _state.update { it.copy(done = true) }
        }
    }
}
