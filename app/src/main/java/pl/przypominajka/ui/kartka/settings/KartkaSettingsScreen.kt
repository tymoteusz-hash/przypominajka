@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.kartka.components.KartkaButton
import pl.przypominajka.ui.kartka.components.KartkaDefaults
import pl.przypominajka.ui.kartka.components.KartkaDivider
import pl.przypominajka.ui.kartka.components.KartkaEyebrow
import pl.przypominajka.ui.kartka.components.KartkaIconButton
import pl.przypominajka.ui.kartka.components.KartkaIcons
import pl.przypominajka.ui.kartka.components.KartkaListRow
import pl.przypominajka.ui.kartka.components.KartkaNavTile
import pl.przypominajka.ui.kartka.components.KartkaOptionRow
import pl.przypominajka.ui.kartka.components.KartkaPageTitle
import pl.przypominajka.ui.kartka.components.KartkaScaffold
import pl.przypominajka.ui.kartka.components.KartkaSectionHeader
import pl.przypominajka.ui.kartka.components.KartkaSwitchRow
import pl.przypominajka.ui.kartka.components.KartkaTextButton
import pl.przypominajka.ui.kartka.components.KartkaTimePickerDialog
import pl.przypominajka.ui.kartka.components.KartkaType
import pl.przypominajka.ui.look.Look
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.settings.SettingsUiState
import pl.przypominajka.ui.settings.SettingsViewModel
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

/** Ustawienia Kartki: płaskie sekcje i wiersze oddzielone liniami. */
@Composable
fun KartkaSettingsScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshReminderReliability() }
    val c = LookTheme.kartka
    var editingDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    KartkaScaffold(onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .imePadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            KartkaPageTitle("Ustawienia", subtitle = "Dopasuj Przypominajkę do siebie")
            Column(
                modifier = Modifier.padding(horizontal = KartkaDefaults.PagePadding),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                SettingsSection(
                    "Wygląd",
                    "Razem z wyglądem zmienia się ikona aplikacji. Na Samsungu może zniknąć z ekranu " +
                        "głównego – wtedy przeciągnij ją z listy aplikacji jeszcze raz.",
                ) {
                    Column(Modifier.selectableGroup()) {
                        Look.entries.forEach { look ->
                            KartkaOptionRow(
                                title = look.label, subtitle = look.description,
                                selected = state.appearance.look == look,
                                onClick = { viewModel.setLook(look) },
                            )
                        }
                    }
                    KartkaEyebrow("Motyw")
                    Column(Modifier.selectableGroup()) {
                        listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach { mode ->
                            KartkaOptionRow(
                                title = mode.label, selected = state.appearance.theme == mode,
                                onClick = { viewModel.setTheme(mode) },
                            )
                        }
                    }
                }
                SettingsSection(
                    "Dźwięk powiadomień",
                    "Czym dzwoni przypomnienie. Przycisk odtwarzania pozwala posłuchać przez głośność multimediów.",
                ) {
                    Column(Modifier.selectableGroup()) {
                        BellTone.entries.forEach { tone ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                KartkaOptionRow(
                                    title = tone.label, selected = state.appearance.notificationTone == tone,
                                    onClick = { viewModel.setNotificationTone(tone) },
                                    modifier = Modifier.weight(1f), showDivider = false,
                                )
                                if (tone != BellTone.SILENT) KartkaIconButton(
                                    icon = KartkaIcons.Play, contentDescription = "Posłuchaj: ${tone.label}",
                                    onClick = { viewModel.preview(tone) },
                                )
                            }
                            KartkaDivider()
                        }
                    }
                    KartkaTextButton(
                        "Więcej w ustawieniach telefonu",
                        { openNotificationSettings(context, viewModel.reminderChannelId) },
                        icon = KartkaIcons.External,
                    )
                }
                ReminderReliabilitySection(viewModel)
                LeadDaysSection(state, viewModel::setLeadDays, viewModel::setIncludeTrips)
                SettingsSection(
                    "Koniec lekcji",
                    if (state.librusConnected) "Gdy w planie z Librusa brakuje danego dnia, użyjemy tych godzin."
                    else "Na tej podstawie przychodzą przypomnienia „po lekcjach” (godzinę później).",
                ) {
                    SchoolPrefs.SCHOOL_DAYS.forEach { day ->
                        val time = state.weekdayEnds[day] ?: LocalTime.of(15, 0)
                        KartkaListRow(
                            title = dayName(LocalDate.now().with(day)).replaceFirstChar { it.uppercase() },
                            subtitle = time.format(TimeFormatter),
                            onClick = { editingDay = day }, onClickLabel = "Zmień godzinę końca lekcji",
                            trailing = { KartkaTextButton("Zmień", { editingDay = day }) },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KartkaNavTile("Foldery", KartkaIcons.Folder, onOpenFolders, Modifier.fillMaxWidth())
                    Text("Dodawaj, zmieniaj nazwy i kolory folderów.", style = KartkaType.Caption, color = c.ink2)
                }
                UpdatesSection(viewModel.updates)
                KartkaDivider()
                Text("Przypominajka $version", style = KartkaType.Caption, color = c.ink2)
            }
        }
    }

    editingDay?.let { day ->
        KartkaTimePickerDialog(
            initialTime = state.weekdayEnds[day] ?: LocalTime.of(15, 0),
            title = "Koniec lekcji: ${dayName(LocalDate.now().with(day))}",
            onDismiss = { editingDay = null },
            onConfirm = { viewModel.setWeekdayEnd(day, it); editingDay = null },
        )
    }
}

