package pl.przypominajka.widget

import android.content.Context
import androidx.annotation.DrawableRes
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
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pl.przypominajka.R
import pl.przypominajka.appContainer
import pl.przypominajka.data.ChecklistItem
import pl.przypominajka.ui.look.Look
import pl.przypominajka.ui.navigation.StartRequest
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.relativeDayLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Widżet „Checklista” (w Tablicy „Lista”) na ekran główny: punkty do odhaczania w stylu wybranego wyglądu.
 * Pole wyboru odhacza od razu (bez otwierania aplikacji), tekst otwiera checklistę w aplikacji,
 * a „+” – małe okienko do dopisania punktu ([QuickAddActivity]).
 */
class ChecklistWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = context.appContainer.checklistRepository
        // Wartości startowe; dalsze zmiany bazy i wyglądu zbieramy WEWNĄTRZ provideContent –
        // dopóki sesja widżetu żyje, update() nie uruchamia provideGlance od nowa.
        val initialItems = withContext(Dispatchers.IO) { repository.observeItems().first() }
        val initialLook = WidgetLook.current(context)
        provideContent {
            val items = remember(repository) { repository.observeItems() }.collectAsState(initialItems).value
            val look = rememberWidgetLook(context, initialLook)
            ChecklistContent(context, items, look)
        }
    }

    companion object {
        /** Parametry akcji [ToggleChecklistItemAction]: id punktu i stan, który ma mieć po dotknięciu. */
        val ItemIdKey = ActionParameters.Key<Long>("checklist_item_id")
        val DoneKey = ActionParameters.Key<Boolean>("checklist_item_done")
    }
}

class ChecklistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistWidget()
}

/**
 * Odhaczenie / odznaczenie punktu z widżetu. Dostaje docelowy stan (a nie „przełącz”), więc podwójne
 * dotknięcie przy wolnym odświeżaniu nie cofa zmiany. Publiczna, z konstruktorem bez argumentów –
 * Glance tworzy ją przez refleksję (reguła w proguard-rules.pro).
 */
class ToggleChecklistItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ChecklistWidget.ItemIdKey] ?: return
        val done = parameters[ChecklistWidget.DoneKey] ?: return
        context.appContainer.checklistRepository.setDone(itemId, done)
        ChecklistWidget().update(context, glanceId)
    }
}

// ---------------------------------------------------------------- wspólne

/** Wysokość wiersza listy; pole wyboru ma cel dotyku 48 × 44 dp. */
private val RowHeight = 44.dp
private val CheckCellWidth = 48.dp

private const val EMPTY_TEXT = "Pusto – dotknij +, żeby dopisać"

/** Co zostało: „3 do zrobienia”, „wszystko zrobione”, „pusto”. */
private fun summaryText(items: List<ChecklistItem>): String {
    val left = items.count { !it.done }
    return when {
        items.isEmpty() -> "pusto"
        left == 0 -> "wszystko zrobione"
        else -> "$left do zrobienia"
    }
}

/** Krótko, na kiedy przypomnienie: „19:00” (dziś), „jutro”, „czw.”, „24.10”. */
private fun shortWhen(millis: Long?, today: LocalDate): String? {
    if (millis == null) return null
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return if (dateTime.toLocalDate() == today) dateTime.toLocalTime().format(TimeFormatter)
    else relativeDayLabel(millis, today)
}

private fun toggleAction(item: ChecklistItem): Action = actionRunCallback<ToggleChecklistItemAction>(
    actionParametersOf(ChecklistWidget.ItemIdKey to item.id, ChecklistWidget.DoneKey to !item.done),
)

private fun checkLabel(item: ChecklistItem): String = (if (item.done) "Odznacz: " else "Odhacz: ") + item.text

@Composable
private fun ChecklistContent(context: Context, items: List<ChecklistItem>, style: WidgetLook) {
    val actions = ChecklistActions(
        openList = actionStartActivity(openAppIntent(context, StartRequest.CHECKLIST)),
        quickAdd = actionStartActivity(quickAddIntent(context)),
    )
    val today = LocalDate.now()
    when (style.look) {
        Look.KARTKA -> KartkaChecklist(items, today, actions, remember(style.theme) { KartkaWidgetColors(style.theme) })
        Look.DZWON -> DzwonChecklist(items, today, actions, remember(style.theme) { DzwonWidgetColors(style.theme) })
        Look.TABLICA -> TablicaChecklist(
            items, today, actions, remember(style.theme) { TablicaWidgetColors(style.theme) { it.yellow } },
        )
    }
}

