package pl.przypominajka.ui.plan

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.TimetableLesson
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.LocalBottomBarPadding
import pl.przypominajka.ui.components.NoticeCard
import pl.przypominajka.ui.components.PastelBadge
import pl.przypominajka.ui.components.Pill
import pl.przypominajka.ui.components.PrimaryButton
import pl.przypominajka.ui.components.RoundIconButton
import pl.przypominajka.ui.components.ScreenHeader
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.floating
import pl.przypominajka.ui.components.icon
import pl.przypominajka.ui.components.pastel
import pl.przypominajka.ui.components.pressScale
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.Pastel
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.dayLetters
import pl.przypominajka.util.formatLongDate
import pl.przypominajka.util.formatWeekRange
import pl.przypominajka.util.pluralPl
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val now = LocalTime.now()
    val lessons = state.lessonsOfSelectedDay
    val dayEvents = state.events[state.selectedDay].orEmpty()

    Scaffold(
        containerColor = AppSurfaces.page,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            BellTopBar(
                title = "Plan lekcji",
                eyebrow = formatWeekRange(state.weekStart, state.weekStart.plusDays(4)),
                onBack = onBack,
            ) {
                RoundIconButton(
                    icon = Icons.Rounded.ChevronLeft,
                    contentDescription = "Poprzedni tydzień",
                    onClick = { viewModel.changeWeek(-1) },
                )
                RoundIconButton(
                    icon = Icons.Rounded.ChevronRight,
                    contentDescription = "Następny tydzień",
                    onClick = { viewModel.changeWeek(1) },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 4.dp, bottom = LocalBottomBarPadding.current + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            item(key = "days") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .appear(1),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.days.forEach { day ->
                        DayChip(
                            day = day,
                            selected = day == state.selectedDay,
                            isToday = day == today,
                            hasEvent = state.events[day].orEmpty().isNotEmpty(),
                            onClick = { viewModel.selectDay(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (!state.librusConnected && !state.hasAnyTimetable) {
                item(key = "connect") {
                    NoticeCard(
                        title = "Plan prosto z Librusa",
                        body = "Połącz konto Librus Synergia w zakładce Konto, a plan lekcji pojawi się tutaj. " +
                            "Do tego czasu przypomnienia „po lekcjach” korzystają z godzin ustawionych w Koncie.",
                        containerColor = Pastel.GOLD.container,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        icon = Icons.Rounded.CalendarMonth,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .appear(2),
                        actions = { PrimaryButton(text = "Przejdź do Konta", onClick = onOpenAccount) },
                    )
                }
            }

            item(key = "summary_${state.selectedDay}") {
                DaySummary(
                    day = state.selectedDay,
                    lessons = lessons,
                    schoolTimes = state.schoolTimes,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem()
                        .appear(2),
                )
            }

            itemsIndexed(items = dayEvents, key = { _, event -> "event_${event.reminderId}" }) { index, event ->
                val pastel = event.type.pastel
                AppCard(
                    onClick = { onOpenReminder(event.reminderId) },
                    color = pastel.container,
                    border = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateItem()
                        .appear(3 + index),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(pastel.content),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(event.type.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(event.type.label, style = MaterialTheme.typography.labelMedium, color = pastel.content)
                            Text(event.title, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            if (lessons.isEmpty()) {
                item(key = "empty_${state.selectedDay}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 28.dp)
                            .animateItem()
                            .appear(3),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PastelBadge(
                            icon = Icons.Rounded.EventBusy,
                            pastel = Pastel.DUSK,
                            size = 76.dp,
                            modifier = Modifier.floating(),
                        )
                        Text(
                            text = if (state.librusConnected) "Brak lekcji w planie na ten dzień" else "Brak planu lekcji",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        if (state.librusConnected) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            } else {
                                PrimaryButton(text = "Pobierz plan z Librusa", icon = Icons.Rounded.Sync, onClick = viewModel::sync)
                            }
                        }
                    }
                }
            } else {
                val isToday = state.selectedDay == today
                itemsIndexed(items = lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                    LessonRow(
                        lesson = lesson,
                        isNow = isToday && !lesson.canceled && now >= lesson.startTime && now < lesson.endTime,
                        isPast = isToday && now >= lesson.endTime,
                        isLast = index == lessons.lastIndex,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem()
                            .appear(4 + index),
                    )
                }
            }
        }
    }
}

/** Dzień tygodnia – wybrany zmienia się płynnie w ciemną kapsułę, dziś ma koralową kropkę. */
@Composable
private fun DayChip(
    day: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.primary else AppSurfaces.card, label = "dayChip")
    val content by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface, label = "dayChipText")
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, AppSurfaces.cardBorder),
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction, pressedScale = 0.9f),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = dayLetters(day),
                style = MaterialTheme.typography.labelSmall,
                color = if (isToday && !selected) AppSurfaces.accent else Color.Unspecified,
            )
            Text(text = day.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasEvent -> Pastel.TERRACOTTA.content
                            isToday -> AppSurfaces.accent
                            else -> Color.Transparent
                        },
                    ),
            )
        }
    }
}

