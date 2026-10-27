@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.account.AccountUiState
import pl.przypominajka.ui.account.AccountViewModel
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.util.formatSyncTime
import java.time.LocalDate

/** Konto Librus i klucz AI jako osobne karteczki na tablicy. */
@Composable
fun TablicaAccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LookTheme.tablica
    var disconnect by rememberSaveable { mutableStateOf(false) }
    var showLog by rememberSaveable { mutableStateOf(false) }
    TablicaScreen {
        Column(Modifier.fillMaxSize().imePadding()) {
            TablicaTopBar("Konto", onBack, subtitle = "Librus i klucz AI")
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = LocalDockPadding.current + 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                AccountNote("Librus Synergia", colors.mint, -0.6f) {
                    Text(if (state.status.connected) "Połączono · ${state.status.loginHint}" else "Nie połączono", style = TablicaType.Strong)
                }
                if (state.status.connected) {
                    AccountNote("Synchronizacja", colors.blue, 0.5f) {
                        Text("Automatycznie co ok. 6 godzin, gdy jest internet.", style = TablicaType.BodyLight)
                        if (state.status.lastSyncAt > 0L) {
                            val time = formatSyncTime(state.status.lastSyncAt, LocalDate.now())
                            Text(if (state.status.lastSyncOk) "Ostatnio: $time" else "Błąd ($time)", style = TablicaType.Strong)
                            Text(state.status.lastSyncMessage, style = TablicaType.BodyLight)
                        }
                        InkButton(
                            if (state.isWorking) "Synchronizuję…" else "Synchronizuj teraz",
                            viewModel::syncNow, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    AccountNote("Co zyskasz", colors.yellow, 0.5f) {
                        Text("Plan lekcji w zakładce Plan, a sprawdziany, kartkówki i wyjścia z terminarza same trafią do folderu Szkoła. Przypomnienia przyjdą godzinę po ostatniej lekcji. Zadania domowe dalej wpisujesz sam.", style = TablicaType.BodyLight)
                    }
                    AccountNote("Bezpieczeństwo", colors.lilac, -0.5f) {
                        Text("Librus nie ma oficjalnego API dla uczniów – aplikacja loguje się tak jak przeglądarka. Login i hasło są zaszyfrowane i zostają tylko na tym telefonie; połączenia idą wyłącznie do *.librus.pl.", style = TablicaType.BodyLight)
                    }
                    LoginNote(state, viewModel)
                }
                AccountNote("AI do quizów", colors.lilac, 0.4f) {
                    Text("Pytania generuje Claude (Anthropic). Potrzebny jest Twój klucz API z console.anthropic.com – płacisz za użycie, jeden quiz to zwykle kilka groszy. Klucz jest zaszyfrowany i zostaje na telefonie.", style = TablicaType.BodyLight)
                    if (state.aiHasKey) {
                        Text("Klucz zapisany: ${state.aiKeyHint}", style = TablicaType.Strong)
                        OutlineInkButton("Usuń klucz", viewModel::clearAiKey)
                    } else {
                        SecretField(
                            value = state.aiKeyInput, onChange = viewModel::onAiKeyChange,
                            label = "Klucz API", placeholder = "sk-ant-…", onDone = viewModel::saveAiKey,
                        )
                        InkButton("Zapisz klucz", viewModel::saveAiKey, enabled = state.aiKeyInput.isNotBlank(), modifier = Modifier.fillMaxWidth())
                    }
                }
                if (state.status.connected) {
                    if (state.status.lastSyncLog.isNotBlank()) {
                        AccountNote("Szczegóły synchronizacji", colors.yellow, -0.4f) {
                            Text("Przydatne, gdy coś nie działa. Bez hasła.", style = TablicaType.Hint)
                            InkTextButton(if (showLog) "Ukryj szczegóły" else "Pokaż szczegóły", { showLog = !showLog })
                            if (showLog) SelectionContainer {
                                Text(state.status.lastSyncLog, style = TablicaType.Hint, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    OutlineInkButton("Rozłącz konto Librus", { disconnect = true }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    if (disconnect) {
        NoteDialog(
            onDismissRequest = { disconnect = false }, title = "Rozłączyć Librusa?", color = colors.pink,
            buttons = { InkTextButton("Anuluj", { disconnect = false }) },
        ) {
            Text("Login i hasło zostaną usunięte z telefonu, a synchronizacja w tle wyłączona.", style = TablicaType.BodyLight)
            Spacer(Modifier.height(12.dp))
            InkButton("Rozłącz i usuń dane z Librusa", {
                disconnect = false
                viewModel.disconnect(deleteData = true)
            }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlineInkButton("Rozłącz, zostaw przypomnienia i plan", {
                disconnect = false
                viewModel.disconnect(deleteData = false)
            }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LoginNote(state: AccountUiState, viewModel: AccountViewModel) {
    AccountNote("Zaloguj się", LookTheme.tablica.blue, -0.3f) {
        Text("Te same dane, którymi logujesz się na synergia.librus.pl.", style = TablicaType.BodyLight)
        NoteTextField(
            value = state.login, onValueChange = viewModel::onLoginChange,
            label = "Login", placeholder = "Login, np. 1234567u", handwritten = false,
            singleLine = true, enabled = !state.isWorking,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
        SecretField(
            value = state.password, onChange = viewModel::onPasswordChange, label = "Hasło",
            visible = state.showPassword, onToggle = viewModel::toggleShowPassword,
            enabled = !state.isWorking, onDone = viewModel::connect,
        )
        state.formError?.let {
            Text("Nie udało się połączyć", style = TablicaType.Strong)
            Text(it, style = TablicaType.BodyLight)
        }
        InkButton(
            if (state.isWorking) "Łączenie z Librusem…" else "Połącz i pobierz dane",
            viewModel::connect, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Pole z ukrytym hasłem; kolory pochodzą z papieru karteczki. */
@Composable
private fun SecretField(
    value: String, onChange: (String) -> Unit, label: String, onDone: () -> Unit,
    placeholder: String = "", visible: Boolean = false, onToggle: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val ink = LocalContentColor.current
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        placeholder = { Text(placeholder) }, singleLine = true, enabled = enabled,
        textStyle = TablicaType.BodyLight,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = {
            if (onToggle != null) InkTextButton(if (visible) "Ukryj" else "Pokaż", onToggle, enabled = enabled)
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ink, unfocusedTextColor = ink, cursorColor = ink,
            focusedBorderColor = ink, unfocusedBorderColor = ink.copy(alpha = 0.6f),
            focusedLabelColor = ink, unfocusedLabelColor = ink,
            focusedPlaceholderColor = ink.copy(alpha = 0.6f), unfocusedPlaceholderColor = ink.copy(alpha = 0.6f),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AccountNote(title: String, color: NoteColor, tilt: Float, content: @Composable ColumnScope.() -> Unit) {
    Note(color, Modifier.fillMaxWidth(), tilt = tilt, decoration = NoteDecoration.Tape,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(title)
        content()
    }
}
