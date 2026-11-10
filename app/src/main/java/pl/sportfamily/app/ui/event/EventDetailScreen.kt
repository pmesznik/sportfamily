package pl.sportfamily.app.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.domain.Occurrence
import pl.sportfamily.app.domain.Planner
import pl.sportfamily.app.domain.PlannerData
import pl.sportfamily.app.ui.WeekdayShort
import pl.sportfamily.app.ui.components.IconBadge
import pl.sportfamily.app.ui.components.KidChip
import pl.sportfamily.app.ui.components.ProgressBar
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.components.SquareCheck
import pl.sportfamily.app.ui.components.ToneChip
import pl.sportfamily.app.ui.components.categoryLabel
import pl.sportfamily.app.ui.components.eventIcon
import pl.sportfamily.app.ui.components.schoolKindLabel
import pl.sportfamily.app.ui.hm
import pl.sportfamily.app.ui.longLabel
import pl.sportfamily.app.ui.shortLabel
import pl.sportfamily.app.ui.theme.Sf
import pl.sportfamily.app.ui.today.openNavigation
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    data: PlannerData,
    eventId: Long,
    day: LocalDate,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleCheck: (Long, LocalDate) -> Unit,
    onCancelOccurrence: () -> Unit,
    onDelete: () -> Unit,
) {
    val occ: Occurrence? = remember(data, eventId, day) {
        Planner.occurrences(data.copy(events = data.events.filter { it.id == eventId }), day, day).firstOrNull()
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sf = Sf.colors

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz") } },
                actions = {
                    if (occ != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edytuj") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, "Usuń") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        if (occ == null) {
            Text("Tego terminu już nie ma.", Modifier.padding(pad).padding(24.dp))
            return@Scaffold
        }
        val e = occ.event
        val tone = sf.of(e.category)
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
        ) {
            ToneChip(e.schoolKind?.let { schoolKindLabel(it) } ?: categoryLabel(e.category), tone, eventIcon(e))
            Text(e.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 10.dp))
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                occ.children.forEach { KidChip(it) }
            }

            SfCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val whenText = when {
                        occ.allDay && occ.multiDay -> "${occ.start.toLocalDate().shortLabel()} do ${occ.end.toLocalDate().shortLabel()}, cały dzień"
                        occ.allDay -> "${day.longLabel()}, cały dzień"
                        occ.multiDay -> "${occ.start.toLocalDate().shortLabel()} ${occ.start.hm()} do ${occ.end.toLocalDate().shortLabel()} ${occ.end.hm()}"
                        else -> "${day.longLabel()}, ${occ.start.hm()} do ${occ.end.hm()}"
                    }
                    Detail(Icons.Rounded.Schedule, whenText, occ.departure?.let { "Wyjazd ${it.hm()} (${e.travelMinutes} min dojazdu + ${e.bufferMinutes} min zapasu)" })
                    if (e.repeatDays != 0) {
                        val days = (0..6).filter { e.repeatDays and (1 shl it) != 0 }.joinToString(", ") { WeekdayShort[it] }
                        Detail(Icons.Rounded.Repeat, "Co tydzień: $days", e.repeatUntilDay?.let { "do ${LocalDate.ofEpochDay(it).shortLabel()}" })
                    }
                    if (e.placeName.isNotBlank() || e.address.isNotBlank()) {
                        Detail(Icons.Rounded.LocationOn, e.placeName.ifBlank { e.address }, e.address.takeIf { e.placeName.isNotBlank() && it.isNotBlank() })
                    }
                    if (e.notes.isNotBlank()) Detail(Icons.AutoMirrored.Rounded.Notes, e.notes, null)
                }
            }

            if (occ.needsTransport || occ.dropOff != null || occ.pickUp != null) {
                SectionHeader("Transport")
                SfCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TransportRow("Tam", occ.dropOff?.name, occ.departure?.hm()?.let { "wyjazd $it" })
                        TransportRow("Powrót", occ.pickUp?.name, "odbiór ${occ.end.hm()}")
                    }
                }
            }

            if (occ.checklist.isNotEmpty()) {
                SectionHeader("Lista rzeczy")
                SfCard(Modifier.fillMaxWidth(), padding = PaddingValues(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
                    Row(Modifier.padding(start = 12.dp, bottom = 4.dp)) {
                        Text("Spakowane", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Text("${occ.checklistDone} z ${occ.checklist.size}", style = MaterialTheme.typography.labelLarge)
                    }
                    ProgressBar(occ.checklistDone / occ.checklist.size.toFloat(), sf.school.main, Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 4.dp))
                    occ.checklist.forEach { item ->
                        val checked = item.id in occ.checkedIds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SquareCheck(checked, { onToggleCheck(item.id, day) }, item.text)
                            Text(item.text, style = MaterialTheme.typography.bodyMedium,
                                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (e.address.isNotBlank() || e.placeName.isNotBlank()) {
                    Button(
                        onClick = { openNavigation(context, e.address.ifBlank { e.placeName }) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.Navigation, null, Modifier.size(20.dp)); Text("  Nawiguj") }
                }
                if (e.repeatDays != 0) {
                    OutlinedButton(
                        onClick = { confirmCancel = true },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.EventBusy, null, Modifier.size(20.dp)); Text("  Odwołaj ten") }
                }
            }
        }
    }

    if (confirmDelete && occ != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (occ.event.repeatDays != 0) "Usunąć całą serię?" else "Usunąć wydarzenie?") },
            text = {
                Text(
                    if (occ.event.repeatDays != 0) "Znikną wszystkie terminy \"${occ.event.title}\". Żeby usunąć tylko ten jeden dzień, użyj \"Odwołaj ten\"."
                    else "\"${occ.event.title}\" zostanie usunięte razem z listą rzeczy."
                )
            },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Usuń", color = sf.error.main) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anuluj") } },
        )
    }
    if (confirmCancel && occ != null) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Odwołać ten termin?") },
            text = { Text("${occ.event.title}, ${day.longLabel()}. Pozostałe terminy serii zostaną.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; onCancelOccurrence() }) { Text("Odwołaj") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Zostaw") } },
        )
    }
}

@Composable
private fun Detail(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String?) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TransportRow(label: String, person: String?, sub: String?) {
    val sf = Sf.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(Icons.Rounded.DirectionsCar, if (person == null) sf.error else sf.sport)
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                "$label: " + (person ?: "nikt nie przypisany"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (person == null) sf.error.onContainer else MaterialTheme.colorScheme.onSurface,
            )
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
