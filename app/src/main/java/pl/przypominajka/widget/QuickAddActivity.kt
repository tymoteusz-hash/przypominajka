package pl.przypominajka.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.przypominajka.appContainer
import pl.przypominajka.ui.look.Look
import pl.przypominajka.ui.look.LookFonts
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.ThemeMode

/**
 * Małe okienko nad ekranem głównym, otwierane przyciskiem „+” w widżecie Checklisty: jedno pole
 * z klawiaturą od razu, „Anuluj” / „Dodaj”. Wygląd jak wybrany w aplikacji (Kartka / Dzwon / Tablica).
 */
class QuickAddActivity : ComponentActivity() {

    /** Zapis w toku – drugie „Dodaj” nic nie robi. */
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appearance = appContainer.appearancePrefs.current()
        // Jak w widżecie: przed wyborem wyglądu – Dzwon.
        val look = appearance.look ?: Look.DZWON
        setContent {
            val dark = when (appearance.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LookTheme(look = look, dark = dark) {
                QuickAddScreen(look = look, onCancel = { finish() }, onAdd = { save(it) })
            }
        }
    }

    /** Okienko zamykamy dopiero po zapisie – inaczej system mógłby zakończyć proces przed wstawieniem punktu. */
    private fun save(text: String) {
        if (saving) return
        if (text.isBlank()) {
            finish()
            return
        }
        saving = true
        lifecycleScope.launch {
            withContext(NonCancellable) {
                if (appContainer.checklistRepository.add(text) > 0) {
                    runCatching { ChecklistWidget().updateAll(applicationContext) }
                }
            }
            finish()
        }
    }
}

/** Teksty i kolory okienka w danym wyglądzie. */
private class QuickAddSpec(
    val title: String,
    val placeholder: String,
    val addLabel: String,
    val scrim: Color,
)

private fun specFor(look: Look): QuickAddSpec = when (look) {
    Look.KARTKA -> QuickAddSpec("Nowy punkt", "Co dopisać do listy?", "Dodaj", Color(0x73080A0E))
    Look.DZWON -> QuickAddSpec("Nowy punkt", "Co dopisać do listy?", "Dodaj", Color(0x800A091E))
    Look.TABLICA -> QuickAddSpec("Nowa karteczka", "Co dopisać?", "Przyklej", Color(0x800C0E12))
}

@Composable
private fun QuickAddScreen(look: Look, onCancel: () -> Unit, onAdd: (String) -> Unit) {
    val spec = specFor(look)
    var text by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        // Po jednej klatce pole jest już w drzewie – wtedy fokus i klawiatura.
        withFrameNanos { }
        focusRequester.requestFocus()
        keyboard?.show()
    }
    val canAdd = text.isNotBlank() && !saving
    // Gotowe przy pustym polu po prostu zamyka okienko.
    val submit: () -> Unit = {
        if (canAdd) {
            saving = true
            onAdd(text)
        } else if (text.isBlank()) {
            onCancel()
        }
    }
    val state = QuickAddState(
        spec = spec,
        text = text,
        onTextChange = { text = it },
        canAdd = canAdd,
        onSubmit = submit,
        onCancel = onCancel,
        focusRequester = focusRequester,
    )

    // Przyciemnione tło – dotknięcie poza okienkiem zamyka je.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(spec.scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            ),
    ) {
        val bottom = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
        when (look) {
            Look.KARTKA -> KartkaQuickAdd(state, bottom.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)))
            Look.DZWON -> DzwonQuickAdd(state, bottom)
            Look.TABLICA -> TablicaQuickAdd(state, bottom.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)))
        }
    }
}

private class QuickAddState(
    val spec: QuickAddSpec,
    val text: String,
    val onTextChange: (String) -> Unit,
    val canAdd: Boolean,
    val onSubmit: () -> Unit,
    val onCancel: () -> Unit,
    val focusRequester: FocusRequester,
)

/** Dotknięcia wewnątrz okienka nie zamykają go (nie przechodzą do tła). */
private fun Modifier.swallowTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

/** Jednolinijkowe pole; Gotowe na klawiaturze = Dodaj. [container] – wygląd pola (tło / linia). */
@Composable
private fun QuickAddField(
    state: QuickAddState,
    textStyle: TextStyle,
    placeholderColor: Color,
    cursor: Color,
    container: Modifier,
) {
    BasicTextField(
        value = state.text,
        onValueChange = state.onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(state.focusRequester),
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(cursor),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { state.onSubmit() }),
        decorationBox = { inner ->
            Box(modifier = container, contentAlignment = Alignment.CenterStart) {
                if (state.text.isEmpty()) {
                    Text(text = state.spec.placeholder, style = textStyle, color = placeholderColor, maxLines = 1)
                }
                inner()
            }
        },
    )
}

