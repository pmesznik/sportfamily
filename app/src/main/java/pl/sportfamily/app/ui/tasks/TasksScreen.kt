package pl.sportfamily.app.ui.tasks

import pl.sportfamily.app.ui.components.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.data.TaskEntity
import pl.sportfamily.app.domain.PlannerData
import pl.sportfamily.app.ui.components.EmptyState
import pl.sportfamily.app.ui.components.KidDot
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.today.TaskRow
import java.time.LocalDate

@Composable
fun TasksScreen(
    data: PlannerData,
    today: LocalDate,
    contentPadding: PaddingValues,
    addRequest: Boolean,
    onAddRequestHandled: () -> Unit,
    onToggle: (TaskEntity) -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
) {
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    if (addRequest) { editing = TaskEntity(title = ""); onAddRequestHandled() }
    val open = data.tasks.filter { !it.done }
    val done = data.tasks.filter { it.done }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp)) {
        item { Text("Zadania", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 8.dp)) }
        if (data.tasks.isEmpty()) {
            item {
                EmptyState(AppIcons.zadanie, "Nic do zrobienia", "Opłaty, zgody, zakupy sprzętu. Wpisz je tutaj, a najpilniejsze pokażą się na ekranie Dzisiaj.") {
                    Button(onClick = { editing = TaskEntity(title = "") }) { Text("Dodaj zadanie") }
                }
            }
        }
        if (open.isNotEmpty()) {
            item { SectionHeader("Do zrobienia (${open.size})") }
            items(open, key = { it.id }) { t -> SwipeTask(t, data, today, onToggle, onDelete) { editing = t } }
        }
        if (done.isNotEmpty()) {
            item { SectionHeader("Zrobione") }
            items(done.take(20), key = { it.id }) { t -> SwipeTask(t, data, today, onToggle, onDelete) { editing = t } }
        }
        if (data.tasks.isNotEmpty()) item {
            Text("Przesuń zadanie w bok, żeby je usunąć.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
    }

    editing?.let { t -> TaskDialog(t, data, today, onDismiss = { editing = null }, onSave = { onSave(it); editing = null }) }
}

@Composable
private fun SwipeTask(t: TaskEntity, data: PlannerData, today: LocalDate, onToggle: (TaskEntity) -> Unit, onDelete: (TaskEntity) -> Unit, onEdit: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it != SwipeToDismissBoxValue.Settled) { onDelete(t); true } else false
    })
    SwipeToDismissBox(state, backgroundContent = {}, modifier = Modifier.padding(bottom = 8.dp)) {
        SfCard(Modifier.fillMaxWidth(), onClick = onEdit, padding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
            TaskRow(t, data, today, onToggle = { onToggle(t) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDialog(initial: TaskEntity, data: PlannerData, today: LocalDate, onDismiss: () -> Unit, onSave: (TaskEntity) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var childId by remember { mutableStateOf(initial.childId) }
    var due by remember { mutableStateOf(initial.dueDay) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nowe zadanie" else "Zadanie") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Co trzeba zrobić") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Notatka, np. kwota") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Text("Termin", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(null to "Bez terminu", 0L to "Dziś", 1L to "Jutro", 3L to "Za 3 dni", 7L to "Za tydzień").forEach { (off, label) ->
                        val value = off?.let { today.toEpochDay() + it }
                        FilterChip(selected = due == value, onClick = { due = value }, label = { Text(label) })
                    }
                }
                if (data.children.isNotEmpty()) {
                    Text("Dotyczy", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = childId == null, onClick = { childId = null }, label = { Text("Rodzina") })
                        data.children.forEach { c ->
                            FilterChip(selected = childId == c.id, onClick = { childId = c.id }, label = { Text(c.name) }, leadingIcon = { KidDot(c, 18.dp) })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(initial.copy(title = title.trim(), note = note.trim(), childId = childId, dueDay = due)) }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}
