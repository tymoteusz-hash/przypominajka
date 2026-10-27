package pl.przypominajka.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pl.przypominajka.MainActivity
import pl.przypominajka.R
import pl.przypominajka.alarm.ReminderSchedule
import pl.przypominajka.appContainer
import pl.przypominajka.data.Reminder
import pl.przypominajka.data.SchoolTimes
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.isFinishedOn
import pl.przypominajka.ui.look.Look
import pl.przypominajka.ui.navigation.StartRequest
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.formatNextTrigger
import pl.przypominajka.util.formatShortDay
import pl.przypominajka.util.pluralPl
import java.time.LocalDate
import java.time.LocalTime

/** Dane widżetu wyliczone z bazy (bez Compose). */
private data class TodayData(
    /** Ile powiadomień jeszcze dziś przyjdzie. */
    val remaining: Int,
    /** Dzisiejsze godziny powiadomień z tytułami, po kolei. */
    val agenda: List<Pair<LocalTime, String>>,
    /** Najbliższe powiadomienie w ogóle („jutro 7:30 · Fizyka”) – gdy dziś już nic. */
    val next: String?,
    /** Najbliższy sprawdzian / kartkówka / zadanie. */
    val school: String?,
)

/**
 * Widżet "Dziś" na ekran główny telefonu: ile powiadomień zostało, najbliższe godziny
 * i najbliższy sprawdzian / kartkówka. Przycisk "+" otwiera od razu nowe przypomnienie.
 * Wygląd (Kartka / Dzwon / Tablica) i motyw – jak w aplikacji.
 */
class TodayWidget : GlanceAppWidget() {

    /** Dokładny rozmiar – liczba wierszy planu zależy od wysokości widżetu. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = context.appContainer.repository
        // Wartości startowe; dalsze zmiany zbieramy WEWNĄTRZ provideContent – dopóki sesja widżetu
        // żyje, update() nie uruchamia provideGlance od nowa.
        val initialReminders = withContext(Dispatchers.IO) { repository.observeReminders().first() }
        val initialSchoolTimes = withContext(Dispatchers.IO) { repository.currentSchoolTimes() }
        val initialLook = WidgetLook.current(context)
        provideContent {
            val reminders = remember(repository) { repository.observeReminders() }
                .collectAsState(initialReminders).value
            val schoolTimes = remember(repository) { repository.observeSchoolTimes() }
                .collectAsState(initialSchoolTimes).value
            val look = rememberWidgetLook(context, initialLook)
            val data = remember(reminders, schoolTimes) { computeToday(reminders, schoolTimes) }
            TodayContent(context, data, look)
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

private fun computeToday(reminders: List<Reminder>, schoolTimes: SchoolTimes): TodayData {
    val now = System.currentTimeMillis()
    val today = LocalDate.now()
    val active = reminders.filter { it.enabled && !it.isFinishedOn(today) }

    val agenda = active
        .flatMap { reminder -> ReminderSchedule.upcomingToday(reminder, now, schoolTimes).map { it to reminder.title } }
        .sortedBy { it.first }
    val next = active
        .mapNotNull { reminder -> ReminderSchedule.nextTrigger(reminder, now, schoolTimes)?.let { it to reminder.title } }
        .minByOrNull { it.first }
    val event = reminders
        .mapNotNull { reminder ->
            val type = SchoolType.fromName(reminder.schoolType) ?: return@mapNotNull null
            val date = reminder.eventDate ?: return@mapNotNull null
            if (date.isBefore(today)) null else Triple(date, type, reminder.title)
        }
        .minByOrNull { it.first }

    return TodayData(
        remaining = agenda.size,
        agenda = agenda,
        next = next?.let { "${formatNextTrigger(it.first, today)} · ${it.second}" },
        school = event?.let { (date, type, title) -> "${type.label} ${countdownText(date, today)} · $title" },
    )
}

// ---------------------------------------------------------------- wspólne

/** Wysokość wiersza planu i plakietki sprawdzianu (z odstępem) – do liczenia, ile się zmieści. */
private const val AGENDA_ROW_DP = 22f
private const val SCHOOL_DP = 36f
private const val MAX_EXTRA_ROWS = 4

/** Ile dodatkowych wierszy planu zmieści się pod nagłówkiem i czy zmieści się plakietka sprawdzianu. */
private fun fit(height: Dp, chrome: Dp, hasSchool: Boolean): Pair<Int, Boolean> {
    var free = height.value - chrome.value
    val showSchool = hasSchool && free >= SCHOOL_DP
    if (showSchool) free -= SCHOOL_DP
    val rows = (free / AGENDA_ROW_DP).toInt().coerceIn(0, MAX_EXTRA_ROWS)
    return rows to showSchool
}

/** Druga linia nagłówka: pierwsze dzisiejsze powiadomienie albo najbliższe w ogóle. */
private fun TodayData.firstLine(separator: String): String {
    val first = agenda.firstOrNull()
    return when {
        first != null -> "${first.first.format(TimeFormatter)}$separator${first.second}"
        next != null -> "Następne: $next"
        else -> "Nic więcej na dziś"
    }
}

private class TodayActions(val openApp: Action, val newReminder: Action)

@Composable
private fun TodayContent(context: Context, data: TodayData, style: WidgetLook) {
    val actions = TodayActions(
        openApp = actionStartActivity(Intent(context, MainActivity::class.java)),
        newReminder = actionStartActivity(openAppIntent(context, StartRequest.NEW_REMINDER)),
    )
    when (style.look) {
        Look.KARTKA -> KartkaToday(data, actions, remember(style.theme) { KartkaWidgetColors(style.theme) })
        Look.DZWON -> DzwonToday(data, actions, remember(style.theme) { DzwonWidgetColors(style.theme) })
        Look.TABLICA -> TablicaToday(
            data, actions, remember(style.theme) { TablicaWidgetColors(style.theme) { it.pink } },
        )
    }
}

@Composable
private fun AddReminderButton(action: Action, background: ColorProvider, plus: ColorProvider, shape: WidgetShape) {
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .rounded(background, shape)
            .clickable(action)
            .semantics { contentDescription = "Nowe przypomnienie" },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_plus),
            contentDescription = null,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(plus),
        )
    }
}

