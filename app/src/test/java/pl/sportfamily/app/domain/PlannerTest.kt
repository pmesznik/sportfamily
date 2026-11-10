package pl.sportfamily.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventChild
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.EventException
import pl.sportfamily.app.data.Person
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class PlannerTest {
    private val mon = LocalDate.of(2026, 11, 9) // poniedziałek
    private val tueThu = Planner.dayBit(DayOfWeek.TUESDAY) or Planner.dayBit(DayOfWeek.THURSDAY)

    private fun training(id: Long = 1, until: LocalDate? = null) = EventEntity(
        id = id, title = "Tenis", category = Category.SPORT, startDay = mon.toEpochDay(),
        startMinute = 16 * 60 + 30, endMinute = 18 * 60, repeatDays = tueThu,
        repeatUntilDay = until?.toEpochDay(), travelMinutes = 25, bufferMinutes = 10, dropOffPersonId = 1,
    )

    @Test fun weeklySeriesProducesTuesdaysAndThursdays() {
        val dates = Planner.datesFor(training(), mon, mon.plusDays(13))
        assertEquals(listOf(10, 12, 17, 19), dates.map { it.dayOfMonth })
    }

    @Test fun seriesRespectsEndDateAndCancelledDay() {
        val dates = Planner.datesFor(training(until = mon.plusDays(10)), mon, mon.plusDays(30), setOf(mon.plusDays(3).toEpochDay()))
        assertEquals(listOf(10, 17, 19), dates.map { it.dayOfMonth })
    }

    @Test fun seriesDoesNotStartBeforeStartDay() {
        val dates = Planner.datesFor(training().copy(startDay = mon.plusDays(3).toEpochDay()), mon, mon.plusDays(6))
        assertEquals(listOf(12), dates.map { it.dayOfMonth })
    }

    @Test fun departureIsStartMinusTravelAndBuffer() {
        val data = PlannerData(events = listOf(training()), persons = listOf(Person(1, "Mama")))
        val o = Planner.occurrences(data, mon.plusDays(1), mon.plusDays(1)).single()
        assertEquals(LocalDateTime.of(2026, 11, 10, 15, 55), o.departure)
    }

    @Test fun multiDayEventVisibleOnEveryDay() {
        val sat = mon.plusDays(5)
        val e = EventEntity(id = 2, title = "Turniej", category = Category.IMPORTANT, startDay = sat.toEpochDay(),
            endDay = sat.plusDays(1).toEpochDay(), startMinute = 9 * 60, endMinute = 16 * 60)
        val occ = Planner.occurrences(PlannerData(events = listOf(e)), sat.plusDays(1), sat.plusDays(1)).single()
        assertTrue(occ.overlapsDay(sat) && occ.overlapsDay(sat.plusDays(1)))
        assertEquals(LocalDateTime.of(2026, 11, 15, 16, 0), occ.end)
    }

    @Test fun detectsMissingDriverAndChildOverlap() {
        val kid = Child(1, "Kuba", 0)
        val football = EventEntity(id = 3, title = "Piłka", category = Category.SPORT, startDay = mon.plusDays(3).toEpochDay(),
            startMinute = 17 * 60, endMinute = 18 * 60 + 30, travelMinutes = 15)
        val guitar = EventEntity(id = 4, title = "Gitara", category = Category.EXTRA, startDay = mon.plusDays(3).toEpochDay(),
            startMinute = 18 * 60, endMinute = 19 * 60, dropOffPersonId = 1)
        val data = PlannerData(
            children = listOf(kid), persons = listOf(Person(1, "Tata")), events = listOf(football, guitar),
            eventChildren = listOf(EventChild(3, 1), EventChild(4, 1)),
        )
        val c = Planner.conflicts(Planner.occurrences(data, mon, mon.plusDays(6)))
        assertTrue(c.any { it.kind == ConflictKind.NO_DRIVER && it.a.event.id == 3L })
        assertTrue(c.any { it.kind == ConflictKind.CHILD_OVERLAP })
    }

    @Test fun cancelledOccurrenceIsSkipped() {
        val data = PlannerData(events = listOf(training()), exceptions = listOf(EventException(1, mon.plusDays(1).toEpochDay())))
        val occ = Planner.occurrences(data, mon, mon.plusDays(6))
        assertEquals(listOf(12), occ.map { it.date.dayOfMonth })
    }
}
