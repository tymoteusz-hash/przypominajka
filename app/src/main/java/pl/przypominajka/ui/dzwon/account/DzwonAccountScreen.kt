@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.dzwon.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.account.AccountUiState
import pl.przypominajka.ui.account.AccountViewModel
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.formatSyncTime
import java.time.LocalDate

/** Konto w arkuszu Dzwonu; logowanie i synchronizację prowadzi wspólny ViewModel. */
@Composable
fun DzwonAccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    var showDisconnect by rememberSaveable { mutableStateOf(false) }

    DzwonSheetScaffold(title = "Konto", subtitle = "Librus i AI", onClose = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DzwonNotice(
                title = "Librus Synergia",
                text = if (state.status.connected) "Połączono · ${state.status.loginHint}" else "Nie połączono",
                icon = DzwonIcons.Person,
                containerColor = c.field,
            )
            if (state.status.connected) {
                SyncSection(state, viewModel::syncNow)
            } else {
                DzwonNotice(
                    title = "Co zyskasz",
                    text = "Plan lekcji w zakładce Plan, a sprawdziany, kartkówki i wyjścia z terminarza same trafią " +
                        "do folderu Szkoła. Przypomnienia przyjdą godzinę po ostatniej lekcji. Zadania domowe dalej wpisujesz sam.",
                    icon = Icons.Rounded.Info,
                    containerColor = c.field,
                )
                DzwonNotice(
                    title = "Bezpieczeństwo",
                    text = "Librus nie ma oficjalnego API dla uczniów – aplikacja loguje się tak jak przeglądarka. " +
                        "Login i hasło są zaszyfrowane i zostają tylko na tym telefonie; połączenia idą wyłącznie do *.librus.pl.",
                    icon = Icons.Rounded.Lock,
                    containerColor = c.field,
                )
                AccountSection("Zaloguj się", "Te same dane, którymi logujesz się na synergia.librus.pl.") {
                    DzwonTextField(
                        value = state.login,
                        onValueChange = viewModel::onLoginChange,
                        label = "Login",
                        placeholder = "np. 1234567u",
                        enabled = !state.isWorking,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                    )
                    SecretField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Hasło",
                        visible = state.showPassword,
                        enabled = !state.isWorking,
                        onDone = viewModel::connect,
                        trailing = {
                            DzwonTextButton(
                                text = if (state.showPassword) "Ukryj" else "Pokaż",
                                onClick = viewModel::toggleShowPassword,
                            )
                        },
                    )
                    state.formError?.let {
                        Text(
                            "Nie udało się połączyć: $it",
                            color = MaterialTheme.colorScheme.error,
                            style = DzwonType.Body,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    DzwonPrimaryButton(
                        text = if (state.isWorking) "Łączenie z Librusem…" else "Połącz i pobierz dane",
                        onClick = viewModel::connect,
                        enabled = !state.isWorking,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            AccountSection(
                "AI do quizów",
                "Pytania generuje Claude (Anthropic). Potrzebny jest Twój klucz API z console.anthropic.com – " +
                    "płacisz za użycie, jeden quiz to zwykle kilka groszy. Klucz jest zaszyfrowany i zostaje na telefonie.",
            ) {
                if (state.aiHasKey) {
                    DzwonListRow(
                        title = "Klucz zapisany: ${state.aiKeyHint}",
                        trailing = {
                            DzwonTextButton("Usuń", viewModel::clearAiKey, color = MaterialTheme.colorScheme.error)
                        },
                    )
                } else {
                    SecretField(
                        value = state.aiKeyInput,
                        onValueChange = viewModel::onAiKeyChange,
                        label = "Klucz API",
                        placeholder = "sk-ant-…",
                        onDone = viewModel::saveAiKey,
                    )
                    DzwonPrimaryButton(
                        text = "Zapisz klucz",
                        onClick = viewModel::saveAiKey,
                        enabled = state.aiKeyInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.status.connected) {
                if (state.status.lastSyncLog.isNotBlank()) LogSection(state.status.lastSyncLog)
                DzwonTextButton(
                    text = "Rozłącz konto Librus",
                    onClick = { showDisconnect = true },
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (showDisconnect) {
        DzwonBottomSheet(onDismissRequest = { showDisconnect = false }, title = "Rozłączyć Librusa?") {
            Text(
                "Login i hasło zostaną usunięte z telefonu, a synchronizacja w tle wyłączona.",
                style = DzwonType.Body, color = c.ink2,
            )
            DzwonTextButton(
                text = "Rozłącz i usuń dane z Librusa",
                onClick = { showDisconnect = false; viewModel.disconnect(deleteData = true) },
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
            DzwonSecondaryButton(
                text = "Rozłącz, zostaw przypomnienia i plan",
                onClick = { showDisconnect = false; viewModel.disconnect(deleteData = false) },
                modifier = Modifier.fillMaxWidth(),
            )
            DzwonTextButton("Anuluj", { showDisconnect = false }, modifier = Modifier.fillMaxWidth(), color = c.ink)
        }
    }
}

@Composable
private fun SyncSection(state: AccountUiState, onSync: () -> Unit) {
    val status = state.status
    AccountSection("Synchronizacja", "Automatycznie co ok. 6 godzin, gdy jest internet.") {
        if (status.lastSyncAt > 0L) {
            val time = formatSyncTime(status.lastSyncAt, LocalDate.now())
            Text(
                text = if (status.lastSyncOk) "Ostatnio: $time" else "Błąd ($time)",
                style = DzwonType.RowTitle,
                color = if (status.lastSyncOk) LookTheme.dzwon.goldText else MaterialTheme.colorScheme.error,
            )
            Text(status.lastSyncMessage, style = DzwonType.Body, color = LookTheme.dzwon.ink)
        }
        DzwonPrimaryButton(
            text = if (state.isWorking) "Synchronizuję…" else "Synchronizuj teraz",
            onClick = onSync,
            enabled = !state.isWorking,
            icon = Icons.Rounded.Sync,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LogSection(log: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    AccountSection("Szczegóły synchronizacji", "Przydatne, gdy coś nie działa. Bez hasła.") {
        DzwonTextButton(if (expanded) "Ukryj szczegóły" else "Pokaż szczegóły", { expanded = !expanded })
        if (expanded) {
            SelectionContainer {
                Text(log, style = DzwonType.Caption, fontFamily = FontFamily.Monospace, color = LookTheme.dzwon.ink)
            }
        }
    }
}

@Composable
private fun AccountSection(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    DzwonCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DzwonSectionHeader(title)
            Text(description, style = DzwonType.Caption, color = LookTheme.dzwon.ink2)
            content()
        }
    }
}

// Wspólne pole Dzwonu nie maskuje tekstu; poufne dane mają lokalny wariant w tych samych kolorach.
@Composable
private fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onDone: () -> Unit,
    placeholder: String? = null,
    visible: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LookTheme.dzwon
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = DzwonType.CaptionStrong) },
        placeholder = { if (placeholder != null) Text(placeholder, style = DzwonType.Body) },
        enabled = enabled,
        singleLine = true,
        textStyle = DzwonType.Body,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.ink, unfocusedTextColor = c.ink,
            disabledTextColor = c.ink2,
            focusedContainerColor = c.field, unfocusedContainerColor = c.field, disabledContainerColor = c.field,
            focusedBorderColor = c.gold, unfocusedBorderColor = c.line, disabledBorderColor = c.line,
            focusedLabelColor = c.goldText, unfocusedLabelColor = c.ink2,
            focusedPlaceholderColor = c.ink2, unfocusedPlaceholderColor = c.ink2, cursorColor = c.goldText,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
