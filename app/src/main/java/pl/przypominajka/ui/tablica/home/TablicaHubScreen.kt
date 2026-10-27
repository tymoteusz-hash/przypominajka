package pl.przypominajka.ui.tablica.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.alarm.TrainingTrigger
import pl.przypominajka.data.ChecklistItem
import pl.przypominajka.data.TimetableLesson
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.checklist.ChecklistSummary
import pl.przypominajka.ui.checklist.ChecklistViewModel
import pl.przypominajka.ui.home.HomeViewModel
import pl.przypominajka.ui.home.TodaySummary
import pl.przypominajka.ui.home.UpcomingEvent
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.ui.plan.PlanUiState
import pl.przypominajka.ui.plan.PlanViewModel
import pl.przypominajka.ui.tablica.components.BoardHeader
import pl.przypominajka.ui.tablica.components.InkButton
import pl.przypominajka.ui.tablica.components.InkTextButton
import pl.przypominajka.ui.tablica.components.LocalDockPadding
import pl.przypominajka.ui.tablica.components.NewNoteTile
import pl.przypominajka.ui.tablica.components.Note
import pl.przypominajka.ui.tablica.components.NoteDecoration
import pl.przypominajka.ui.tablica.components.NoteSquareCheck
import pl.przypominajka.ui.tablica.components.RoundIconButton
import pl.przypominajka.ui.tablica.components.TablicaIcons
import pl.przypominajka.ui.tablica.components.TablicaPermissionNotes
import pl.przypominajka.ui.tablica.components.TablicaScreen
import pl.przypominajka.ui.tablica.components.TablicaType
import pl.przypominajka.ui.tablica.components.rememberMinuteClock
import pl.przypominajka.ui.workout.TrainingUi
import pl.przypominajka.ui.workout.WorkoutUiState
import pl.przypominajka.ui.workout.WorkoutViewModel
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.dayName
import pl.przypominajka.util.formatLongDate
import pl.przypominajka.util.pluralPl
import pl.przypominajka.util.remindersText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Ile punktów listy pokazać na karteczce „Lista”. */
private const val LIST_PREVIEW_SIZE = 4

/**
 * Strona główna Tablicy: nazwa dnia odręcznie, a pod nią tablica z karteczkami w dwóch przesuniętych
 * kolumnach – Dziś, Plan, Trening (lewa) oraz Lista, najbliższy sprawdzian / Quizy i „Nowa karteczka”
 * (prawa). Karteczki prowadzą do działów; punkty listy można odhaczać od razu.
 *
 * @param onOpenTab przejście do działu (trasa z [Routes]) – jak zakładka doku.
 * @param onNewNote okno szybkiego dopisania karteczki (prowadzi je powłoka).
 */
