package pl.sportfamily.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.domain.Occurrence
import pl.sportfamily.app.domain.Planner
import pl.sportfamily.app.domain.PlannerData
import pl.sportfamily.app.ui.WeekdayShort
import pl.sportfamily.app.ui.components.KidDot
import pl.sportfamily.app.ui.components.KidDots
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.components.ToneChip
import pl.sportfamily.app.ui.components.categoryIcon
import pl.sportfamily.app.ui.components.categoryLabel
import pl.sportfamily.app.ui.components.eventIcon
import pl.sportfamily.app.ui.dayMonthLabel
import pl.sportfamily.app.ui.hm
import pl.sportfamily.app.ui.longLabel
import pl.sportfamily.app.ui.monthYearLabel
import pl.sportfamily.app.ui.theme.Sf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    data: PlannerData,
    today: LocalDate,
    contentPadding: PaddingValues,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onOpen: (Occurrence) -> Unit,
) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 tydzień, 1 miesiąc
    var hiddenChildren by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var hiddenCats by rememberSaveable { mutableStateOf(setOf<String>()) }

    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val monthStart = selected.withDayOfMonth(1)
    val rangeFrom = if (mode == 0) monday else monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val rangeTo = if (mode == 0) monday.plusDays(6) else monthStart.plusMonths(1).minusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val all = remember(data, rangeFrom, rangeTo) { Planner.occurrences(data, rangeFrom, rangeTo) }
    val visible = remember(all, hiddenChildren, hiddenCats) {
        all.filter { o ->
            o.event.category.name !in hiddenCats &&
                (o.children.isEmpty() || o.children.any { it.id !in hiddenChildren })
        }
    }
    val conflicts = remember(visible) { Planner.conflicts(visible) }
    val conflictKeys = remember(conflicts) { conflicts.flatMap { listOfNotNull(it.a.key, it.b?.key) }.toSet() }
    val daysOff = remember(data, rangeFrom, rangeTo) { data.daysOff(rangeFrom, rangeTo) }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (mode == 0) "Tydzień ${monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}" else "Miesiąc",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (mode == 0) weekTitle(monday) else selected.monthYearLabel(),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                IconButton(onClick = { onSelect(if (mode == 0) selected.minusWeeks(1) else selected.minusMonths(1)) }) { Icon(Icons.Rounded.ChevronLeft, "Wcześniej") }
                IconButton(onClick = { onSelect(if (mode == 0) selected.plusWeeks(1) else selected.plusMonths(1)) }) { Icon(Icons.Rounded.ChevronRight, "Później") }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                listOf("Tydzień", "Miesiąc").forEachIndexed { i, label ->
                    SegmentedButton(selected = mode == i, onClick = { mode = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(data.children, key = { "k${it.id}" }) { c ->
                    val on = c.id !in hiddenChildren
                    FilterChip(
                        selected = on,
                        onClick = { hiddenChildren = if (on) hiddenChildren + c.id else hiddenChildren - c.id },
                        label = { Text(c.name) },
                        leadingIcon = { KidDot(c, 20.dp) },
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
                items(Category.entries.toList(), key = { it.name }) { cat ->
                    val on = cat.name !in hiddenCats
                    FilterChip(
                        selected = on,
                        onClick = { hiddenCats = if (on) hiddenCats + cat.name else hiddenCats - cat.name },
                        label = { Text(categoryLabel(cat)) },
                        leadingIcon = { Icon(categoryIcon(cat), null, tint = Sf.colors.of(cat).main, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
            }
        }

        if (mode == 0) {
            item { WeekStrip(monday, selected, today, visible, conflictKeys, onSelect) }
            val days = (0..6).map { monday.plusDays(it.toLong()) }
            days.forEach { day ->
                val dayOcc = visible.filter { it.overlapsDay(day) }
                val offLabel = daysOff[day]?.takeIf { day.dayOfWeek.value <= 5 }
                if (dayOcc.isEmpty() && offLabel == null) return@forEach
                item(key = "h$day") {
                    val hasConflict = dayOcc.any { it.key in conflictKeys }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) {
                        Text(
                            if (day == today) "Dziś, ${day.dayMonthLabel()}" else day.longLabel(),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (hasConflict) ToneChip("Konflikt", Sf.colors.error, Icons.Rounded.Warning, Modifier.padding(start = 8.dp))
                    }
                    if (offLabel != null) {
                        ToneChip("Bez lekcji: $offLabel", Sf.colors.school, Icons.Rounded.EventAvailable, Modifier.padding(bottom = 8.dp))
                    }
                }
                dayOcc.forEach { o ->
                    item(key = "o${o.key}_$day") { AgendaCard(o, day, o.key in conflictKeys, onClick = { onOpen(o) }) }
                }
            }
            if (visible.isEmpty()) item {
                Text("W tym tygodniu nic nie zaplanowano.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
            }
        } else {
            item { MonthGrid(selected, today, visible, daysOff, onSelect = { onSelect(it); mode = 0 }) }
        }
    }
}

private fun weekTitle(monday: LocalDate): String {
    val sun = monday.plusDays(6)
    return if (monday.month == sun.month) "${monday.dayOfMonth} do ${sun.dayMonthLabel()}"
    else "${monday.dayMonthLabel()} do ${sun.dayMonthLabel()}"
}

@Composable
private fun WeekStrip(monday: LocalDate, selected: LocalDate, today: LocalDate, occ: List<Occurrence>, conflictKeys: Set<String>, onSelect: (LocalDate) -> Unit) {
    val sf = Sf.colors
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (0..6).forEach { i ->
            val d = monday.plusDays(i.toLong())
            val isSel = d == selected
            val dayOcc = occ.filter { it.overlapsDay(d) }
            val dots: List<Color> = buildList {
                if (dayOcc.any { it.key in conflictKeys }) add(sf.error.main)
                dayOcc.flatMap { it.children }.distinctBy { it.id }.forEach { add(Color(it.colorArgb)) }
                if (dayOcc.any { it.event.category == Category.IMPORTANT }) add(sf.important.main)
            }.take(3)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .then(if (d == today && !isSel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
                    .clickable { onSelect(d) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text(WeekdayShort[i], style = MaterialTheme.typography.labelSmall, color = if (isSel) fg else MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = fg)
                Row(Modifier.height(8.dp).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    dots.forEach { Box(Modifier.size(6.dp).clip(CircleShape).background(if (isSel) fg else it)) }
                }
            }
        }
    }
}

@Composable
fun AgendaCard(o: Occurrence, day: LocalDate, conflict: Boolean, onClick: () -> Unit) {
    val sf = Sf.colors
    val tone = sf.of(o.event.category)
    val important = o.event.category == Category.IMPORTANT
    SfCard(
        Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .then(if (conflict) Modifier.border(2.dp, sf.error.main, MaterialTheme.shapes.medium) else Modifier),
        onClick = onClick,
        padding = PaddingValues(12.dp),
        color = if (important) tone.container else MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(52.dp)) {
                val startsToday = o.start.toLocalDate() == day
                Text(
                    when { o.allDay -> "cały"; startsToday -> o.start.hm(); else -> "cd." },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    when { o.allDay -> "dzień"; o.end.toLocalDate() == day -> o.end.hm(); else -> "" },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(tone.main))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(o.event.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (important) tone.onContainer else MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                    if (o.children.isNotEmpty()) { KidDots(o.children, 20.dp); Box(Modifier.width(6.dp)) }
                    val noDriver = o.needsTransport && o.dropOff == null
                    if (noDriver) {
                        Text("Brak kierowcy", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = sf.error.onContainer)
                    } else if (o.dropOff != null) {
                        Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text(" ${o.dropOff.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (o.event.placeName.isNotBlank()) {
                        Text(o.event.placeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Icon(eventIcon(o.event), null, tint = if (important) tone.onContainer else tone.main, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MonthGrid(selected: LocalDate, today: LocalDate, occ: List<Occurrence>, daysOff: Map<LocalDate, String>, onSelect: (LocalDate) -> Unit) {
    val sf = Sf.colors
    val first = selected.withDayOfMonth(1)
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    SfCard(Modifier.fillMaxWidth().padding(top = 14.dp), padding = PaddingValues(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            WeekdayShort.forEach { Text(it, Modifier.weight(1f).padding(vertical = 6.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        var week = start
        while (week <= first.plusMonths(1).minusDays(1)) {
            val ws = week
            Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { i ->
                    val d = ws.plusDays(i.toLong())
                    val inMonth = d.month == first.month
                    val dayOcc = occ.filter { it.overlapsDay(d) && !(it.event.category == Category.SCHOOL && it.event.schoolKind == pl.sportfamily.app.data.SchoolKind.LESSONS) }
                    Column(
                        Modifier
                            .weight(1f)
                            .aspectRatio(0.82f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    d == today -> sf.brand.container
                                    d in daysOff && d.dayOfWeek.value <= 5 && inMonth -> sf.school.container
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onSelect(d) }
                            .padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "${d.dayOfMonth}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (d == today) FontWeight.Bold else FontWeight.Medium),
                            color = when {
                                !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                d == today -> sf.brand.onContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            dayOcc.take(3).forEach { o ->
                                Box(Modifier.width(22.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(sf.of(o.event.category).main.copy(alpha = if (inMonth) 1f else 0.4f)))
                            }
                        }
                    }
                }
            }
            week = week.plusWeeks(1)
        }
    }
    Text(
        "Dotknij dnia, żeby zobaczyć jego tydzień. Paski to kolory kategorii, zwykłe lekcje są ukryte. Zielone tło: dzień bez lekcji.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
    )
}