@Composable
private fun DaySummary(
    day: LocalDate,
    lessons: List<TimetableLesson>,
    schoolTimes: SchoolTimes,
    modifier: Modifier = Modifier,
) {
    val active = lessons.filter { !it.canceled }
    val text = when {
        active.isNotEmpty() -> {
            val count = active.size
            "$count ${pluralPl(count, "lekcja", "lekcje", "lekcji")}, " +
                "${active.minOf { it.startTime }.format(TimeFormatter)}–${active.maxOf { it.endTime }.format(TimeFormatter)}"
        }
        schoolTimes.lessonsEnd(day) != null -> "Według ustawień lekcje do ${schoolTimes.lessonsEnd(day)?.format(TimeFormatter)}"
        else -> "Dzień bez lekcji"
    }
    AppCard(modifier = modifier.fillMaxWidth(), color = Pastel.GOLD.container, border = null) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = formatLongDate(day).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = CircleShape, color = Pastel.GOLD.content.copy(alpha = 0.14f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = Pastel.GOLD.content,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Przypomnienia „po lekcjach”: ${schoolTimes.afterSchoolTime(day).format(TimeFormatter)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Pastel.GOLD.content,
                    )
                }
            }
        }
    }
}

/** Lekcja na osi czasu: godziny, kropka na linii i karta. Trwająca teraz – z koralowym obramowaniem. */
@Composable
private fun LessonRow(
    lesson: TimetableLesson,
    isNow: Boolean,
    isPast: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val accent = AppSurfaces.accent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(48.dp), horizontalAlignment = Alignment.End) {
            Text(
                text = lesson.startTime.format(TimeFormatter),
                style = MaterialTheme.typography.labelLarge,
                color = if (isNow) accent else colors.onSurface,
            )
            Text(
                text = lesson.endTime.format(TimeFormatter),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
        // Oś czasu.
        Box(
            modifier = Modifier
                .width(26.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight(if (isLast) 0.5f else 1f)
                    .align(if (isLast) Alignment.TopCenter else Alignment.Center)
                    .background(colors.outlineVariant),
            )
            Box(
                modifier = Modifier
                    .size(if (isNow) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isNow -> accent
                            lesson.canceled -> colors.outline
                            isPast -> colors.outline
                            else -> colors.onSurface
                        },
                    ),
            )
        }
        AppCard(
            color = when {
                lesson.canceled -> colors.surfaceVariant.copy(alpha = 0.5f)
                isNow -> colors.tertiaryContainer
                else -> AppSurfaces.card
            },
            border = BorderStroke(if (isNow) 2.dp else 1.dp, if (isNow) accent else AppSurfaces.cardBorder),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 5.dp),
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (lesson.canceled) colors.outlineVariant else Pastel.DUSK.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = lesson.lessonNo.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (lesson.canceled) colors.onSurfaceVariant else Pastel.DUSK.content,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = lesson.subject.ifBlank { "Lekcja" },
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (lesson.canceled) TextDecoration.LineThrough else null,
                        color = if (lesson.canceled || isPast) colors.onSurfaceVariant else colors.onSurface,
                    )
                    val details = listOf(lesson.teacher, lesson.classroom.let { if (it.isBlank()) "" else "sala $it" })
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (details.isNotEmpty()) {
                        Text(details, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                when {
                    lesson.canceled -> Pill("odwołana", colors.errorContainer, colors.onErrorContainer)
                    isNow -> Pill("teraz", accent, Color.White)
                    lesson.substitution -> Pill("zastępstwo", colors.tertiaryContainer, colors.onTertiaryContainer)
                }
            }
        }
    }
}