@Composable
fun TablicaHubScreen(
    onOpenTab: (route: String) -> Unit,
    onNewNote: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
    checklistViewModel: ChecklistViewModel = viewModel(factory = AppViewModelProvider.Factory),
    planViewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory),
    workoutViewModel: WorkoutViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    val checklist by checklistViewModel.items.collectAsStateWithLifecycle()
    val checklistSummary by checklistViewModel.summary.collectAsStateWithLifecycle()
    val plan by planViewModel.state.collectAsStateWithLifecycle()
    val training by workoutViewModel.training.collectAsStateWithLifecycle()
    val workout by workoutViewModel.state.collectAsStateWithLifecycle()
    val now by rememberMinuteClock()
    val today = now.toLocalDate()
    // Punkty odhaczone tutaj zostają na karteczce (przekreślone), żeby nie uciekały spod palca.
    val touched = remember { mutableStateListOf<Long>() }

    TablicaScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = LocalDockPadding.current + 12.dp),
        ) {
            BoardHeader(
                title = dayName(today).replaceFirstChar { it.uppercase() },
                subtitle = "${formatLongDate(today).substringAfter(", ")} · ${todayCountText(state.today.remainingToday)}",
                modifier = Modifier.padding(start = 20.dp, end = 14.dp, top = 18.dp),
                trailing = {
                    RoundIconButton(
                        icon = TablicaIcons.Account,
                        contentDescription = "Konto",
                        onClick = { onOpenTab(Routes.ACCOUNT) },
                    )
                },
            )

            TablicaPermissionNotes(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 26.dp),
                askOnFirstRun = true,
            )

            Spacer(Modifier.height(30.dp))

            // Dwie kolumny karteczek; prawa zaczyna się ~8 dp wyżej – tablica wygląda na przypinaną ręcznie.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    TodayNote(today = state.today, onClick = { onOpenTab(Routes.REMINDERS) })
                    LinesNote(
                        title = "Plan",
                        lines = planLines(plan, now),
                        color = LookTheme.tablica.blue,
                        tilt = 1.2f,
                        onClickLabel = "Plan lekcji",
                        onClick = { onOpenTab(Routes.PLAN) },
                    )
                    LinesNote(
                        title = "Trening",
                        lines = trainingLines(training, workout, today),
                        color = LookTheme.tablica.lilac,
                        tilt = -1f,
                        onClickLabel = "Trening",
                        onClick = { onOpenTab(Routes.WORKOUT) },
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ListNote(
                        items = checklist,
                        summary = checklistSummary,
                        touched = touched,
                        onToggle = { id ->
                            if (id !in touched) touched.add(id)
                            checklistViewModel.toggle(id)
                        },
                        onOpen = { onOpenTab(Routes.CHECKLIST) },
                    )
                    val test = state.upcoming.firstOrNull { it.type.supportsQuiz }
                    if (test != null) {
                        TestNote(event = test, today = today, onClick = { onOpenTab(Routes.QUIZZES) })
                    } else {
                        QuizzesNote(onClick = { onOpenTab(Routes.QUIZZES) })
                    }
                    NewNoteTile(onClick = onNewNote, modifier = Modifier.fillMaxWidth())
                }
            }

            if (state.showLibrusPrompt) {
                LibrusNote(
                    onConnect = { onOpenTab(Routes.ACCOUNT) },
                    onDismiss = homeViewModel::dismissLibrusPrompt,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 34.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- karteczki

/** „Dziś”: dzisiejsze przypomnienia z godzinami i link do wszystkich. */
@Composable
private fun TodayNote(today: TodaySummary, onClick: () -> Unit) {
    Note(
        color = LookTheme.tablica.yellow,
        tilt = -2f,
        decoration = NoteDecoration.Tape,
        onClick = onClick,
        onClickLabel = "Wszystkie przypomnienia",
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Dziś", style = TablicaType.NoteTitle, modifier = Modifier.semantics { heading() })
        if (today.agenda.isEmpty()) {
            Text(text = if (today.totalToday > 0) "Na dziś wszystko za Tobą" else "Nic na dziś", style = TablicaType.Body)
        }
        today.agenda.forEach { entry ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = entry.time.format(TimeFormatter), style = TablicaType.Time)
                Text(text = entry.title, style = TablicaType.Body, modifier = Modifier.weight(1f))
            }
        }
        val more = today.remainingToday - today.agenda.size
        if (more > 0) Text(text = "i jeszcze $more", style = TablicaType.Hint)
        Spacer(Modifier.weight(1f))
        Text(text = "Wszystkie przypomnienia", style = TablicaType.Link)
    }
}

/** Dwa wiersze karteczki: wyróżniony i opis. */
private data class NoteLines(val headline: String, val detail: String?)

/** Karteczka z tytułem i dwoma wierszami (Plan, Trening). */
@Composable
private fun LinesNote(
    title: String,
    lines: NoteLines,
    color: NoteColor,
    tilt: Float,
    onClickLabel: String,
    onClick: () -> Unit,
) {
    Note(
        color = color,
        tilt = tilt,
        decoration = NoteDecoration.Tape,
        onClick = onClick,
        onClickLabel = onClickLabel,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = TablicaType.NoteTitle.copy(fontSize = 30.sp, lineHeight = 31.sp),
            modifier = Modifier
                .padding(bottom = 2.dp)
                .semantics { heading() },
        )
        Text(text = lines.headline, style = TablicaType.Strong)
        if (lines.detail != null) Text(text = lines.detail, style = TablicaType.Hint)
    }
}

