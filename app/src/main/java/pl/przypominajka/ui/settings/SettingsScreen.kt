package pl.przypominajka.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.SchoolType
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.Bell
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.FormSection
import pl.przypominajka.ui.components.IconBadge
import pl.przypominajka.ui.components.LocalBottomBarPadding
import pl.przypominajka.ui.components.PastelBadge
import pl.przypominajka.ui.components.ReminderTimePickerDialog
import pl.przypominajka.ui.components.accent
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.icon
import pl.przypominajka.ui.components.pressScale
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.Pastel
import pl.przypominajka.update.UpdateManager
import pl.przypominajka.update.UpdateState
import pl.przypominajka.util.Appearance
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

/** Zakładka Ustawienia: wygląd, dzwon w aplikacji, dźwięk powiadomień, szkoła, foldery. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editingDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Scaffold(
        containerColor = AppSurfaces.page,
        contentWindowInsets = WindowInsets(0),
        topBar = { BellTopBar(title = "Ustawienia", eyebrow = "Wygląd i dźwięki", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = LocalBottomBarPadding.current + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ThemeSection(
                appearance = state.appearance,
                onThemeChange = viewModel::setTheme,
                modifier = Modifier.appear(0),
            )
            BellSoundSection(
                appearance = state.appearance,
                onEnabledChange = viewModel::setBellSound,
                onToneChange = viewModel::setTransitionTone,
                onVolumeChange = viewModel::setBellVolume,
                onQuietChange = viewModel::setQuietWhenSilent,
                modifier = Modifier.appear(1),
            )
            NotificationSoundSection(
                selected = state.appearance.notificationTone,
                onSelect = viewModel::setNotificationTone,
                onPreview = viewModel::preview,
                onOpenSystem = {
                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, viewModel.reminderChannelId)
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                },
                modifier = Modifier.appear(2),
            )
            LeadDaysSection(state = state, onLeadDaysChange = viewModel::setLeadDays, onTripsChange = viewModel::setIncludeTrips)
            SchoolEndSection(state = state, onEdit = { editingDay = it })

            AppCard(onClick = onOpenFolders, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    PastelBadge(icon = Icons.Rounded.Folder, pastel = Pastel.DUSK)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Foldery", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Dodawaj, zmieniaj nazwy i kolory",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }

            UpdatesSection(manager = viewModel.updates)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Bell(size = 56.dp, showWaves = false, reportPosition = false)
                Text(
                    text = "Przypominajka $version",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    editingDay?.let { day ->
        ReminderTimePickerDialog(
            initialTime = state.weekdayEnds[day] ?: LocalTime.of(15, 0),
            title = "Koniec lekcji: ${dayName(LocalDate.now().with(day))}",
            onDismiss = { editingDay = null },
            onConfirm = { picked ->
                viewModel.setWeekdayEnd(day, picked)
                editingDay = null
            },
        )
    }
}

// ---------------------------------------------------------------- wygląd

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Icons.Rounded.Smartphone
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
    }

@Composable
private fun ThemeSection(appearance: Appearance, onThemeChange: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    FormSection(title = "Wygląd", description = "Motyw całej aplikacji.", modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val selected = appearance.theme == mode
                val container by animateColorAsState(if (selected) colors.primary else colors.surfaceVariant, label = "themeTile")
                val content by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface, label = "themeTileText")
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    selected = selected,
                    onClick = { onThemeChange(mode) },
                    shape = RoundedCornerShape(22.dp),
                    color = container,
                    contentColor = content,
                    interactionSource = interaction,
                    modifier = Modifier
                        .weight(1f)
                        .pressScale(interaction, pressedScale = 0.92f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(mode.icon, contentDescription = null)
                        Spacer(Modifier.height(4.dp))
                        Text(mode.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- dzwon w aplikacji

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BellSoundSection(
    appearance: Appearance,
    onEnabledChange: (Boolean) -> Unit,
    onToneChange: (BellTone) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onQuietChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    FormSection(
        title = "Dzwon w aplikacji",
        description = "Uderza przy każdym przejściu między ekranami. Gra jak dźwięk aplikacji – " +
            "na głośności multimediów, nie jako powiadomienie.",
        modifier = modifier,
    ) {
        SwitchRow(
            title = "Dźwięk przy przejściach",
            subtitle = "Dzwon bije, gdy zmieniasz zakładkę",
            checked = appearance.bellSound,
            onCheckedChange = onEnabledChange,
        )
        if (appearance.bellSound) {
            Text("Dźwięk", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BellTone.entries.filter { it.forTransitions }.forEach { tone ->
                    val selected = appearance.transitionTone == tone
                    val container by animateColorAsState(if (selected) colors.primary else colors.surfaceVariant, label = "toneChip")
                    val content by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface, label = "toneChipText")
                    val interaction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = { onToneChange(tone) },
                        shape = CircleShape,
                        color = container,
                        contentColor = content,
                        interactionSource = interaction,
                        modifier = Modifier.pressScale(interaction, pressedScale = 0.92f),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tone.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            var volume by remember(appearance.bellVolume) { mutableFloatStateOf(appearance.bellVolume) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Głośność", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("${(volume * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall, color = AppSurfaces.accent)
            }
            Slider(
                value = volume,
                onValueChange = { volume = it },
                onValueChangeFinished = { onVolumeChange(volume) },
                colors = SliderDefaults.colors(inactiveTrackColor = colors.surfaceVariant),
            )
            HorizontalDivider(color = colors.outlineVariant)
            SwitchRow(
                title = "Cisza, gdy telefon jest wyciszony",
                subtitle = "W trybie cichym i wibracji dzwon tylko się kołysze",
                checked = appearance.quietWhenSilent,
                onCheckedChange = onQuietChange,
            )
        }
    }
}

// ---------------------------------------------------------------- dźwięk powiadomień

@Composable
private fun NotificationSoundSection(
    selected: BellTone,
    onSelect: (BellTone) -> Unit,
    onPreview: (BellTone) -> Unit,
    onOpenSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormSection(
        title = "Dźwięk powiadomień",
        description = "Czym dzwoni przypomnienie. Dotknij ▶, żeby posłuchać.",
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BellTone.entries.forEach { tone ->
                val isSelected = tone == selected
                val background by animateColorAsState(
                    if (isSelected) Pastel.GOLD.container else Color.Transparent,
                    label = "toneRow",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(background)
                        .clickable { onSelect(tone) }
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = { onSelect(tone) })
                    Text(tone.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (tone != BellTone.SILENT) {
                        IconButton(onClick = { onPreview(tone) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Posłuchaj: ${tone.label}")
                        }
                    }
                }
            }
        }
        TextButton(onClick = onOpenSystem) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Więcej w ustawieniach telefonu")
        }
    }
}

// ---------------------------------------------------------------- szkoła

@Composable
private fun LeadDaysSection(
    state: SettingsUiState,
    onLeadDaysChange: (SchoolType, Int) -> Unit,
    onTripsChange: (Boolean) -> Unit,
) {
    FormSection(
        title = "Kiedy przypominać",
        description = "Ile dni przed terminem zacząć przypominać. Dotyczy nowych wpisów i tych z Librusa.",
    ) {
        listOf(SchoolType.TEST, SchoolType.QUIZ, SchoolType.HOMEWORK, SchoolType.TRIP).forEach { type ->
            LeadDaysSlider(
                type = type,
                value = state.leadDays[type] ?: type.defaultLeadDays,
                enabled = !state.isSyncing,
                onCommit = { days -> onLeadDaysChange(type, days) },
            )
        }
        if (state.librusConnected) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "Wycieczki z Librusa",
                subtitle = "Przypominaj też o wyjściach z terminarza",
                checked = state.includeTrips,
                onCheckedChange = onTripsChange,
            )
        }
    }
}

@Composable
private fun SchoolEndSection(state: SettingsUiState, onEdit: (DayOfWeek) -> Unit) {
    FormSection(
        title = "Koniec lekcji",
        description = if (state.librusConnected) {
            "Gdy w planie z Librusa brakuje danego dnia, użyjemy tych godzin."
        } else {
            "Na tej podstawie przychodzą przypomnienia „po lekcjach” (godzinę później)."
        },
    ) {
        SchoolPrefs.SCHOOL_DAYS.forEach { day ->
            val time = state.weekdayEnds[day] ?: LocalTime.of(15, 0)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dayName(LocalDate.now().with(day)).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = time.format(TimeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppSurfaces.accent,
                )
                TextButton(onClick = { onEdit(day) }) { Text("Zmień") }
            }
        }
    }
}

/** Suwak 1–7 dni; zapis dopiero po puszczeniu (żeby nie synchronizować przy każdym ruchu). */
@Composable
private fun LeadDaysSlider(type: SchoolType, value: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val accent = type.accent
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = type.icon, color = accent, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(type.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(leadDaysText(local.roundToInt()), style = MaterialTheme.typography.titleSmall, color = accent)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.roundToInt()) },
            enabled = enabled,
            valueRange = SchoolPrefs.MIN_LEAD_DAYS.toFloat()..SchoolPrefs.MAX_LEAD_DAYS.toFloat(),
            steps = SchoolPrefs.MAX_LEAD_DAYS - SchoolPrefs.MIN_LEAD_DAYS - 1,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = Color.White.copy(alpha = 0.7f),
                inactiveTickColor = accent.copy(alpha = 0.5f),
            ),
        )
    }
}