private class ChecklistActions(val openList: Action, val quickAdd: Action)

/** Okrągły / zaokrąglony przycisk „+” 44 dp. */
@Composable
private fun AddButton(
    action: Action,
    label: String,
    background: ColorProvider,
    plus: ColorProvider,
    shape: WidgetShape,
) {
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .rounded(background, shape)
            .clickable(action)
            .semantics { contentDescription = label },
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

/** Komórka pola wyboru: cel dotyku 48 × 44 dp, odhacza od razu. */
@Composable
private fun CheckCell(item: ChecklistItem, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .width(CheckCellWidth)
            .fillMaxHeight()
            .clickable(toggleAction(item))
            .semantics { contentDescription = checkLabel(item) },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Mała ikona barwiona kolorem z motywu. */
@Composable
private fun Tinted(@DrawableRes drawable: Int, size: Dp, color: ColorProvider) {
    Image(
        provider = ImageProvider(drawable),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(color),
    )
}

/** Tekst punktu (+ godzina przypomnienia); dotknięcie otwiera checklistę w aplikacji. */
@Composable
private fun RowScope.RowText(
    item: ChecklistItem,
    time: String?,
    openList: Action,
    textStyle: TextStyle,
    timeStyle: TextStyle,
) {
    Row(
        modifier = GlanceModifier.defaultWeight().fillMaxHeight().clickable(openList),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.text,
            style = textStyle,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (time != null) {
            Text(
                text = time,
                style = timeStyle,
                maxLines = 1,
                modifier = GlanceModifier.padding(start = 6.dp, end = 6.dp),
            )
        }
    }
}

/** Lista punktów albo podpowiedź, gdy pusto. Odhaczone zostają na swoim miejscu (przekreślone). */
@Composable
private fun ChecklistBody(
    list: List<ChecklistItem>,
    emptyColor: ColorProvider,
    /** Dotknięcie pustej listy – od razu okienko dopisywania. */
    onEmptyClick: Action,
    modifier: GlanceModifier,
    row: @Composable (ChecklistItem) -> Unit,
) {
    if (list.isEmpty()) {
        Box(modifier = modifier.clickable(onEmptyClick), contentAlignment = Alignment.Center) {
            Text(
                text = EMPTY_TEXT,
                style = TextStyle(color = emptyColor, fontSize = 14.sp),
                maxLines = 2,
                modifier = GlanceModifier.padding(horizontal = 12.dp),
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(list, itemId = { it.id }) { item -> row(item) }
        }
    }
}

// ---------------------------------------------------------------- Kartka

@Composable
private fun KartkaChecklist(items: List<ChecklistItem>, today: LocalDate, actions: ChecklistActions, c: KartkaWidgetColors) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .rounded(c.card, WidgetShape.R22)
            .padding(top = 10.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight().clickable(actions.openList)) {
                Text(
                    text = "Checklista",
                    style = TextStyle(color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
                Text(
                    text = summaryText(items).replaceFirstChar { it.uppercase() },
                    style = TextStyle(color = c.ink2, fontSize = 13.sp),
                    maxLines = 1,
                )
            }
            AddButton(actions.quickAdd, "Dodaj punkt", c.accent, c.onAccent, WidgetShape.R12)
        }
        ChecklistBody(
            list = items,
            emptyColor = c.ink2,
            onEmptyClick = actions.quickAdd,
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(start = 2.dp, end = 4.dp),
        ) { item ->
            val time = shortWhen(item.remindAt, today)
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(RowHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckCell(item) {
                    if (item.done) {
                        Box(contentAlignment = Alignment.Center) {
                            Tinted(R.drawable.ic_widget_box_kartka_on, 19.dp, c.accent)
                            Tinted(R.drawable.ic_widget_check, 13.dp, c.onAccent)
                        }
                    } else {
                        Tinted(R.drawable.ic_widget_box_kartka_off, 19.dp, c.ink2)
                    }
                }
                RowText(
                    item = item,
                    time = time,
                    openList = actions.openList,
                    textStyle = TextStyle(
                        color = if (item.done) c.ink2 else c.ink,
                        fontSize = 15.sp,
                        textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    timeStyle = TextStyle(
                        color = if (item.done) c.ink2 else c.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Dzwon

@Composable
private fun DzwonChecklist(items: List<ChecklistItem>, today: LocalDate, actions: ChecklistActions, c: DzwonWidgetColors) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .rounded(c.card, WidgetShape.R30)
            .padding(top = 12.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(start = 12.dp, end = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mały dzwon nad liną z węzłami (środek w tej samej linii co pola wyboru).
            Box(modifier = GlanceModifier.size(40.dp), contentAlignment = Alignment.Center) {
                Box(modifier = GlanceModifier.width(32.dp).height(34.dp)) {
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
                }
            }
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight().clickable(actions.openList)) {
                Text(
                    text = "Checklista",
                    style = TextStyle(
                        color = c.ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = summaryText(items),
                    style = TextStyle(color = c.ink2, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
            }
            AddButton(actions.quickAdd, "Dodaj punkt", c.gold, c.onGold, WidgetShape.CIRCLE44)
        }
        ChecklistBody(
            list = items,
            emptyColor = c.ink2,
            onEmptyClick = actions.quickAdd,
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(start = 8.dp, end = 4.dp),
        ) { item ->
            val time = shortWhen(item.remindAt, today)?.takeUnless { item.done }
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(RowHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckCell(item) {
                    // Lina przez całą wysokość wiersza (złota przy odhaczonych), na niej okrągły węzeł.
                    Box(
                        modifier = GlanceModifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(if (item.done) c.gold else c.track),
                    ) {}
                    if (item.done) {
                        Box(contentAlignment = Alignment.Center) {
                            Tinted(R.drawable.ic_widget_circle, 20.dp, c.gold)
                            Tinted(R.drawable.ic_widget_check, 12.dp, c.onGold)
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Tinted(R.drawable.ic_widget_circle, 20.dp, c.card)
                            Tinted(R.drawable.ic_widget_box_dzwon_ring, 20.dp, c.ink2)
                        }
                    }
                }
                RowText(
                    item = item,
                    time = time,
                    openList = actions.openList,
                    textStyle = TextStyle(
                        color = if (item.done) c.ink2 else c.ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    timeStyle = TextStyle(color = c.goldText, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Tablica

@Composable
private fun TablicaChecklist(items: List<ChecklistItem>, today: LocalDate, actions: ChecklistActions, c: TablicaWidgetColors) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .rounded(c.paper, WidgetShape.R6),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(top = 16.dp, bottom = 10.dp)) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.defaultWeight().clickable(actions.openList),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "Lista",
                        style = TextStyle(color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = summaryText(items),
                        style = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(bottom = 2.dp),
                    )
                }
                AddButton(actions.quickAdd, "Dodaj karteczkę", c.ink, c.paper, WidgetShape.CIRCLE44)
            }
            ChecklistBody(
                list = items,
                emptyColor = c.inkMuted,
                onEmptyClick = actions.quickAdd,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(start = 8.dp, end = 10.dp),
            ) { item ->
                val time = shortWhen(item.remindAt, today)?.takeUnless { item.done }
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().height(RowHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckCell(item) {
                            if (item.done) {
                                Box(contentAlignment = Alignment.Center) {
                                    Tinted(R.drawable.ic_widget_box_tablica_on, 18.dp, c.ink)
                                    Tinted(R.drawable.ic_widget_check, 12.dp, c.paper)
                                }
                            } else {
                                Tinted(R.drawable.ic_widget_box_tablica_off, 18.dp, c.ink)
                            }
                        }
                        RowText(
                            item = item,
                            time = time,
                            openList = actions.openList,
                            textStyle = TextStyle(
                                color = if (item.done) c.inkMuted else c.ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            timeStyle = TextStyle(color = c.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                    // Przerywana linia jak na kartce w linie.
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_dash),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxWidth().height(1.dp),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.tint(c.rule),
                    )
                }
            }
        }
        // Taśma klejąca na górnej krawędzi karteczki.
        Box(modifier = GlanceModifier.width(64.dp).height(10.dp).background(c.tape)) {}
    }
}
