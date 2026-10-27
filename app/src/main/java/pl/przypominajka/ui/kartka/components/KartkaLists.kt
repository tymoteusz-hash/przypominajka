package pl.przypominajka.ui.kartka.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme

/** Nadtytuł WIELKIMI LITERAMI z rozstrzeleniem (13 sp, ink2) – np. data na stronie głównej. */
@Composable
fun KartkaEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val c = LookTheme.kartka
    Text(
        text = text.uppercase(),
        style = KartkaType.Eyebrow,
        color = if (color.isSpecified) color else c.ink2,
        modifier = modifier,
    )
}

/**
 * Nagłówek sekcji: nadtytuł po lewej (np. „NAJBLIŻEJ”) i opcjonalny przycisk tekstowy po prawej
 * (np. „Wszystkie”). Bez marginesów poziomych – stawia się go w kolumnie z marginesem strony.
 */
@Composable
fun KartkaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KartkaEyebrow(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            KartkaTextButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Cienka linia 1 dp w kolorze `line` – Kartka oddziela liniami zamiast cieni. */
@Composable
fun KartkaDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = KartkaDefaults.LineWidth, color = LookTheme.kartka.line)
}

/**
 * Wiersz listy Kartki: opcjonalny element z przodu (np. [KartkaTimeLabel] albo ikona), tytuł 16 sp
 * i druga linia 13 sp ink2, opcjonalny element z tyłu (np. [KartkaChip], [KartkaSwitch]), linia 1 dp pod spodem.
 * Bez marginesów poziomych (w kolumnie z marginesem strony linia kończy się na marginesie, jak w makiecie).
 * Tekst się zawija – nic nie jest ucinane, chyba że podasz [titleMaxLines].
 */
@Composable
fun KartkaListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    showDivider: Boolean = true,
    titleColor: Color = Color.Unspecified,
    subtitleColor: Color = Color.Unspecified,
    titleMaxLines: Int = Int.MAX_VALUE,
) {
    val c = LookTheme.kartka
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .heightIn(min = 56.dp)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = KartkaType.RowTitle,
                    color = if (titleColor.isSpecified) titleColor else c.ink,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = KartkaType.Caption,
                        color = if (subtitleColor.isSpecified) subtitleColor else c.ink2,
                    )
                }
            }
            trailing?.invoke()
        }
        if (showDivider) KartkaDivider()
    }
}

/** Godzina / dzień w kolumnie z przodu wiersza (18 sp, cyfry o stałej szerokości, min. 54 dp szerokości). */
@Composable
fun KartkaTimeLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        style = KartkaType.Time,
        color = if (color.isSpecified) color else LookTheme.kartka.ink,
        modifier = modifier.widthIn(min = 54.dp),
    )
}

/**
 * Miękki szary blok (surface, zaokrąglenie 16 dp) – jedyny „kartowy” element Kartki, np. podgląd checklisty.
 * Bez cienia. [onClick] robi z całej karty przycisk (elementy w środku mogą mieć własne kliknięcia).
 */
@Composable
fun KartkaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LookTheme.kartka
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(KartkaDefaults.CardShape)
            .background(c.surface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** Płaska karta z obwódką 1 dp (bez tła) – np. grupa ustawień albo element listy, który ma się wyróżnić. */
@Composable
fun KartkaOutlineCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LookTheme.kartka
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(KartkaDefaults.CardShape)
            .border(KartkaDefaults.LineWidth, c.line, KartkaDefaults.CardShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** Kafelek działu (siatka na stronie głównej): obwódka 1 dp, ikona 24 dp i nazwa 15 sp, min. 54 dp wysokości. */
@Composable
fun KartkaNavTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LookTheme.kartka
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(KartkaDefaults.TileShape)
            .border(KartkaDefaults.LineWidth, c.line, KartkaDefaults.TileShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            style = KartkaType.BodySmall.copy(fontWeight = FontWeight.Medium),
            color = c.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Pusty stan: wyśrodkowany tytuł 20 sp, opis 15 sp ink2 i opcjonalny przycisk pod spodem. */
@Composable
fun KartkaEmptyState(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val c = LookTheme.kartka
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = KartkaType.TitleLarge, color = c.ink, textAlign = TextAlign.Center)
        Text(text = text, style = KartkaType.BodySmall, color = c.ink2, textAlign = TextAlign.Center)
        if (action != null) {
            Column(modifier = Modifier.padding(top = 8.dp)) { action() }
        }
    }
}

/**
 * Ogłoszenie / prośba (np. o uprawnienia, „Połącz z Librusem”): szary blok, ikona w akcencie, tytuł i opis,
 * pod spodem przyciski tekstowe ([dismissLabel] grafitowy, [actionLabel] w akcencie).
 */
@Composable
fun KartkaNotice(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = KartkaIcons.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val c = LookTheme.kartka
    KartkaCard(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(end = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = KartkaType.RowTitle.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                Text(text = text, style = KartkaType.BodySmall, color = c.ink2)
            }
        }
        if ((actionLabel != null && onAction != null) || (dismissLabel != null && onDismiss != null)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dismissLabel != null && onDismiss != null) {
                    KartkaTextButton(text = dismissLabel, onClick = onDismiss, color = c.ink2)
                }
                if (actionLabel != null && onAction != null) {
                    KartkaTextButton(text = actionLabel, onClick = onAction)
                }
            }
        }
    }
}
