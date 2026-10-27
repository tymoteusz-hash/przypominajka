@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import pl.przypominajka.ui.kartka.components.KartkaButton
import pl.przypominajka.ui.kartka.components.KartkaCard
import pl.przypominajka.ui.kartka.components.KartkaDefaults
import pl.przypominajka.ui.kartka.components.KartkaDialog
import pl.przypominajka.ui.kartka.components.KartkaDivider
import pl.przypominajka.ui.kartka.components.KartkaIcons
import pl.przypominajka.ui.kartka.components.KartkaListRow
import pl.przypominajka.ui.kartka.components.KartkaNotice
import pl.przypominajka.ui.kartka.components.KartkaOutlineButton
import pl.przypominajka.ui.kartka.components.KartkaOutlineCard
import pl.przypominajka.ui.kartka.components.KartkaPageTitle
import pl.przypominajka.ui.kartka.components.KartkaScaffold
import pl.przypominajka.ui.kartka.components.KartkaSectionHeader
import pl.przypominajka.ui.kartka.components.KartkaTextButton
import pl.przypominajka.ui.kartka.components.KartkaTextField
import pl.przypominajka.ui.kartka.components.KartkaType
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.util.formatSyncTime
import java.time.LocalDate

/** Konto w Kartce: połączenie Librusa, stan synchronizacji i klucz do quizów. */
@Composable
fun KartkaAccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDisconnect by rememberSaveable { mutableStateOf(false) }
    var showLog by rememberSaveable { mutableStateOf(false) }
    val c = LookTheme.kartka

    KartkaScaffold(onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .imePadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            KartkaPageTitle(title = "Konto", subtitle = "Librus i AI do quizów")
            Column(
                modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                KartkaOutlineCard {
                    KartkaListRow(
                        title = "Librus Synergia",
                        subtitle = if (state.status.connected) "Połączono · ${state.status.loginHint}" else "Nie połączono",
                        showDivider = false,
                    )
                }
                if (state.status.connected) {
                    AccountSection("Synchronizacja", "Automatycznie co ok. 6 godzin, gdy jest internet.") {
                        val status = state.status
                        if (status.lastSyncAt > 0L) {
                            val whenText = formatSyncTime(status.lastSyncAt, LocalDate.now())
                            KartkaListRow(
                                title = if (status.lastSyncOk) "Ostatnio: $whenText" else "Błąd ($whenText)",
                                subtitle = status.lastSyncMessage,
                                titleColor = if (status.lastSyncOk) c.accent else MaterialTheme.colorScheme.error,
                                showDivider = false,
                            )
                        }
                        KartkaButton(
                            text = if (state.isWorking) "Synchronizuję…" else "Synchronizuj teraz",
                            onClick = viewModel::syncNow,
                            enabled = !state.isWorking,
                            icon = KartkaIcons.Refresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    LoginSection(state, viewModel)
                }

                AccountSection(
                    "AI do quizów",
                    "Pytania generuje Claude (Anthropic). Potrzebny jest Twój klucz API z console.anthropic.com – " +
                        "płacisz za użycie, jeden quiz to zwykle kilka groszy. Klucz jest zaszyfrowany i zostaje na telefonie.",
                ) {
                    if (state.aiHasKey) {
                        KartkaListRow(
                            title = "Klucz zapisany",
                            subtitle = state.aiKeyHint,
                            trailing = {
                                KartkaTextButton("Usuń", viewModel::clearAiKey, color = MaterialTheme.colorScheme.error)
                            },
                        )
                    } else {
                        KartkaTextField(
                            value = state.aiKeyInput,
                            onValueChange = viewModel::onAiKeyChange,
                            label = "Klucz API",
                            placeholder = "sk-ant-…",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { viewModel.saveAiKey() }),
                        )
                        KartkaButton(
                            "Zapisz klucz", viewModel::saveAiKey,
                            modifier = Modifier.fillMaxWidth(), enabled = state.aiKeyInput.isNotBlank(),
                        )
                    }
                }

                if (state.status.connected) {
                    if (state.status.lastSyncLog.isNotBlank()) {
                        AccountSection("Szczegóły synchronizacji", "Przydatne, gdy coś nie działa. Bez hasła.") {
                            KartkaTextButton(
                                text = if (showLog) "Ukryj szczegóły" else "Pokaż szczegóły",
                                onClick = { showLog = !showLog },
                            )
                            if (showLog) KartkaCard {
                                Text(
                                    state.status.lastSyncLog,
                                    style = KartkaType.Caption.copy(fontFamily = FontFamily.Monospace), color = c.ink,
                                )
                            }
                        }
                    }
                    KartkaDivider()
                    KartkaTextButton(
                        "Rozłącz konto Librus", { showDisconnect = true },
                        modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showDisconnect) KartkaDialog(
        onDismissRequest = { showDisconnect = false },
        title = "Rozłączyć Librusa?",
        text = "Login i hasło zostaną usunięte z telefonu, a synchronizacja w tle wyłączona.",
        content = {
            KartkaButton(
                "Rozłącz i usuń dane z Librusa",
                { showDisconnect = false; viewModel.disconnect(deleteData = true) },
                modifier = Modifier.fillMaxWidth(), destructive = true,
            )
            KartkaOutlineButton(
                "Rozłącz, zostaw przypomnienia i plan",
                { showDisconnect = false; viewModel.disconnect(deleteData = false) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
private fun LoginSection(state: AccountUiState, viewModel: AccountViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KartkaNotice(
            title = "Co zyskasz",
            text = "Plan lekcji w zakładce Plan, a sprawdziany, kartkówki i wyjścia z terminarza same trafią " +
                "do folderu Szkoła. Przypomnienia przyjdą godzinę po ostatniej lekcji. Zadania domowe dalej wpisujesz sam.",
            icon = KartkaIcons.Plan,
        )
        KartkaNotice(
            title = "Bezpieczeństwo",
            text = "Librus nie ma oficjalnego API dla uczniów – aplikacja loguje się tak jak przeglądarka. " +
                "Login i hasło są zaszyfrowane i zostają tylko na tym telefonie; połączenia idą wyłącznie do *.librus.pl.",
        )
    }
    AccountSection("Zaloguj się", "Te same dane, którymi logujesz się na synergia.librus.pl.") {
        KartkaTextField(
            value = state.login, onValueChange = viewModel::onLoginChange,
            label = "Login", placeholder = "np. 1234567u", enabled = !state.isWorking,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        )
        KartkaTextField(
            value = state.password, onValueChange = viewModel::onPasswordChange,
            label = "Hasło", enabled = !state.isWorking,
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.connect() }),
            trailing = {
                KartkaTextButton(if (state.showPassword) "Ukryj" else "Pokaż", viewModel::toggleShowPassword)
            },
        )
        state.formError?.let { error ->
            Text("Nie udało się połączyć: $error", style = KartkaType.BodySmall, color = MaterialTheme.colorScheme.error)
        }
        KartkaButton(
            text = if (state.isWorking) "Łączenie z Librusem…" else "Połącz i pobierz dane",
            onClick = viewModel::connect, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AccountSection(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KartkaSectionHeader(title)
        Text(description, style = KartkaType.BodySmall, color = LookTheme.kartka.ink2)
        content()
    }
}
