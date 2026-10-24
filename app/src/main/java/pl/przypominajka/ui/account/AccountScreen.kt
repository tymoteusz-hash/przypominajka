package pl.przypominajka.ui.account

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.Bell
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.FormSection
import pl.przypominajka.ui.components.HeroCard
import pl.przypominajka.ui.components.IconBadge
import pl.przypominajka.ui.components.PastelBadge
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.pressScale
import pl.przypominajka.ui.theme.Pastel
import pl.przypominajka.ui.components.LocalBottomBarPadding
import pl.przypominajka.ui.components.NoticeCard
import pl.przypominajka.ui.components.ReminderTimePickerDialog
import pl.przypominajka.ui.components.ScreenHeader
import pl.przypominajka.ui.components.accent
import pl.przypominajka.ui.components.icon
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.util.Appearance
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.ThemeMode
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.dayName
import pl.przypominajka.util.formatSyncTime
import pl.przypominajka.util.leadDaysText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDisconnect by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = AppSurfaces.page,
        contentWindowInsets = WindowInsets(0),
        topBar = { BellTopBar(title = "Konto", eyebrow = "Librus i AI", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileCard(connected = state.status.connected, loginHint = state.status.loginHint)

                if (state.status.connected) {
                    SyncSection(state = state, onSyncNow = viewModel::syncNow)
                } else {
                    LoginSection(
                        state = state,
                        onLoginChange = viewModel::onLoginChange,
                        onPasswordChange = viewModel::onPasswordChange,
                        onToggleShowPassword = viewModel::toggleShowPassword,
                        onConnect = viewModel::connect,
                    )
                }

                AiKeySection(
                    state = state,
                    onKeyChange = viewModel::onAiKeyChange,
                    onSave = viewModel::saveAiKey,
                    onClear = viewModel::clearAiKey,
                )

                if (state.status.connected) {
                    LogSection(log = state.status.lastSyncLog)
                    OutlinedButton(
                        onClick = { showDisconnect = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Rozłącz konto Librus") }
                }
                Spacer(Modifier.height(LocalBottomBarPadding.current + 16.dp))
            }
        }
    }

    if (showDisconnect) {
        AlertDialog(
            onDismissRequest = { showDisconnect = false },
            title = { Text("Rozłączyć Librusa?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Login i hasło zostaną usunięte z telefonu, a synchronizacja w tle wyłączona.")
                    Button(
                        onClick = {
                            showDisconnect = false
                            viewModel.disconnect(deleteData = true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text("Rozłącz i usuń dane z Librusa") }
                    OutlinedButton(
                        onClick = {
                            showDisconnect = false
                            viewModel.disconnect(deleteData = false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Rozłącz, zostaw przypomnienia i plan") }
                }
            },
            confirmButton = { TextButton(onClick = { showDisconnect = false }) { Text("Anuluj") } },
        )
    }
}

@Composable
private fun ProfileCard(connected: Boolean, loginHint: String) {
    HeroCard(modifier = Modifier.appear(1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Librus Synergia", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    text = if (connected) "Połączono · $loginHint" else "Nie połączono",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (connected) Color(0xFFA8CFA3) else Color.White.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun LoginSection(
    state: AccountUiState,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onConnect: () -> Unit,
) {
    NoticeCard(
        title = "Co zyskasz",
        body = "Plan lekcji w zakładce Plan, a sprawdziany, kartkówki i wyjścia z terminarza same trafią " +
            "do folderu Szkoła. Przypomnienia przyjdą godzinę po ostatniej lekcji. Zadania domowe dalej wpisujesz sam.",
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = Icons.Rounded.Info,
    )
    NoticeCard(
        title = "Bezpieczeństwo",
        body = "Librus nie ma oficjalnego API dla uczniów – aplikacja loguje się tak jak przeglądarka. " +
            "Login i hasło są zaszyfrowane i zostają tylko na tym telefonie; połączenia idą wyłącznie do *.librus.pl.",
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        icon = Icons.Rounded.Lock,
    )
    FormSection(title = "Zaloguj się", description = "Te same dane, którymi logujesz się na synergia.librus.pl.") {
        OutlinedTextField(
            value = state.login,
            onValueChange = onLoginChange,
            label = { Text("Login") },
            placeholder = { Text("np. 1234567u") },
            singleLine = true,
            enabled = !state.isWorking,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Hasło") },
            singleLine = true,
            enabled = !state.isWorking,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConnect() }),
            trailingIcon = {
                TextButton(onClick = onToggleShowPassword) { Text(if (state.showPassword) "Ukryj" else "Pokaż") }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        state.formError?.let { error ->
            NoticeCard(
                title = "Nie udało się połączyć",
                body = error,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Rounded.Warning,
            )
        }
        Button(
            onClick = onConnect,
            enabled = !state.isWorking,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.isWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(12.dp))
                Text("Łączenie z Librusem…")
            } else {
                Text("Połącz i pobierz dane")
            }
        }
    }
}

@Composable
private fun SyncSection(state: AccountUiState, onSyncNow: () -> Unit) {
    val status = state.status
    FormSection(title = "Synchronizacja", description = "Automatycznie co ok. 6 godzin, gdy jest internet.") {
        if (status.lastSyncAt > 0L) {
            val whenText = formatSyncTime(status.lastSyncAt, LocalDate.now())
            NoticeCard(
                title = if (status.lastSyncOk) "Ostatnio: $whenText" else "Błąd ($whenText)",
                body = status.lastSyncMessage,
                containerColor = if (status.lastSyncOk) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                contentColor = if (status.lastSyncOk) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                icon = if (status.lastSyncOk) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            )
        }
        FilledTonalButton(
            onClick = onSyncNow,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Synchronizuję…")
            } else {
                Icon(Icons.Rounded.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Synchronizuj teraz")
            }
        }
    }
}

/** Klucz API Anthropic do generowania pytań w quizach. */
@Composable
private fun AiKeySection(
    state: AccountUiState,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    FormSection(
        title = "AI do quizów",
        description = "Pytania generuje Claude (Anthropic). Potrzebny jest Twój klucz API z console.anthropic.com – " +
            "płacisz za użycie, jeden quiz to zwykle kilka groszy. Klucz jest zaszyfrowany i zostaje na telefonie.",
    ) {
        if (state.aiHasKey) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Klucz zapisany: ${state.aiKeyHint}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClear) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            }
        } else {
            OutlinedTextField(
                value = state.aiKeyInput,
                onValueChange = onKeyChange,
                label = { Text("Klucz API") },
                placeholder = { Text("sk-ant-…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                enabled = state.aiKeyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Zapisz klucz") }
        }
    }
}

@Composable
private fun LogSection(log: String) {
    if (log.isBlank()) return
    var showLog by rememberSaveable { mutableStateOf(false) }
    FormSection(title = "Szczegóły synchronizacji", description = "Przydatne, gdy coś nie działa. Bez hasła.") {
        TextButton(onClick = { showLog = !showLog }) { Text(if (showLog) "Ukryj szczegóły" else "Pokaż szczegóły") }
        if (showLog) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
