package pl.przypominajka.ui.kartka.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.przypominajka.data.ChecklistItem
import pl.przypominajka.data.LIBRUS_EXTERNAL_PREFIX
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.checklist.ChecklistSummary
import pl.przypominajka.ui.checklist.ChecklistViewModel
import pl.przypominajka.ui.home.HomeUiState
import pl.przypominajka.ui.home.HomeViewModel
import pl.przypominajka.ui.home.ReminderItem
import pl.przypominajka.ui.home.TodaySummary
import pl.przypominajka.ui.kartka.components.KartkaCard
import pl.przypominajka.ui.kartka.components.KartkaCheckMark
import pl.przypominajka.ui.kartka.components.KartkaChip
import pl.przypominajka.ui.kartka.components.KartkaChipTone
import pl.przypominajka.ui.kartka.components.KartkaDefaults
import pl.przypominajka.ui.kartka.components.KartkaEyebrow
import pl.przypominajka.ui.kartka.components.KartkaHourFormatter
import pl.przypominajka.ui.kartka.components.KartkaIconButton
import pl.przypominajka.ui.kartka.components.KartkaIcons
import pl.przypominajka.ui.kartka.components.KartkaListRow
import pl.przypominajka.ui.kartka.components.KartkaNavTile
import pl.przypominajka.ui.kartka.components.KartkaNotice
import pl.przypominajka.ui.kartka.components.KartkaPermissionNotices
import pl.przypominajka.ui.kartka.components.KartkaSectionHeader
import pl.przypominajka.ui.kartka.components.KartkaTextButton
import pl.przypominajka.ui.kartka.components.KartkaTimeLabel
import pl.przypominajka.ui.kartka.components.KartkaType
import pl.przypominajka.ui.kartka.components.kartkaCountdown
import pl.przypominajka.ui.kartka.components.kartkaDateLabel
import pl.przypominajka.ui.kartka.components.kartkaDayLead
import pl.przypominajka.ui.kartka.components.kartkaWhenShort
import pl.przypominajka.ui.kartka.components.rememberMinuteClock
import pl.przypominajka.ui.kartka.components.toLocalDateTime
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.util.pointsText
import pl.przypominajka.util.remindersText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Ile wierszy z godziną (dzisiejsze przypomnienia i punkty checklisty) i ile terminów szkolnych w „Najbliżej”. */
private const val MAX_TIMED = 3
private const val MAX_UPCOMING = 2

/** Po ilu ms od dotknięcia punkt podglądu checklisty jest naprawdę odhaczany (widać ptaszek, potem znika). */
private const val PREVIEW_TOGGLE_DELAY_MS = 350L

/**
 * Strona główna Kartki (makieta Main): data, wielki nagłówek z liczbą przypomnień i punktów, „Najbliżej”,
 * podgląd checklisty z prawdziwym odhaczaniem, siatka działów i prośby o uprawnienia.
 *
 * @param onOpen przejście do trasy z [Routes] (działy, konto, ustawienia).
 */
