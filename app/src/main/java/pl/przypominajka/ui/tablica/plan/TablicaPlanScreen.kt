@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.TimetableLesson
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.plan.PlanViewModel
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.util.*

/** Tydzień przypięty do tablicy, wydarzenia i lekcje na osobnych karteczkach. */
@Composable
fun TablicaPlanScreen(
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clock by rememberMinuteClock()
    val colors = LookTheme.tablica
    val lessons = state.lessonsOfSelectedDay
    TablicaScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = dockContentPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("header") {
                BoardHeader("Plan lekcji", subtitle = formatWeekRange(state.weekStart, state.weekStart.plusDays(4))) {
                    RoundIconButton(TablicaIcons.Back, "Wróć", onBack)
                }
            }
            item("week") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.Rounded.ChevronLeft, "Poprzedni tydzień", { viewModel.changeWeek(-1) })
                    Text("Wybierz dzień", style = TablicaType.Section, modifier = Modifier.weight(1f))
                    RoundIconButton(Icons.Rounded.ChevronRight, "Następny tydzień", { viewModel.changeWeek(1) })
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.days, key = { it.toString() }) { day ->
                        val today = day == clock.toLocalDate()
                        val event = state.events[day].orEmpty().isNotEmpty()
                        TablicaChip(
                            text = "${dayLetters(day)} ${day.dayOfMonth}" +
                                (if (today) " · dziś" else "") + (if (event) " · wydarzenie" else ""),
                            selected = day == state.selectedDay,
                            onClick = { viewModel.selectDay(day) },
                        )
                    }
                }
            }
            if (!state.librusConnected && !state.hasAnyTimetable) {
                item("connect") {
                    Note(colors.blue, modifier = Modifier.fillMaxWidth(), decoration = NoteDecoration.Tape) {
                        Text("Plan prosto z Librusa", style = TablicaType.NoteTitle)
                        Text("Połącz konto Librus Synergia w zakładce Konto, a plan lekcji pojawi się tutaj. Do tego czasu przypomnienia „po lekcjach” korzystają z godzin ustawionych w Koncie.", style = TablicaType.BodyLight)
                        InkButton("Przejdź do Konta", onOpenAccount)
                    }
                }
            }
            item("summary") {
                val active = lessons.filter { !it.canceled }
                Note(colors.yellow, tilt = -0.7f, decoration = NoteDecoration.Pin, modifier = Modifier.fillMaxWidth()) {
                    Text(formatLongDate(state.selectedDay).replaceFirstChar { it.uppercase() }, style = TablicaType.NoteTitle)
                    Text(when {
                        active.isNotEmpty() -> "${active.size} ${pluralPl(active.size, "lekcja", "lekcje", "lekcji")}, " +
                            "${active.minOf { it.startTime }.format(TimeFormatter)}–${active.maxOf { it.endTime }.format(TimeFormatter)}"
                        state.schoolTimes.lessonsEnd(state.selectedDay) != null -> "Według ustawień lekcje do ${state.schoolTimes.lessonsEnd(state.selectedDay)?.format(TimeFormatter)}"
                        else -> "Dzień bez lekcji"
                    }, style = TablicaType.Body)
                    NoteDivider()
                    Text("Przypomnienia „po lekcjach”: ${state.schoolTimes.afterSchoolTime(state.selectedDay).format(TimeFormatter)}", style = TablicaType.Hint)
                }
            }
            items(state.events[state.selectedDay].orEmpty(), key = { "event_${it.reminderId}" }) { event ->
                Note(colors.pink, onClick = { onOpenReminder(event.reminderId) }, modifier = Modifier.fillMaxWidth()) {
                    Text(event.type.label, style = TablicaType.Tag)
                    Text(event.title, style = TablicaType.NoteText)
                    Text("Otwórz przypomnienie", style = TablicaType.Link)
                }
            }
            if (lessons.isEmpty()) {
                item("empty") {
                    BoardEmptyState(
                        title = if (state.librusConnected) "Brak lekcji w planie na ten dzień" else "Brak planu lekcji",
                        text = "Wybierz inny dzień lub tydzień, aby zobaczyć lekcje.",
                        action = if (state.librusConnected) ({
                            if (state.isSyncing) CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            else InkButton("Pobierz plan z Librusa", viewModel::sync)
                        }) else null,
                    )
                }
            }
            items(lessons, key = { "lesson_${it.id}" }) { lesson ->
                val isToday = state.selectedDay == clock.toLocalDate()
                val isNow = isToday && !lesson.canceled && clock.toLocalTime() >= lesson.startTime && clock.toLocalTime() < lesson.endTime
                LessonNote(lesson, isNow, isToday && clock.toLocalTime() >= lesson.endTime)
            }
        }
    }
}

@Composable
private fun LessonNote(lesson: TimetableLesson, isNow: Boolean, isPast: Boolean) {
    val colors = LookTheme.tablica
    Note(
        color = when { lesson.canceled -> colors.lilac; isNow -> colors.mint; else -> colors.blue },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${lesson.lessonNo}.  ${lesson.startTime.format(TimeFormatter)}–${lesson.endTime.format(TimeFormatter)}", style = TablicaType.Time, modifier = Modifier.weight(1f))
            when {
                lesson.canceled -> NoteBadge("odwołana")
                isNow -> NoteBadge("teraz")
                lesson.substitution -> NoteBadge("zastępstwo")
                isPast -> NoteBadge("zakończona")
            }
        }
        Text(lesson.subject.ifBlank { "Lekcja" }, style = TablicaType.NoteTitle,
            textDecoration = if (lesson.canceled) TextDecoration.LineThrough else null)
        val details = listOf(lesson.teacher, lesson.classroom.takeIf { it.isNotBlank() }?.let { "sala $it" }.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
        if (details.isNotEmpty()) Text(details, style = TablicaType.BodyLight)
    }
}
