package pl.sportfamily.app.domain

import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.SchoolKind
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Jedna pozycja z planu: dzień tygodnia, godziny i nazwa. */
data class PlanItem(val day: DayOfWeek, val startMinute: Int, val endMinute: Int, val title: String)

/** Sekcja planu, np. "Lekcje" albo "Treningi". [rowTimes] = godziny w osobnej kolumnie (typowe dla lekcji). */
data class PlanSection(val label: String, val category: Category, val items: List<PlanItem>, val rowTimes: Boolean)

/** Rodzaj wpisu z listy dat (np. kalendarium szkoły). */
enum class DatedKind(val label: String) {
    EVENT("Wydarzenie"), DAY_OFF("Dzień wolny"), GRADES("Oceny i egzaminy"), MEETING("Zebranie"), FAMILY("Rodzina")
}

/** Wpis z konkretną datą: wydarzenie albo dni wolne. Godziny opcjonalne. */
data class DatedItem(
    val from: LocalDate, val to: LocalDate,
    val startMinute: Int?, val endMinute: Int?,
    val title: String, val kind: DatedKind,
)

data class ParsedPlan(
    val title: String,
    val sections: List<PlanSection>,
    val warnings: List<String>,
    val schoolYearEnd: LocalDate?,
    val dated: List<DatedItem> = emptyList(),
) {
    val isEmpty: Boolean get() = sections.isEmpty() && dated.isEmpty()
}

/** Ustawienia importu wybrane w podglądzie. */
data class ImportOptions(
    val childId: Long,
    val sectionCategories: List<Category>,
    val lessonsAsDailyBlock: Boolean = true,
    val from: LocalDate,
    val until: LocalDate?,
)

/**
 * Czyta plan w układzie siatki: dni tygodnia w kolumnach, w wierszach lekcje lub zajęcia.
 * Godzina może być w pierwszej kolumnie ("8:15 - 9:00") albo w samej komórce ("Tenis\n14:30 - 16:00").
 */
object PlanImport {
    const val IMPORT_TAG = "xlsx-plan"
    const val EVENTS_TAG = "xlsx-events"
    private val timeRange = Regex("""(\d{1,2})[:.](\d{2})\s*[-–—]+\s*(\d{1,2})[:.](\d{2})""")
    private val schoolYear = Regex("""(20\d{2})\s*/\s*(20\d{2})""")

