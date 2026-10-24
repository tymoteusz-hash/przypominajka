package pl.przypominajka.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.przypominajka.ui.components.Bell
import pl.przypominajka.update.UpdateInfo
import pl.przypominajka.update.UpdateManager
import pl.przypominajka.update.UpdateState
import kotlin.math.roundToInt

/** Okno "Nowa wersja" – nad każdym ekranem, gdy GitHub ma nowsze wydanie. */
@Composable
fun UpdateDialogHost(manager: UpdateManager) {
    val state by manager.state.collectAsStateWithLifecycle()
    val visible by manager.dialogVisible.collectAsStateWithLifecycle()
    if (!visible) return

    val info: UpdateInfo = when (val s = state) {
        is UpdateState.Available -> s.info
        is UpdateState.Downloading -> s.info
        is UpdateState.ReadyToInstall -> s.info
        else -> return
    }
    val downloading = state as? UpdateState.Downloading

    AlertDialog(
        onDismissRequest = { if (downloading == null) manager.dismissDialog() },
        icon = { Bell(size = 48.dp, showWaves = false, reportPosition = false) },
        title = { Text("Nowa wersja ${info.version}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Masz wersję ${manager.currentVersion}." + sizeText(info.sizeBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (info.notes.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text("Co nowego", style = MaterialTheme.typography.titleSmall)
                        Text(info.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                when (val s = state) {
                    is UpdateState.Downloading -> {
                        val progress = s.progress
                        if (progress != null) {
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            Text("Pobieranie… ${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Pobieranie…", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    is UpdateState.Available -> s.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    is UpdateState.ReadyToInstall -> if (!manager.canInstall()) {
                        Text(
                            text = "Telefon zapyta o zgodę na instalowanie z Przypominajki. " +
                                "Włącz ją, wróć tutaj i stuknij „Zainstaluj”.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    else -> Unit
                }
                Text(
                    text = "Twoje przypomnienia i ustawienia zostaną.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = manager::downloadAndInstall, enabled = downloading == null) {
                Text(if (state is UpdateState.ReadyToInstall) "Zainstaluj" else "Pobierz i zainstaluj")
            }
        },
        dismissButton = {
            if (downloading == null) TextButton(onClick = manager::dismissDialog) { Text("Później") }
        },
    )
}

private fun sizeText(bytes: Long): String =
    if (bytes > 0) " Plik: ${"%.1f".format(bytes / 1_048_576.0).replace('.', ',')} MB." else ""
