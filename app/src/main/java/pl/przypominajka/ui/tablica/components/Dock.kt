package pl.przypominajka.ui.tablica.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.przypominajka.ui.look.LookTheme

/** Wysokość doku i odstęp od dołu (nad paskiem systemowym) – z makiety. */
val DockHeight = 72.dp
val DockBottomMargin = 20.dp

/** Zakładki doku. MORE nie jest ekranem – otwiera arkusz „Więcej” (podświetla się na jego działach). */
enum class DockTab(val label: String) {
    HUB("Tablica"),
    CHECKLIST("Lista"),
    PLAN("Plan"),
    MORE("Więcej"),
    ;

    val icon: ImageVector
        get() = when (this) {
            HUB -> TablicaIcons.Board
            CHECKLIST -> TablicaIcons.Checklist
            PLAN -> TablicaIcons.Calendar
            MORE -> TablicaIcons.More
        }
}

/**
 * Pływający dok Tablicy (wys. 72 dp, rogi 24 dp, kolor dock): Tablica, Lista, okrągły żółty „+”
 * (54 dp) na środku, Plan, Więcej. Aktywna zakładka [active] w kolorze dockActive.
 * Odstępy zewnętrzne (16 dp z boków, 20 dp od dołu nad paskiem systemowym) ustawia wywołujący.
 */
@Composable
fun TablicaDock(
    active: DockTab?,
    onTabClick: (DockTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LookTheme.tablica
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DockHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.dock)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockItem(DockTab.HUB, active == DockTab.HUB, onTabClick, Modifier.weight(1f))
        DockItem(DockTab.CHECKLIST, active == DockTab.CHECKLIST, onTabClick, Modifier.weight(1f))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(colors.dockActive)
                    .clickable(role = Role.Button, onClick = onAddClick)
                    .semantics { contentDescription = "Dodaj" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(TablicaIcons.Plus, contentDescription = null, tint = PinInk, modifier = Modifier.size(24.dp))
            }
        }
        DockItem(DockTab.PLAN, active == DockTab.PLAN, onTabClick, Modifier.weight(1f))
        DockItem(DockTab.MORE, active == DockTab.MORE, onTabClick, Modifier.weight(1f))
    }
}

@Composable
private fun DockItem(tab: DockTab, selected: Boolean, onClick: (DockTab) -> Unit, modifier: Modifier) {
    val colors = LookTheme.tablica
    val tint by animateColorAsState(if (selected) colors.dockActive else colors.dockInk, tween(200), label = "dockTint")
    // Podpis rośnie z czcionką systemową najwyżej do 1,3× – dok ma stałą wysokość.
    val fontScale = LocalDensity.current.fontScale
    val labelSize = (11f * minOf(fontScale, 1.3f) / fontScale).sp
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = { onClick(tab) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            text = tab.label,
            color = tint,
            style = TablicaType.Tag.copy(
                fontSize = labelSize,
                lineHeight = labelSize * 1.25f,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            ),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
