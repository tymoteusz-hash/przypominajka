package pl.przypominajka.ui.dzwon

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.LIBRUS_EXTERNAL_PREFIX
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.checklist.ChecklistSummary
import pl.przypominajka.ui.dzwon.components.DzwonBell
import pl.przypominajka.ui.dzwon.components.DzwonCard
import pl.przypominajka.ui.dzwon.components.DzwonChip
import pl.przypominajka.ui.dzwon.components.DzwonIcons
import pl.przypominajka.ui.dzwon.components.DzwonListRow
import pl.przypominajka.ui.dzwon.components.DzwonNotice
import pl.przypominajka.ui.dzwon.components.DzwonPermissionPrompts
import pl.przypominajka.ui.dzwon.components.DzwonPrimaryButton
import pl.przypominajka.ui.dzwon.components.DzwonRoundIconButton
import pl.przypominajka.ui.dzwon.components.DzwonSectionHeader
import pl.przypominajka.ui.dzwon.components.DzwonTextButton
import pl.przypominajka.ui.dzwon.components.DzwonType
import pl.przypominajka.ui.home.HomeUiState
import pl.przypominajka.ui.home.HomeViewModel
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.navigation.Routes
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.dayShortName
import pl.przypominajka.util.formatLongDate
import pl.przypominajka.util.greeting
import pl.przypominajka.util.pluralPl
import pl.przypominajka.util.relativeDayLabel
import pl.przypominajka.util.remindersText
import pl.przypominajka.util.ShortDateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Układ orbity z makiety (B_Start): pole 358×358, pierścień r = 128, poświata r = 104,
// dzwon 120×128 w punkcie (119, 98), podpis na wysokości 232.
private const val ORBIT = 358f
private const val RING_R = 128f
private const val HALO_R = 104f

/** Kąty działów na pierścieniu (stopnie, 0 = w prawo, −90 = u góry). */
private const val ANGLE_CHECKLIST = -90f
private const val ANGLE_PLAN = -18f
private const val ANGLE_WORKOUT = 54f
private const val ANGLE_QUIZZES = 126f
private const val ANGLE_REMINDERS = 198f

private val AgendaTime: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/**
 * Strona główna Dzwonu: data i powitanie, a pod nimi orbita – złoty dzwon z liczbą dzisiejszych
 * przypomnień w środku pierścienia postępu checklisty, wokół działy jak przyciski-pigułki.
 * Niżej prośby o uprawnienia, propozycja Librusa i karta „Następne”.
 * Dzwon wychyla się raz, gdy ekran się pojawia (także po powrocie z działu) i po dotknięciu.
 */
@Composable
fun DzwonStartScreen(
    onOpen: (route: String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    val today = LocalDate.now()
    // Każda zmiana = jedno wychylenie; świeże 0 przy każdym pojawieniu się ekranu (także po powrocie).
    var swing by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        StartHeader(
            today = today,
            onAccount = { onOpen(Routes.ACCOUNT) },
            onSettings = { onOpen(Routes.SETTINGS) },
        )

        Orbit(
            remaining = state.today.remainingToday,
            checklist = state.checklist,
            swingKey = swing,
            onBellTap = { swing++ },
            onOpen = onOpen,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
        )

        RingLegend(
            checklist = state.checklist,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
        )

        DzwonPermissionPrompts(
            askOnFirstRun = true,
            itemPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp),
        )

        if (state.showLibrusPrompt) {
            DzwonNotice(
                title = "Masz Librusa?",
                text = "Połącz konto Synergii – sprawdziany, kartkówki i plan lekcji pojawią się same, " +
                    "a dzwon zadzwoni godzinę po lekcjach.",
                icon = Icons.Rounded.School,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                actions = {
                    DzwonTextButton(text = "Nie, dzięki", onClick = viewModel::dismissLibrusPrompt, color = c.ink)
                    DzwonPrimaryButton(text = "Połącz", onClick = { onOpen(Routes.ACCOUNT) })
                },
            )
        }

        if (!state.isLoading) {
            NextCard(
                state = state,
                today = today,
                onOpenReminder = { id -> onOpen(Routes.edit(reminderId = id)) },
                onAddReminder = { onOpen(Routes.edit()) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp),
            )
        }
    }
}

/** Górny rząd: Konto (okrągły przycisk), data i powitanie na środku, Ustawienia. */
@Composable
private fun StartHeader(today: LocalDate, onAccount: () -> Unit, onSettings: () -> Unit) {
    val c = LookTheme.dzwon
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DzwonRoundIconButton(icon = DzwonIcons.Person, contentDescription = "Konto", onClick = onAccount)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = formatLongDate(today),
                style = DzwonType.CaptionStrong,
                color = c.ink2,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${greeting()}!",
                style = DzwonType.Greeting,
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
        }
        DzwonRoundIconButton(icon = DzwonIcons.Sliders, contentDescription = "Ustawienia", onClick = onSettings)
    }
}

