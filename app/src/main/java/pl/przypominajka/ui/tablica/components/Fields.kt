package pl.przypominajka.ui.tablica.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pole tekstowe „na papierze”: bez ramki, tylko kreska pod spodem (2 dp; po ustawieniu kursora
 * pełny atrament). [handwritten] = pismo odręczne Caveat 24 (treść karteczki), inaczej Nunito 16.
 * [label] trafia do czytnika ekranu (podpowiedź [placeholder] znika po wpisaniu tekstu).
 * Wysokość ≥ 44 dp; zawija długi tekst, chyba że [singleLine].
 */
@Composable
fun NoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    handwritten: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    ink: Color = LocalContentColor.current,
    textStyle: TextStyle = if (handwritten) {
        TablicaType.NoteText
    } else {
        TablicaType.BodyLight.copy(fontSize = 16.sp, lineHeight = 22.sp)
    },
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        enabled = enabled,
        textStyle = textStyle.copy(color = ink),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        cursorBrush = SolidColor(ink),
        interactionSource = interaction,
        decorationBox = { inner ->
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(text = placeholder, style = textStyle, color = ink.copy(alpha = ink.alpha * 0.5f))
                    }
                    inner()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (focused) 2.5.dp else 2.dp)
                        .background(if (focused) ink else ink.copy(alpha = ink.alpha * 0.55f)),
                )
            }
        },
    )
}