@Composable
private fun ReminderReliabilitySection(viewModel: SettingsViewModel) {
    val status by viewModel.reliability.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsSection("Czy przypomnienia działają?", "Sprawdź zgody telefonu i wyślij próbne powiadomienie.") {
        Text("Powiadomienia: ${if (status.notificationsAllowed) "dozwolone" else "zablokowane"}",
            style = KartkaType.RowTitle, color = LookTheme.kartka.ink)
        KartkaTextButton("Ustawienia powiadomień", { viewModel.openNotificationSettings(context) })
        KartkaDivider()
        Text("Dokładne alarmy: ${if (status.exactAlarmsAllowed) "dozwolone" else "brak zgody"}",
            style = KartkaType.RowTitle, color = LookTheme.kartka.ink)
        KartkaTextButton("Ustawienia dokładnych alarmów", { viewModel.openExactAlarmSettings(context) })
        KartkaDivider()
        Text("Kanał przypomnień: ${if (status.channelBlocked) "zablokowany" else "włączony"}",
            style = KartkaType.RowTitle, color = LookTheme.kartka.ink)
        KartkaTextButton("Ustawienia kanału przypomnień", { viewModel.openReminderChannelSettings(context) })
        KartkaButton("Wyślij próbne powiadomienie za minutę", viewModel::scheduleTestNotification,
            modifier = Modifier.fillMaxWidth(), enabled = status.exactAlarmsAllowed)
        if (!status.exactAlarmsAllowed) Text("Aby wysłać próbę, włącz zgodę na dokładne alarmy.",
            style = KartkaType.BodySmall, color = LookTheme.kartka.ink2)
        status.testMessage?.let { Text(it, style = KartkaType.BodySmall, color = LookTheme.kartka.ink) }
    }
}

@Composable
private fun LeadDaysSection(
    state: SettingsUiState,
    onLeadDaysChange: (SchoolType, Int) -> Unit,
    onTripsChange: (Boolean) -> Unit,
) {
    SettingsSection("Kiedy przypominać", "Ile dni przed terminem zacząć przypominać. Dotyczy nowych wpisów i tych z Librusa.") {
        listOf(SchoolType.TEST, SchoolType.QUIZ, SchoolType.HOMEWORK, SchoolType.TRIP).forEach { type ->
            LeadDaysSlider(
                type = type, value = state.leadDays[type] ?: type.defaultLeadDays,
                enabled = !state.isSyncing, onCommit = { onLeadDaysChange(type, it) },
            )
        }
        if (state.librusConnected) KartkaSwitchRow(
            title = "Wycieczki z Librusa", subtitle = "Przypominaj też o wyjściach z terminarza",
            checked = state.includeTrips, onCheckedChange = onTripsChange,
        )
    }
}

/** Zapis dopiero po puszczeniu suwaka, aby nie powtarzać synchronizacji w trakcie ruchu. */
@Composable
private fun LeadDaysSlider(type: SchoolType, value: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val c = LookTheme.kartka
    Column {
        Text(type.label, style = KartkaType.RowTitle, color = c.ink)
        Text(leadDaysText(local.roundToInt()), style = KartkaType.CaptionStrong, color = c.accent)
        Slider(
            value = local, onValueChange = { local = it }, onValueChangeFinished = { onCommit(local.roundToInt()) },
            enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = KartkaDefaults.TouchTarget),
            valueRange = SchoolPrefs.MIN_LEAD_DAYS.toFloat()..SchoolPrefs.MAX_LEAD_DAYS.toFloat(),
            steps = SchoolPrefs.MAX_LEAD_DAYS - SchoolPrefs.MIN_LEAD_DAYS - 1,
            colors = SliderDefaults.colors(
                thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.surface,
                activeTickColor = c.onAccent, inactiveTickColor = c.ink2,
            ),
        )
    }
}

@Composable
private fun UpdatesSection(manager: UpdateManager) {
    val state by manager.state.collectAsStateWithLifecycle()
    val autoCheck by manager.autoCheck.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsSection("Aktualizacje", "Nowe wersje Przypominajki z GitHuba. Instalacja nie kasuje Twoich danych.") {
        KartkaSwitchRow(
            title = "Sprawdzaj automatycznie", subtitle = "Przy otwarciu aplikacji, najwyżej co 6 godzin",
            checked = autoCheck, onCheckedChange = manager::setAutoCheck,
        )
        val (status, isError) = when (val s = state) {
            UpdateState.Idle -> "Masz wersję ${manager.currentVersion}" to false
            UpdateState.Checking -> "Sprawdzam…" to false
            UpdateState.UpToDate -> "Masz najnowszą wersję (${manager.currentVersion})" to false
            is UpdateState.Failed -> s.message to true
            is UpdateState.Available -> "Dostępna wersja ${s.info.version}" to false
            is UpdateState.Downloading -> "Pobieram wersję ${s.info.version}…" to false
            is UpdateState.ReadyToInstall -> "Wersja ${s.info.version} gotowa do instalacji" to false
        }
        Text(status, style = KartkaType.BodySmall, color = if (isError) MaterialTheme.colorScheme.error else LookTheme.kartka.ink)
        when (state) {
            is UpdateState.Available, is UpdateState.Downloading, is UpdateState.ReadyToInstall ->
                KartkaButton("Pokaż", manager::showDialog, modifier = Modifier.fillMaxWidth())
            else -> KartkaButton(
                "Sprawdź aktualizacje", { manager.check() },
                modifier = Modifier.fillMaxWidth(), enabled = state != UpdateState.Checking,
            )
        }
        KartkaTextButton(
            "GitHub", { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.RELEASES_PAGE))) } },
            icon = KartkaIcons.External,
        )
    }
}

@Composable
private fun SettingsSection(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KartkaSectionHeader(title)
        Text(description, style = KartkaType.BodySmall, color = LookTheme.kartka.ink2)
        content()
    }
}

private fun openNotificationSettings(context: Context, channelId: String) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channelId),
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
}