/**
 * Orbita: poświata, tor i złoty łuk postępu checklisty, dzwon z liczbą przypomnień w środku
 * i pięć działów na pierścieniu. Promień rośnie z szerokością ekranu (pole = szerokość − 32 dp).
 */
@Composable
private fun Orbit(
    remaining: Int,
    checklist: ChecklistSummary,
    swingKey: Int,
    onBellTap: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LookTheme.dzwon
    val progress = animateFloatAsState(checklist.progress, tween(350), label = "orbitProgress")
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Na tabletach i w poziomie orbita nie rośnie bez końca.
        val side = min(maxWidth, 420.dp)
        val s = side / ORBIT.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(side),
        ) {
            Canvas(Modifier.matchParentSize()) {
                val u = this.size.width / ORBIT
                val stroke = 6.dp.toPx()
                drawCircle(c.halo, radius = HALO_R * u)
                drawCircle(c.track, radius = RING_R * u, style = Stroke(stroke))
                val p = progress.value
                if (p > 0.001f) {
                    val r = RING_R * u
                    drawArc(
                        color = c.gold,
                        startAngle = -90f,
                        sweepAngle = 360f * p,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(2 * r, 2 * r),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }

            DzwonBell(
                size = 120.dp * s,
                number = when {
                    remaining <= 0 -> null
                    remaining > 99 -> "99+"
                    else -> remaining.toString()
                },
                swingKey = swingKey,
                contentDescription = if (remaining > 0) {
                    "Dzwon: dziś jeszcze ${remindersText(remaining)}"
                } else {
                    "Dzwon: na dziś to wszystko"
                },
                modifier = Modifier
                    .offset(x = 119.dp * s, y = 98.dp * s)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Rozkołysz dzwon",
                        onClick = onBellTap,
                    ),
            )

            Text(
                text = if (remaining > 0) {
                    pluralPl(remaining, "przypomnienie dziś", "przypomnienia dziś", "przypomnień dziś")
                } else {
                    "na dziś to wszystko"
                },
                style = DzwonType.CaptionStrong,
                color = c.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 232.dp * s),
            )

            RingLayout(
                radius = RING_R.dp * s,
                angles = listOf(ANGLE_CHECKLIST, ANGLE_PLAN, ANGLE_WORKOUT, ANGLE_QUIZZES, ANGLE_REMINDERS),
                overflow = 12.dp,
                modifier = Modifier.matchParentSize(),
            ) {
                DzwonChip(
                    text = "Checklista",
                    selected = true,
                    icon = DzwonIcons.Check,
                    onClick = { onOpen(Routes.CHECKLIST) },
                    modifier = Modifier
                        .widthIn(min = 120.dp)
                        .semantics {
                            stateDescription = if (checklist.total == 0) {
                                "pusta"
                            } else {
                                "${checklist.done} z ${checklist.total} odhaczone"
                            }
                        },
                )
                OrbitChip("Plan") { onOpen(Routes.PLAN) }
                OrbitChip("Trening") { onOpen(Routes.WORKOUT) }
                OrbitChip("Quizy") { onOpen(Routes.QUIZZES) }
                OrbitChip("Przypomnienia", small = true) { onOpen(Routes.REMINDERS) }
            }
        }
    }
}

@Composable
private fun OrbitChip(text: String, small: Boolean = false, onClick: () -> Unit) {
    DzwonChip(
        text = text,
        onClick = onClick,
        textStyle = if (small) DzwonType.Chip.copy(fontSize = 13.sp) else DzwonType.Chip,
        modifier = Modifier.widthIn(min = 120.dp),
    )
}

/**
 * Układa dzieci środkami na okręgu o promieniu [radius] (środek = środek pola) pod kątami [angles].
 * Dzieci mogą wystawać poza pole najwyżej o [overflow] (żeby nie wyjść poza ekran).
 */
@Composable
private fun RingLayout(
    radius: Dp,
    angles: List<Float>,
    overflow: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val r = radius.toPx()
        val over = overflow.roundToPx()
        val childConstraints = Constraints(maxWidth = (width * 0.55f).roundToInt().coerceAtLeast(1))
        val placeables = measurables.map { it.measure(childConstraints) }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val angle = Math.toRadians(angles.getOrElse(index) { 0f }.toDouble())
                val cx = width / 2f + r * cos(angle).toFloat()
                val cy = height / 2f + r * sin(angle).toFloat()
                val maxX = (width + over - placeable.width).coerceAtLeast(-over)
                val x = (cx - placeable.width / 2f).roundToInt().coerceIn(-over, maxX)
                val y = (cy - placeable.height / 2f).roundToInt()
                placeable.place(x, y)
            }
        }
    }
}

