package pl.sportfamily.app.ui.today

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.TaskEntity
import pl.sportfamily.app.domain.Conflict
import pl.sportfamily.app.domain.ConflictKind
import pl.sportfamily.app.domain.Occurrence
import pl.sportfamily.app.domain.Planner
import pl.sportfamily.app.domain.PlannerData
import pl.sportfamily.app.ui.components.EmptyState
import pl.sportfamily.app.ui.components.KidChip
import pl.sportfamily.app.ui.components.KidDot
import pl.sportfamily.app.ui.components.KidDots
import pl.sportfamily.app.ui.components.ProgressBar
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.components.SquareCheck
import pl.sportfamily.app.ui.components.ToneChip
import pl.sportfamily.app.ui.components.VSpace
import pl.sportfamily.app.ui.components.eventIcon
import pl.sportfamily.app.ui.hm
import pl.sportfamily.app.ui.longLabel
import pl.sportfamily.app.ui.relativeLabel
import pl.sportfamily.app.ui.theme.Sf
import pl.sportfamily.app.ui.untilLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun TodayScreen(
    data: PlannerData,
    now: LocalDateTime,
    contentPadding: PaddingValues,
    onOpen: (Occurrence) -> Unit,
    onToggleCheck: (Long, LocalDate) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onSeedDemo: () -> Unit,
    onAddChild: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTasks: () -> Unit,
) {
    val today = now.toLocalDate()
    val week = remember(data, today) { Planner.occurrences(data, today, today.plusDays(14)) }
    val todayOcc = remember(week, today) { week.filter { it.overlapsDay(today) } }
    // Karta główna: najbliższe wydarzenie, które jeszcze się nie skończyło. Zwykłe lekcje pomijamy, są na osi dnia.
    val next = remember(week, now) {
        week.firstOrNull {
            !it.allDay && it.end.isAfter(now) &&
                !(it.event.category == Category.SCHOOL && it.event.schoolKind == pl.sportfamily.app.data.SchoolKind.LESSONS)
        }
    }
    val conflicts = remember(week, now) {
        // Ta sama seria bez kierowcy daje konflikt co tydzień; pokazujemy tylko najbliższy.
        Planner.conflicts(week.filter { it.start.toLocalDate() <= today.plusDays(7) && it.end.isAfter(now) })
            .distinctBy { Triple(it.kind, it.a.event.id, it.b?.event?.id) }
    }
    val openTasks = data.tasks.filter { !it.done }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp)) {
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(today.longLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(greeting(now.toLocalTime()), style = MaterialTheme.typography.headlineMedium)
            }
            VSpace(8.dp)
        }

        if (data.children.isEmpty() && data.events.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "Zacznijmy od dzieci",
                    text = "Dodaj dzieci i ich stałe zajęcia. Aplikacja sama ułoży plan dnia, policzy godzinę wyjazdu i przypomni o sprzęcie.",
                ) {
                    Button(onClick = onAddChild, modifier = Modifier.heightIn(min = 48.dp)) { Text("Dodaj dziecko") }
                    VSpace(8.dp)
                    OutlinedButton(onClick = onSeedDemo, modifier = Modifier.heightIn(min = 48.dp)) { Text("Wczytaj przykładowy tydzień") }
                }
            }
            return@LazyColumn
        }

        conflicts.take(2).forEach { c -> item(key = "c_${c.kind}_${c.a.key}_${c.b?.key}") { ConflictBanner(c, today) { onOpen(c.a) }; VSpace(8.dp) } }

        if (next != null) {
            item(key = "hero") { HeroCard(next, now, onOpen = { onOpen(next) }, onToggleCheck = onToggleCheck) }
        } else {
            item {
                SfCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                    Text("Spokojnie", style = MaterialTheme.typography.titleLarge)
                    Text("Nic więcej zaplanowanego w najbliższych dniach.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { SectionHeader("Plan dnia", "Kalendarz", onOpenCalendar) }
        if (todayOcc.isEmpty()) {
            item { Text("Dziś nic nie zaplanowano.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
        } else {
            val timeline = buildTimeline(todayOcc, today)
            var nowShown = false
            timeline.forEach { row ->
                if (!nowShown && row.time.isAfter(now)) {
                    nowShown = true
                    item(key = "now") { NowLine(now) }
                }
                item(key = row.key) { TimelineRow(row, now, onClick = { onOpen(row.occ) }) }
            }
            if (!nowShown) item(key = "now") { NowLine(now) }
        }

        if (openTasks.isNotEmpty()) {
            item { SectionHeader("Do zrobienia", "Wszystkie (${openTasks.size})", onOpenTasks) }
            item {
                SfCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    openTasks.take(3).forEach { t ->
                        TaskRow(t, data, today, onToggle = { onToggleTask(t) })
                    }
                }
            }
        }
    }
}

private fun greeting(t: LocalTime) = when (t.hour) {
    in 5..10 -> "Dzień dobry"
    in 11..17 -> "Cześć"
    else -> "Dobry wieczór"
}

@Composable
private fun ConflictBanner(c: Conflict, today: LocalDate, onClick: () -> Unit) {
    val tone = Sf.colors.error
    val day = c.a.date.relativeLabel(today).replaceFirstChar { it.uppercase() }
    val who = c.a.children.joinToString { it.name }.ifEmpty { c.a.event.title }
    val (title, sub) = when (c.kind) {
        ConflictKind.NO_DRIVER -> "$day: nikt nie wiezie ($who)" to "${c.a.event.title} ${c.a.start.hm()}, brak kierowcy"
        ConflictKind.CHILD_OVERLAP -> "$day: $who w dwóch miejscach" to "${c.a.event.title} i ${c.b?.event?.title} nakładają się"
        ConflictKind.DRIVER_OVERLAP -> "$day: ${c.a.dropOff?.name ?: c.a.pickUp?.name} ma dwa kursy" to "${c.a.event.title} ${c.a.start.hm()} i ${c.b?.event?.title} ${c.b?.start?.hm()}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tone.container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Warning, null, tint = tone.onContainer)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = tone.onContainer, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Text(sub, color = tone.onContainer, style = MaterialTheme.typography.bodySmall)
        }
        Text("Popraw", color = tone.onContainer, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(o: Occurrence, now: LocalDateTime, onOpen: () -> Unit, onToggleCheck: (Long, LocalDate) -> Unit) {
    val sf = Sf.colors
    val context = LocalContext.current
    val ongoing = !o.start.isAfter(now)
    SfCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, onClick = onOpen, padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (ongoing) sf.school.main else sf.important.main))
            Text(
                if (ongoing) "Trwa teraz" else "Następne, ${untilLabel(now, o.start)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
            Text(o.start.hm(), style = MaterialTheme.typography.displayMedium)
            Text(
                if (o.start.toLocalDate() == now.toLocalDate()) "  do ${o.end.hm()}" else "  ${o.date.relativeLabel(now.toLocalDate())}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(eventIcon(o.event), null, tint = sf.of(o.event.category).main, modifier = Modifier.size(22.dp))
            Text(o.event.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
        }
        if (o.children.isNotEmpty()) {
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { o.children.forEach { KidChip(it) } }
        }

        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            o.departure?.let { dep ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToneChip("Wyjazd ${dep.hm()}", sf.important, Icons.Rounded.Schedule)
                    Text(
                        "${o.event.travelMinutes} min dojazdu + ${o.event.bufferMinutes} min zapasu",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (o.event.placeName.isNotBlank() || o.event.address.isNotBlank()) {
                InfoRow(Icons.Rounded.LocationOn, o.event.placeName.ifBlank { o.event.address }, o.event.address.takeIf { o.event.placeName.isNotBlank() && it.isNotBlank() })
            }
            if (o.needsTransport || o.dropOff != null || o.pickUp != null) {
                InfoRow(
                    Icons.Rounded.DirectionsCar,
                    "Wiezie: " + (o.dropOff?.name ?: "nieprzypisane"),
                    o.pickUp?.let { "Odbiera: ${it.name}, ${o.end.hm()}" },
                    warn = o.needsTransport && o.dropOff == null,
                )
            }
        }

        if (o.checklist.isNotEmpty()) {
            Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Backpack, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text("Do zabrania", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp).weight(1f))
                Text("${o.checklistDone} z ${o.checklist.size}", style = MaterialTheme.typography.labelLarge)
            }
            ProgressBar(o.checklistDone / o.checklist.size.toFloat(), sf.school.main, Modifier.fillMaxWidth().padding(vertical = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                o.checklist.forEach { item ->
                    val done = item.id in o.checkedIds
                    val tone = if (done) sf.school else sf.neutral
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(tone.container)
                            .clickable { onToggleCheck(item.id, o.date) }
                            .heightIn(min = 36.dp)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (done) Icon(Icons.Rounded.Check, null, tint = tone.onContainer, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                        Text(item.text, style = MaterialTheme.typography.bodySmall, color = tone.onContainer)
                    }
                }
            }
        }

        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (o.event.address.isNotBlank() || o.event.placeName.isNotBlank()) {
                Button(
                    onClick = { openNavigation(context, o.event.address.ifBlank { o.event.placeName }) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Navigation, null, Modifier.size(20.dp)); Text("  Nawiguj")
                }
            }
            FilledTonalButton(
                onClick = onOpen,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) { Icon(Icons.Rounded.Checklist, null, Modifier.size(20.dp)); Text("  Szczegóły") }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String?, warn: Boolean = false) {
    val tint = if (warn) Sf.colors.error.main else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = if (warn) Sf.colors.error.onContainer else MaterialTheme.colorScheme.onSurface)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun openNavigation(context: Context, query: String) {
    val uri = Uri.parse("google.navigation:q=" + Uri.encode(query))
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))) } catch (_: Exception) {}
    }
}

