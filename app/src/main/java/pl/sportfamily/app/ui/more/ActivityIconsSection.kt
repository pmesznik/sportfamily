package pl.sportfamily.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.ui.components.ActivityIcons
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.theme.Sf

/** Ściąga: jakie ikony zajęć są w aplikacji i jakie słowo w nazwie je włącza. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityIconsSection() {
    var open by rememberSaveable { mutableStateOf(false) }
    SectionHeader("Ikony zajęć")
    SfCard(Modifier.fillMaxWidth()) {
        Text(
            "Ikona dobiera się sama po nazwie zajęć, np. \"Szachy\", \"Chór\", \"Nauka pływania\". Pod ikoną słowo, które ją włącza.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { open = !open }) { Text(if (open) "Zwiń" else "Pokaż wszystkie (${ActivityIcons.size})") }
        if (open) {
            ActivityIcons.groupBy { it.group }.forEach { (group, icons) ->
                Text(group, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { ic ->
                        Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(32.dp)) {
                                Icon(ic.line, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.matchParentSize())
                                Icon(ic.dot, null, tint = Sf.colors.accent, modifier = Modifier.matchParentSize())
                            }
                            Text(ic.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text(ic.keywords.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
