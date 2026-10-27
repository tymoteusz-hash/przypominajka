@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.dzwon.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.dzwon.components.*
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

/** Ustawienia Dzwonu: wybory w kartach na wspólnym arkuszu. */
@Composable
fun DzwonSettingsScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = LookTheme.dzwon
    var editingDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }

    DzwonSheetScaffold(title = "Ustawienia", subtitle = "Dopasuj do siebie", onClose = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsSection(
                "Wygląd",
                "Razem z wyglądem zmienia się ikona aplikacji. Na Samsungu może zniknąć z ekranu głównego – " +
                    "wtedy przeciągnij ją z listy aplikacji jeszcze raz.",
            ) {
                Column(Modifier.selectableGroup()) {
                    Look.entries.forEach { look ->
                        ChoiceRow(
                            title = look.label,
                            description = look.description,
                            selected = state.appearance.look == look,
                            onSelect = { viewModel.setLook(look) },
                        )
                    }
                }
                DzwonDivider()
                DzwonSectionHeader("Motyw")
                Column(Modifier.selectableGroup()) {
                    listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach { mode ->
                        ChoiceRow(
                            title = mode.label,
                            selected = state.appearance.theme == mode,
                            onSelect = { viewModel.setTheme(mode) },
                        )
                    }
                }
            }
            SettingsSection("Dźwięk powiadomień", "Czym dzwoni przypomnienie. Przycisk odtwarzania pozwala posłuchać.") {
                Column(Modifier.selectableGroup()) {
                    BellTone.entries.forEach { tone ->
                        // Odsłuch jest oddzielnym przyciskiem i nie zmienia wybranego dźwięku.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChoiceRow(
                                title = tone.label,
                                selected = state.appearance.notificationTone == tone,
                                onSelect = { viewModel.setNotificationTone(tone) },
                                modifier = Modifier.weight(1f),
                            )
                            if (tone != BellTone.SILENT) {
                                DzwonRoundIconButton(
                                    icon = Icons.Rounded.PlayArrow,
                                    contentDescription = "Posłuchaj: ${tone.label}",
                                    onClick = { viewModel.preview(tone) },
                                    containerColor = c.field,
                                    contentColor = c.goldText,
                                )
                            }
                        }
                    }
                }
                DzwonTextButton(
                    text = "Więcej w ustawieniach telefonu",
                    icon = DzwonIcons.Sliders,
                    onClick = {
                        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, viewModel.reminderChannelId)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        }
                    },
                )
            }
            SchoolSections(state, viewModel::setLeadDays, viewModel::setIncludeTrips, { editingDay = it })
            DzwonCard(Modifier.fillMaxWidth(), onClick = onOpenFolders) {
                DzwonListRow(
                    title = "Foldery",
                    subtitle = "Dodawaj, zmieniaj nazwy i kolory",
                    leading = { Icon(Icons.Rounded.Folder, contentDescription = null, tint = c.goldText) },
                    trailing = { Icon(DzwonIcons.ChevronRight, contentDescription = null, tint = c.ink2) },
                )
            }
            UpdatesSection(viewModel.updates)
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DzwonBell(size = 48.dp)
                Text("Przypominajka ${viewModel.updates.currentVersion}", style = DzwonType.CaptionStrong, color = c.ink2)
            }
        }
    }
    editingDay?.let { day ->
        DzwonTimePickerDialog(
            initialTime = state.weekdayEnds[day] ?: LocalTime.of(15, 0),
            title = "Koniec lekcji",
            subtitle = dayName(LocalDate.now().with(day)).replaceFirstChar { it.uppercase() },
            onDismiss = { editingDay = null },
            onConfirm = { viewModel.setWeekdayEnd(day, it); editingDay = null },
        )
    }
}