/** Przycisk tekstowy 48 dp (pełny albo przezroczysty). */
@Composable
private fun QuickButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    shape: Shape,
    horizontalPadding: Dp,
    style: TextStyle,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = container.alpha * 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = style, color = if (enabled) content else content.copy(alpha = 0.5f), maxLines = 1)
    }
}

@Composable
private fun QuickButtons(state: QuickAddState, cancel: @Composable () -> Unit, add: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cancel()
        add()
    }
}

// ---------------------------------------------------------------- Kartka: karta 24 dp nad klawiaturą

@Composable
private fun KartkaQuickAdd(state: QuickAddState, modifier: Modifier) {
    val k = LookTheme.kartka
    val dark = LookTheme.isDark
    val card = if (dark) k.surface else k.bg
    val field = if (dark) k.raised else k.surface
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(card)
            .swallowTaps()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = state.spec.title, style = MaterialTheme.typography.titleMedium, color = k.ink)
        QuickAddField(
            state = state,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = k.ink),
            placeholderColor = k.ink2,
            cursor = k.accent,
            container = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(field)
                .padding(horizontal = 14.dp),
        )
        val label = MaterialTheme.typography.labelLarge
        QuickButtons(
            state = state,
            cancel = {
                QuickButton("Anuluj", state.onCancel, Color.Transparent, k.ink, shape, 16.dp, label)
            },
            add = {
                QuickButton(state.spec.addLabel, state.onSubmit, k.accent, k.onAccent, shape, 20.dp, label, state.canAdd)
            },
        )
    }
}

// ---------------------------------------------------------------- Dzwon: arkusz od dołu z uchwytem

@Composable
private fun DzwonQuickAdd(state: QuickAddState, modifier: Modifier) {
    val d = LookTheme.dzwon
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(d.surface)
            .swallowTaps()
            // Arkusz sięga do dołu ekranu; treść – nad klawiaturą i paskiem nawigacji.
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(d.track),
        )
        Text(text = state.spec.title, style = MaterialTheme.typography.headlineSmall, color = d.ink)
        QuickAddField(
            state = state,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = d.ink),
            placeholderColor = d.ink2,
            cursor = d.goldText,
            container = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(shape)
                .background(d.field)
                .padding(horizontal = 16.dp),
        )
        val label = MaterialTheme.typography.labelLarge
        QuickButtons(
            state = state,
            cancel = {
                QuickButton(
                    "Anuluj", state.onCancel, Color.Transparent, d.ink, shape, 18.dp,
                    label.copy(fontWeight = FontWeight.Bold),
                )
            },
            add = {
                QuickButton(state.spec.addLabel, state.onSubmit, d.gold, d.onGold, shape, 22.dp, label, state.canAdd)
            },
        )
    }
}

// ---------------------------------------------------------------- Tablica: przechylona żółta karteczka z taśmą

@Composable
private fun TablicaQuickAdd(state: QuickAddState, modifier: Modifier) {
    val t = LookTheme.tablica
    val note = t.yellow
    val noteShape = RoundedCornerShape(6.dp)
    val handwriting = TextStyle(
        fontFamily = LookFonts.TablicaHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        color = note.ink,
    )
    Box(
        modifier = modifier
            .padding(horizontal = 28.dp, vertical = 24.dp)
            .graphicsLayer { rotationZ = -1.5f },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 18.dp, shape = noteShape)
                .background(note.bg, noteShape)
                .swallowTaps()
                .padding(start = 18.dp, top = 24.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = state.spec.title, style = MaterialTheme.typography.headlineSmall, color = note.ink)
            QuickAddField(
                state = state,
                textStyle = handwriting,
                placeholderColor = note.ink.copy(alpha = 0.45f),
                cursor = note.ink,
                container = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .drawBehind {
                        val stroke = 2.5.dp.toPx()
                        val y = size.height - stroke / 2
                        drawLine(note.ink, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
                    }
                    .padding(horizontal = 2.dp),
            )
            val label = MaterialTheme.typography.labelLarge
            QuickButtons(
                state = state,
                cancel = {
                    QuickButton("Anuluj", state.onCancel, Color.Transparent, note.ink, CircleShape, 16.dp, label)
                },
                add = {
                    QuickButton(state.spec.addLabel, state.onSubmit, note.ink, note.bg, CircleShape, 20.dp, label, state.canAdd)
                },
            )
        }
        // Taśma klejąca na górnej krawędzi.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp)
                .size(width = 70.dp, height = 20.dp)
                .background(t.tape),
        )
    }
}
