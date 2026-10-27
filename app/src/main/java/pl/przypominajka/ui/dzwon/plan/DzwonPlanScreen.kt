@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.dzwon.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.TimetableLesson
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.plan.PlanViewModel
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.dayLetters
import pl.przypominajka.util.formatLongDate
import pl.przypominajka.util.formatWeekRange
import pl.przypominajka.util.pluralPl
import java.time.LocalDate
import java.time.LocalTime

/** Plan w arkuszu Dzwonu: wybór tygodnia, dni i lekcje na złotej osi. */
@Composable
fun DzwonPlanScreen(
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    val today = LocalDate.now()
    val now = LocalTime.now()
    val lessons = state.lessonsOfSelectedDay
    val active = lessons.filter { !it.canceled }

    DzwonSheetScaffold(
        title = "Plan lekcji",
        subtitle = formatWeekRange(state.weekStart, state.weekStart.plusDays(4)),
        onClose = onBack,
        headerExtra = { DzwonBell(size = 52.dp) },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("week") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DzwonRoundIconButton(Icons.Rounded.ChevronLeft, "Poprzedni tydzień", { viewModel.changeWeek(-1) })
                    Text("Wybierz dzień", style = DzwonType.SectionTitle, color = c.ink,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    DzwonRoundIconButton(Icons.Rounded.ChevronRight, "Następny tydzień", { viewModel.changeWeek(1) })
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.days, key = { it.toEpochDay() }) { day ->
                        val events = state.events[day].orEmpty().isNotEmpty()
                        DzwonChip(
                            text = "${dayLetters(day)} ${day.dayOfMonth}" +
                                (if (day == today) " · dziś" else "") + (if (events) " · wydarzenia" else ""),
                            selected = day == state.selectedDay,
                            onClick = { viewModel.selectDay(day) },
                        )
                    }
                }
            }
            if (!state.librusConnected && !state.hasAnyTimetable) {
                item("connect") {
                    DzwonNotice(
                        title = "Plan prosto z Librusa",
                        text = "Połącz konto Librus Synergia w zakładce Konto, a plan lekcji pojawi się tutaj. " +
                            "Do tego czasu przypomnienia „po lekcjach” korzystają z godzin ustawionych w Koncie.",
                        icon = DzwonIcons.Calendar,
                        actions = { DzwonPrimaryButton("Przejdź do Konta", onOpenAccount) },
                    )
                }
            }
            item("summary") {
                DzwonCard(color = c.field, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                    Text(formatLongDate(state.selectedDay).replaceFirstChar { it.uppercase() },
                        style = DzwonType.SectionTitle, color = c.ink)
                    Spacer(Modifier.height(6.dp))
                    val summary = when {
                        active.isNotEmpty() -> "${active.size} ${pluralPl(active.size, "lekcja", "lekcje", "lekcji")}, " +
                            "${active.minOf { it.startTime }.format(TimeFormatter)}–${active.maxOf { it.endTime }.format(TimeFormatter)}"
                        state.schoolTimes.lessonsEnd(state.selectedDay) != null ->
                            "Według ustawień lekcje do ${state.schoolTimes.lessonsEnd(state.selectedDay)?.format(TimeFormatter)}"
                        else -> "Dzień bez lekcji"
                    }
                    Text(summary, style = DzwonType.Body, color = c.ink2)
                    Text("Przypomnienia „po lekcjach”: ${state.schoolTimes.afterSchoolTime(state.selectedDay).format(TimeFormatter)}",
                        style = DzwonType.CaptionStrong, color = c.goldText, modifier = Modifier.padding(top = 10.dp))
                }
            }
            items(state.events[state.selectedDay].orEmpty(), key = { "event_${it.reminderId}" }) { event ->
                DzwonCard(onClick = { onOpenReminder(event.reminderId) }) {
                    DzwonListRow(title = event.title, subtitle = event.type.label,
                        trailing = { DzwonRoundIconButton(DzwonIcons.ChevronRight, "Otwórz przypomnienie: ${event.title}",
                            { onOpenReminder(event.reminderId) }) })
                }
            }
            if (lessons.isEmpty()) {
                item("empty") {
                    DzwonEmptyState(
                        title = if (state.librusConnected) "Brak lekcji w planie na ten dzień" else "Brak planu lekcji",
                        text = if (state.librusConnected) "Możesz pobrać aktualny plan z Librusa." else "Połącz konto, aby zobaczyć lekcje.",
                        action = if (state.librusConnected) ({
                            if (state.isSyncing) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = c.gold, modifier = Modifier.size(28.dp))
                                    Text("Pobieranie planu…", style = DzwonType.Caption, color = c.ink2)
                                }
                            } else DzwonPrimaryButton("Pobierz plan z Librusa", viewModel::sync)
                        }) else null,
                    )
                }
            } else {
                items(lessons, key = { "lesson_${it.id}" }) { lesson ->
                    LessonRow(lesson,
                        isNow = state.selectedDay == today && !lesson.canceled && now >= lesson.startTime && now < lesson.endTime,
                        isPast = state.selectedDay == today && now >= lesson.endTime)
                }
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: TimetableLesson, isNow: Boolean, isPast: Boolean) {
    val c = LookTheme.dzwon
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(lesson.startTime.format(TimeFormatter), style = DzwonType.TimeLarge, color = if (isNow) c.goldText else c.ink)
            Text(lesson.endTime.format(TimeFormatter), style = DzwonType.Caption, color = c.ink2)
        }
        Box(Modifier.padding(horizontal = 10.dp).width(2.dp).height(64.dp).background(if (isNow) c.gold else c.track))
        DzwonCard(
            modifier = Modifier.weight(1f).then(if (isNow) Modifier.border(2.dp, c.gold, RoundedCornerShape(26.dp)) else Modifier),
            color = if (isNow) c.field else c.surface,
            contentPadding = PaddingValues(16.dp),
        ) {
            Text("${lesson.lessonNo}. ${lesson.subject.ifBlank { "Lekcja" }}", style = DzwonType.RowTitle,
                color = if (lesson.canceled || isPast) c.ink2 else c.ink,
                textDecoration = if (lesson.canceled) TextDecoration.LineThrough else null)
            val details = listOf(lesson.teacher, lesson.classroom.let { if (it.isBlank()) "" else "sala $it" })
                .filter { it.isNotBlank() }.joinToString(" · ")
            if (details.isNotEmpty()) Text(details, style = DzwonType.Caption, color = c.ink2)
            val status = when {
                lesson.canceled -> "Odwołana"
                isNow -> "Teraz"
                lesson.substitution -> "Zastępstwo"
                else -> null
            }
            if (status != null) Text(status, style = DzwonType.CaptionStrong, color = c.goldText,
                modifier = Modifier.padding(top = 6.dp))
        }
    }
}
