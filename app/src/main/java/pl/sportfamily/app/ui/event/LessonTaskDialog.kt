package pl.sportfamily.app.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import pl.sportfamily.app.data.SchoolKind
import pl.sportfamily.app.domain.Lesson
import pl.sportfamily.app.domain.Occurrence
import pl.sportfamily.app.domain.SchoolTasks
import pl.sportfamily.app.ui.components.SfIcon
import pl.sportfamily.app.ui.components.eventIcon
import pl.sportfamily.app.ui.longLabel
import pl.sportfamily.app.ui.minutesLabel
import pl.sportfamily.app.ui.theme.Sf
import java.time.LocalDate

/**
 * Menu przedmiotu w bloku Szkoła: dodanie kartkówki, sprawdzianu, pracy domowej albo projektu
 * na tę lekcję (godziny lekcji, nie cały dzień) i podgląd tego, co już jest dodane.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonTaskDialog(
    lesson: Lesson,
    day: LocalDate,
    existing: List<Occurrence>,
    onOpen: (Occurrence) -> Unit,
    onDismiss: () -> Unit,
    onSave: (SchoolKind, String) -> Unit,
) {
    var kind by remember { mutableStateOf(SchoolKind.QUIZ) }
    var scope by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(lesson.subject)
                Text(
                    "${day.longLabel()}, lekcja ${lesson.number}: ${minutesLabel(lesson.startMinute)}-${minutesLabel(lesson.endMinute)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (existing.isNotEmpty()) {
                    Text("Już dodane", style = MaterialTheme.typography.labelLarge)
                    existing.forEach { o ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            SfIcon(eventIcon(o.event), null, tint = Sf.colors.school.main)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(o.event.title, style = MaterialTheme.typography.bodyMedium)
                                if (o.event.notes.isNotBlank()) Text(o.event.notes, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                            TextButton(onClick = { onOpen(o) }) { Text("Otwórz") }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                Text("Dodaj na tę lekcję", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SchoolTasks.kinds.forEach { k ->
                        FilterChip(
                            selected = kind == k, onClick = { kind = k },
                            label = { Text(SchoolTasks.label(k)) },
                            modifier = Modifier.heightIn(min = 40.dp),
                        )
                    }
                }
                OutlinedTextField(
                    scope, { scope = it }, label = { Text("Zakres materiału (opcjonalnie)") },
                    placeholder = { Text("np. ułamki, dział 3") }, minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Pojawi się w planie dnia na czas tej lekcji, a wieczorem dzień wcześniej przyjdzie przypomnienie.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(kind, scope) }) { Text("Dodaj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}
