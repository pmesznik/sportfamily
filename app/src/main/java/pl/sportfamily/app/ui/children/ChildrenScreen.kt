package pl.sportfamily.app.ui.children

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.SchoolKind
import pl.sportfamily.app.domain.Planner
import pl.sportfamily.app.domain.PlannerData
import pl.sportfamily.app.ui.components.EmptyState
import pl.sportfamily.app.ui.components.KidDot
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.components.ToneChip
import pl.sportfamily.app.ui.components.eventIcon
import pl.sportfamily.app.ui.hm
import pl.sportfamily.app.ui.relativeLabel
import pl.sportfamily.app.ui.theme.ChildPalette
import pl.sportfamily.app.ui.theme.Sf
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChildrenScreen(
    data: PlannerData,
    now: LocalDateTime,
    contentPadding: PaddingValues,
    editRequest: Child?,
    onEditRequestHandled: () -> Unit,
    onSave: (Child) -> Unit,
    onDelete: (Child) -> Unit,
) {
    val today = now.toLocalDate()
    var editing by remember { mutableStateOf<Child?>(null) }
    if (editRequest != null) { editing = editRequest; onEditRequestHandled() }
    val monthStart = today.withDayOfMonth(1)
    val occ = remember(data, today) { Planner.occurrences(data, monthStart, today.plusDays(30)) }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp)) {
        item { Text("Dzieci", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 8.dp)) }
        if (data.children.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.Groups, "Brak dzieci", "Dodaj dziecko i wybierz mu kolor. Ten kolor zobaczysz w kalendarzu, na osi dnia i na widgetach.") {
                    Button(onClick = { editing = Child(name = "", colorArgb = ChildPalette.first()) }) { Text("Dodaj dziecko") }
                }
            }
        }
        items(data.children, key = { it.id }) { c ->
            val mine = occ.filter { o -> o.children.any { it.id == c.id } }
            val monthSport = mine.filter { it.event.category == Category.SPORT && it.date < today.plusDays(1) && it.date >= monthStart }
            val hours = monthSport.sumOf { Duration.between(it.start, it.end).toMinutes() } / 60.0
            val nextSport = mine.firstOrNull { it.start.isAfter(now) && it.event.category != Category.SCHOOL }
            val nextSchool = mine.firstOrNull { it.start.isAfter(now) && it.event.category == Category.SCHOOL && it.event.schoolKind != SchoolKind.LESSONS }
            SfCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), onClick = { editing = c }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KidDot(c, 52.dp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(c.name, style = MaterialTheme.typography.headlineSmall)
                        if (c.note.isNotBlank()) Text(c.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("${monthSport.size}", "treningów w tym miesiącu", Modifier.weight(1f))
                    Stat("%.0f h".format(hours), "aktywności", Modifier.weight(1f))
                }
                if (nextSport != null || nextSchool != null) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOfNotNull(nextSport, nextSchool).forEach { o ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(eventIcon(o.event), null, tint = Sf.colors.of(o.event.category).main, modifier = Modifier.size(20.dp))
                                Text(o.event.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp).weight(1f))
                                ToneChip("${o.date.relativeLabel(today)} ${o.start.hm()}", Sf.colors.neutral)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { c ->
        ChildDialog(
            initial = c,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = if (c.id != 0L) ({ onDelete(c); editing = null }) else null,
            nextOrder = data.children.size,
        )
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChildDialog(initial: Child, onDismiss: () -> Unit, onSave: (Child) -> Unit, onDelete: (() -> Unit)?, nextOrder: Int) {
    var name by remember { mutableStateOf(initial.name) }
    var note by remember { mutableStateOf(initial.note) }
    var color by remember { mutableIntStateOf(initial.colorArgb) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nowe dziecko" else "Edytuj dziecko") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Imię") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Klasa lub opis") }, placeholder = { Text("np. 11 lat, klasa 5a") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Text("Kolor", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChildPalette.forEachIndexed { i, argb ->
                        val sel = argb == color
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .then(if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = argb }
                                .semantics { contentDescription = "Kolor ${i + 1}" + if (sel) ", wybrany" else "" },
                            contentAlignment = Alignment.Center,
                        ) { if (sel) Icon(Icons.Rounded.Check, null, tint = Color.White) }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 12.dp)) { Text("Usuń dziecko", color = Sf.colors.error.main) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(initial.copy(name = name.trim(), note = note.trim(), colorArgb = color, sortOrder = if (initial.id == 0L) nextOrder else initial.sortOrder))
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Usunąć ${initial.name}?") },
            text = { Text("Dziecko zniknie z wydarzeń. Same wydarzenia zostaną.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Usuń", color = Sf.colors.error.main) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anuluj") } },
        )
    }
}
