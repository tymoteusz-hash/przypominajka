package pl.przypominajka.ui.dzwon.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme

/** Zaokrąglenie kart Dzwonu (karta „Następne”). */
val DzwonCardShape = RoundedCornerShape(26.dp)

/**
 * Karta Dzwonu: tło `surface`, zaokrąglenie 26 dp, wnętrze 18/6 dp jak w makiecie.
 * Z [onClick] cała karta jest klikalna. Na arkuszu (`sheet`) podaj `color = LookTheme.dzwon.field`.
 */
@Composable
fun DzwonCard(
    modifier: Modifier = Modifier,
    color: Color = LookTheme.dzwon.surface,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(DzwonCardShape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Nagłówek karty lub sekcji (Fraunces 19 sp, np. „Następne”), opcjonalnie z akcjami po prawej. */
@Composable
fun DzwonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = DzwonType.SectionTitle,
            color = LookTheme.dzwon.ink,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (trailing != null) trailing()
    }
}

/** Mały podpis grupy na liście (13 sp, szary), np. „Aktywne”, „Zakończone”. */
@Composable
fun DzwonSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = DzwonType.CaptionStrong,
        color = LookTheme.dzwon.ink2,
        modifier = modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

/**
 * Wiersz listy jak w karcie „Następne”: po lewej złoty krótki tekst [lead] (godzina, dzień) albo
 * własny [leading], w środku [title] i [subtitle], po prawej [trailing]. [divider] – cienka linia pod spodem.
 */
@Composable
fun DzwonListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    lead: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    divider: Boolean = false,
    titleColor: Color = LookTheme.dzwon.ink,
) {
    val c = LookTheme.dzwon
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(
                if (divider) {
                    Modifier.drawBehind {
                        val y = size.height - 0.5.dp.toPx()
                        drawLine(c.line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                } else {
                    Modifier
                },
            )
            .heightIn(min = 56.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
        } else if (lead != null) {
            Text(text = lead, style = DzwonType.TimeLarge, color = c.goldText)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(text = title, style = DzwonType.RowTitle, color = titleColor)
            if (subtitle != null) {
                Text(text = subtitle, style = DzwonType.Caption, color = c.ink2)
            }
        }
        if (trailing != null) trailing()
    }
}

/** Cienka linia oddzielająca (kolor `line`). */
@Composable
fun DzwonDivider(modifier: Modifier = Modifier, color: Color = LookTheme.dzwon.line) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

/** Uchwyt arkusza: pigułka 40×5 dp w kolorze toru. */
@Composable
fun DzwonSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 40.dp, height = 5.dp)
            .clip(CircleShape)
            .background(LookTheme.dzwon.track),
    )
}

/**
 * Pusty stan: (opcjonalnie) mały dzwon, tytuł Fraunces 22 sp i opis, pod spodem np. przycisk.
 * Przykład: „Dzwon odpoczywa” / „Na liście nic nie zostało.”.
 */
@Composable
fun DzwonEmptyState(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    showBell: Boolean = true,
    action: (@Composable () -> Unit)? = null,
) {
    val c = LookTheme.dzwon
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showBell) {
            DzwonBell(
                size = 52.dp,
                color = c.track,
                deepColor = c.track,
                shine = false,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text = title,
            style = DzwonType.SheetTitle,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = text, style = DzwonType.Body, color = c.ink2, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(10.dp))
            action()
        }
    }
}

/**
 * Komunikat w karcie (prośba o uprawnienie, „Masz Librusa?”): ikona w złotym kółku, tytuł, opis
 * i rząd przycisków [actions] wyrównany do prawej.
 */
@Composable
fun DzwonNotice(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = LookTheme.dzwon.surface,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = LookTheme.dzwon
    CompositionLocalProvider(LocalContentColor provides c.ink) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(DzwonCardShape)
                .background(containerColor)
                .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(end = 6.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c.gold),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = c.onGold, modifier = Modifier.size(20.dp))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = DzwonType.SmallTitle,
                        color = c.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(text = text, style = DzwonType.Caption.copy(lineHeight = DzwonType.RowTitle.lineHeight), color = c.ink2)
                }
            }
            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