// ---------------------------------------------------------------- aktualizacje

@Composable
private fun UpdatesSection(manager: UpdateManager) {
    val state by manager.state.collectAsStateWithLifecycle()
    val autoCheck by manager.autoCheck.collectAsStateWithLifecycle()
    val context = LocalContext.current
    FormSection(
        title = "Aktualizacje",
        description = "Nowe wersje Przypominajki z GitHuba. Instalacja nie kasuje Twoich danych.",
    ) {
        SwitchRow(
            title = "Sprawdzaj automatycznie",
            subtitle = "Przy otwarciu aplikacji, najwyżej co 6 godzin",
            checked = autoCheck,
            onCheckedChange = manager::setAutoCheck,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val (status, isError) = when (val s = state) {
            UpdateState.Idle -> "Masz wersję ${manager.currentVersion}" to false
            UpdateState.Checking -> "Sprawdzam…" to false
            UpdateState.UpToDate -> "Masz najnowszą wersję (${manager.currentVersion})" to false
            is UpdateState.Failed -> s.message to true
            is UpdateState.Available -> "Dostępna wersja ${s.info.version}" to false
            is UpdateState.Downloading -> "Pobieram wersję ${s.info.version}…" to false
            is UpdateState.ReadyToInstall -> "Wersja ${s.info.version} gotowa do instalacji" to false
        }
        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (state) {
                is UpdateState.Available, is UpdateState.Downloading, is UpdateState.ReadyToInstall ->
                    Button(onClick = manager::showDialog) { Text("Pokaż") }
                else -> Button(
                    onClick = { manager.check() },
                    enabled = state != UpdateState.Checking,
                ) { Text("Sprawdź aktualizacje") }
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.RELEASES_PAGE)))
                    }
                },
            ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("GitHub")
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
