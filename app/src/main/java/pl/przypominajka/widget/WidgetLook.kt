package pl.przypominajka.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import pl.przypominajka.MainActivity
import pl.przypominajka.R
import pl.przypominajka.appContainer
import pl.przypominajka.ui.look.DzwonColors
import pl.przypominajka.ui.look.KartkaColors
import pl.przypominajka.ui.look.Look
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.look.TablicaColors
import pl.przypominajka.ui.navigation.StartRequest
import pl.przypominajka.util.Appearance
import pl.przypominajka.util.ThemeMode
import androidx.glance.color.ColorProvider as dayNightColor

// =====================================================================================================
// WYGLĄD WIDŻETÓW – ten sam wygląd (Kartka / Dzwon / Tablica) i motyw co w aplikacji.
// Kolory biorą się z tokenów ui/look/LookColors.kt. Widżety nie mają własnych fontów, cieni, gradientów
// ani obrotów (ograniczenia ekranu głównego), więc to uproszczona wersja makiet *_Pulpit.
// =====================================================================================================

/** Wygląd i motyw widżetów. Przed wyborem wyglądu – Dzwon (jak domyślna ikona aplikacji). */
internal data class WidgetLook(val look: Look, val theme: ThemeMode) {
    companion object {
        fun from(appearance: Appearance): WidgetLook = WidgetLook(appearance.look ?: Look.DZWON, appearance.theme)

        fun current(context: Context): WidgetLook = from(context.appContainer.appearancePrefs.current())
    }
}

/**
 * Wygląd na bieżąco – wołać WEWNĄTRZ provideContent. Dopóki sesja widżetu żyje, update() nie uruchamia
 * provideGlance od nowa, więc zmiany z Ustawień trzeba zbierać tutaj.
 */
@Composable
internal fun rememberWidgetLook(context: Context, initial: WidgetLook): WidgetLook {
    val prefs = context.appContainer.appearancePrefs
    val flow = remember(prefs) { prefs.observe().map { WidgetLook.from(it) }.distinctUntilChanged() }
    return flow.collectAsState(initial).value
}

/**
 * Kolor zgodny z motywem: Jasny → tylko dzienny, Ciemny → tylko nocny, „Jak w telefonie” → para
 * dzień/noc (na Androidzie 12+ przełącza ją sam ekran główny, bez odświeżania widżetu).
 */
internal fun themed(theme: ThemeMode, day: Color, night: Color): ColorProvider = when (theme) {
    ThemeMode.LIGHT -> ColorProvider(day)
    ThemeMode.DARK -> ColorProvider(night)
    ThemeMode.SYSTEM -> dayNightColor(day = day, night = night)
}

/** Kartka: biała (w ciemnym grafitowa) karta i kobaltowy akcent. */
internal class KartkaWidgetColors(theme: ThemeMode) {
    private val l = KartkaColors.Light
    private val d = KartkaColors.Dark
    val card = themed(theme, l.bg, d.surface)
    /** Przycisk drugorzędny (np. „+” w widżecie Dziś). */
    val field = themed(theme, l.surface, d.raised)
    val ink = themed(theme, l.ink, d.ink)
    val ink2 = themed(theme, l.ink2, d.ink2)
    val accent = themed(theme, l.accent, d.accent)
    val accentSoft = themed(theme, l.accentSoft, d.accentSoft)
    val onAccent = themed(theme, l.onAccent, d.onAccent)
}

/** Dzwon: karta w kolorze nocy (albo biała) i złoto. */
internal class DzwonWidgetColors(theme: ThemeMode) {
    private val l = DzwonColors.Light
    private val d = DzwonColors.Dark
    val card = themed(theme, l.surface, d.surface)
    val field = themed(theme, l.field, d.field)
    val track = themed(theme, l.track, d.track)
    val ink = themed(theme, l.ink, d.ink)
    val ink2 = themed(theme, l.ink2, d.ink2)
    val gold = themed(theme, l.gold, d.gold)
    val goldDeep = themed(theme, l.goldDeep, d.goldDeep)
    val goldText = themed(theme, l.goldText, d.goldText)
    val onGold = themed(theme, l.onGold, d.onGold)
}

/** Tablica: karteczka w danym kolorze (żółta – checklista, różowa – Dziś) z taśmą. */
internal class TablicaWidgetColors(theme: ThemeMode, note: (TablicaColors) -> NoteColor) {
    private val l = TablicaColors.Light
    private val d = TablicaColors.Dark
    private val nl = note(l)
    private val nd = note(d)
    val paper = themed(theme, nl.bg, nd.bg)
    val ink = themed(theme, nl.ink, nd.ink)
    /** Odhaczony tekst – przygaszony atrament (jak opacity 0.55 w makiecie). */
    val inkMuted = themed(theme, nl.ink.copy(alpha = 0.55f), nd.ink.copy(alpha = 0.55f))
    /** Linia pod wierszem – zmieszana z papierem, bo barwienie obrazka nie przenosi przezroczystości. */
    val rule = themed(theme, nl.rule.compositeOver(nl.bg), nd.rule.compositeOver(nd.bg))
    val tape = themed(theme, l.tape, d.tape)
}

/** Zaokrąglenia teł w widżetach. Na Androidzie 8–11 cornerRadius nie działa – wtedy barwiony kształt z res. */
internal enum class WidgetShape(val radius: Dp, @DrawableRes val drawable: Int) {
    R6(6.dp, R.drawable.ic_widget_shape_6),
    R12(12.dp, R.drawable.ic_widget_shape_12),
    R16(16.dp, R.drawable.ic_widget_shape_16),
    R22(22.dp, R.drawable.ic_widget_shape_22),
    R30(30.dp, R.drawable.ic_widget_shape_30),

    /** Koło dla przycisków 44 dp. */
    CIRCLE44(22.dp, R.drawable.ic_widget_circle),
}

/** Tło w kolorze [color] z zaokrągleniem [shape] (kolor musi być nieprzezroczysty). */
internal fun GlanceModifier.rounded(color: ColorProvider, shape: WidgetShape): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        background(color).cornerRadius(shape.radius)
    } else {
        background(ImageProvider(shape.drawable), colorFilter = ColorFilter.tint(color))
    }

/** Otwiera aplikację od razu na wskazanym ekranie (przypominajka://open/…, obsługiwane przez StartRequest). */
internal fun openAppIntent(context: Context, target: StartRequest): Intent =
    Intent(context, MainActivity::class.java)
        .setData(Uri.parse("${StartRequest.SCHEME}://${StartRequest.HOST}/${target.key}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

/** Okienko szybkiego dopisania punktu do checklisty (nad ekranem głównym). */
internal fun quickAddIntent(context: Context): Intent =
    Intent(context, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
