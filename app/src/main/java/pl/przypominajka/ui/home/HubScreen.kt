package pl.przypominajka.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.Bell
import pl.przypominajka.ui.components.LocalBottomBarPadding
import pl.przypominajka.ui.components.NoticeCard
import pl.przypominajka.ui.components.PermissionsSection
import pl.przypominajka.ui.components.PrimaryButton
import pl.przypominajka.ui.components.SectionLabel
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.icon
import pl.przypominajka.ui.components.pressScale
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.Pastel
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.formatLongDate
import pl.przypominajka.util.formatNextTrigger
import pl.przypominajka.util.greeting
import pl.przypominajka.util.pluralPl
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

/** Dział aplikacji na łuku wokół dzwonu. */
private data class HubSection(val route: String, val label: String, val icon: ImageVector, val pastel: Pastel)

private val Sections = listOf(
    HubSection(Routes.REMINDERS, "Przypomnienia", Icons.Rounded.NotificationsActive, Pastel.GOLD),
    HubSection(Routes.PLAN, "Plan", Icons.Rounded.CalendarMonth, Pastel.DUSK),
    HubSection(Routes.WORKOUT, "Trening", Icons.Rounded.FitnessCenter, Pastel.SAGE),
    HubSection(Routes.QUIZZES, "Quizy", Icons.Rounded.Quiz, Pastel.TERRACOTTA),
    HubSection(Routes.ACCOUNT, "Konto", Icons.Rounded.Person, Pastel.PLUM),
    HubSection(Routes.SETTINGS, "Ustawienia", Icons.Rounded.Settings, Pastel.SAND),
)
/**
 * Kąty działów wokół dzwonu (w stopniach, 0 = w prawo, 90 = w dół): po lewej z góry na dół
 * Przypomnienia, Plan, Trening, po prawej z góry na dół Quizy, Konto, Ustawienia.
 */
private val SectionAngles = listOf(220.0, 180.0, 140.0, 320.0, 0.0, 40.0)

/**
 * Strona główna: duży dzwon z liczbą dzisiejszych powiadomień, wokół niego łukiem działy.
 * Dotknięcie działu – dzwon bije i ekran działu rozchodzi się od niego. Pod spodem karta "Dziś".
 */