@Composable
fun KartkaHomeScreen(
    onOpen: (route: String) -> Unit,
    onEditReminder: (reminderId: Long) -> Unit,
    onAddReminder: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
    checklistViewModel: ChecklistViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val c = LookTheme.kartka
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Pełna checklista – w HomeUiState są tylko 3 pierwsze nieodhaczone, a „Najbliżej” potrzebuje godzin.
    val checklistItems by checklistViewModel.items.collectAsStateWithLifecycle()
    val now by rememberMinuteClock()
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    val nearby = remember(state, checklistItems, now) { buildNearby(state, checklistItems, today, now) }

    // Odhaczanie z podglądu: ptaszek od razu, zapis po chwili (punkt znika wtedy z podglądu).
    val pending = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()
    val togglePreview: (Long) -> Unit = { id ->
        if (id in pending) {
            pending.remove(id)
        } else {
            pending.add(id)
            scope.launch {
                delay(PREVIEW_TOGGLE_DELAY_MS)
                if (pending.remove(id)) viewModel.toggleChecklistItem(id)
            }
        }
    }
    // Wyjście z ekranu przed upływem chwili nie może zgubić odhaczenia.
    DisposableEffect(Unit) {
        onDispose {
            pending.toList().forEach(viewModel::toggleChecklistItem)
            pending.clear()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            // Treść nie wjeżdża pod pasek stanu; dolny pasek nawigacji tylko jako odstęp na końcu.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(start = KartkaDefaults.PagePadding, end = 14.dp, top = 14.dp, bottom = 24.dp),
    ) {
        // ------------------------------------------------ data + konto, ustawienia
        Row(verticalAlignment = Alignment.CenterVertically) {
            KartkaEyebrow(text = kartkaDateLabel(today), modifier = Modifier.weight(1f))
            KartkaIconButton(icon = KartkaIcons.Account, contentDescription = "Konto", onClick = { onOpen(Routes.ACCOUNT) })
            KartkaIconButton(
                icon = KartkaIcons.Settings,
                contentDescription = "Ustawienia",
                onClick = { onOpen(Routes.SETTINGS) },
            )
        }

        AnimatedVisibility(
            visible = !state.isLoading,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(0)),
        ) {
            Column(modifier = Modifier.padding(end = 6.dp)) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = headline(state.today, state.checklist, c.accent),
                    style = KartkaType.Hero,
                    color = c.ink,
                    modifier = Modifier.semantics { heading() },
                )

                KartkaPermissionNotices(modifier = Modifier.padding(top = 24.dp), askOnFirstRun = true)

                // ------------------------------------------------ najbliżej
                Spacer(Modifier.height(20.dp))
                KartkaSectionHeader(
                    title = "Najbliżej",
                    actionLabel = "Wszystkie",
                    onAction = { onOpen(Routes.REMINDERS) },
                )
                if (nearby.isEmpty()) {
                    EmptyNearby(onAddReminder = onAddReminder)
                } else {
                    nearby.forEachIndexed { index, entry ->
                        key(entry.key) {
                            val chip = entry.chip
                            KartkaListRow(
                                title = entry.title,
                                subtitle = entry.source,
                                leading = { KartkaTimeLabel(text = entry.lead) },
                                trailing = if (chip != null) {
                                    { KartkaChip(text = chip) }
                                } else {
                                    null
                                },
                                onClick = {
                                    when (val target = entry.target) {
                                        is NearTarget.Reminder -> onEditReminder(target.id)
                                        NearTarget.Checklist -> onOpen(Routes.CHECKLIST)
                                    }
                                },
                                showDivider = index < nearby.lastIndex,
                            )
                        }
                    }
                }

                // ------------------------------------------------ podgląd checklisty
                Spacer(Modifier.height(20.dp))
                ChecklistPreviewCard(
                    summary = state.checklist,
                    preview = state.checklistPreview,
                    pending = pending,
                    today = today,
                    now = now,
                    onToggle = togglePreview,
                    onOpen = { onOpen(Routes.CHECKLIST) },
                )

                // ------------------------------------------------ działy 2×2
                Spacer(Modifier.height(24.dp))
                SectionGrid(onOpen = onOpen)

                if (state.showLibrusPrompt) {
                    KartkaNotice(
                        title = "Połącz z Librusem",
                        text = "Sprawdziany, kartkówki i plan lekcji pojawią się w aplikacji same.",
                        icon = KartkaIcons.Plan,
                        actionLabel = "Połącz",
                        onAction = { onOpen(Routes.ACCOUNT) },
                        dismissLabel = "Nie teraz",
                        onDismiss = viewModel::dismissLibrusPrompt,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- nagłówek

/** „Dziś masz 3 przypomnienia i 4 punkty na liście.” – liczby w kolorze akcentu, poprawna odmiana i zera. */
private fun headline(today: TodaySummary, checklist: ChecklistSummary, accent: Color): AnnotatedString =
    buildAnnotatedString {
        val n = today.remainingToday
        val m = checklist.left
        val highlight = SpanStyle(color = accent)
        when {
            n > 0 -> {
                append("Dziś masz ")
                withStyle(highlight) { append(remindersText(n)) }
            }
            today.totalToday > 0 -> append("Dzisiejsze przypomnienia już za Tobą")
            else -> append("Dziś nie masz przypomnień")
        }
        when {
            m > 0 && n > 0 -> {
                append(" i ")
                withStyle(highlight) { append(pointsText(m)) }
                append(" na liście.")
            }
            m > 0 -> {
                append(", ale masz ")
                withStyle(highlight) { append(pointsText(m)) }
                append(" na liście.")
            }
            checklist.total > 0 -> append(", a lista jest odhaczona.")
            n > 0 -> append(".")
            today.totalToday > 0 -> append(", a lista jest pusta.")
            else -> append(" ani punktów na liście.")
        }
    }

// ---------------------------------------------------------------- najbliżej

private sealed interface NearTarget {
    data class Reminder(val id: Long) : NearTarget
    data object Checklist : NearTarget
}

/** Wiersz „Najbliżej”: godzina/dzień, tytuł, skąd (przypomnienie, checklista, Librus) i opcjonalny chip. */
private data class NearEntry(
    val key: String,
    val lead: String,
    val title: String,
    val source: String,
    val chip: String?,
    val target: NearTarget,
)

private fun buildNearby(
    state: HomeUiState,
    checklist: List<ChecklistItem>,
    today: LocalDate,
    now: Long,
): List<NearEntry> {
    val reminders = state.sections.flatMap { it.items }.associateBy { it.reminder.id }

    // Dzisiejsze godziny: przypomnienia (agenda z ViewModelu) i punkty checklisty z godziną.
    val timed = buildList<Pair<LocalTime, NearEntry>> {
        state.today.agenda.forEach { entry ->
            val item = reminders[entry.reminderId]
            val eventDate = item?.reminder?.eventDate?.takeIf { entry.type != null && !it.isBefore(today) }
            add(
                entry.time to NearEntry(
                    key = "r${entry.reminderId}-${entry.time}",
                    lead = entry.time.format(KartkaHourFormatter),
                    title = entry.title,
                    source = reminderSource(item, entry.type),
                    chip = eventDate?.let { kartkaCountdown(it, today) },
                    target = NearTarget.Reminder(entry.reminderId),
                ),
            )
        }
        checklist.forEach { point ->
            val at = point.remindAt ?: return@forEach
            if (point.done || at < now) return@forEach
            val dateTime = at.toLocalDateTime()
            if (dateTime.toLocalDate() != today) return@forEach
            add(
                dateTime.toLocalTime() to NearEntry(
                    key = "c${point.id}",
                    lead = dateTime.toLocalTime().format(KartkaHourFormatter),
                    title = point.text,
                    source = "Checklista",
                    chip = null,
                    target = NearTarget.Checklist,
                ),
            )
        }
    }.sortedBy { it.first }.take(MAX_TIMED).map { it.second }

    // Terminy szkolne (sprawdziany, kartkówki…) – bez tych, które już są wyżej.
    val shownIds = timed.mapNotNull { (it.target as? NearTarget.Reminder)?.id }.toSet()
    val upcoming = state.upcoming
        .filter { it.reminder.id !in shownIds }
        .take(MAX_UPCOMING)
        .map { event ->
            val fromLibrus = event.reminder.externalId?.startsWith(LIBRUS_EXTERNAL_PREFIX) == true
            NearEntry(
                key = "u${event.reminder.id}",
                lead = kartkaDayLead(event.date, today),
                title = event.reminder.title,
                source = if (fromLibrus) "${event.type.label} · Z Librusa" else event.type.label,
                chip = if (event.date == today) null else kartkaCountdown(event.date, today),
                target = NearTarget.Reminder(event.reminder.id),
            )
        }
    return timed + upcoming
}

private fun reminderSource(item: ReminderItem?, type: SchoolType?): String {
    if (item == null) return "Przypomnienie"
    if (item.reminder.externalId?.startsWith(LIBRUS_EXTERNAL_PREFIX) == true) {
        return "${type?.label ?: "Przypomnienie"} · Z Librusa"
    }
    val folder = item.folder ?: return "Przypomnienie"
    return "Przypomnienie · ${folder.name}"
}

@Composable
private fun EmptyNearby(onAddReminder: () -> Unit) {
    val c = LookTheme.kartka
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Text(
            text = "Na dziś i najbliższe dni nic nie czeka.",
            style = KartkaType.BodySmall,
            color = c.ink2,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        KartkaTextButton(text = "Nowe przypomnienie", onClick = onAddReminder, icon = KartkaIcons.Plus)
    }
}

// ---------------------------------------------------------------- checklista

@Composable
private fun ChecklistPreviewCard(
    summary: ChecklistSummary,
    preview: List<ChecklistItem>,
    pending: List<Long>,
    today: LocalDate,
    now: Long,
    onToggle: (Long) -> Unit,
    onOpen: () -> Unit,
) {
    val c = LookTheme.kartka
    KartkaCard(
        onClick = onOpen,
        onClickLabel = "Otwórz checklistę",
        contentPadding = PaddingValues(start = 2.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
        modifier = Modifier.animateContentSize(tween(200)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Checklista",
                style = KartkaType.Title,
                color = c.ink,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Text(
                text = when {
                    summary.left > 0 -> "${summary.left} do zrobienia"
                    summary.total == 0 -> "Otwórz"
                    else -> "Wszystko zrobione"
                },
                style = KartkaType.Link,
                color = c.accent,
            )
            Icon(
                imageVector = KartkaIcons.ChevronRight,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        if (preview.isEmpty()) {
            Text(
                text = if (summary.total == 0) "Lista jest pusta – dopisz pierwszy punkt." else "Wszystko odhaczone.",
                style = KartkaType.BodySmall,
                color = c.ink2,
                modifier = Modifier.padding(start = 14.dp, top = 4.dp, bottom = 10.dp),
            )
        }
        preview.forEach { item ->
            key(item.id) {
                PreviewRow(
                    item = item,
                    checked = item.id in pending,
                    today = today,
                    now = now,
                    onToggle = { onToggle(item.id) },
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(
    item: ChecklistItem,
    checked: Boolean,
    today: LocalDate,
    now: Long,
    onToggle: () -> Unit,
) {
    val c = LookTheme.kartka
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KartkaDefaults.TouchTarget)
            .clip(KartkaDefaults.ButtonShape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cel dotyku to cały wiersz; kwadracik 18 dp jak w makiecie.
        Row(
            modifier = Modifier.size(KartkaDefaults.TouchTarget),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KartkaCheckMark(checked = checked, size = 18.dp)
        }
        Text(
            text = item.text,
            style = KartkaType.BodySmall,
            color = if (checked) c.ink2 else c.ink,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        )
        val remindAt = item.remindAt
        if (remindAt != null) {
            KartkaChip(
                text = kartkaWhenShort(remindAt, today),
                tone = if (remindAt < now && !checked) KartkaChipTone.Warning else KartkaChipTone.Neutral,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- działy

@Composable
private fun SectionGrid(onOpen: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KartkaNavTile(
                label = "Przypomnienia",
                icon = KartkaIcons.Reminders,
                onClick = { onOpen(Routes.REMINDERS) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            KartkaNavTile(
                label = "Plan",
                icon = KartkaIcons.Plan,
                onClick = { onOpen(Routes.PLAN) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KartkaNavTile(
                label = "Trening",
                icon = KartkaIcons.Workout,
                onClick = { onOpen(Routes.WORKOUT) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            KartkaNavTile(
                label = "Quizy",
                icon = KartkaIcons.Quiz,
                onClick = { onOpen(Routes.QUIZZES) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}