/** Pozycja osi dnia: wydarzenie albo wyjazd na nie. */
private data class TimelineItem(val key: String, val time: LocalDateTime, val occ: Occurrence, val travel: Boolean)

private fun buildTimeline(occ: List<Occurrence>, today: LocalDate): List<TimelineItem> {
    val items = ArrayList<TimelineItem>()
    occ.forEach { o ->
        val startToday = if (o.start.toLocalDate() == today) o.start else today.atStartOfDay()
        o.departure?.takeIf { it.toLocalDate() == today }?.let { items += TimelineItem("d_${o.key}", it, o, travel = true) }
        items += TimelineItem("e_${o.key}", startToday, o, travel = false)
    }
    return items.sortedWith(compareBy({ it.time }, { !it.travel }))
}

@Composable
private fun NowLine(now: LocalDateTime) {
    val c = Sf.colors.error.main
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(now.hm(), style = MaterialTheme.typography.labelSmall, color = c, modifier = Modifier.width(48.dp).padding(end = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Box(Modifier.size(10.dp).clip(CircleShape).background(c))
        Box(Modifier.weight(1f).height(2.dp).background(c))
    }
}

@Composable
private fun TimelineRow(row: TimelineItem, now: LocalDateTime, onClick: () -> Unit) {
    val sf = Sf.colors
    val o = row.occ
    val past = if (row.travel) row.time.isBefore(now) else o.end.isBefore(now)
    val tone = if (row.travel) sf.important else sf.of(o.event.category)
    val alpha = if (past) 0.55f else 1f
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Text(
            if (o.allDay) "cały" else row.time.hm(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (past) FontWeight.Medium else FontWeight.Bold),
            color = if (past) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(48.dp).padding(top = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        Box(Modifier.width(26.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
            Box(Modifier.padding(top = 18.dp).size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background).padding(2.dp).clip(CircleShape).background(tone.main.copy(alpha = alpha)))
        }
        if (row.travel) {
            Row(
                Modifier.weight(1f).padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.DirectionsCar, null, tint = tone.main.copy(alpha = alpha), modifier = Modifier.size(20.dp))
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Wyjazd: ${o.event.title.lowercase()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                    Text(
                        (o.dropOff?.name ?: "brak kierowcy") + ", ${o.event.travelMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (o.dropOff == null) sf.error.main else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            SfCard(
                Modifier.weight(1f).padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                onClick = onClick,
                padding = PaddingValues(0.dp),
                color = if (past) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
            ) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(4.dp).fillMaxHeight().background(tone.main.copy(alpha = alpha)))
                    Row(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(o.event.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                if (o.children.isNotEmpty()) { KidDots(o.children, 20.dp); Box(Modifier.width(6.dp)) }
                                Text(
                                    listOfNotNull(o.event.placeName.takeIf { it.isNotBlank() }, if (o.allDay) null else "do ${o.end.hm()}").joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(eventIcon(o.event), null, tint = tone.main.copy(alpha = alpha), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(t: TaskEntity, data: PlannerData, today: LocalDate, onToggle: () -> Unit) {
    val sf = Sf.colors
    val child = data.children.firstOrNull { it.id == t.childId }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
        SquareCheck(t.done, onToggle, t.title)
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                t.title, style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
                color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            val meta = listOfNotNull(t.note.takeIf { it.isNotBlank() }, child?.name).joinToString(", ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!t.done && t.dueDay != null) {
            val due = LocalDate.ofEpochDay(t.dueDay)
            val days = t.dueDay - today.toEpochDay()
            val tone = when {
                days < 0 -> sf.error
                days <= 1 -> sf.important
                days <= 3 -> sf.important
                else -> sf.neutral
            }
            ToneChip(if (days < 0) "Po terminie" else "Do " + due.relativeLabel(today), tone)
        }
    }
}
