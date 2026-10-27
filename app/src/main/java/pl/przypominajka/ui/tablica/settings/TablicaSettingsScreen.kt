@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.look.Look
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.look.NoteColor
import pl.przypominajka.ui.settings.SettingsUiState
import pl.przypominajka.ui.settings.SettingsViewModel
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.update.UpdateManager
import pl.przypominajka.update.UpdateState
import pl.przypominajka.util.BellTone
import pl.przypominajka.util.SchoolPrefs
import pl.przypominajka.util.ThemeMode
import pl.przypominajka.util.TimeFormatter
import pl.przypominajka.util.dayName
import pl.przypominajka.util.leadDaysText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/** Preferencje aplikacji na przypiętych karteczkach Tablicy. */
@Composable
fun TablicaSettingsScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LookTheme.tablica
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshReminderReliability() }
    var editingDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    TablicaScreen {
        Column(Modifier.fillMaxSize().imePadding()) {
            TablicaTopBar("Ustawienia", onBack, subtitle = "Twoja tablica, Twoje zasady")
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = LocalDockPadding.current + 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                SettingsNote("Wygląd", colors.yellow, -0.5f) {
                    Text("Razem z wyglądem zmienia się ikona aplikacji. Na Samsungu może zniknąć z ekranu głównego – wtedy przeciągnij ją z listy aplikacji jeszcze raz.", style = TablicaType.BodyLight)
                    Column(Modifier.selectableGroup()) {
                        Look.entries.forEach { look ->
                            ChoiceRow(look.label, state.appearance.look == look, { viewModel.setLook(look) }, look.description)
                        }
                    }
                    NoteDivider()
                    SectionLabel("Motyw")
                    Column(Modifier.selectableGroup()) {
                        listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach { mode ->
                            ChoiceRow(mode.label, state.appearance.theme == mode, { viewModel.setTheme(mode) })
                        }
                    }
                }
                SettingsNote("Dźwięk powiadomień", colors.blue, 0.4f) {
                    Text("Czym dzwoni przypomnienie. Użyj przycisku odsłuchu, żeby posłuchać.", style = TablicaType.BodyLight)
                    Column(Modifier.selectableGroup()) {
                        BellTone.entries.forEach { tone ->
                            ChoiceRow(tone.label, state.appearance.notificationTone == tone,
                                { viewModel.setNotificationTone(tone) }, trailing = {
                                    if (tone != BellTone.SILENT) RoundIconButton(
                                        Icons.Rounded.PlayArrow, "Posłuchaj: ${tone.label}",
                                        { viewModel.preview(tone) }, style = RoundIconStyle.Plain,
                                    )
                                })
                        }
                    }
                    InkTextButton("Więcej w ustawieniach telefonu", {
                        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, viewModel.reminderChannelId)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                        }
                    }, modifier = Modifier.fillMaxWidth())
                }
                ReminderReliabilityNote(viewModel)
                LeadDaysNote(state, viewModel)
                SettingsNote("Koniec lekcji", colors.mint, 0.4f) {
                    Text(
                        if (state.librusConnected) "Gdy w planie z Librusa brakuje danego dnia, użyjemy tych godzin."
                        else "Na tej podstawie przychodzą przypomnienia „po lekcjach” (godzinę później).",
                        style = TablicaType.BodyLight,
                    )
                    SchoolPrefs.SCHOOL_DAYS.forEach { day ->
                        val time = state.weekdayEnds[day] ?: LocalTime.of(15, 0)
                        NoteRow(
                            title = dayName(LocalDate.now().with(day)).replaceFirstChar { it.uppercase() },
                            subtitle = time.format(TimeFormatter), onClick = { editingDay = day },
                            onClickLabel = "Zmień godzinę końca lekcji",
                            trailing = { Text("Zmień", style = TablicaType.Link) },
                        )
                    }
                }
                Note(colors.lilac, Modifier.fillMaxWidth(), tilt = -0.4f, decoration = NoteDecoration.Tape,
                    onClick = onOpenFolders, onClickLabel = "Otwórz foldery") {
                    NoteRow("Foldery", subtitle = "Dodawaj, zmieniaj nazwy i kolory", trailing = {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    })
                }
                UpdatesNote(viewModel.updates)
                Text("Przypominajka $version", style = TablicaType.Hint,
                    color = colors.ink2, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
    editingDay?.let { day ->
        TablicaTimePickerDialog(
            initial = state.weekdayEnds[day] ?: LocalTime.of(15, 0),
            title = "Koniec lekcji: ${dayName(LocalDate.now().with(day))}",
            onDismiss = { editingDay = null },
            onConfirm = { time ->
                viewModel.setWeekdayEnd(day, time)
                editingDay = null
            },
        )
    }
}

