@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import pl.przypominajka.ui.kartka.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.plan.PlanViewModel
import pl.przypominajka.util.dayLetters
import pl.przypominajka.util.formatLongDate
import pl.przypominajka.util.formatWeekRange
import pl.przypominajka.util.pluralPl
import java.time.Instant
import java.time.ZoneId

/** Plan Kartki: wybór dni, wydarzenia i lekcje rozdzielone cienkimi liniami. */
@Composable
fun KartkaPlanScreen(
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clock by rememberMinuteClock()
    val now = Instant.ofEpochMilli(clock).atZone(ZoneId.systemDefault())
    val lessons = state.lessonsOfSelectedDay
    val active = lessons.filter { !it.canceled }
    val c = LookTheme.kartka
    KartkaScaffold(onBack = onBack, actions = {
        KartkaIconButton(KartkaIcons.Back, "Poprzedni tydzień", { viewModel.changeWeek(-1) })
        KartkaIconButton(KartkaIcons.ChevronRight, "Następny tydzień", { viewModel.changeWeek(1) })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding.withExtraBottom(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("title") { KartkaPageTitle("Plan lekcji", subtitle = formatWeekRange(state.weekStart, state.weekStart.plusDays(4))) }
            item("days") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = KartkaDefaults.PagePadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.days, key = { it.toEpochDay() }) { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            KartkaFilterChip("${dayLetters(day)} ${day.dayOfMonth}", day == state.selectedDay, { viewModel.selectDay(day) })
                            Text(
                                buildList {
                                    if (day == now.toLocalDate()) add("dziś")
                                    if (state.events[day].orEmpty().isNotEmpty()) add("wydarzenia")
                                }.joinToString(" · "),
                                style = KartkaType.Caption, color = c.accent,
                            )
                        }
                    }
                }
            }
            if (!state.librusConnected && !state.hasAnyTimetable) {
                item("connect") {
                    KartkaNotice(
                        title = "Plan prosto z Librusa",
                        text = "Połącz konto Librus Synergia w zakładce Konto, a plan lekcji pojawi się tutaj. " +
                            "Do tego czasu przypomnienia „po lekcjach” korzystają z godzin ustawionych w Koncie.",
                        icon = KartkaIcons.Plan, actionLabel = "Przejdź do Konta", onAction = onOpenAccount,
                        modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding),
                    )
                }
            }
            item("summary") {
                KartkaCard(
                    modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KartkaEyebrow(formatLongDate(state.selectedDay))
                    Text(
                        when {
                            active.isNotEmpty() -> "${active.size} ${pluralPl(active.size, "lekcja", "lekcje", "lekcji")}, " +
                                "${active.minOf { it.startTime }.format(KartkaHourFormatter)}–${active.maxOf { it.endTime }.format(KartkaHourFormatter)}"
                            state.schoolTimes.lessonsEnd(state.selectedDay) != null ->
                                "Według ustawień lekcje do ${state.schoolTimes.lessonsEnd(state.selectedDay)?.format(KartkaHourFormatter)}"
                            else -> "Dzień bez lekcji"
                        }, style = KartkaType.Title, color = c.ink,
                    )
                    Text(
                        "Przypomnienia „po lekcjach”: ${state.schoolTimes.afterSchoolTime(state.selectedDay).format(KartkaHourFormatter)}",
                        style = KartkaType.CaptionStrong, color = c.accent,
                    )
                }
            }
            items(state.events[state.selectedDay].orEmpty(), key = { "event_${it.reminderId}" }) { event ->
                KartkaListRow(
                    title = event.title, subtitle = event.type.label,
                    leading = { KartkaChip("Wydarzenie") },
                    onClick = { onOpenReminder(event.reminderId) }, onClickLabel = "Otwórz przypomnienie",
                    modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding),
                )
            }
            if (lessons.isEmpty()) {
                item("empty") {
                    KartkaEmptyState(
                        title = if (state.librusConnected) "Brak lekcji w planie na ten dzień" else "Brak planu lekcji",
                        text = if (state.isSyncing) "Pobieranie planu z Librusa…" else "Wybierz inny dzień lub sprawdź połączenie z Librusem.",
                        action = if (state.librusConnected) {
                            { KartkaButton("Pobierz plan z Librusa", viewModel::sync, enabled = !state.isSyncing, icon = KartkaIcons.Refresh) }
                        } else null,
                    )
                }
            } else {
                items(lessons, key = { "lesson_${it.id}" }) { lesson ->
                    LessonRow(
                        lesson,
                        isNow = state.selectedDay == now.toLocalDate() && !lesson.canceled &&
                            now.toLocalTime() >= lesson.startTime && now.toLocalTime() < lesson.endTime,
                        isPast = state.selectedDay == now.toLocalDate() && now.toLocalTime() >= lesson.endTime,
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: TimetableLesson, isNow: Boolean, isPast: Boolean) {
    val c = LookTheme.kartka
    Column(Modifier.padding(horizontal = KartkaDefaults.PagePadding)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(if (isNow) c.accentSoft else c.bg, KartkaDefaults.ButtonShape)
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.width(54.dp)) {
                KartkaTimeLabel(lesson.startTime.format(KartkaHourFormatter), color = if (isNow) c.accent else c.ink)
                Text(lesson.endTime.format(KartkaHourFormatter), style = KartkaType.Caption, color = c.ink2)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${lesson.lessonNo}. ${lesson.subject.ifBlank { "Lekcja" }}", style = KartkaType.RowTitle,
                    color = if (lesson.canceled || isPast) c.ink2 else c.ink,
                    textDecoration = if (lesson.canceled) TextDecoration.LineThrough else null,
                )
                val details = listOf(lesson.teacher, lesson.classroom.let { if (it.isBlank()) "" else "sala $it" })
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (details.isNotEmpty()) Text(details, style = KartkaType.Caption, color = c.ink2)
                when {
                    lesson.canceled -> KartkaChip("Odwołana", tone = KartkaChipTone.Warning)
                    isNow -> KartkaChip("Teraz")
                    lesson.substitution -> KartkaChip("Zastępstwo", tone = KartkaChipTone.Neutral)
                }
            }
        }
        KartkaDivider()
    }
}
