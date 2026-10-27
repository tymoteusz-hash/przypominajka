package pl.przypominajka.ui.kartka.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.look.LookTheme

/**
 * Rama ekranu Kartki (zwykły stos ekranów): u góry mały wiersz ze strzałką „Wróć” (bez tytułu – tytuł
 * stawia się w treści przez [KartkaPageTitle]) i opcjonalnymi akcjami po prawej, na dole opcjonalny pasek
 * (np. [KartkaBottomBar] z polem dodawania – sam omija klawiaturę), nad nim miejsce na pasek „Cofnij”
 * ([KartkaUndoHost] / [KartkaSnackbarHost]). Treść dostaje [PaddingValues] z górnym wierszem, dolnym paskiem
 * i paskami systemowymi – trzeba je przekazać do listy (contentPadding) albo kolumny (padding).
 *
 * @param onBack null = bez strzałki (np. strona główna).
 * @param topBar własny górny pasek zamiast domyślnego [KartkaTopBar] (`{}` = brak).
 */
@Composable
fun KartkaScaffold(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    topBar: @Composable () -> Unit = { KartkaTopBar(onBack = onBack, actions = actions) },
    bottomBar: @Composable () -> Unit = {},
    snackbar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val c = LookTheme.kartka
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbar,
        containerColor = c.bg,
        contentColor = c.ink,
        // safeDrawing = paski systemowe + wycięcie + klawiatura (gdy nie ma dolnego paska).
        contentWindowInsets = WindowInsets.safeDrawing,
        content = content,
    )
}

/**
 * Górny wiersz Kartki: strzałka „Wróć” 48 dp (bez tytułu) i akcje po prawej (np. [KartkaIconButton]).
 * Sam omija pasek stanu.
 */
@Composable
fun KartkaTopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = LookTheme.kartka
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = 6.dp, end = 6.dp, top = 6.dp)
            .heightIn(min = KartkaDefaults.TouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            KartkaIconButton(icon = KartkaIcons.Back, contentDescription = "Wróć", onClick = onBack)
        }
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/**
 * Duży tytuł ekranu (32 sp, ciasny) i opcjonalny podtytuł pod nim – pierwszy element treści pod [KartkaTopBar].
 * Ma własne marginesy poziome ([KartkaDefaults.PagePadding]).
 */
@Composable
fun KartkaPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val c = LookTheme.kartka
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = KartkaDefaults.PagePadding, end = KartkaDefaults.PagePadding, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = KartkaType.PageTitle,
            color = c.ink,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(text = subtitle, style = KartkaType.BodySmall, color = c.ink2)
        }
    }
}

/**
 * Gotowy ekran „do przewijania”: [KartkaScaffold] + przewijana kolumna z [KartkaPageTitle] na górze.
 * Treść ma domyślnie poziome marginesy strony ([contentPadding]); wiersze list mogą je zignorować,
 * podając `contentPadding = PaddingValues(0.dp)` i dodając marginesy same.
 */
@Composable
fun KartkaPage(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbar: @Composable () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = KartkaDefaults.PagePadding),
    verticalSpacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    KartkaScaffold(
        onBack = onBack,
        modifier = modifier,
        actions = actions,
        bottomBar = bottomBar,
        snackbar = snackbar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(bottom = 24.dp),
        ) {
            KartkaPageTitle(title = title, subtitle = subtitle)
            Spacer(Modifier.heightIn(min = 16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                content = content,
            )
        }
    }
}

/**
 * Dolny pasek Kartki (np. pole „Dodaj punkt…”): tło strony, cienka linia u góry, marginesy 16 dp.
 * Sam omija pasek nawigacji i klawiaturę – wstaw go do `bottomBar` w [KartkaScaffold].
 */
@Composable
fun KartkaBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LookTheme.kartka
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
    ) {
        KartkaDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** Padding z ramy ([KartkaScaffold]) z dodatkowym odstępem na dole – do `contentPadding` list. */
@Composable
fun PaddingValues.withExtraBottom(extra: Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction),
        top = calculateTopPadding(),
        end = calculateEndPadding(direction),
        bottom = calculateBottomPadding() + extra,
    )
}
