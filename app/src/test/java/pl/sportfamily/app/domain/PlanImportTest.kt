package pl.sportfamily.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventChild
import pl.sportfamily.app.data.xlsx.SheetOut
import pl.sportfamily.app.data.xlsx.Xlsx
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/** Plan przykładowy (zmyślony) w układzie typowym dla szkolnego planu z treningami. */
class PlanImportTest {
    private val grid = listOf(
        listOf("Plan lekcji i treningów"),
        listOf("Klasa 5A, rok szkolny 2026/2027"),
        listOf(""),
        listOf("Godzina", "Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek"),
        listOf("8:00 - 8:45", "Matematyka", "Język polski", "Historia", "Przyroda", "Muzyka"),
        listOf("8:55 - 9:40", "Język polski", "Matematyka", "", "Matematyka", "Plastyka"),
        listOf("9:50 - 10:35", "", "WF", "Matematyka", "", ""),
        listOf(""),
        listOf("Treningi", "Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek"),
        listOf("Trening 1", "Pływanie\n15:00 - 16:00", "Pływanie\n15:00 - 16:00", "", "Judo\n16:30 - 17:30", "Bez godziny"),
        listOf(""),
        listOf("Uwagi: lekcja trwa 45 minut."),
    )

    @Test fun findsTwoSectionsWithCorrectCategories() {
        val p = PlanImport.parse(grid)
        assertEquals(listOf("Lekcje", "Treningi"), p.sections.map { it.label })
        assertEquals(listOf(Category.SCHOOL, Category.SPORT), p.sections.map { it.category })
        assertEquals(11, p.sections[0].items.size)
        assertEquals(3, p.sections[1].items.size)
        assertEquals(LocalDate.of(2027, 6, 25), p.schoolYearEnd)
        assertTrue(p.warnings.single().contains("Bez godziny"))
    }

    @Test fun cellTimeWinsAndTitleIsCleaned() {
        val judo = PlanImport.parse(grid).sections[1].items.first { it.day == DayOfWeek.THURSDAY }
        assertEquals("Judo", judo.title)
        assertEquals(16 * 60 + 30, judo.startMinute)
        assertEquals(17 * 60 + 30, judo.endMinute)
    }

    @Test fun lessonsBecomeDailyBlocksAndTrainingsMergeAcrossDays() {
        val p = PlanImport.parse(grid)
        val from = LocalDate.of(2026, 11, 10)
        val ev = PlanImport.toEvents(p, ImportOptions(7, p.sections.map { it.category }, true, from, p.schoolYearEnd))
        val school = ev.filter { it.event.category == Category.SCHOOL }
        assertEquals(5, school.size)
        val monday = school.first { it.event.repeatDays == Planner.dayBit(DayOfWeek.MONDAY) }.event
        assertEquals(8 * 60, monday.startMinute)
        assertEquals(9 * 60 + 40, monday.endMinute)
        assertTrue(monday.notes.contains("1. 8:00-8:45 Matematyka"))
        val swim = ev.single { it.event.title == "Pływanie" }.event
        assertEquals(Planner.dayBit(DayOfWeek.MONDAY) or Planner.dayBit(DayOfWeek.TUESDAY), swim.repeatDays)
        assertTrue(ev.all { it.event.importTag == PlanImport.IMPORT_TAG && it.childIds == listOf(7L) })
        assertEquals(LocalDate.of(2027, 6, 25).toEpochDay(), swim.repeatUntilDay)
    }

    @Test fun shortAndEnglishDayNames() {
        assertEquals(DayOfWeek.WEDNESDAY, PlanImport.dayOf("Śr."))
        assertEquals(DayOfWeek.FRIDAY, PlanImport.dayOf("PIĄTEK"))
        assertEquals(DayOfWeek.SUNDAY, PlanImport.dayOf("Nd"))
        assertEquals(DayOfWeek.THURSDAY, PlanImport.dayOf("Thursday"))
        assertEquals(null, PlanImport.dayOf("Matematyka"))
    }

    @Test fun xlsxWriteReadRoundTrip() {
        val out = ByteArrayOutputStream()
        Xlsx.write(out, listOf(SheetOut("Plan <test>", grid, headerRows = setOf(3, 8), titleRows = setOf(0))))
        val back = Xlsx.read(ByteArrayInputStream(out.toByteArray())).single()
        assertEquals("Pływanie\n15:00 - 16:00", back.rows[9][1])
        assertEquals("Godzina", back.rows[3][0])
        assertEquals(PlanImport.parse(grid).sections.map { it.items }, PlanImport.parse(back.rows).sections.map { it.items })
    }

    @Test fun exportedChildPlanImportsBackTheSame() {
        val p = PlanImport.parse(grid)
        val from = LocalDate.of(2026, 11, 10)
        val events = PlanImport.toEvents(p, ImportOptions(1, p.sections.map { it.category }, true, from, null))
            .mapIndexed { i, n -> n.event.copy(id = i + 1L) }
        val data = PlannerData(
            children = listOf(Child(1, "Ola", 0)),
            events = events,
            eventChildren = events.map { EventChild(it.id, 1) },
        )
        val sheet = PlanExport.childSheet(data, data.children[0], from)
        val again = PlanImport.parse(sheet.rows)
        assertEquals(p.sections.flatMap { it.items }.toSet(), again.sections.flatMap { it.items }.toSet())
    }

    /** Sprawdzenie na prawdziwym pliku użytkownika, tylko lokalnie: -Dsf.realPlan=ścieżka (plik nie trafia do repozytorium). */
    @Test fun realPlanIfProvided() {
        val path = System.getProperty("sf.realPlan") ?: System.getenv("SF_REAL_PLAN")
        assumeTrue(path != null && File(path).exists())
        val sheet = File(path!!).inputStream().use { Xlsx.read(it) }.first()
        val p = PlanImport.parse(sheet.rows)
        println("REAL: ${p.title} | sekcje=${p.sections.map { "${it.label}:${it.category}:${it.items.size}" }} | koniec=${p.schoolYearEnd} | uwagi=${p.warnings}")
        p.sections.forEach { s -> s.items.forEach { println("  ${s.label} ${it.day} ${PlanImport.hm(it.startMinute)}-${PlanImport.hm(it.endMinute)} ${it.title}") } }
        assertTrue(p.sections.isNotEmpty())
    }
}
