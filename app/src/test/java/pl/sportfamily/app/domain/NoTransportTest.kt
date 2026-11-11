package pl.sportfamily.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventChild
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.OccurrenceOverride
import pl.sportfamily.app.domain.Planner.dayBit
import java.time.DayOfWeek
import java.time.LocalDate

/** "Transport nie dotyczy": bez wyjazdu, ostrzeżeń o kierowcy i przypomnień o przewozie. */
class NoTransportTest {
    private val mon = LocalDate.of(2026, 11, 16)
    private val tennis = EventEntity(
        id = 1, title = "Tenis", category = Category.SPORT, startDay = mon.toEpochDay(),
        startMinute = 16 * 60, endMinute = 17 * 60 + 30, travelMinutes = 20, placeName = "Korty",
        repeatDays = dayBit(DayOfWeek.MONDAY),
    )

    private fun data(e: EventEntity, ov: List<OccurrenceOverride> = emptyList()) = PlannerData(
        children = listOf(Child(1, "Staś", 0)), events = listOf(e), eventChildren = listOf(EventChild(e.id, 1)), overrides = ov,
    )

    @Test fun withoutFlagNobodyDrivingIsAWarning() {
        val occ = Planner.occurrences(data(tennis), mon, mon)
        assertTrue(Planner.conflicts(occ).isNotEmpty())
        assertTrue(ReminderPlanner.plan(data(tennis), ReminderPrefs(), mon.atTime(8, 0)).any { it.kind == ReminderKind.PICKUP })
    }

    @Test fun seriesNoTransportRemovesDepartureWarningsAndReminders() {
        val e = tennis.copy(noTransport = true)
        val o = Planner.occurrences(data(e), mon, mon).single()
        assertTrue(o.noTransport)
        assertNull(o.departure)
        assertFalse(o.needsTransport)
        assertTrue(Planner.conflicts(listOf(o)).isEmpty())
        val kinds = ReminderPlanner.plan(data(e), ReminderPrefs(), mon.atTime(8, 0)).map { it.kind }
        assertTrue(ReminderKind.DEPARTURE !in kinds && ReminderKind.PICKUP !in kinds)
        assertTrue(ReminderKind.START in kinds) // zostaje zwykłe przypomnienie o zajęciach
    }

    @Test fun oneDayOverrideOnlyThatDay() {
        val ov = listOf(OccurrenceOverride(1, mon.toEpochDay(), noTransport = true))
        val occ = Planner.occurrences(data(tennis, ov), mon, mon.plusDays(7))
        assertTrue(occ.first { it.date == mon }.noTransport)
        assertFalse(occ.first { it.date == mon.plusDays(7) }.noTransport)
        // i odwrotnie: w serii "nie dotyczy", ale tego dnia trzeba wieźć
        val back = listOf(OccurrenceOverride(1, mon.toEpochDay(), noTransport = false))
        assertFalse(Planner.occurrences(data(tennis.copy(noTransport = true), back), mon, mon).single().noTransport)
    }
}