    private fun norm(s: String) = Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}"), "").replace("ł", "l")

    fun dayOf(cell: String): DayOfWeek? {
        val n = norm(cell).trimEnd('.')
        if (n.isEmpty() || n.length > 14) return null
        return when {
            n.startsWith("pon") || n == "pn" -> DayOfWeek.MONDAY
            n.startsWith("wt") -> DayOfWeek.TUESDAY
            n.startsWith("sr") -> DayOfWeek.WEDNESDAY
            n.startsWith("czw") || n == "cz" -> DayOfWeek.THURSDAY
            n.startsWith("pi") || n == "pt" -> DayOfWeek.FRIDAY
            n.startsWith("sob") || n == "so" -> DayOfWeek.SATURDAY
            n.startsWith("niedz") || n == "nd" || n == "nie" || n == "ndz" -> DayOfWeek.SUNDAY
            n.startsWith("mon") -> DayOfWeek.MONDAY
            n.startsWith("tue") -> DayOfWeek.TUESDAY
            n.startsWith("wed") -> DayOfWeek.WEDNESDAY
            n.startsWith("thu") -> DayOfWeek.THURSDAY
            n.startsWith("fri") -> DayOfWeek.FRIDAY
            n.startsWith("sat") -> DayOfWeek.SATURDAY
            n.startsWith("sun") -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun range(text: String): Pair<Int, Int>? {
        val m = timeRange.find(text) ?: return null
        val (h1, m1, h2, m2) = m.destructured
        val s = h1.toInt() * 60 + m1.toInt()
        val e = h2.toInt() * 60 + m2.toInt()
        return if (s in 0 until 24 * 60 && e in 1..24 * 60 && e > s) s to e else null
    }

    private fun guessCategory(label: String, rowTimes: Boolean, titles: List<String>): Category {
        val l = norm(label)
        val t = titles.joinToString(" ") { norm(it) }
        return when {
            "trening" in l || "sport" in l -> Category.SPORT
            "zajec" in l || "dodatk" in l || "kolko" in l -> Category.EXTRA
            "lekc" in l || "szkol" in l || "godzin" in l || rowTimes -> Category.SCHOOL
            listOf("tenis", "pilk", "basen", "plyw", "trening").any { it in t } -> Category.SPORT
            else -> Category.EXTRA
        }
    }

    fun parse(rows: List<List<String>>): ParsedPlan {
        val grid = parseGrid(rows)
        val list = parseList(rows, grid.schoolYearEnd)
        val firstText = rows.firstOrNull { r -> r.any { it.isNotBlank() } }?.firstOrNull { it.isNotBlank() }
        return if (list == null) grid else grid.copy(
            title = if (grid.sections.isEmpty()) firstText ?: grid.title else grid.title,
            dated = list.first,
            warnings = (if (grid.sections.isEmpty()) emptyList() else grid.warnings) + list.second,
        )
    }

    private fun parseGrid(rows: List<List<String>>): ParsedPlan {
        val warnings = ArrayList<String>()
        // Wiersz nagłówka: co najmniej 3 komórki z nazwą dnia.
        val headers = rows.indices.filter { r -> rows[r].count { dayOf(it) != null } >= 3 }
        val title = rows.firstOrNull { row -> row.any { it.isNotBlank() } && rows.indexOf(row) < (headers.firstOrNull() ?: 0) }
            ?.firstOrNull { it.isNotBlank() } ?: "Plan"
        val yearEnd = rows.asSequence().flatten().mapNotNull { schoolYear.find(it) }.firstOrNull()
            ?.let { lastFridayOfJune(it.groupValues[2].toInt()) }

        if (headers.isEmpty()) {
            return ParsedPlan(title, emptyList(), listOf("Nie znalazłem wiersza z nazwami dni tygodnia (Poniedziałek, Wtorek...)."), yearEnd)
        }
        val sections = headers.mapIndexed { idx, h ->
            val header = rows[h]
            val dayCols = header.indices.mapNotNull { c -> dayOf(header[c])?.let { c to it } }.toMap()
            val label = header.indices.filter { it !in dayCols }.map { header[it] }.firstOrNull { it.isNotBlank() } ?: "Plan"
            val end = headers.getOrNull(idx + 1) ?: rows.size
            val items = ArrayList<PlanItem>()
            var usedRowTimes = false
            for (r in h + 1 until end) {
                val row = rows[r]
                val rowTime = row.indices.filter { it !in dayCols }.firstNotNullOfOrNull { range(row[it]) }
                for ((c, day) in dayCols) {
                    val cell = row.getOrNull(c)?.trim().orEmpty()
                    if (cell.isEmpty()) continue
                    val cellTime = range(cell)
                    val time = cellTime ?: rowTime
                    val name = cell.replace(timeRange, " ").lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
                    if (time == null) {
                        warnings += "Pominięto \"${name.ifEmpty { cell }}\" (${dayName(day)}): brak godziny."
                        continue
                    }
                    if (cellTime == null) usedRowTimes = true
                    items += PlanItem(day, time.first, time.second, name.ifEmpty { label })
                }
            }
            val cat = guessCategory(label, usedRowTimes, items.map { it.title })
            // Nagłówek kolumny godzin ("Godzina", "Godz.") to nie nazwa sekcji.
            val shown = if (norm(label).startsWith("godz") || norm(label) == "plan") (if (cat == Category.SCHOOL) "Lekcje" else "Zajęcia") else label
            PlanSection(shown, cat, items.sortedWith(compareBy({ it.day }, { it.startMinute })), usedRowTimes)
        }.filter { it.items.isNotEmpty() }
        if (sections.isEmpty()) warnings += "Nie znalazłem żadnych zajęć z godzinami."
        return ParsedPlan(title, sections, warnings, yearEnd)
    }

    // ---------- Lista dat (kalendarium) ----------

    private val dateFull = Regex("""^(\d{1,2})[.\-/](\d{1,2})[.\-/](\d{4})$""")
    private val dateIso = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""")
    private val dateShort = Regex("""^(\d{1,2})[.\-/](\d{1,2})\.?$""")
    private val singleTime = Regex("""(\d{1,2})[:.](\d{2})""")

    /** Data z komórki: 24.06.2027, 2027-06-24, 24.06 (rok z roku szkolnego) albo liczba dni Excela. */
    fun parseDate(cell: String, schoolYearEnd: LocalDate?): LocalDate? {
        val t = cell.trim()
        return runCatching {
            dateFull.find(t)?.let { val (d, m, y) = it.destructured; return@runCatching LocalDate.of(y.toInt(), m.toInt(), d.toInt()) }
            dateIso.find(t)?.let { val (y, m, d) = it.destructured; return@runCatching LocalDate.of(y.toInt(), m.toInt(), d.toInt()) }
            dateShort.find(t)?.let {
                val (d, m) = it.destructured
                val endYear = schoolYearEnd?.year ?: return@runCatching null
                val month = m.toInt()
                return@runCatching LocalDate.of(if (month >= 9) endYear - 1 else endYear, month, d.toInt())
            }
            t.toIntOrNull()?.takeIf { it in 20000..80000 }?.let { LocalDate.of(1899, 12, 30).plusDays(it.toLong()) }
        }.getOrNull()
    }

    fun kindOf(rodzaj: String, title: String): DatedKind {
        val r = norm(rodzaj)
        val t = norm(title)
        return when {
            "woln" in r || "ferie" in r || "przerw" in r -> DatedKind.DAY_OFF
            "zebran" in r || "konsult" in r -> DatedKind.MEETING
            "ocen" in r || "egzam" in r -> DatedKind.GRADES
            "rodzin" in r -> DatedKind.FAMILY
            r.isNotEmpty() -> DatedKind.EVENT
            "woln" in t || "ferie" in t || "przerwa" in t || "majowk" in t -> DatedKind.DAY_OFF
            "zebran" in t || "konsultac" in t || "wywiadow" in t -> DatedKind.MEETING
            "ocen" in t || "egzamin" in t -> DatedKind.GRADES
            "rodzin" in t || "piknik" in t -> DatedKind.FAMILY
            else -> DatedKind.EVENT
        }
    }

    /** Tabela z nagłówkiem "Od"/"Data" i "Wydarzenie"/"Nazwa". Null, gdy w arkuszu jej nie ma. */
    private fun parseList(rows: List<List<String>>, schoolYearEnd: LocalDate?): Pair<List<DatedItem>, List<String>>? {
        fun isFrom(s: String) = s == "od" || s == "data" || s == "data od" || s == "dzien"
        fun isTitle(s: String) = s == "wydarzenie" || s == "nazwa" || s == "co"
        val h = rows.indexOfFirst { row -> val n = row.map { norm(it) }; n.any(::isFrom) && n.any(::isTitle) }
        if (h < 0) return null
        val head = rows[h].map { norm(it) }
        val cFrom = head.indexOfFirst(::isFrom)
        val cTo = head.indexOfFirst { it == "do" || it == "data do" }
        val cTime = head.indexOfFirst { it.startsWith("godz") }
        val cTitle = head.indexOfFirst(::isTitle)
        val cKind = head.indexOfFirst { it == "rodzaj" || it == "typ" || it == "kategoria" }
        val items = ArrayList<DatedItem>()
        val warnings = ArrayList<String>()
        for (r in h + 1 until rows.size) {
            val row = rows[r]
            fun cell(c: Int) = if (c >= 0) row.getOrNull(c)?.trim().orEmpty() else ""
            val title = cell(cTitle).lines().joinToString(" ") { it.trim() }.trim()
            // Wiersz bez nazwy wydarzenia (np. przypis pod tabelą) pomijamy bez ostrzeżenia.
            if (title.isEmpty()) continue
            val from = parseDate(cell(cFrom), schoolYearEnd)
            if (from == null) {
                warnings += "Pominięto \"$title\": brak daty."
                continue
            }
            val to = parseDate(cell(cTo), schoolYearEnd)?.takeIf { !it.isBefore(from) } ?: from
            val timeText = cell(cTime)
            val tr = range(timeText)
            val start = tr?.first ?: singleTime.find(timeText)?.let { it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt() }
            items += DatedItem(from, to, start, tr?.second, title, kindOf(cell(cKind), title))
        }
        return items to warnings
    }

    /** Dni wolne z listy dat (każdy dzień zakresu osobno), od [from] wzwyż. */
    fun daysOff(plan: ParsedPlan, from: LocalDate): List<CustomDayOff> =
        plan.dated.filter { it.kind == DatedKind.DAY_OFF && !it.to.isBefore(from) }.flatMap { d ->
            (0..(d.to.toEpochDay() - d.from.toEpochDay())).map { d.from.plusDays(it) }
                .filter { !it.isBefore(from) && it.dayOfWeek.value <= 5 }.map { CustomDayOff(it, d.title) }
        }

    /** Polski rok szkolny kończy się w ostatni piątek czerwca. */
    fun lastFridayOfJune(year: Int): LocalDate =
        LocalDate.of(year, 6, 30).with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))

    data class NewEvent(val event: EventEntity, val childIds: List<Long>)

    /** Zamienia plan na wydarzenia powtarzalne. Lekcje jednego dnia mogą stać się jednym blokiem "Szkoła". */
    fun toEvents(plan: ParsedPlan, o: ImportOptions): List<NewEvent> {
        val out = ArrayList<NewEvent>()
        plan.sections.forEachIndexed { i, sec ->
            val cat = o.sectionCategories.getOrElse(i) { sec.category }
            fun base(title: String, s: Int, e: Int, days: Int, notes: String = "", kind: SchoolKind? = null) = EventEntity(
                title = title, category = cat, schoolKind = if (cat == Category.SCHOOL) (kind ?: SchoolKind.LESSONS) else null,
                startDay = o.from.toEpochDay(), startMinute = s, endMinute = e,
                repeatDays = days, repeatUntilDay = o.until?.toEpochDay(), notes = notes, importTag = IMPORT_TAG,
            )
            if (cat == Category.SCHOOL && o.lessonsAsDailyBlock) {
                sec.items.groupBy { it.day }.toSortedMap().forEach { (day, lessons) ->
                    val sorted = lessons.sortedBy { it.startMinute }
                    val notes = sorted.mapIndexed { n, l -> "${n + 1}. ${hm(l.startMinute)}-${hm(l.endMinute)} ${l.title}" }.joinToString("\n")
                    out += NewEvent(base("Szkoła", sorted.first().startMinute, sorted.maxOf { it.endMinute }, Planner.dayBit(day), notes), listOf(o.childId))
                }
            } else {
                // Te same zajęcia o tych samych godzinach w kilka dni = jedna seria.
                sec.items.groupBy { Triple(it.title, it.startMinute, it.endMinute) }.forEach { (k, list) ->
                    val days = list.map { Planner.dayBit(it.day) }.distinct().sum()
                    out += NewEvent(base(k.first, k.second, k.third, days), listOf(o.childId))
                }
            }
        }
        plan.dated.filter { it.kind != DatedKind.DAY_OFF && !it.to.isBefore(o.from) }.forEach { d ->
            val (cat, kind) = when (d.kind) {
                DatedKind.MEETING -> Category.IMPORTANT to null
                DatedKind.FAMILY -> Category.FAMILY to null
                DatedKind.GRADES -> Category.SCHOOL to (if ("egzamin" in norm(d.title) || "sprawdzian" in norm(d.title)) SchoolKind.TEST else SchoolKind.SCHOOL_EVENT)
                else -> Category.SCHOOL to SchoolKind.SCHOOL_EVENT
            }
            val timed = d.startMinute != null
            out += NewEvent(
                EventEntity(
                    title = d.title, category = cat, schoolKind = kind,
                    startDay = d.from.toEpochDay(), endDay = d.to.toEpochDay(),
                    allDay = !timed,
                    startMinute = d.startMinute ?: 0,
                    endMinute = d.endMinute ?: ((d.startMinute ?: 0) + 90).coerceAtMost(23 * 60 + 59),
                    importTag = EVENTS_TAG,
                ),
                listOf(o.childId),
            )
        }
        return out
    }

    fun hm(m: Int) = "%d:%02d".format(m / 60, m % 60)

    fun dayName(d: DayOfWeek) = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")[d.value - 1]
}
