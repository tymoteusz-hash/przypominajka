package pl.przypominajka.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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
import pl.przypominajka.data.SchoolType
import pl.przypominajka.data.isFinishedOn
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.formatNextTrigger
import pl.przypominajka.util.formatShortDay
import pl.przypominajka.util.pluralPl
import java.time.LocalDate
import java.time.LocalTime

/** Dane widżetu wyliczone z bazy (bez Compose). */
private data class WidgetData(
    val remaining: Int,
    val agenda: List<Pair<LocalTime, String>>,
    val next: String?,
    val school: String?,
)

/**
 * Widżet "Dziś" na ekran główny telefonu: ile powiadomień zostało, najbliższe godziny
 * i najbliższy sprawdzian / kartkówka. Przycisk "+" otwiera od razu nowe przypomnienie.
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { load(context) }
        provideContent { WidgetContent(context, data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val repository = context.appContainer.repository
        val reminders = repository.observeReminders().first()
        val schoolTimes = repository.currentSchoolTimes()
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

        return WidgetData(
            remaining = agenda.size,
            agenda = agenda.take(3),
            next = next?.let { "${formatNextTrigger(it.first, today)} · ${it.second}" },
            school = event?.let { (date, type, title) -> "${type.label} ${countdownText(date, today)} · $title" },
        )
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

// Kolory z palety aplikacji: espresso i złoto dzwonu.
private val Ink = Color(0xFF3A2A20)
private val Coral = Color(0xFFFFC62E)
private val White = Color.White
private val Muted = Color(0xB3FFFFFF)

@Composable
private fun WidgetContent(context: Context, data: WidgetData) {
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    val addReminder = actionStartActivity(
        Intent(context, MainActivity::class.java).setData(Uri.parse("przypominajka://open/new")),
    )
    val today = LocalDate.now()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Ink)
            .cornerRadius(26.dp)
            .padding(16.dp)
            .clickable(openApp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "DZIŚ · ${formatShortDay(today).uppercase()}",
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    text = if (data.remaining == 0) {
                        "Na dziś wszystko"
                    } else {
                        "${data.remaining} " + pluralPl(data.remaining, "powiadomienie", "powiadomienia", "powiadomień")
                    },
                    style = TextStyle(color = ColorProvider(White), fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = "Nowe przypomnienie",
                modifier = GlanceModifier.size(40.dp).clickable(addReminder),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        if (data.agenda.isNotEmpty()) {
            data.agenda.forEachIndexed { index, (time, title) ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = time.format(TimeFormatter),
                        style = TextStyle(
                            color = ColorProvider(if (index == 0) Coral else White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = GlanceModifier.width(52.dp),
                    )
                    Text(
                        text = title,
                        style = TextStyle(color = ColorProvider(White), fontSize = 14.sp),
                        maxLines = 1,
                    )
                }
            }
        } else if (data.next != null) {
            Text(
                text = "Następne: ${data.next}",
                style = TextStyle(color = ColorProvider(Muted), fontSize = 13.sp),
                maxLines = 2,
            )
        }
        if (data.school != null) {
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(Color(0x1FFFFFFF))
                    .cornerRadius(14.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data.school,
                    style = TextStyle(color = ColorProvider(Coral), fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
            }
        }
    }
}
