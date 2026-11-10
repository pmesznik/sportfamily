package pl.sportfamily.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.EventDayRule
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.NOBODY
import pl.sportfamily.app.data.OccurrenceOverride
import pl.sportfamily.app.data.Person
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class DayRulesTest {
    private val mon = LocalDate.of(2026, 11, 16)
    private val mama = Person(1, "Mama")
    private val tata = Person(2, "Tata")
    private val babcia = Person(3, "Babcia")

    // Tenis pn, wt, pt; domyślnie 16:00-17:30, zawozi Mama, odbiera Tata.
    private val tennis = EventEntity(
        id = 5, title = "Tenis", category = Category.SPORT, startDay = mon.toEpochDay(),
        startMinute = 16 * 60, endMinute = 17 * 60 + 30, travelMinutes = 20,
        repeatDays = Planner.dayBit(DayOfWeek.MONDAY) or Planner.dayBit(DayOfWeek.TUESDAY) or Planner.dayBit(DayOfWeek.FRIDAY),
        dropOffPersonId = 1, pickUpPersonId = 2,
    )

    private fun data(rules: List<EventDayRule> = emptyList(), ov: List<OccurrenceOverride> = emptyList()) =
        PlannerData(persons = listOf(mama, tata, babcia), events = listOf(tennis), dayRules = rules, overrides = ov)

    @Test fun defaultsApplyToEveryDay() {
        val occ = Planner.occurrences(data(), mon, mon.plusDays(6))
        assertEquals(listOf(16, 17, 20), occ.map { it.date.dayOfMonth })
        assertTrue(occ.all { it.dropOff == mama && it.pickUp == tata && it.start.toLocalTime() == LocalTime.of(16, 0) })
    }

    @Test fun weekdayRuleChangesTimeAndDriverOnlyThatDay() {
        // Piątek: 14:30-16:00, zawozi Tata. Odbiór zostaje domyślny.
        val r = EventDayRule(5, 5, startMinute = 14 * 60 + 30, endMinute = 16 * 60, dropOffPersonId = 2)
        val occ = Planner.occurrences(data(listOf(r)), mon, mon.plusDays(13))
        val fridays = occ.filter { it.date.dayOfWeek == DayOfWeek.FRIDAY }
        assertEquals(2, fridays.size)
        assertTrue(fridays.all { it.start.toLocalTime() == LocalTime.of(14, 30) && it.end.toLocalTime() == LocalTime.of(16, 0) })
        assertTrue(fridays.all { it.dropOff == tata && it.pickUp == tata })
        assertEquals(LocalTime.of(14, 0), fridays.first().departure?.toLocalTime()) // 14:30 - 20 min - 10 min zapasu
        val others = occ.filter { it.date.dayOfWeek != DayOfWeek.FRIDAY }
        assertTrue(others.all { it.dropOff == mama && it.start.toLocalTime() == LocalTime.of(16, 0) })
    }

    @Test fun oneDayOverrideWinsAndNobodyMeansNoDriver() {
        val r = EventDayRule(5, 2, dropOffPersonId = 2) // wtorki Tata
        val ov = listOf(
            OccurrenceOverride(5, mon.plusDays(1).toEpochDay(), dropOffPersonId = 3), // ten wtorek Babcia
            OccurrenceOverride(5, mon.plusDays(4).toEpochDay(), pickUpPersonId = NOBODY), // ten piątek nikt nie odbiera
        )
        val occ = Planner.occurrences(data(listOf(r), ov), mon, mon.plusDays(8))
        val tue1 = occ.first { it.date == mon.plusDays(1) }
        assertEquals(babcia, tue1.dropOff)
        assertTrue(tue1.changedThisDay)
        val tue2 = occ.first { it.date == mon.plusDays(8) }
        assertEquals(tata, tue2.dropOff)
        assertFalse(tue2.changedThisDay)
        val fri = occ.first { it.date == mon.plusDays(4) }
        assertNull(fri.pickUp)
        assertEquals(mama, fri.dropOff)
    }

    @Test fun nobodyAsDriverRaisesConflict() {
        val r = EventDayRule(5, 1, dropOffPersonId = NOBODY)
        val occ = Planner.occurrences(data(listOf(r)), mon, mon)
        assertTrue(Planner.conflicts(occ).any { it.kind == ConflictKind.NO_DRIVER })
    }
}
