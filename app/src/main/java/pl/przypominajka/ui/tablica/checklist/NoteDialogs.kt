package pl.przypominajka.ui.tablica.checklist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.przypominajka.data.ChecklistItem
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.tablica.components.InkTextButton
import pl.przypominajka.ui.tablica.components.NoteDialog
import pl.przypominajka.ui.tablica.components.NoteDialogButtons
import pl.przypominajka.ui.tablica.components.NoteTextField
import pl.przypominajka.ui.tablica.components.OutlineInkButton
import pl.przypominajka.ui.tablica.components.PinInk
import pl.przypominajka.ui.tablica.components.TablicaChip
import pl.przypominajka.ui.tablica.components.TablicaDateTimePicker
import pl.przypominajka.ui.tablica.components.TablicaIcons
import pl.przypominajka.ui.tablica.components.TablicaType
import pl.przypominajka.ui.tablica.components.noteFor
import pl.przypominajka.ui.tablica.components.reminderTimeText

/**
 * Szybkie dopisanie karteczki (z „+” w doku i z kafelka na stronie głównej): żółta karteczka z polem
 * „Co dopisać?” (od razu z klawiaturą), opcjonalną godziną i przyciskiem „Przyklej”.
 */
@Composable
fun NewNoteDialog(
    onDismiss: () -> Unit,
    onAdd: (text: String, remindAt: Long?) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var remindAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    var timeError by rememberSaveable(remindAt) { mutableStateOf(false) }
    val submit: () -> Unit = submit@{
        timeError = remindAt?.let { it <= System.currentTimeMillis() } == true
        if (timeError) return@submit
        if (text.isNotBlank()) {
            onAdd(text.trim(), remindAt)
            onDismiss()
        }
    }

    NoteDialog(
        onDismissRequest = onDismiss,
        title = "Nowa karteczka",
        buttons = {
            NoteDialogButtons(
                confirmText = "Przyklej",
                onConfirm = submit,
                onDismiss = onDismiss,
                confirmEnabled = text.isNotBlank(),
            )
        },
    ) {
        NoteTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "Co dopisać?",
            label = "Treść karteczki",
            focusRequester = focus,
            textStyle = TablicaType.NoteText.copy(fontSize = 26.sp, lineHeight = 28.sp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        Spacer(Modifier.height(12.dp))
        ReminderTimeControls(remindAt = remindAt, onPick = { picking = true }, onClear = { remindAt = null })
        if (timeError) Text(text = "Wybierz przyszłą datę i godzinę.", style = TablicaType.Hint, color = PinInk)
    }

    if (picking) {
        TablicaDateTimePicker(
            initialMillis = remindAt,
            onDismiss = { picking = false },
            onConfirm = {
                remindAt = it
                picking = false
            },
        )
    }

    LaunchedEffect(Unit) {
        // Okno może jeszcze nie mieć fokusu – wtedy użytkownik po prostu stuknie w pole.
        runCatching { focus.requestFocus() }
    }
}

/**
 * Poprawianie karteczki (dotknięcie jej tekstu): treść, data i godzina przypomnienia („Usuń godzinę”)
 * i „Usuń karteczkę”. Okno ma kolor tej karteczki.
 */
@Composable
fun EditNoteDialog(
    item: ChecklistItem,
    onDismiss: () -> Unit,
    onSave: (ChecklistItem) -> Unit,
    onDelete: () -> Unit,
) {
    var text by rememberSaveable(item.id) { mutableStateOf(item.text) }
    var remindAt by rememberSaveable(item.id) { mutableStateOf(item.remindAt) }
    var picking by rememberSaveable(item.id) { mutableStateOf(false) }
    var timeError by rememberSaveable(item.id, remindAt) { mutableStateOf(false) }
    val save: () -> Unit = save@{
        timeError = remindAt != item.remindAt && remindAt?.let { it <= System.currentTimeMillis() } == true
        if (timeError) return@save
        if (text.isNotBlank()) onSave(item.copy(text = text.trim(), remindAt = remindAt))
    }

    NoteDialog(
        onDismissRequest = onDismiss,
        title = "Popraw karteczkę",
        color = LookTheme.tablica.noteFor(item.id),
        tilt = 1.2f,
        buttons = {
            NoteDialogButtons(
                confirmText = "Zapisz",
                onConfirm = save,
                onDismiss = onDismiss,
                confirmEnabled = text.isNotBlank(),
            )
        },
    ) {
        NoteTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "Treść karteczki",
            label = "Treść karteczki",
            maxLines = 8,
        )
        Spacer(Modifier.height(12.dp))
        ReminderTimeControls(remindAt = remindAt, onPick = { picking = true }, onClear = { remindAt = null })
        if (timeError) Text(text = "Wybierz przyszłą datę i godzinę.", style = TablicaType.Hint, color = PinInk)
        Spacer(Modifier.height(4.dp))
        InkTextButton(text = "Usuń karteczkę", icon = Icons.Rounded.DeleteOutline, onClick = onDelete)
    }

    if (picking) {
        TablicaDateTimePicker(
            initialMillis = remindAt,
            onDismiss = { picking = false },
            onConfirm = {
                remindAt = it
                picking = false
            },
        )
    }
}

/** Godzina przypomnienia w oknie: „Dodaj godzinę” albo chip z godziną (zmiana) i „Usuń godzinę”. */
@Composable
private fun ReminderTimeControls(remindAt: Long?, onPick: () -> Unit, onClear: () -> Unit) {
    if (remindAt == null) {
        OutlineInkButton(text = "Dodaj godzinę", icon = TablicaIcons.Clock, onClick = onPick)
    } else {
        Column(horizontalAlignment = Alignment.Start) {
            TablicaChip(
                text = reminderTimeText(remindAt),
                selected = true,
                icon = TablicaIcons.Clock,
                onClick = onPick,
            )
            InkTextButton(text = "Usuń godzinę", onClick = onClear)
        }
    }
}
