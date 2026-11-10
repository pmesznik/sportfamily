package pl.sportfamily.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.domain.ImportOptions
import pl.sportfamily.app.domain.PlanImport
import pl.sportfamily.app.ui.ImportDraft
import pl.sportfamily.app.ui.components.KidDot
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.components.categoryIcon
import pl.sportfamily.app.ui.components.categoryLabel
import pl.sportfamily.app.ui.longLabel
import pl.sportfamily.app.ui.theme.Sf
import java.time.LocalDate

private val ImportCategories = listOf(Category.SCHOOL, Category.SPORT, Category.EXTRA, Category.FAMILY)

/** Podgląd planu z Excela: wybór dziecka i rodzaju zajęć, potem zapis do kalendarza. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportPreviewDialog(
    draft: ImportDraft,
    children: List<Child>,
    today: LocalDate,
    onConfirm: (ImportOptions, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val plan = draft.plan
    var childId by remember { mutableStateOf(children.singleOrNull()?.id) }
    val cats = remember(plan) { mutableStateListOf(*plan.sections.map { it.category }.toTypedArray()) }
    var asBlock by remember { mutableStateOf(true) }
    var replace by remember { mutableStateOf(true) }
    val until = plan.schoolYearEnd?.takeIf { it.isAfter(today) }

    val options = childId?.let { ImportOptions(it, cats.toList(), asBlock, today, until) }
    val count = options?.let { PlanImport.toEvents(plan, it).size } ?: 0

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Anuluj") }
                    Text("Import planu", style = MaterialTheme.typography.titleLarge)
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                ) {
                    Text(plan.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp))
                    Text("Arkusz: ${draft.sheetName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    SectionHeader("Dla kogo")
                    if (children.isEmpty()) {
                        Text("Najpierw dodaj dziecko w zakładce Dzieci, potem wróć do importu.", color = Sf.colors.error.onContainer)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            children.forEach { c ->
                                FilterChip(
                                    selected = childId == c.id, onClick = { childId = c.id },
                                    label = { Text(c.name) }, leadingIcon = { KidDot(c, 20.dp) },
                                    modifier = Modifier.heightIn(min = 40.dp),
                                )
                            }
                        }
                    }

                    plan.sections.forEachIndexed { i, sec ->
                        SectionHeader(sec.label)
                        SfCard(Modifier.fillMaxWidth()) {
                            Text("Rodzaj", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                ImportCategories.forEach { c ->
                                    FilterChip(
                                        selected = cats[i] == c, onClick = { cats[i] = c },
                                        label = { Text(categoryLabel(c)) },
                                        leadingIcon = { Icon(categoryIcon(c), null, tint = Sf.colors.of(c).main, modifier = Modifier.size(18.dp)) },
                                    )
                                }
                            }
                            sec.items.groupBy { it.day }.toSortedMap().forEach { (day, items) ->
                                Row(Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        PlanImport.dayName(day).replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(104.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        if (cats[i] == Category.SCHOOL && asBlock) {
                                            Text(
                                                "Szkoła ${PlanImport.hm(items.minOf { it.startMinute })}-${PlanImport.hm(items.maxOf { it.endMinute })}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            )
                                            Text(
                                                "${items.size} lekcji: " + items.joinToString(", ") { it.title },
                                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        } else items.forEach {
                                            Text("${PlanImport.hm(it.startMinute)}-${PlanImport.hm(it.endMinute)}  ${it.title}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SectionHeader("Ustawienia")
                    SfCard(Modifier.fillMaxWidth()) {
                        if (cats.any { it == Category.SCHOOL }) {
                            SwitchLine("Lekcje jako jeden blok \"Szkoła\" dziennie", "Lista przedmiotów będzie w szczegółach bloku.", asBlock) { asBlock = it }
                        }
                        SwitchLine("Zastąp wcześniej zaimportowany plan", "Usuwa zajęcia z poprzedniego importu dla tego dziecka. Dodane ręcznie zostają.", replace) { replace = it }
                        Text(
                            "Od dziś do " + (until?.let { "${it.longLabel()} ${it.year} (koniec roku szkolnego)" } ?: "odwołania"),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (plan.warnings.isNotEmpty()) {
                        SectionHeader("Pominięte")
                        plan.warnings.forEach {
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Icon(Icons.Rounded.Info, null, tint = Sf.colors.important.main, modifier = Modifier.size(18.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    Text(" ", Modifier.padding(bottom = 16.dp))
                }
                Button(
                    onClick = { options?.let { onConfirm(it, replace) } },
                    enabled = options != null && count > 0,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(if (options == null) "Wybierz dziecko" else "Dodaj do kalendarza ($count)") }
            }
        }
    }
}

@Composable
private fun SwitchLine(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}
