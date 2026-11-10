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

data class ParsedPlan(
    val title: String,
    val sections: List<PlanSection>,
    val warnings: List<String>,
    val schoolYearEnd: LocalDate?,
)

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
        return out
    }

    fun hm(m: Int) = "%d:%02d".format(m / 60, m % 60)

    fun dayName(d: DayOfWeek) = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")[d.value - 1]
}
