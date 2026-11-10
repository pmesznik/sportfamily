package pl.sportfamily.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.SchoolKind
import pl.sportfamily.app.data.xlsx.Xlsx
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

class SchoolCalendarTest {

    @Test fun easterDates() {
        assertEquals(LocalDate.of(2026, 4, 5), SchoolCalendar.easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), SchoolCalendar.easter(2027))
        assertEquals(LocalDate.of(2025, 4, 20), SchoolCalendar.easter(2025))
    }

    @Test fun holidaysAndBreaks2026_27() {
        val off = SchoolCalendar.daysOff(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), Voivodeship.SLASKIE)
        assertEquals("Święto Niepodległości", off[LocalDate.of(2026, 11, 11)])
        assertEquals("Wigilia", off[LocalDate.of(2026, 12, 24)])
        assertEquals("Zimowa przerwa świąteczna", off[LocalDate.of(2026, 12, 30)])
        assertEquals("Ferie zimowe", off[LocalDate.of(2027, 1, 18)])
        assertEquals("Ferie zimowe", off[LocalDate.of(2027, 1, 29)])
        assertFalse(LocalDate.of(2027, 2, 1) in off)
        // Przerwa wiosenna: czwartek przed Wielkanocą do wtorku po.
        assertEquals("Wiosenna przerwa świąteczna", off[LocalDate.of(2027, 3, 25)])
        assertTrue(LocalDate.of(2027, 3, 30) in off)
        assertFalse(LocalDate.of(2027, 3, 31) in off)
        assertEquals("Boże Ciało", off[LocalDate.of(2027, 5, 27)])
        assertEquals("Wakacje", off[LocalDate.of(2027, 6, 28)])
        assertFalse(LocalDate.of(2026, 11, 12) in off)
    }

    @Test fun ferieDependOnVoivodeship() {
        val maz = SchoolCalendar.daysOff(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 1), Voivodeship.MAZOWIECKIE)
        assertFalse(LocalDate.of(2027, 1, 20) in maz)
        assertTrue(LocalDate.of(2027, 2, 3) in maz)
        val none = SchoolCalendar.daysOff(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 1), null)
        assertTrue(none.values.none { it == "Ferie zimowe" })
    }

    @Test fun lessonsSkippedOnDaysOffButTrainingsStay() {
        val mon = LocalDate.of(2026, 11, 9)
        val school = EventEntity(id = 1, title = "Szkoła", category = Category.SCHOOL, schoolKind = SchoolKind.LESSONS,
            startDay = mon.toEpochDay(), startMinute = 8 * 60, endMinute = 14 * 60, repeatDays = (1..5).sumOf { Planner.dayBit(DayOfWeek.of(it)) })
        val tennis = EventEntity(id = 2, title = "Tenis", category = Category.SPORT, startDay = mon.toEpochDay(),
            startMinute = 16 * 60, endMinute = 17 * 60, repeatDays = Planner.dayBit(DayOfWeek.WEDNESDAY) or Planner.dayBit(DayOfWeek.THURSDAY))
        val data = PlannerData(events = listOf(school, tennis), customDaysOff = listOf(CustomDayOff(mon.plusDays(3), "Dzień dyrektorski")))
        val occ = Planner.occurrences(data, mon, mon.plusDays(4))
        val schoolDays = occ.filter { it.event.id == 1L }.map { it.date.dayOfMonth }
        assertEquals(listOf(9, 10, 13), schoolDays) // 11.11 święto, 12.11 dzień dyrektorski
        assertEquals(listOf(11, 12), occ.filter { it.event.id == 2L }.map { it.date.dayOfMonth })
    }

    private val list = listOf(
        listOf("Kalendarium roku szkolnego 2026/2027"),
        listOf(""),
        listOf("Od", "Do", "Godzina", "Wydarzenie", "Rodzaj"),
        listOf("29.09.2026", "", "", "Gala", "wydarzenie"),
        listOf("12.11", "13.11", "", "Dni wolne", ""),
        listOf("26.11.2026", "", "", "Zebranie z rodzicami", ""),
        listOf("10.12.2026", "", "19:00 - 20:30", "Koncert", "wydarzenie"),
        listOf("46562", "", "19:00", "Zakończenie roku", "wydarzenie"),
        listOf("", "", "", "bez daty", ""),
        listOf("przypis bez nazwy"),
    )

    @Test fun parsesDatedListWithKindsTimesAndShortDates() {
        val p = PlanImport.parse(list)
        assertTrue(p.sections.isEmpty())
        assertEquals(5, p.dated.size)
        val off = p.dated.single { it.kind == DatedKind.DAY_OFF }
        assertEquals(LocalDate.of(2026, 11, 12), off.from)
        assertEquals(LocalDate.of(2026, 11, 13), off.to)
        assertEquals(DatedKind.MEETING, p.dated.single { it.title.startsWith("Zebranie") }.kind)
        val concert = p.dated.single { it.title == "Koncert" }
        assertEquals(19 * 60, concert.startMinute); assertEquals(20 * 60 + 30, concert.endMinute)
        assertEquals(LocalDate.of(2027, 6, 24), p.dated.single { it.title == "Zakończenie roku" }.from) // liczba dni Excela
        assertEquals(1, p.warnings.size)
    }

    @Test fun datedListToEventsSkipsPastAndSplitsDaysOff() {
        val p = PlanImport.parse(list)
        val from = LocalDate.of(2026, 11, 10)
        val ev = PlanImport.toEvents(p, ImportOptions(3, emptyList(), true, from, null))
        assertEquals(listOf("Zebranie z rodzicami", "Koncert", "Zakończenie roku"), ev.map { it.event.title })
        assertTrue(ev.all { it.event.importTag == PlanImport.EVENTS_TAG })
        assertTrue(ev.first().event.allDay)
        assertFalse(ev[1].event.allDay)
        assertEquals(Category.IMPORTANT, ev.first().event.category)
        assertEquals(listOf(12, 13), PlanImport.daysOff(p, from).map { it.day.dayOfMonth })
    }

    /** Lokalnie na prawdziwym kalendarium: SF_REAL_CAL=ścieżka (plik nie trafia do repozytorium). */
    @Test fun realCalendarIfProvided() {
        val path = System.getenv("SF_REAL_CAL")
        assumeTrue(path != null && File(path).exists())
        val p = PlanImport.parse(File(path!!).inputStream().use { Xlsx.read(it) }.first().rows)
        p.dated.forEach { println("  ${it.from}..${it.to} ${it.startMinute?.let(PlanImport::hm) ?: ""} ${it.kind} ${it.title}") }
        println("REAL CAL: wpisów=${p.dated.size}, dni wolnych od 10.11=${PlanImport.daysOff(p, LocalDate.of(2026, 11, 10)).size}, uwagi=${p.warnings}")
        assertEquals(23, p.dated.size)
    }
}
