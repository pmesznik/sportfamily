package pl.sportfamily.app.ui.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.sportfamily.app.backup.Backup
import pl.sportfamily.app.backup.BackupFrequency
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.theme.Sf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Kopia zapasowa w chmurze: miejsce, częstotliwość, kopia teraz i przywracanie. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { Backup.load(context) }
    val s by Backup.state.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun runBackup() {
        working = true
        scope.launch { message = Backup.backupNow(context) ?: "Kopia zapisana."; working = false }
    }

    val pickTarget = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            val err = Backup.setTarget(context, uri)
            if (err != null) message = err
            else {
                if (s.frequency == BackupFrequency.OFF) Backup.setFrequency(context, BackupFrequency.DAILY) else Backup.schedule(context)
                runBackup()
            }
        }
    }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    SectionHeader("Kopia zapasowa")
    SfCard(Modifier.fillMaxWidth()) {
        Text(
            "Aplikacja sama zapisuje kopię wszystkich danych do pliku ${Backup.FILE_NAME} w wybranym miejscu. " +
                "Wybierz folder na Dysku Google, a kopia będzie w chmurze. Na nowym telefonie użyj Przywróć z kopii.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (s.target == null) "Miejsce kopii: nie wybrano" else "Miejsce kopii: ${s.targetName}",
            style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp),
        )
        OutlinedButton(
            onClick = { pickTarget.launch(Backup.FILE_NAME) },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 6.dp).heightIn(min = 48.dp),
        ) { Text(if (s.target == null) "Wybierz miejsce (np. Dysk Google)" else "Zmień miejsce") }

        Text("Jak często", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackupFrequency.entries.forEach { f ->
                FilterChip(
                    selected = s.frequency == f, enabled = s.target != null || f == BackupFrequency.OFF,
                    onClick = { Backup.setFrequency(context, f) },
                    label = { Text(f.label) }, modifier = Modifier.heightIn(min = 40.dp),
                )
            }
        }

        val last = if (s.lastOk > 0) DateTimeFormatter.ofPattern("d.MM.yyyy, H:mm").format(Instant.ofEpochMilli(s.lastOk).atZone(ZoneId.systemDefault())) else null
        Text(
            when {
                s.lastError != null -> s.lastError!!
                last != null -> "Ostatnia kopia: $last"
                else -> "Kopii jeszcze nie było."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (s.lastError != null) Sf.colors.error.main else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (s.frequency != BackupFrequency.OFF && s.target != null) Text(
            "Kolejna kopia: ${s.frequency.label.lowercase()}, gdy bateria nie jest prawie pusta.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { runBackup() }, enabled = s.target != null && !working, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp)) { Text(if (working) "Zapisuję…" else "Zrób kopię teraz") }
            OutlinedButton(onClick = { pickRestore.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                shape = RoundedCornerShape(12.dp), modifier = Modifier.heightIn(min = 48.dp)) { Text("Przywróć z kopii") }
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp),
                color = if (it == "Kopia zapisana.") Sf.colors.school.main else Sf.colors.error.main)
        }
    }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Przywrócić dane z kopii?") },
            text = { Text("Wszystkie obecne dane w aplikacji zostaną zastąpione danymi z kopii. Aplikacja uruchomi się ponownie.") },
            confirmButton = {
                TextButton(onClick = {
                    restoreUri = null
                    scope.launch {
                        val err = context.contentResolver.openInputStream(uri)?.use { Backup.restore(context, it) } ?: "Nie można otworzyć pliku."
                        message = err
                    }
                }) { Text("Przywróć", color = Sf.colors.error.main) }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Anuluj") } },
        )
    }
}