@Composable
fun HubScreen(
    onOpen: (route: String) -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        containerColor = AppSurfaces.page,
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocalBottomBarPadding.current + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ------------------------------------------------ powitanie
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                    .appear(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppSurfaces.accent),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = formatLongDate(today).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(greeting(), style = MaterialTheme.typography.headlineLarge)
            }

            // ------------------------------------------------ dzwon i działy
            BellStage(
                remaining = state.today.remainingToday,
                activeCount = state.today.activeCount,
                onOpen = onOpen,
            )

            PermissionsSection()

            if (state.showLibrusPrompt) {
                NoticeCard(
                    title = "Masz Librusa?",
                    body = "Połącz konto Synergii – sprawdziany, kartkówki i plan lekcji pojawią się same, " +
                        "a przypomnienia przyjdą godzinę po lekcjach.",
                    containerColor = Pastel.GOLD.container,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Rounded.School,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    actions = {
                        TextButton(onClick = viewModel::dismissLibrusPrompt) { Text("Nie, dzięki") }
                        PrimaryButton(text = "Połącz", onClick = { onOpen(Routes.ACCOUNT) })
                    },
                )
            }

            // ------------------------------------------------ dziś
            TodayCard(
                summary = state.today,
                today = today,
                onOpenReminder = onEditReminder,
                onAddReminder = onAddReminder,
                onOpenAll = { onOpen(Routes.REMINDERS) },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .appear(6),
            )

            if (state.upcoming.isNotEmpty()) {
                Column(modifier = Modifier.appear(7), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(
                        text = "W szkole wkrótce",
                        trailing = "${state.upcoming.size}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = state.upcoming, key = { it.reminder.id }) { event ->
                            UpcomingEventCard(
                                event = event,
                                today = today,
                                onClick = { onEditReminder(event.reminder.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Dzwon na środku, a wokół niego jak aureola sześć działów – po trzy z każdej strony. */
@Composable
private fun BellStage(remaining: Int, activeCount: Int, onOpen: (String) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        val width = maxWidth
        val bellSize = minOf(width * 0.46f, 190.dp)
        val circle = 62.dp
        val itemWidth = 104.dp
        val rowGap = 92.dp
        val top = 40.dp
        val centerX = width / 2
        val centerY = top + bellSize / 2
        val radiusX = minOf(width / 2 - circle / 2 - 6.dp, 160.dp)
        val stageHeight = maxOf(centerY + rowGap + circle / 2 + 34.dp, top + bellSize + 16.dp)
        val rowFactor = 1f / sin(Math.toRadians(40.0)).toFloat()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stageHeight),
        ) {
            Bell(
                size = bellSize,
                label = if (remaining > 0) remaining.toString() else null,
                onClick = {},
                modifier = Modifier
                    .offset(x = centerX - bellSize / 2, y = top)
                    .semantics {
                        contentDescription = if (remaining > 0) {
                            "Dzwon: dziś jeszcze $remaining " +
                                pluralPl(remaining, "powiadomienie", "powiadomienia", "powiadomień")
                        } else {
                            "Dzwon: na dziś wszystko"
                        }
                    },
            )
            Sections.forEachIndexed { index, section ->
                val angle = Math.toRadians(SectionAngles[index])
                val x = centerX + radiusX * cos(angle).toFloat()
                val y = centerY + rowGap * (sin(angle).toFloat() * rowFactor)
                HubButton(
                    section = section,
                    badge = if (section.route == Routes.REMINDERS && activeCount > 0) activeCount.toString() else null,
                    onClick = { onOpen(section.route) },
                    modifier = Modifier
                        .offset(x = (x - itemWidth / 2).coerceIn(0.dp, width - itemWidth), y = y - circle / 2)
                        .width(itemWidth)
                        .appear(index + 1),
                )
            }
        }
    }
}

@Composable
private fun HubButton(section: HubSection, badge: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.86f)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(section.pastel.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(section.icon, contentDescription = null, tint = section.pastel.content, modifier = Modifier.size(28.dp))
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                ) {
                    Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = section.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------- karta "Dziś"

@Composable
private fun TodayCard(
    summary: TodaySummary,
    today: LocalDate,
    onOpenReminder: (Long) -> Unit,
    onAddReminder: () -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Dziś", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    text = when {
                        summary.remainingToday > 0 -> "jeszcze ${summary.remainingToday} " +
                            pluralPl(summary.remainingToday, "powiadomienie", "powiadomienia", "powiadomień")
                        summary.totalToday > 0 -> "wszystkie już były"
                        else -> "spokojny dzień"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val nextAt = summary.nextAt
            if (summary.agenda.isNotEmpty()) {
                Timeline(
                    agenda = summary.agenda,
                    more = summary.remainingToday - summary.agenda.size,
                    onOpenReminder = onOpenReminder,
                )
            } else if (nextAt != null && summary.nextTitle != null) {
                Text(
                    text = "Następne: ${formatNextTrigger(nextAt, today)} · ${summary.nextTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Brak aktywnych przypomnień. Dodaj nowe – dzwon da znać, kiedy trzeba.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = "Nowe",
                    icon = Icons.Rounded.Add,
                    onClick = onAddReminder,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = onOpenAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) { Text("Wszystkie", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

/** Oś czasu: godzina – kropka na linii – tytuł. Pierwsza pozycja (najbliższa) jest koralowa. */
@Composable
private fun Timeline(agenda: List<AgendaEntry>, more: Int, onOpenReminder: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        agenda.forEachIndexed { index, entry ->
            val first = index == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onOpenReminder(entry.reminderId) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.time.format(TimeFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (first) AppSurfaces.accent else colors.onSurface,
                    modifier = Modifier.widthIn(min = 52.dp),
                )
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (agenda.size > 1) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(colors.outlineVariant),
                        )
                    }
                    Box(
                        Modifier
                            .size(if (first) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (first) AppSurfaces.accent else colors.primary),
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (entry.type != null) {
                    Icon(entry.type.icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (first) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 9.dp),
                )
            }
        }
        if (more > 0) {
            Text(
                text = "+ $more " + pluralPl(more, "kolejne", "kolejne", "kolejnych"),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 82.dp, top = 2.dp),
            )
        }
    }
}