/** „Złoty pierścień: checklista, 3 z 7 odhaczone” ze złotą kreską. */
@Composable
private fun RingLegend(checklist: ChecklistSummary, modifier: Modifier = Modifier) {
    val c = LookTheme.dzwon
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 18.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.gold),
        )
        Text(
            text = if (checklist.total == 0) {
                "Złoty pierścień: checklista – na razie pusta"
            } else {
                "Złoty pierścień: checklista, ${checklist.done} z ${checklist.total} odhaczone"
            },
            style = DzwonType.Caption,
            color = c.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/** Wiersz karty „Następne”. */
private data class NextEntry(
    val lead: String,
    val title: String,
    val subtitle: String,
    val reminderId: Long?,
)

/**
 * Karta „Następne”: najbliższe dzisiejsze przypomnienia (albo najbliższe w ogóle, gdy dziś nic nie zostało)
 * i nadchodzące sprawdziany/kartkówki. Dotknięcie wiersza otwiera przypomnienie.
 */
@Composable
private fun NextCard(
    state: HomeUiState,
    today: LocalDate,
    onOpenReminder: (Long) -> Unit,
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LookTheme.dzwon
    val entries = remember(state, today) { buildNextEntries(state, today) }
    DzwonCard(modifier = modifier.fillMaxWidth()) {
        DzwonSectionHeader(
            title = "Następne",
            trailing = {
                DzwonRoundIconButton(
                    icon = DzwonIcons.Plus,
                    contentDescription = "Nowe przypomnienie",
                    onClick = onAddReminder,
                    size = 36.dp,
                    iconSize = 18.dp,
                    containerColor = c.field,
                )
            },
        )
        if (entries.isEmpty()) {
            Text(
                text = "Na razie nic nie czeka – dzwon odpoczywa.",
                style = DzwonType.Body,
                color = c.ink2,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
        } else {
            entries.forEachIndexed { index, entry ->
                DzwonListRow(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    lead = entry.lead,
                    divider = index < entries.lastIndex,
                    onClick = entry.reminderId?.let { id -> { onOpenReminder(id) } },
                )
            }
        }
    }
}

private fun buildNextEntries(state: HomeUiState, today: LocalDate): List<NextEntry> {
    val byId = state.sections.flatMap { it.items }.associateBy { it.reminder.id }
    val result = ArrayList<NextEntry>()

    val agenda = state.today.agenda.take(3)
    agenda.forEach { entry ->
        val folder = byId[entry.reminderId]?.folder?.name
        result += NextEntry(
            lead = entry.time.format(AgendaTime),
            title = entry.title,
            subtitle = listOfNotNull(entry.type?.label ?: "Przypomnienie", folder).joinToString(" · "),
            reminderId = entry.reminderId,
        )
    }

    // Dziś już nic – pokaż najbliższe przypomnienie w ogóle (jutro, w czwartek…).
    val nextAt = state.today.nextAt
    val nextTitle = state.today.nextTitle
    if (agenda.isEmpty() && nextAt != null && nextTitle != null) {
        val item = byId.values.firstOrNull { it.nextTriggerAt == nextAt && it.reminder.title == nextTitle }
        result += NextEntry(
            lead = relativeDayLabel(nextAt, today).removeSuffix("."),
            title = nextTitle,
            subtitle = "Przypomnienie · o ${
                Instant.ofEpochMilli(nextAt).atZone(ZoneId.systemDefault()).toLocalTime().format(AgendaTime)
            }",
            reminderId = item?.reminder?.id,
        )
    }

    state.upcoming.take(2).forEach { event ->
        val days = ChronoUnit.DAYS.between(today, event.date)
        val lead = when {
            days == 0L -> "dziś"
            days in 1L..6L -> dayShortName(event.date).removeSuffix(".")
            else -> event.date.format(ShortDateFormatter)
        }
        val source = if (event.reminder.externalId?.startsWith(LIBRUS_EXTERNAL_PREFIX) == true) {
            "Z Librusa"
        } else {
            event.type.label
        }
        result += NextEntry(
            lead = lead,
            title = "${event.type.label}: ${event.reminder.title}",
            subtitle = "$source · ${countdownText(event.date, today)}",
            reminderId = event.reminder.id,
        )
    }
    return result
}