@Composable
private fun SchoolSections(
    state: SettingsUiState,
    onLeadDays: (SchoolType, Int) -> Unit,
    onTrips: (Boolean) -> Unit,
    onEditDay: (DayOfWeek) -> Unit,
) {
    SettingsSection(
        "Kiedy przypominać",
        "Ile dni przed terminem zacząć przypominać. Dotyczy nowych wpisów i tych z Librusa.",
    ) {
        listOf(SchoolType.TEST, SchoolType.QUIZ, SchoolType.HOMEWORK, SchoolType.TRIP).forEach { type ->
            LeadDaysSlider(
                type = type,
                value = state.leadDays[type] ?: type.defaultLeadDays,
                enabled = !state.isSyncing,
                onCommit = { onLeadDays(type, it) },
            )
        }
        if (state.librusConnected) {
            DzwonDivider()
            SettingSwitch("Wycieczki z Librusa", "Przypominaj też o wyjściach z terminarza", state.includeTrips, onTrips)
        }
    }
    SettingsSection(
        "Koniec lekcji",
        if (state.librusConnected) "Gdy w planie z Librusa brakuje danego dnia, użyjemy tych godzin."
        else "Na tej podstawie przychodzą przypomnienia „po lekcjach” (godzinę później).",
    ) {
        SchoolPrefs.SCHOOL_DAYS.forEach { day ->
            val time = state.weekdayEnds[day] ?: LocalTime.of(15, 0)
            DzwonListRow(
                title = dayName(LocalDate.now().with(day)).replaceFirstChar { it.uppercase() },
                subtitle = "Koniec o ${time.format(TimeFormatter)}",
                onClick = { onEditDay(day) },
                trailing = { DzwonTextButton("Zmień", { onEditDay(day) }, icon = DzwonIcons.Clock) },
                divider = day != SchoolPrefs.SCHOOL_DAYS.last(),
            )
        }
    }
}

@Composable
private fun LeadDaysSlider(type: SchoolType, value: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val c = LookTheme.dzwon
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(type.label, style = DzwonType.RowTitle, color = c.ink, modifier = Modifier.weight(1f))
            Text(leadDaysText(local.roundToInt()), style = DzwonType.Time, color = c.goldText)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.roundToInt()) },
            enabled = enabled,
            valueRange = SchoolPrefs.MIN_LEAD_DAYS.toFloat()..SchoolPrefs.MAX_LEAD_DAYS.toFloat(),
            steps = SchoolPrefs.MAX_LEAD_DAYS - SchoolPrefs.MIN_LEAD_DAYS - 1,
            colors = SliderDefaults.colors(
                thumbColor = c.gold, activeTrackColor = c.gold, inactiveTrackColor = c.track,
                activeTickColor = c.onGold, inactiveTickColor = c.ink2,
            ),
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }
}

@Composable
private fun UpdatesSection(manager: UpdateManager) {
    val state by manager.state.collectAsStateWithLifecycle()
    val autoCheck by manager.autoCheck.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsSection("Aktualizacje", "Nowe wersje Przypominajki z GitHuba. Instalacja nie kasuje Twoich danych.") {
        SettingSwitch("Sprawdzaj automatycznie", "Przy otwarciu aplikacji, najwyżej co 6 godzin", autoCheck, manager::setAutoCheck)
        DzwonDivider()
        val status = when (val s = state) {
            UpdateState.Idle -> "Masz wersję ${manager.currentVersion}"
            UpdateState.Checking -> "Sprawdzam…"
            UpdateState.UpToDate -> "Masz najnowszą wersję (${manager.currentVersion})"
            is UpdateState.Failed -> s.message
            is UpdateState.Available -> "Dostępna wersja ${s.info.version}"
            is UpdateState.Downloading -> "Pobieram wersję ${s.info.version}…"
            is UpdateState.ReadyToInstall -> "Wersja ${s.info.version} gotowa do instalacji"
        }
        Text(status, style = DzwonType.Body, color = if (state is UpdateState.Failed) MaterialTheme.colorScheme.error else LookTheme.dzwon.ink)
        when (state) {
            is UpdateState.Available, is UpdateState.Downloading, is UpdateState.ReadyToInstall ->
                DzwonPrimaryButton("Pokaż", manager::showDialog, modifier = Modifier.fillMaxWidth())
            else -> DzwonPrimaryButton(
                text = "Sprawdź aktualizacje",
                onClick = { manager.check() },
                enabled = state != UpdateState.Checking,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DzwonTextButton(
            text = "GitHub",
            icon = Icons.Rounded.OpenInNew,
            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.RELEASES_PAGE))) } },
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DzwonType.RowTitle, color = LookTheme.dzwon.ink)
            Text(description, style = DzwonType.Caption, color = LookTheme.dzwon.ink2)
        }
        DzwonSwitch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val c = LookTheme.dzwon
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.field else c.surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected, onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = c.goldText, unselectedColor = c.ink2),
            modifier = Modifier.size(24.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = DzwonType.RowTitle, color = c.ink)
            if (description != null) Text(description, style = DzwonType.Caption, color = c.ink2)
        }
    }
}

@Composable
private fun SettingsSection(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    DzwonCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DzwonSectionHeader(title)
            Text(description, style = DzwonType.Caption, color = LookTheme.dzwon.ink2)
            content()
        }
    }
}
