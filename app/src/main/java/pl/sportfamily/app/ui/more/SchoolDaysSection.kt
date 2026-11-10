package pl.sportfamily.app.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.domain.PlannerData
import pl.sportfamily.app.domain.SchoolCalendar
import pl.sportfamily.app.domain.Voivodeship
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.shortLabel
import pl.sportfamily.app.ui.theme.SchoolSettings
import pl.sportfamily.app.ui.theme.Sf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Kolejne dni o tym samym opisie łączymy w jeden zakres, np. "23.12 do 3.01 Przerwa świąteczna". */
data class DayOffRange(val from: LocalDate, val to: LocalDate, val label: String)

fun mergeDaysOff(days: Map<LocalDate, String>): List<DayOffRange> {
    val out = ArrayList<DayOffRange>()
    days.toSortedMap().forEach { (d, l) ->
        val last = out.lastOrNull()
        if (last != null && last.label == l && last.to.plusDays(1) == d) out[out.size - 1] = last.copy(to = d)
        else out += DayOffRange(d, d, l)
    }
    return out
}

fun DayOffRange.dates(): String =
    if (from == to) from.shortLabel() else "${from.shortLabel()} do ${to.shortLabel()}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolDaysSection(
    data: PlannerData,
    today: LocalDate,
    onAdd: (LocalDate, String) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    val voivodeship by SchoolSettings.voivodeship.collectAsState()
    var open by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }

    SectionHeader("Szkoła: dni wolne")
    SfCard(Modifier.fillMaxWidth()) {
        Text(
            "W dni wolne lekcje znikają z planu. Święta, przerwy świąteczne i wakacje aplikacja zna sama. Ferie zależą od województwa.",
            style = MaterialTheme.typography.bodyMedium,
        )
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = Modifier.padding(top = 12.dp)) {
            OutlinedTextField(
                value = voivodeship?.label ?: "nie wybrano",
                onValueChange = {}, readOnly = true,
                label = { Text("Województwo (ferie zimowe)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text("nie wybrano") }, onClick = { SchoolSettings.set(context, null); open = false })
                Voivodeship.entries.forEach { v ->
                    DropdownMenuItem(text = { Text(v.label) }, onClick = { SchoolSettings.set(context, v); open = false })
                }
            }
        }
        if (!SchoolCalendar.ferieKnown(today)) {
            Text(
                "Terminy ferii na ten rok szkolny nie są jeszcze w aplikacji. Dodaj je niżej jako dni wolne szkoły.",
                style = MaterialTheme.typography.bodySmall, color = Sf.colors.important.onContainer, modifier = Modifier.padding(top = 6.dp),
            )
        }

        val upcoming = remember(data, today) {
            mergeDaysOff(data.daysOff(today, today.plusDays(120)))
                .filter { r -> (0..((r.to.toEpochDay() - r.from.toEpochDay()))).any { r.from.plusDays(it).dayOfWeek.value <= 5 } }
                .take(6)
        }
        if (upcoming.isNotEmpty()) {
            Text("Najbliższe dni bez lekcji", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            upcoming.forEach { r ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(r.dates(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(end = 10.dp))
                    Text(r.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Text("Dni wolne tej szkoły", style = MaterialTheme.typography.labelLarge)
        Text(
            "Dni dyrektorskie i inne dni wolne z kalendarium szkoły. Można je też zaimportować z Excela.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val custom = mergeDaysOff(data.customDaysOff.associate { it.day to it.label }).filter { !it.to.isBefore(today) }
        if (custom.isEmpty()) {
            Text("Brak", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
        custom.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EventAvailable, null, tint = Sf.colors.school.main, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(r.dates(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(r.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    var d = r.from
                    while (!d.isAfter(r.to)) { onDelete(d); d = d.plusDays(1) }
                }) { Icon(Icons.Rounded.DeleteOutline, "Usuń ${r.label}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        OutlinedButton(onClick = { adding = true }, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Rounded.Add, null); Text("  Dodaj dzień wolny")
        }
    }

    if (adding) AddDayOffDialog(today, onDismiss = { adding = false }, onSave = { from, to, label ->
        var d = from
        while (!d.isAfter(to)) { onAdd(d, label); d = d.plusDays(1) }
        adding = false
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDayOffDialog(today: LocalDate, onDismiss: () -> Unit, onSave: (LocalDate, LocalDate, String) -> Unit) {
    var from by remember { mutableStateOf(today) }
    var to by remember { mutableStateOf(today) }
    var label by remember { mutableStateOf("Dzień dyrektorski") }
    var picking by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dzień wolny od lekcji") },
        text = {
            Column {
                OutlinedTextField(label, { label = it }, label = { Text("Opis") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { picking = "from" }) { Text("Od: ${from.shortLabel()}") }
                TextButton(onClick = { picking = "to" }) { Text("Do: ${to.shortLabel()}") }
            }
        },
        confirmButton = { TextButton(enabled = label.isNotBlank(), onClick = { onSave(from, maxOf(from, to), label.trim()) }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
    picking?.let { which ->
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (if (which == "from") from else to).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        if (which == "from") { from = d; if (to < d) to = d } else to = d
                    }
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Anuluj") } },
        ) { DatePicker(state) }
    }
}