@Composable
private fun ReminderReliabilityNote(viewModel: SettingsViewModel) {
    val status by viewModel.reliability.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsNote("Czy przypomnienia działają?", LookTheme.tablica.yellow, -0.3f) {
        Text("Sprawdź zgody telefonu i wyślij próbne powiadomienie.", style = TablicaType.BodyLight)
        NoteRow("Powiadomienia", subtitle = if (status.notificationsAllowed) "Dozwolone" else "Zablokowane")
        InkTextButton("Ustawienia powiadomień", { viewModel.openNotificationSettings(context) }, modifier = Modifier.fillMaxWidth())
        NoteDivider()
        NoteRow("Dokładne alarmy", subtitle = if (status.exactAlarmsAllowed) "Dozwolone" else "Brak zgody")
        InkTextButton("Ustawienia dokładnych alarmów", { viewModel.openExactAlarmSettings(context) }, modifier = Modifier.fillMaxWidth())
        NoteDivider()
        NoteRow("Kanał przypomnień", subtitle = if (status.channelBlocked) "Zablokowany" else "Włączony")
        InkTextButton("Ustawienia kanału przypomnień", { viewModel.openReminderChannelSettings(context) }, modifier = Modifier.fillMaxWidth())
        InkButton("Wyślij próbne powiadomienie za minutę", viewModel::scheduleTestNotification,
            modifier = Modifier.fillMaxWidth(), enabled = status.exactAlarmsAllowed)
        if (!status.exactAlarmsAllowed) Text("Aby wysłać próbę, włącz zgodę na dokładne alarmy.", style = TablicaType.Hint)
        status.testMessage?.let { Text(it, style = TablicaType.BodyLight) }
    }
}

@Composable
private fun LeadDaysNote(state: SettingsUiState, viewModel: SettingsViewModel) {
    SettingsNote("Kiedy przypominać", LookTheme.tablica.pink, -0.4f) {
        Text("Ile dni przed terminem zacząć przypominać. Dotyczy nowych wpisów i tych z Librusa.", style = TablicaType.BodyLight)
        listOf(SchoolType.TEST, SchoolType.QUIZ, SchoolType.HOMEWORK, SchoolType.TRIP).forEach { type ->
            LeadDaysControl(type, state.leadDays[type] ?: type.defaultLeadDays,
                enabled = !state.isSyncing, onCommit = { viewModel.setLeadDays(type, it) })
        }
        if (state.librusConnected) {
            NoteDivider()
            SwitchRow("Wycieczki z Librusa", state.includeTrips, viewModel::setIncludeTrips,
                subtitle = "Przypominaj też o wyjściach z terminarza")
        }
    }
}

@Composable
private fun LeadDaysControl(type: SchoolType, value: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val ink = LocalContentColor.current
    val paper = LocalPaper.current
    Column {
        Text(type.label, style = TablicaType.Strong)
        Text(leadDaysText(local.roundToInt()), style = TablicaType.Hint)
        Slider(
            value = local, onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.roundToInt()) }, enabled = enabled,
            valueRange = SchoolPrefs.MIN_LEAD_DAYS.toFloat()..SchoolPrefs.MAX_LEAD_DAYS.toFloat(),
            steps = SchoolPrefs.MAX_LEAD_DAYS - SchoolPrefs.MIN_LEAD_DAYS - 1,
            colors = SliderDefaults.colors(
                thumbColor = ink, activeTrackColor = ink, inactiveTrackColor = ink.copy(alpha = 0.18f),
                activeTickColor = paper, inactiveTickColor = ink.copy(alpha = 0.5f),
            ), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
    }
}

@Composable
private fun UpdatesNote(manager: UpdateManager) {
    val state by manager.state.collectAsStateWithLifecycle()
    val autoCheck by manager.autoCheck.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsNote("Aktualizacje", LookTheme.tablica.yellow, 0.4f) {
        Text("Nowe wersje Przypominajki z GitHuba. Instalacja nie kasuje Twoich danych.", style = TablicaType.BodyLight)
        SwitchRow("Sprawdzaj automatycznie", autoCheck, manager::setAutoCheck,
            subtitle = "Przy otwarciu aplikacji, najwyżej co 6 godzin")
        NoteDivider()
        val status = when (val current = state) {
            UpdateState.Idle -> "Masz wersję ${manager.currentVersion}"
            UpdateState.Checking -> "Sprawdzam…"
            UpdateState.UpToDate -> "Masz najnowszą wersję (${manager.currentVersion})"
            is UpdateState.Failed -> "Błąd aktualizacji: ${current.message}"
            is UpdateState.Available -> "Dostępna wersja ${current.info.version}"
            is UpdateState.Downloading -> "Pobieram wersję ${current.info.version}…"
            is UpdateState.ReadyToInstall -> "Wersja ${current.info.version} gotowa do instalacji"
        }
        Text(status, style = TablicaType.Strong)
        when (state) {
            is UpdateState.Available, is UpdateState.Downloading, is UpdateState.ReadyToInstall ->
                InkButton("Pokaż", manager::showDialog, modifier = Modifier.fillMaxWidth())
            else -> InkButton("Sprawdź aktualizacje", { manager.check() },
                enabled = state != UpdateState.Checking, modifier = Modifier.fillMaxWidth())
        }
        InkTextButton("GitHub", {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.RELEASES_PAGE))) }
        }, modifier = Modifier.fillMaxWidth())
    }
}

/** Wybór dostępny w całym wierszu; odsłuch pozostaje osobną akcją. */
@Composable
private fun ChoiceRow(
    title: String, selected: Boolean, onSelect: () -> Unit, description: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    val ink = LocalContentColor.current
    Row(
        Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = 56.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = ink, unselectedColor = ink.copy(alpha = 0.65f)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TablicaType.Strong)
            description?.let { Text(it, style = TablicaType.Hint) }
        }
        trailing()
    }
}

@Composable
private fun SettingsNote(title: String, color: NoteColor, tilt: Float, content: @Composable ColumnScope.() -> Unit) {
    Note(color, Modifier.fillMaxWidth(), tilt = tilt, decoration = NoteDecoration.Tape,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(title)
        content()
    }
}
