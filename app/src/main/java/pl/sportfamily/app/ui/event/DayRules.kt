package pl.sportfamily.app.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.data.EventDayRule
import pl.sportfamily.app.data.NOBODY
import pl.sportfamily.app.data.Person
import pl.sportfamily.app.ui.minutesLabel
import pl.sportfamily.app.ui.theme.Sf

val WeekdayLong = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")

/** Nazwa osoby: id z ustawień dnia (null = domyślna [fallback], NOBODY = nikt). */
fun personName(persons: List<Person>, id: Long?, fallback: Long?): String {
    val eff = id ?: fallback
    return if (eff == null || eff == NOBODY) "nikt" else persons.firstOrNull { it.id == eff }?.name ?: "nikt"
}

/** Wybór osoby: "jak zwykle" (null), "nikt" (NOBODY) albo konkretna osoba. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonChoice(label: String, selected: Long?, persons: List<Person>, inheritLabel: String, modifier: Modifier = Modifier, onSelect: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val text = when (selected) {
        null -> inheritLabel
        NOBODY -> "nikt"
        else -> persons.firstOrNull { it.id == selected }?.name ?: inheritLabel
    }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = text, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(inheritLabel) }, onClick = { onSelect(null); open = false })
            DropdownMenuItem(text = { Text("nikt") }, onClick = { onSelect(NOBODY); open = false })
            persons.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { onSelect(p.id); open = false }) }
        }
    }
}

/** Wiersz podsumowania dnia w formularzu: godziny i osoby (z ustawień dnia albo domyślne). */
@Composable
fun DayRuleRow(
    weekday: Int, rule: EventDayRule?, defStart: Int, defEnd: Int, defDrop: Long?, defPick: Long?,
    persons: List<Person>, allDay: Boolean, onEdit: () -> Unit,
) {
    val custom = rule != null
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(WeekdayLong[weekday - 1].take(3), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            if (!allDay) Text(
                "${minutesLabel(rule?.startMinute ?: defStart)}-${minutesLabel(rule?.endMinute ?: defEnd)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (rule?.startMinute != null || rule?.endMinute != null) FontWeight.Bold else FontWeight.Normal),
            )
            if (persons.isNotEmpty()) Text(
                "zawozi ${personName(persons, rule?.dropOffPersonId, defDrop)}, odbiera ${personName(persons, rule?.pickUpPersonId, defPick)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (rule?.dropOffPersonId != null || rule?.pickUpPersonId != null) Sf.colors.brand.onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onEdit) { Text(if (custom) "Zmień*" else "Zmień") }
    }
}

/** Okno ustawień jednego dnia tygodnia. Zwraca null, gdy dzień ma być taki jak domyślnie. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayRuleDialog(
    weekday: Int, rule: EventDayRule?, defStart: Int, defEnd: Int, defDrop: Long?, defPick: Long?,
    persons: List<Person>, allDay: Boolean,
    onDismiss: () -> Unit, onSave: (EventDayRule?) -> Unit,
) {
    var s by remember { mutableStateOf(rule?.startMinute) }
    var e by remember { mutableStateOf(rule?.endMinute) }
    var drop by remember { mutableStateOf(rule?.dropOffPersonId) }
    var pick by remember { mutableStateOf(rule?.pickUpPersonId) }
    var picking by remember { mutableStateOf<String?>(null) }
    val effS = s ?: defStart
    val effE = e ?: defEnd
    val bad = !allDay && effE <= effS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(WeekdayLong[weekday - 1]) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { picking = "s" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) { Text("Od ${minutesLabel(effS)}") }
                        OutlinedButton(onClick = { picking = "e" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) { Text("Do ${minutesLabel(effE)}") }
                    }
                    if (bad) Text("Koniec musi być po początku", color = Sf.colors.error.main, style = MaterialTheme.typography.bodySmall)
                }
                if (persons.isNotEmpty()) {
                    PersonChoice("Zawozi", drop, persons, "jak zwykle (${personName(persons, null, defDrop)})") { drop = it }
                    PersonChoice("Odbiera", pick, persons, "jak zwykle (${personName(persons, null, defPick)})") { pick = it }
                }
                Text(
                    "Dotyczy tego dnia tygodnia w całej serii. Zmianę tylko na jeden termin zrobisz w jego szczegółach.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !bad, onClick = {
                val sameTime = (s == null || s == defStart) && (e == null || e == defEnd)
                onSave(
                    if (sameTime && drop == null && pick == null) null
                    else EventDayRule(0, weekday, s?.takeIf { it != defStart }, e?.takeIf { it != defEnd }, drop, pick)
                )
            }) { Text("Zapisz") }
        },
        dismissButton = {
            Row {
                if (rule != null) TextButton(onClick = { onSave(null) }) { Text("Jak zwykle") }
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
    )

    picking?.let { which ->
        val m = if (which == "s") effS else effE
        val state = rememberTimePickerState(initialHour = m / 60, initialMinute = m % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text(if (which == "s") "Początek" else "Koniec") },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    val v = state.hour * 60 + state.minute
                    if (which == "s") {
                        val len = (effE - effS).coerceAtLeast(30)
                        s = v; e = (v + len).coerceAtMost(23 * 60 + 59)
                    } else e = v
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Anuluj") } },
        )
    }
}
