package pl.sportfamily.app.ui.more

import pl.sportfamily.app.ui.components.AppIcons
import pl.sportfamily.app.ui.components.SfIcon

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.collectAsState
import pl.sportfamily.app.ui.theme.ThemeMode
import pl.sportfamily.app.ui.theme.ThemeSettings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.data.Person
import pl.sportfamily.app.domain.PlannerData
import pl.sportfamily.app.ui.components.IconBadge
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.theme.Sf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    data: PlannerData,
    contentPadding: PaddingValues,
    onSavePerson: (Person) -> Unit,
    onDeletePerson: (Person) -> Unit,
    onSeedDemo: () -> Unit,
    importDraft: pl.sportfamily.app.ui.ImportDraft? = null,
    message: String? = null,
    onImport: (android.net.Uri) -> Unit = {},
    onExport: (android.net.Uri) -> Unit = {},
    onConfirmImport: (pl.sportfamily.app.domain.ImportOptions, Boolean) -> Unit = { _, _ -> },
    onDismissImport: () -> Unit = {},
    onAddDayOff: (java.time.LocalDate, String) -> Unit = { _, _ -> },
    onDeleteDayOff: (java.time.LocalDate) -> Unit = {},
) {
    val pickFile = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImport(uri) }
    val createFile = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument(XLSX_MIME)
    ) { uri -> if (uri != null) onExport(uri) }
    var editing by remember { mutableStateOf<Person?>(null) }
    var confirmDemo by remember { mutableStateOf(false) }
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Więcej", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("Wygląd")
        val context = androidx.compose.ui.platform.LocalContext.current
        val mode by ThemeSettings.mode.collectAsState()
        SfCard(Modifier.fillMaxWidth()) {
            Text("Motyw", style = MaterialTheme.typography.titleMedium)
            Text(
                "Systemowy zmienia się razem z ustawieniem telefonu, np. wieczorem na ciemny.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                ThemeMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { ThemeSettings.set(context, m) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = mode == m, activeContent = { SfIcon(AppIcons.zaznaczone, null, Modifier.size(SegmentedButtonDefaults.IconSize)) }) {
                                SfIcon(
                                    when (m) {
                                        ThemeMode.SYSTEM -> AppIcons.motywAuto
                                        ThemeMode.LIGHT -> AppIcons.motywJasny
                                        ThemeMode.DARK -> AppIcons.motywCiemny
                                    },
                                    null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                    ) { Text(m.label, maxLines = 1) }
                }
            }
        }

        RemindersSection(data.persons)

        BackupSection()

        ActivityIconsSection()

        SectionHeader("Kto wozi dzieci", "Dodaj") { editing = Person(name = "") }
        SfCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            if (data.persons.isEmpty()) Text("Brak osób", Modifier.padding(8.dp))
            data.persons.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    IconBadge(AppIcons.osoba, Sf.colors.brand)
                    TextButton(onClick = { editing = p }, modifier = Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            if (p.phone.isNotBlank()) Text(p.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (p.phone.isNotBlank()) {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        IconButton(onClick = { pl.sportfamily.app.ui.dial(ctx, p.phone) }) {
                            SfIcon(AppIcons.telefon, "Zadzwoń: ${p.name}", tint = Sf.colors.brand.main)
                        }
                    }
                    IconButton(onClick = { onDeletePerson(p) }) { SfIcon(AppIcons.usun, "Usuń ${p.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        Text(
            "Te osoby wybierasz przy wydarzeniu jako zawożące i odbierające. Aplikacja ostrzeże, gdy ktoś ma dwa kursy naraz albo nikt nie wiezie.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )

        SchoolDaysSection(data, java.time.LocalDate.now(), onAddDayOff, onDeleteDayOff)

        SectionHeader("Plan z Excela")
        SfCard(Modifier.fillMaxWidth()) {
            Text(
                "Import: plan lekcji lub zajęć w układzie dni tygodnia w kolumnach. Godzina może być w pierwszej kolumnie albo w komórce, np. \"Tenis 14:30 - 16:00\". Przed zapisaniem zobaczysz podgląd.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Eksport: plan tygodnia każdego dziecka (ten sam układ, do ponownego importu) i lista najbliższych 7 dni.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickFile.launch(arrayOf(XLSX_MIME, "application/vnd.ms-excel", "application/octet-stream")) }) {
                    SfIcon(AppIcons.importuj, null); Text("  Importuj")
                }
                OutlinedButton(onClick = { createFile.launch("SportFamily_plan_${java.time.LocalDate.now()}.xlsx") }) {
                    SfIcon(AppIcons.eksportuj, null); Text("  Eksportuj")
                }
            }
            if (message != null) {
                Text(message, style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = Sf.colors.brand.onContainer, modifier = Modifier.padding(top = 10.dp))
            }
        }

        SectionHeader("Dane")
        SfCard(Modifier.fillMaxWidth()) {
            Text(
                "Dane są zapisane na tym telefonie. Kopię w chmurze (np. na Dysku Google) ustawisz wyżej, w sekcji Kopia zapasowa. Sama aplikacja łączy się z internetem tylko po to, żeby sprawdzić, czy jest nowa wersja.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = { confirmDemo = true }, modifier = Modifier.padding(top = 12.dp)) {
                SfIcon(AppIcons.przyklad, null); Text("  Wczytaj przykładowy tydzień")
            }
        }

        SectionHeader("Aktualizacje")
        val (status, check) = pl.sportfamily.app.update.rememberUpdateCheck()
        SfCard(Modifier.fillMaxWidth()) {
            pl.sportfamily.app.ui.components.BrandLockup(Modifier.padding(bottom = 14.dp), logoHeight = 42.dp, centered = false)
            Text("Wersja ${pl.sportfamily.app.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
            Text(
                status ?: "Aplikacja sama sprawdza nowe wersje przy uruchomieniu.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = check, modifier = Modifier.padding(top = 10.dp)) { Text("Sprawdź teraz") }
        }

        SectionHeader("Wkrótce")
        SfCard(Modifier.fillMaxWidth()) {
            listOf(
                "Tryb W drodze z mapą",
                "Plan nauki do sprawdzianów",
                "Statystyki i wydatki",
            ).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
        }
        Text("SportFamily Kalendarz ${pl.sportfamily.app.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp, bottom = 80.dp))
    }

    if (importDraft != null) {
        ImportPreviewDialog(importDraft, data.children, java.time.LocalDate.now(), onConfirmImport, onDismissImport)
    }
    editing?.let { p ->
        var name by remember(p) { mutableStateOf(p.name) }
        var phone by remember(p) { mutableStateOf(p.phone) }
        val ctx = androidx.compose.ui.platform.LocalContext.current
        // Wybór jednego numeru z kontaktów. Android udostępnia tylko wybrany wpis, bez dostępu do całej książki.
        val pickPhone = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { res ->
            val uri = res.data?.data ?: return@rememberLauncherForActivityResult
            runCatching {
                ctx.contentResolver.query(
                    uri,
                    arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null,
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val n = c.getString(0).orEmpty()
                        val num = c.getString(1).orEmpty()
                        if (name.isBlank() || p.id == 0L) name = n
                        phone = num
                    }
                }
            }
        }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (p.id == 0L) "Nowa osoba" else "Edytuj osobę") },
            text = {
                Column {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                pickPhone.launch(
                                    android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { SfIcon(AppIcons.kontakty, null); Text("  Wybierz z kontaktów") }
                    OutlinedTextField(name, { name = it }, label = { Text("Imię lub rola, np. Babcia") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    OutlinedTextField(phone, { phone = it }, label = { Text("Telefon (opcjonalnie)") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Text(
                        "Aplikacja nie ma dostępu do całej książki telefonicznej. Dostaje tylko kontakt, który wybierzesz.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSavePerson(p.copy(name = name.trim(), phone = phone.trim())); editing = null }) { Text("Zapisz") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Anuluj") } },
        )
    }
    if (confirmDemo) {
        AlertDialog(
            onDismissRequest = { confirmDemo = false },
            title = { Text("Wczytać przykładowe dane?") },
            text = { Text("Dodam dwoje przykładowych dzieci (Zosia, Kuba) z typowym tygodniem. Możesz je potem usunąć w zakładce Dzieci.") },
            confirmButton = { TextButton(onClick = { confirmDemo = false; onSeedDemo() }) { Text("Wczytaj") } },
            dismissButton = { TextButton(onClick = { confirmDemo = false }) { Text("Anuluj") } },
        )
    }
}

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