/** Kolejne dzisiejsze powiadomienia (pierwsze jest w nagłówku). */
@Composable
private fun AgendaRows(rows: List<Pair<LocalTime, String>>, timeStyle: TextStyle, titleStyle: TextStyle) {
    rows.forEach { (time, title) ->
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = time.format(TimeFormatter),
                style = timeStyle,
                maxLines = 1,
                modifier = GlanceModifier.width(50.dp),
            )
            Text(text = title, style = titleStyle, maxLines = 1, modifier = GlanceModifier.defaultWeight())
        }
    }
}

// ---------------------------------------------------------------- Kartka

@Composable
private fun KartkaToday(data: TodayData, actions: TodayActions, c: KartkaWidgetColors) {
    val (rows, showSchool) = fit(LocalSize.current.height, chrome = 80.dp, hasSchool = data.school != null)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .rounded(c.card, WidgetShape.R22)
            .clickable(actions.openApp)
            .padding(start = 18.dp, top = 14.dp, end = 10.dp, bottom = 14.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = data.remaining.toString(),
                style = TextStyle(color = c.accent, fontSize = 36.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(14.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = pluralPl(
                        data.remaining,
                        "przypomnienie zostało dziś",
                        "przypomnienia zostały dziś",
                        "przypomnień zostało dziś",
                    ),
                    style = TextStyle(color = c.ink2, fontSize = 13.sp),
                    maxLines = 1,
                )
                Text(
                    text = data.firstLine(" · "),
                    style = TextStyle(color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            AddReminderButton(actions.newReminder, c.field, c.ink, WidgetShape.R12)
        }
        Spacer(GlanceModifier.height(4.dp))
        AgendaRows(
            rows = data.agenda.drop(1).take(rows),
            timeStyle = TextStyle(color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            titleStyle = TextStyle(color = c.ink, fontSize = 13.sp),
        )
        if (showSchool && data.school != null) {
            Spacer(GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .rounded(c.accentSoft, WidgetShape.R12)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data.school,
                    style = TextStyle(color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Dzwon

@Composable
private fun DzwonToday(data: TodayData, actions: TodayActions, c: DzwonWidgetColors) {
    val (rows, showSchool) = fit(LocalSize.current.height, chrome = 84.dp, hasSchool = data.school != null)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .rounded(c.card, WidgetShape.R30)
            .clickable(actions.openApp)
            .padding(start = 14.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Złoty dzwon z liczbą dzisiejszych powiadomień na korpusie (jak w makiecie).
            Box(modifier = GlanceModifier.width(48.dp).height(51.dp), contentAlignment = Alignment.Center) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_bell_body),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(c.gold),
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_bell_rim),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(c.goldDeep),
                )
                Text(
                    text = data.remaining.toString(),
                    style = TextStyle(
                        color = c.onGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = pluralPl(data.remaining, "przypomnienie", "przypomnienia", "przypomnień") + " dziś",
                    style = TextStyle(color = c.ink2, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
                Text(
                    text = data.firstLine(" "),
                    style = TextStyle(color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            AddReminderButton(actions.newReminder, c.gold, c.onGold, WidgetShape.CIRCLE44)
        }
        Spacer(GlanceModifier.height(4.dp))
        AgendaRows(
            rows = data.agenda.drop(1).take(rows),
            timeStyle = TextStyle(color = c.goldText, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            titleStyle = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
        )
        if (showSchool && data.school != null) {
            Spacer(GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .rounded(c.field, WidgetShape.R16)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data.school,
                    style = TextStyle(color = c.goldText, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Tablica

@Composable
private fun TablicaToday(data: TodayData, actions: TodayActions, c: TablicaWidgetColors) {
    val (rows, showSchool) = fit(LocalSize.current.height, chrome = 90.dp, hasSchool = data.school != null)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .rounded(c.paper, WidgetShape.R6)
            .clickable(actions.openApp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(start = 14.dp, top = 16.dp, end = 10.dp, bottom = 12.dp)) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Dziś · ${formatShortDay(LocalDate.now())}",
                        style = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = data.remaining.toString(),
                            style = TextStyle(color = c.ink, fontSize = 34.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            text = pluralPl(data.remaining, "przypomnienie", "przypomnienia", "przypomnień"),
                            style = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            modifier = GlanceModifier.padding(bottom = 6.dp),
                        )
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                AddReminderButton(actions.newReminder, c.ink, c.paper, WidgetShape.CIRCLE44)
            }
            Text(
                text = data.firstLine(" "),
                style = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            AgendaRows(
                rows = data.agenda.drop(1).take((rows - 1).coerceAtLeast(0)),
                timeStyle = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                titleStyle = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
            if (showSchool && data.school != null) {
                Spacer(GlanceModifier.defaultWeight())
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_dash),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxWidth().height(1.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(c.rule),
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = data.school,
                    style = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        // Taśma klejąca na górnej krawędzi karteczki.
        Box(modifier = GlanceModifier.width(56.dp).height(10.dp).background(c.tape)) {}
    }
}