/** „Lista”: do 4 nieodhaczonych punktów z kwadracikami (naprawdę odhaczają) i „+ N zrobione”. */
@Composable
private fun ListNote(
    items: List<ChecklistItem>,
    summary: ChecklistSummary,
    touched: List<Long>,
    onToggle: (Long) -> Unit,
    onOpen: () -> Unit,
) {
    val shown = items.filter { !it.done || it.id in touched }.take(LIST_PREVIEW_SIZE)
    val hiddenLeft = summary.left - shown.count { !it.done }
    val hiddenDone = summary.done - shown.count { it.done }
    Note(
        color = LookTheme.tablica.mint,
        tilt = 1.6f,
        decoration = NoteDecoration.Pin,
        onClick = onOpen,
        onClickLabel = "Otwórz listę",
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 170.dp),
    ) {
        Text(
            text = "Lista",
            style = TablicaType.NoteTitle,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .semantics { heading() },
        )
        if (shown.isEmpty()) {
            Text(
                text = if (summary.total == 0) "Pusto. Przyklej coś!" else "Wszystko zrobione!",
                style = TablicaType.Body,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        shown.forEach { item -> ListPreviewRow(item = item, onToggle = { onToggle(item.id) }) }
        Spacer(Modifier.weight(1f))
        val plain = TablicaType.Link.copy(textDecoration = TextDecoration.None)
        if (hiddenLeft > 0) Text(text = "+ $hiddenLeft do zrobienia", style = plain, modifier = Modifier.padding(top = 4.dp))
        if (hiddenDone > 0) {
            Text(
                text = "+ $hiddenDone ${pluralPl(hiddenDone, "zrobiony", "zrobione", "zrobionych")}",
                style = plain,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ListPreviewRow(item: ChecklistItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .toggleable(value = item.done, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoteSquareCheck(checked = item.done)
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.text,
            style = TablicaType.Body,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            modifier = Modifier
                .weight(1f)
                .alpha(if (item.done) 0.55f else 1f),
        )
    }
}

/** Najbliższy sprawdzian / kartkówka z Librusa – różowa karteczka na pinezce („Matma!”). */
@Composable
private fun TestNote(event: UpcomingEvent, today: LocalDate, onClick: () -> Unit) {
    val title = event.reminder.title.trim()
    Note(
        color = LookTheme.tablica.pink,
        tilt = -2.4f,
        decoration = NoteDecoration.Pin,
        onClick = onClick,
        onClickLabel = "Quizy",
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (title.endsWith("!") || title.endsWith("?") || title.endsWith(".")) title else "$title!",
            style = TablicaType.NoteTitle,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = "${event.type.label.lowercase()} ${dayPhrase(event.date, today)}", style = TablicaType.Body)
        Spacer(Modifier.weight(1f))
        Text(text = "Powtórz w quizie", style = TablicaType.Link)
    }
}

/** Zamiast sprawdzianu (gdy żadnego nie ma) – karteczka „Quizy”. */
@Composable
private fun QuizzesNote(onClick: () -> Unit) {
    Note(
        color = LookTheme.tablica.pink,
        tilt = -2.4f,
        decoration = NoteDecoration.Pin,
        onClick = onClick,
        onClickLabel = "Quizy",
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "Quizy", style = TablicaType.NoteTitle, modifier = Modifier.semantics { heading() })
        Text(text = "Powtórka przed sprawdzianem", style = TablicaType.Body)
        Spacer(Modifier.weight(1f))
        Text(text = "Otwórz quizy", style = TablicaType.Link)
    }
}

/** Zachęta do połączenia z Librusem (dopóki nie połączono albo nie odrzucono). */
@Composable
private fun LibrusNote(onConnect: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Note(
        color = LookTheme.tablica.yellow,
        tilt = 0.8f,
        decoration = NoteDecoration.Tape,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Masz Librusa?",
            style = TablicaType.NoteTitle.copy(fontSize = 30.sp, lineHeight = 31.sp),
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Połącz konto Synergii – sprawdziany, kartkówki i plan lekcji pojawią się same, " +
                "a przypomnienia przyjdą po lekcjach.",
            style = TablicaType.BodyLight,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InkTextButton(text = "Nie, dzięki", onClick = onDismiss)
            InkButton(text = "Połącz", onClick = onConnect)
        }
    }
}

// ---------------------------------------------------------------- teksty

private val DAY_ACCUSATIVE = listOf(
    "w poniedziałek", "we wtorek", "w środę", "w czwartek", "w piątek", "w sobotę", "w niedzielę",
)

/** „dziś”, „jutro”, „w środę”, „za 9 dni”. */
private fun dayPhrase(date: LocalDate, today: LocalDate): String =
    when (ChronoUnit.DAYS.between(today, date)) {
        in 2L..6L -> DAY_ACCUSATIVE[date.dayOfWeek.value - 1]
        else -> countdownText(date, today)
    }

private fun todayCountText(count: Int): String = if (count == 0) "nic na dziś" else remindersText(count)

/** Plan: trwająca lekcja, następna dziś albo pierwsza w kolejny dzień (z wczytanego tygodnia). */
private fun planLines(plan: PlanUiState, now: LocalDateTime): NoteLines {
    if (!plan.hasAnyTimetable) {
        return NoteLines(
            headline = "Brak planu",
            detail = if (plan.librusConnected) "Pobierz go w dziale Plan" else "Połącz Librusa w Koncie",
        )
    }
    val today = now.toLocalDate()
    val time = now.toLocalTime()
    val lessons = plan.lessons
        .filter { !it.canceled }
        .sortedWith(compareBy({ it.date }, { it.startTime }))
    fun detail(lesson: TimetableLesson): String = buildString {
        append("${lesson.startTime.format(TimeFormatter)}–${lesson.endTime.format(TimeFormatter)}")
        if (lesson.classroom.isNotBlank()) append(" · sala ${lesson.classroom}")
    }
    lessons.firstOrNull { it.date == today && !time.isBefore(it.startTime) && time.isBefore(it.endTime) }?.let {
        return NoteLines("teraz: ${it.subject}", detail(it))
    }
    lessons.firstOrNull { it.date == today && it.startTime.isAfter(time) }?.let {
        return NoteLines("potem: ${it.subject}", detail(it))
    }
    lessons.firstOrNull { it.date.isAfter(today) }?.let {
        val day = if (it.date == today.plusDays(1)) "jutro" else dayName(it.date)
        return NoteLines("$day: ${it.subject}", detail(it))
    }
    return NoteLines("Koniec lekcji", "w tym tygodniu")
}

/** Trening: plan w toku (albo pierwszy) i kiedy przypomnienie. */
private fun trainingLines(training: TrainingUi, workout: WorkoutUiState, today: LocalDate): NoteLines {
    val plans = workout.plans
    val current = plans.firstOrNull { it.total > 0 && it.doneCount in 1 until it.total }
        ?: plans.firstOrNull { it.doneCount < it.total }
        ?: plans.firstOrNull()
    val next = training.next
    val detail = when {
        next != null -> {
            val day = dayPhrase(next.at.toLocalDate(), today)
            if (next.source == TrainingTrigger.Source.LESSONS) {
                "$day po lekcjach"
            } else {
                "$day o ${next.at.toLocalTime().format(TimeFormatter)}"
            }
        }
        current != null && current.total > 0 ->
            "${current.doneCount} z ${current.total} ${pluralPl(current.total, "ćwiczenia", "ćwiczeń", "ćwiczeń")}"
        current != null -> "Dodaj ćwiczenia"
        else -> "Ułóż plan ćwiczeń"
    }
    return NoteLines(headline = current?.plan?.name ?: "Twój trening", detail = detail)
}
