package pl.sportfamily.app.domain

import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.SchoolKind
import pl.sportfamily.app.data.xlsx.SheetOut
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Eksport do Excela: plan tygodnia każdego dziecka (układ siatki, zgodny z importem) i lista najbliższych dni. */
object PlanExport {
    private val PL = Locale.forLanguageTag("pl-PL")
    private val dayNames = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")
    private val lessonLine = Regex("""^\s*\d+\.\s*(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})\s+(.+)$""")

    private fun hm(m: Int) = "%d:%02d".format(m / 60, m % 60)
    private fun range(s: Int, e: Int) = "${hm(s)} - ${hm(e)}"

    fun sheets(data: PlannerData, today: LocalDate, days: Int = 7): List<SheetOut> =
        data.children.map { childSheet(data, it, today) } + listOf(listSheet(data, today, days))

    /** Plan tygodnia dziecka: sekcja lekcji (godziny w pierwszej kolumnie) i sekcja zajęć (godziny w komórkach). */
    fun childSheet(data: PlannerData, child: Child, today: LocalDate): SheetOut {
        val ids = data.eventChildren.filter { it.childId == child.id }.map { it.eventId }.toSet()
        val weekly = data.events.filter {
            it.id in ids && it.repeatDays != 0 && (it.repeatUntilDay == null || it.repeatUntilDay >= today.toEpochDay())
        }
        val showWeekend = weekly.any { it.repeatDays and (Planner.dayBit(DayOfWeek.SATURDAY) or Planner.dayBit(DayOfWeek.SUNDAY)) != 0 }
        val dayList = DayOfWeek.entries.filter { showWeekend || it.value <= 5 }
        fun on(e: pl.sportfamily.app.data.EventEntity, d: DayOfWeek) = e.repeatDays and Planner.dayBit(d) != 0

        val rows = ArrayList<List<String>>()
        val headers = HashSet<Int>()
        rows += listOf("Plan: ${child.name}"); rows += listOf(listOfNotNull(child.note.takeIf { it.isNotBlank() }, "SportFamily Kalendarz").joinToString(", "))
        rows += listOf("")

        // Lekcje: z bloków "Szkoła" z listą przedmiotów w notatce albo z pojedynczych lekcji.
        val lessons = HashMap<Pair<Int, Int>, HashMap<DayOfWeek, String>>()
        val schoolEvents = weekly.filter { it.category == Category.SCHOOL && (it.schoolKind == null || it.schoolKind == SchoolKind.LESSONS) }
        schoolEvents.forEach { e ->
            val parsed = e.notes.lines().mapNotNull { lessonLine.find(it)?.destructured }
            dayList.filter { on(e, it) }.forEach { d ->
                if (parsed.isNotEmpty()) parsed.forEach { (h1, m1, h2, m2, name) ->
                    lessons.getOrPut((h1.toInt() * 60 + m1.toInt()) to (h2.toInt() * 60 + m2.toInt())) { HashMap() }[d] = name.trim()
                } else lessons.getOrPut(e.startMinute to e.endMinute) { HashMap() }[d] = e.title
            }
        }
        if (lessons.isNotEmpty()) {
            headers += rows.size
            rows += listOf("Godzina") + dayList.map { dayNames[it.value - 1] }
            lessons.keys.sortedBy { it.first }.forEach { slot ->
                rows += listOf(range(slot.first, slot.second)) + dayList.map { lessons[slot]?.get(it).orEmpty() }
            }
            rows += listOf("")
        }

        // Pozostałe zajęcia: w komórce nazwa i godziny, kolejne wiersze dla kolejnych zajęć tego dnia.
        val others = weekly.filter { it !in schoolEvents }
        if (others.isNotEmpty()) {
            val perDay = dayList.associateWith { d -> others.filter { on(it, d) }.sortedBy { it.startMinute } }
            val n = perDay.values.maxOf { it.size }
            headers += rows.size
            rows += listOf("Zajęcia") + dayList.map { dayNames[it.value - 1] }
            for (i in 0 until n) {
                rows += listOf("Zajęcia ${i + 1}") + dayList.map { d ->
                    perDay[d]?.getOrNull(i)?.let { "${it.title}\n${range(it.startMinute, it.endMinute)}" }.orEmpty()
                }
            }
        }
        if (lessons.isEmpty() && others.isEmpty()) rows += listOf("Brak stałych zajęć w planie.")
        return SheetOut(
            name = child.name, rows = rows, headerRows = headers, titleRows = setOf(0),
            columnWidths = listOf(14.0) + dayList.map { 22.0 },
        )
    }

    /** Plan dnia na najbliższe dni: każde wydarzenie w osobnym wierszu. */
    fun listSheet(data: PlannerData, today: LocalDate, days: Int): SheetOut {
        val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", PL)
        val occ = Planner.occurrences(data, today, today.plusDays(days - 1L))
        val rows = ArrayList<List<String>>()
        rows += listOf("Plan dnia: ${today.format(dateFmt)} do ${today.plusDays(days - 1L).format(dateFmt)}")
        rows += listOf("")
        rows += listOf("Data", "Dzień", "Od", "Do", "Dziecko", "Wydarzenie", "Rodzaj", "Miejsce", "Wyjazd", "Zawozi", "Odbiera")
        occ.forEach { o ->
            val sameDay = o.start.toLocalDate() == o.end.toLocalDate()
            rows += listOf(
                o.date.format(dateFmt),
                dayNames[o.date.dayOfWeek.value - 1],
                if (o.allDay) "cały dzień" else hm(o.start.hour * 60 + o.start.minute),
                if (o.allDay) "" else if (sameDay) hm(o.end.hour * 60 + o.end.minute) else o.end.format(DateTimeFormatter.ofPattern("dd.MM HH:mm")),
                o.children.joinToString(", ") { it.name },
                o.event.title,
                when (o.event.category) {
                    Category.SPORT -> "Sport"; Category.SCHOOL -> "Szkoła"; Category.EXTRA -> "Zajęcia"
                    Category.FAMILY -> "Rodzina"; Category.IMPORTANT -> "Ważne"
                },
                listOf(o.event.placeName, o.event.address).filter { it.isNotBlank() }.joinToString(", "),
                o.departure?.let { hm(it.hour * 60 + it.minute) }.orEmpty(),
                o.dropOff?.name.orEmpty(),
                o.pickUp?.name.orEmpty(),
            )
        }
        if (occ.isEmpty()) rows += listOf("Brak wydarzeń w tym okresie.")
        return SheetOut(
            name = "Najbliższe $days dni", rows = rows, headerRows = setOf(2), titleRows = setOf(0),
            columnWidths = listOf(12.0, 13.0, 9.0, 11.0, 14.0, 26.0, 10.0, 28.0, 9.0, 10.0, 10.0),
        )
    }
}
