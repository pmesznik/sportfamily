package pl.sportfamily.app.ui.event

import pl.sportfamily.app.ui.components.AppIcons
import pl.sportfamily.app.ui.components.SfIcon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.ChecklistItem
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.SchoolKind
import pl.sportfamily.app.domain.PlannerData
import pl.sportfamily.app.ui.EventDraftSource
import pl.sportfamily.app.ui.WeekdayShort
import pl.sportfamily.app.ui.components.KidDot
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.components.categoryIcon
import pl.sportfamily.app.ui.components.categoryLabel
import pl.sportfamily.app.ui.components.schoolKindLabel
import pl.sportfamily.app.ui.longLabel
import pl.sportfamily.app.ui.minutesLabel
import pl.sportfamily.app.ui.shortLabel
import pl.sportfamily.app.ui.theme.Sf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val ChecklistTemplates = linkedMapOf(
    "Trening" to listOf("Strój", "Buty", "Woda", "Ręcznik"),
    "Tenis" to listOf("Rakiety", "Buty na kort", "Strój", "Woda", "Ręcznik"),
    "Piłka" to listOf("Korki", "Ochraniacze", "Getry", "Strój", "Woda"),
    "Basen" to listOf("Strój kąpielowy", "Czepek", "Okularki", "Klapki", "Ręcznik", "Szampon"),
    "Turniej" to listOf("Sprzęt", "Strój x2", "Ręcznik", "Woda i przekąski", "Legitymacja zawodnika", "Opłata startowa"),
    "Wyjazd" to listOf("Dokumenty", "Ubrania", "Kosmetyczka", "Ładowarka", "Leki"),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditEventScreen(
    data: PlannerData,
    eventId: Long?,
    initialDay: LocalDate,
    load: suspend (Long) -> EventDraftSource?,
    onSave: (EventEntity, List<Long>, List<ChecklistItem>, List<pl.sportfamily.app.data.EventDayRule>) -> Unit,
    onClose: () -> Unit,
) {
    val sf = Sf.colors
    var loaded by rememberSaveable { mutableStateOf(eventId == null) }

    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.SPORT) }
    var schoolKind by rememberSaveable { mutableStateOf(SchoolKind.TEST) }
    var childIds by rememberSaveable { mutableStateOf(if (data.children.size == 1) setOf(data.children.first().id) else setOf<Long>()) }
    var date by rememberSaveable { mutableStateOf(initialDay) }
    var multiDay by rememberSaveable { mutableStateOf(false) }
    var endDate by rememberSaveable { mutableStateOf(initialDay) }
    var allDay by rememberSaveable { mutableStateOf(false) }
    var start by rememberSaveable { mutableIntStateOf(16 * 60) }
    var end by rememberSaveable { mutableIntStateOf(17 * 60 + 30) }
    var repeatDays by rememberSaveable { mutableIntStateOf(0) }
    var everyWeeks by rememberSaveable { mutableIntStateOf(1) }
    val dayRules = remember { androidx.compose.runtime.mutableStateMapOf<Int, pl.sportfamily.app.data.EventDayRule>() }
    var editingDay by remember { mutableStateOf<Int?>(null) }
    var repeatUntil by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var place by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var travel by rememberSaveable { mutableStateOf("") }
    var buffer by rememberSaveable { mutableStateOf("10") }
    var dropOff by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickUp by rememberSaveable { mutableStateOf<Long?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    var alsoTake by rememberSaveable { mutableStateOf("") }
    var coachName by rememberSaveable { mutableStateOf("") }
    var coachPhone by rememberSaveable { mutableStateOf("") }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // Wybór trenera z kontaktów: Android oddaje tylko wybrany wpis, bez dostępu do całej książki.
    val pickCoach = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val uri = res.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) { coachName = c.getString(0).orEmpty(); coachPhone = c.getString(1).orEmpty() }
            }
        }
    }
    var confirmDiscard by remember { mutableStateOf(false) }
    // Nowe wydarzenie z wpisanym tytułem: przy wyjściu pytamy, żeby nie stracić danych przez przypadek.
    val tryClose = { if (eventId == null && title.isNotBlank()) confirmDiscard = true else onClose() }
    androidx.activity.compose.BackHandler(onBack = tryClose)
    var importTag by rememberSaveable { mutableStateOf<String?>(null) }
    val checklist = remember { mutableStateListOf<ChecklistItem>() }
    var newItem by rememberSaveable { mutableStateOf("") }

    var picker by remember { mutableStateOf<String?>(null) } // "date", "end", "until", "start", "endTime"
    var showErrors by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        if (eventId != null && !loaded) {
            load(eventId)?.let { src ->
                val e = src.event
                title = e.title; category = e.category; schoolKind = e.schoolKind ?: SchoolKind.TEST
                childIds = src.childIds.toSet()
                date = LocalDate.ofEpochDay(e.startDay); endDate = LocalDate.ofEpochDay(e.endDay); multiDay = e.endDay > e.startDay
                allDay = e.allDay; start = e.startMinute; end = e.endMinute
                repeatDays = e.repeatDays; everyWeeks = e.repeatEveryWeeks; repeatUntil = e.repeatUntilDay?.let { LocalDate.ofEpochDay(it) }
                place = e.placeName; address = e.address
                travel = if (e.travelMinutes > 0) e.travelMinutes.toString() else ""; buffer = e.bufferMinutes.toString()
                dropOff = e.dropOffPersonId; pickUp = e.pickUpPersonId; notes = e.notes; alsoTake = e.alsoTake; coachName = e.coachName; coachPhone = e.coachPhone; importTag = e.importTag
                checklist.clear(); checklist.addAll(src.checklist)
                dayRules.clear(); src.dayRules.forEach { dayRules[it.weekday] = it }
            }
            loaded = true
        }
    }

    val timeError = !allDay && !multiDay && end <= start
    val titleError = title.isBlank()

    fun save() {
        if (titleError || timeError) { showErrors = true; return }
        val ev = EventEntity(
            id = eventId ?: 0,
            title = title.trim(),
            category = category,
            schoolKind = if (category == Category.SCHOOL) schoolKind else null,
            startDay = date.toEpochDay(),
            endDay = if (multiDay && repeatDays == 0) maxOf(endDate, date).toEpochDay() else date.toEpochDay(),
            allDay = allDay,
            startMinute = start,
            endMinute = end,
            repeatDays = repeatDays,
            repeatEveryWeeks = if (repeatDays != 0) everyWeeks else 1,
            repeatUntilDay = if (repeatDays != 0) repeatUntil?.toEpochDay() else null,
            placeName = place.trim(),
            address = address.trim(),
            travelMinutes = travel.toIntOrNull()?.coerceIn(0, 600) ?: 0,
            bufferMinutes = buffer.toIntOrNull()?.coerceIn(0, 120) ?: 0,
            dropOffPersonId = dropOff,
            pickUpPersonId = pickUp,
            notes = notes.trim(),
            alsoTake = alsoTake.trim(),
            coachName = if (category == Category.SPORT) coachName.trim() else "",
            coachPhone = if (category == Category.SPORT) coachPhone.trim() else "",
            importTag = importTag,
        )
        val rules = if (repeatDays == 0) emptyList()
            else dayRules.values.filter { repeatDays and (1 shl (it.weekday - 1)) != 0 }
        onSave(ev, childIds.toList(), checklist.toList(), rules)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == null) "Nowe wydarzenie" else "Edycja") },
                navigationIcon = { IconButton(onClick = tryClose) { SfIcon(AppIcons.zamknij, "Zamknij") } },
                actions = { TextButton(onClick = ::save, modifier = Modifier.padding(end = 8.dp)) { Text("Zapisz") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        if (!loaded) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 40.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Nazwa") }, placeholder = { Text("np. Trening tenisa") },
                isError = showErrors && titleError,
                supportingText = if (showErrors && titleError) ({ Text("Podaj nazwę wydarzenia") }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Label("Rodzaj")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = {
                            category = c
                            if (c == Category.SCHOOL && travel.isBlank()) { /* szkoła zwykle bez dojazdu */ }
                        },
                        label = { Text(categoryLabel(c)) },
                        leadingIcon = { SfIcon(categoryIcon(c), null, tint = sf.of(c).main, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = sf.of(c).container, selectedLabelColor = sf.of(c).onContainer),
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
            }
            if (category == Category.SCHOOL) {
                FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SchoolKind.entries.forEach { k ->
                        FilterChip(selected = schoolKind == k, onClick = { schoolKind = k }, label = { Text(schoolKindLabel(k)) }, modifier = Modifier.heightIn(min = 40.dp))
                    }
                }
            }

            if (data.children.isNotEmpty()) {
                Label("Kogo dotyczy")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.children.forEach { c ->
                        val on = c.id in childIds
                        FilterChip(
                            selected = on,
                            onClick = { childIds = if (on) childIds - c.id else childIds + c.id },
                            label = { Text(c.name) },
                            leadingIcon = { KidDot(c, 20.dp) },
                            modifier = Modifier.heightIn(min = 40.dp),
                        )
                    }
                }
            }

            SectionHeader("Kiedy")
            SfCard(Modifier.fillMaxWidth()) {
                PickRow(AppIcons.data, if (repeatDays != 0) "Od" else "Dzień", date.longLabel()) { picker = "date" }
                SwitchRow("Cały dzień", allDay) { allDay = it }
                if (repeatDays == 0) {
                    SwitchRow("Kilka dni (turniej, obóz)", multiDay) { multiDay = it; if (it && endDate <= date) endDate = date.plusDays(1) }
                    if (multiDay) PickRow(AppIcons.data, "Do", endDate.longLabel()) { picker = "end" }
                }
                if (!allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedButton(onClick = { picker = "start" }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                            SfIcon(AppIcons.wyjazd, null, Modifier.size(18.dp)); Text("  Od ${minutesLabel(start)}")
                        }
                        OutlinedButton(onClick = { picker = "endTime" }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                            Text("Do ${minutesLabel(end)}")
                        }
                    }
                    if (showErrors && timeError) Text("Koniec musi być po początku", color = sf.error.main, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (!multiDay) {
                SectionHeader("Powtarzanie")
                Text(
                    if (repeatDays == 0) "Zaznacz dni, w które są zajęcia, np. poniedziałek, wtorek i piątek. Bez zaznaczenia wydarzenie jest jednorazowe."
                    else "Zajęcia powtarzają się w zaznaczone dni.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    WeekdayShort.forEachIndexed { i, d ->
                        val bit = 1 shl i
                        val on = repeatDays and bit != 0
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(shape)
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
                                .toggleable(value = on, role = Role.Checkbox, onValueChange = {
                                    repeatDays = repeatDays xor bit
                                    if (!on) multiDay = false
                                }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(d, style = MaterialTheme.typography.labelLarge,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (repeatDays != 0) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                        listOf(1 to "Co tydzień", 2 to "Co 2 tygodnie", 3 to "Co 3 tygodnie", 4 to "Co 4 tygodnie").forEach { (n, label) ->
                            FilterChip(selected = everyWeeks == n, onClick = { everyWeeks = n }, label = { Text(label) }, modifier = Modifier.heightIn(min = 40.dp))
                        }
                        FilterChip(selected = false, onClick = { repeatDays = 0; dayRules.clear() }, label = { Text("Jednorazowo") }, modifier = Modifier.heightIn(min = 40.dp))
                    }
                    PickRow(AppIcons.data, "Do kiedy", repeatUntil?.longLabel() ?: "bez końca") { picker = "until" }

                    Label("Godziny i osoby w poszczególne dni")
                    SfCard(Modifier.fillMaxWidth(), padding = PaddingValues(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
                        (1..7).filter { repeatDays and (1 shl (it - 1)) != 0 }.forEach { wd ->
                            DayRuleRow(wd, dayRules[wd], start, end, dropOff, pickUp, data.persons, allDay) { editingDay = wd }
                        }
                    }
                    Text(
                        "Domyślnie każdy dzień ma godziny z sekcji Kiedy i osoby z sekcji Kto wiezie. Stuknij Zmień, jeśli w któryś dzień jest inaczej.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            SectionHeader("Miejsce i dojazd")
            val knownPlaces = remember(data.events) {
                data.events.filter { it.placeName.isNotBlank() }.distinctBy { it.placeName }.take(6)
            }
            if (knownPlaces.isNotEmpty() && place.isBlank()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    knownPlaces.forEach { p ->
                        SuggestionChip(onClick = {
                            place = p.placeName; address = p.address
                            if (p.travelMinutes > 0) travel = p.travelMinutes.toString()
                            buffer = p.bufferMinutes.toString()
                        }, label = { Text(p.placeName) })
                    }
                }
            }
            OutlinedTextField(place, { place = it }, label = { Text("Miejsce") }, placeholder = { Text("np. Korty Orlik") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it }, label = { Text("Adres (do nawigacji)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(travel, { travel = it.filter(Char::isDigit).take(3) }, label = { Text("Dojazd, min") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(buffer, { buffer = it.filter(Char::isDigit).take(3) }, label = { Text("Zapas, min") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            val t = travel.toIntOrNull() ?: 0
            if (t > 0 && !allDay) {
                Text(
                    "Wyjazd o ${minutesLabel((start - t - (buffer.toIntOrNull() ?: 0)).coerceAtLeast(0))}",
                    style = MaterialTheme.typography.labelLarge, color = sf.important.onContainer, modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }

            if (data.persons.isNotEmpty()) {
                SectionHeader(if (repeatDays != 0) "Kto wiezie (domyślnie)" else "Kto wiezie")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersonDropdown("Zawozi", dropOff, data, Modifier.weight(1f)) { dropOff = it }
                    PersonDropdown("Odbiera", pickUp, data, Modifier.weight(1f)) { pickUp = it }
                }
            }

            SectionHeader("Lista rzeczy")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChecklistTemplates.forEach { (name, items) ->
                    SuggestionChip(onClick = {
                        val existing = checklist.map { it.text.lowercase() }.toSet()
                        items.filter { it.lowercase() !in existing }.forEach { checklist += ChecklistItem(eventId = eventId ?: 0, text = it) }
                    }, label = { Text("+ $name") })
                }
            }
            checklist.forEachIndexed { i, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    IconButton(onClick = { checklist.removeAt(i) }) { SfIcon(AppIcons.zamknij, "Usuń ${item.text}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            fun addItem() {
                val txt = newItem.trim()
                if (txt.isNotEmpty()) { checklist += ChecklistItem(eventId = eventId ?: 0, text = txt); newItem = "" }
            }
            OutlinedTextField(
                newItem, { newItem = it }, label = { Text("Dodaj rzecz") }, singleLine = true,
                trailingIcon = { IconButton(onClick = ::addItem) { SfIcon(AppIcons.dodaj, "Dodaj") } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addItem() }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (category == Category.SPORT) {
                SectionHeader("Trener")
                OutlinedTextField(coachName, { coachName = it }, label = { Text("Imię i nazwisko trenera") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(coachPhone, { coachPhone = it }, label = { Text("Telefon do trenera") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = { pickCoach.launch(android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) },
                    modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
                ) { SfIcon(AppIcons.kontakty, null); Text("  Wybierz z kontaktów") }
            }
            SectionHeader("Zabierz też")
            OutlinedTextField(alsoTake, { alsoTake = it }, placeholder = { Text("np. Kuba z klasy, odebrać ze szkoły") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            SectionHeader("Notatki")
            OutlinedTextField(notes, { notes = it }, placeholder = { Text("np. zakres materiału, opłaty, kontakt do trenera") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Button(onClick = ::save, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Zapisz")
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Odrzucić wpisane dane?") },
            text = { Text("To wydarzenie nie zostało zapisane.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onClose() }) { Text("Odrzuć") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Wróć do edycji") } },
        )
    }

    editingDay?.let { wd ->
        DayRuleDialog(
            wd, dayRules[wd], start, end, dropOff, pickUp, data.persons, allDay,
            onDismiss = { editingDay = null },
            onSave = { r -> if (r == null) dayRules.remove(wd) else dayRules[wd] = r; editingDay = null },
        )
    }

    when (picker) {
        "date", "end", "until" -> {
            val current = when (picker) { "date" -> date; "end" -> endDate; else -> repeatUntil ?: date.plusMonths(6) }
            val state = rememberDatePickerState(initialSelectedDateMillis = current.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            DatePickerDialog(
                onDismissRequest = { picker = null },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { ms ->
                            val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                            when (picker) {
                                "date" -> { date = d; if (endDate < d) endDate = d.plusDays(1) }
                                "end" -> endDate = d
                                else -> repeatUntil = d
                            }
                        }
                        picker = null
                    }) { Text("OK") }
                },
                dismissButton = {
                    if (picker == "until") TextButton(onClick = { repeatUntil = null; picker = null }) { Text("Bez końca") }
                    else TextButton(onClick = { picker = null }) { Text("Anuluj") }
                },
            ) { DatePicker(state) }
        }
        "start", "endTime" -> {
            val m = if (picker == "start") start else end
            val state = rememberTimePickerState(initialHour = m / 60, initialMinute = m % 60, is24Hour = true)
            AlertDialog(
                onDismissRequest = { picker = null },
                title = { Text(if (picker == "start") "Początek" else "Koniec") },
                text = { TimePicker(state) },
                confirmButton = {
                    TextButton(onClick = {
                        val v = state.hour * 60 + state.minute
                        if (picker == "start") {
                            val len = (end - start).coerceAtLeast(30)
                            start = v; end = (v + len).coerceAtMost(23 * 60 + 59)
                        } else end = v
                        picker = null
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { picker = null }) { Text("Anuluj") } },
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
}

@Composable
private fun PickRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        SfIcon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonDropdown(label: String, selected: Long?, data: PlannerData, modifier: Modifier, onSelect: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = data.persons.firstOrNull { it.id == selected }?.name ?: "nikt",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("nikt") }, onClick = { onSelect(null); open = false })
            data.persons.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { onSelect(p.id); open = false }) }
        }
    }
}
