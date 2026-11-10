package pl.sportfamily.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.ChecklistCheck
import pl.sportfamily.app.data.ChecklistItem
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventChild
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.Person
import pl.sportfamily.app.data.SchoolKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RemindersTest {
    private val day = LocalDate.of(2026, 11, 17) // wtorek
    private val mama = Person(1, "Mama")
    private val tata = Person(2, "Tata")
    private val kid = Child(1, "Staś", 0)
    private fun m(h: Int, min: Int = 0) = h * 60 + min

    private val tennis = EventEntity(
        id = 1, title = "Tenis", category = Category.SPORT, startDay = day.toEpochDay(),
        startMinute = m(16), endMinute = m(17, 30), travelMinutes = 20, bufferMinutes = 10,
        placeName = "Korty", dropOffPersonId = 1, pickUpPersonId = 2,
    )

    private fun data(vararg e: EventEntity, items: List<ChecklistItem> = emptyList(), checks: List<ChecklistCheck> = emptyList()) =
        PlannerData(children = listOf(kid), persons = listOf(mama, tata), events = e.toList(),
            eventChildren = e.map { EventChild(it.id, 1) }, checklist = items, checks = checks)

    private fun at(h: Int, min: Int = 0, d: LocalDate = day) = d.atTime(h, min)
    private fun plan(d: PlannerData, prefs: ReminderPrefs = ReminderPrefs(), now: LocalDateTime = at(8)) =
        ReminderPlanner.plan(d, prefs, now)

    @Test fun departureAndPickupTimes() {
        val r = plan(data(tennis)).associateBy { it.kind }
        // wyjazd 15:30 (16:00 - 20 - 10), przypomnienie 15 min wcześniej
        assertEquals(at(15, 15), r[ReminderKind.DEPARTURE]!!.at)
        assertTrue(r[ReminderKind.DEPARTURE]!!.title.contains("15:30"))
        assertTrue(r[ReminderKind.DEPARTURE]!!.text.contains("wiezie Mama"))
        // odbiór 17:30, dojazd 20 min -> wyjazd po dziecko 17:10, przypomnienie 16:55
        assertEquals(at(16, 55), r[ReminderKind.PICKUP]!!.at)
        assertTrue(r[ReminderKind.PICKUP]!!.text.contains("odbiera Tata"))
        assertFalse(ReminderKind.START in r)
    }

    @Test fun meFilterKeepsOnlyMyTrips() {
        val r = plan(data(tennis), ReminderPrefs(me = 2)).map { it.kind }
        assertEquals(listOf(ReminderKind.PICKUP), r) // Tata tylko odbiera
    }

    @Test fun noTravelGivesStartReminder() {
        val online = tennis.copy(id = 2, title = "Angielski online", category = Category.EXTRA, travelMinutes = 0, placeName = "")
        val r = plan(data(online)).single()
        assertEquals(ReminderKind.START, r.kind)
        assertEquals(at(15, 30), r.at)
    }

    @Test fun lessonsAreNotReminded() {
        val school = tennis.copy(id = 3, title = "Szkoła", category = Category.SCHOOL, schoolKind = SchoolKind.LESSONS, travelMinutes = 0)
        assertTrue(plan(data(school)).isEmpty())
    }

    @Test fun allDayTestGetsMorningReminder() {
        val test = EventEntity(id = 4, title = "Sprawdzian z matmy", category = Category.SCHOOL, schoolKind = SchoolKind.TEST,
            startDay = day.toEpochDay(), allDay = true)
        val r = plan(data(test)).single()
        assertEquals(ReminderKind.MORNING, r.kind)
        assertEquals(at(7), r.at)
        assertEquals("Sprawdzian", r.text)
    }

    @Test fun packingEveningBeforeOnlyWhenNotPacked() {
        val items = listOf(ChecklistItem(10, 1, "Rakieta"), ChecklistItem(11, 1, "Woda"))
        val r = plan(data(tennis, items = items), now = at(12, d = day.minusDays(1))).first { it.kind == ReminderKind.PACKING }
        assertEquals(at(20, d = day.minusDays(1)), r.at)
        assertTrue(r.text.startsWith("Zostało 2 z 2"))
        val packed = listOf(ChecklistCheck(10, day.toEpochDay()), ChecklistCheck(11, day.toEpochDay()))
        assertTrue(plan(data(tennis, items = items, checks = packed)).none { it.kind == ReminderKind.PACKING })
    }

    @Test fun dueSkipsShownAndExpired() {
        val all = plan(data(tennis))
        // spóźniony alarm o 15:50: wyjazd jeszcze aktualny (zajęcia 16:00)
        assertEquals(listOf(ReminderKind.DEPARTURE), ReminderPlanner.due(all, at(15, 50), emptySet()).map { it.kind })
        // już pokazane
        val k = all.first { it.kind == ReminderKind.DEPARTURE }.key
        assertTrue(ReminderPlanner.due(all, at(15, 50), setOf(k)).isEmpty())
        // po starcie zajęć nie przypominamy o wyjeździe
        assertTrue(ReminderPlanner.due(all, at(16, 5), emptySet()).none { it.kind == ReminderKind.DEPARTURE })
    }

    @Test fun nextWakeIsEarliestOfReminderBoundaryMidnight() {
        val d = data(tennis)
        val all = plan(d)
        val occ = Planner.occurrences(d, day, day)
        assertEquals(at(15, 15), ReminderPlanner.nextWake(all, occ, at(8)))
        assertEquals(at(16), ReminderPlanner.nextWake(all, occ, at(15, 40))) // start zajęć: odświeżenie widżetu
        assertEquals(day.plusDays(1).atTime(0, 1), ReminderPlanner.nextWake(all, occ, at(18)))
    }

    @Test fun sameDayStayHasNoDepartureReminder() {
        val second = tennis.copy(id = 5, title = "Ogólnorozwojowe", startMinute = m(17, 45), endMinute = m(18, 45))
        val kinds = plan(data(tennis, second)).map { it.occ.event.id to it.kind }
        assertTrue((5L to ReminderKind.DEPARTURE) !in kinds)
        assertTrue((1L to ReminderKind.PICKUP) !in kinds) // zostaje na kolejne zajęcia
        assertTrue((5L to ReminderKind.PICKUP) in kinds)
    }

    @Test fun widgetShowsTomorrowWhenTodayIsOver() {
        val (d, items) = ReminderPlanner.widgetItems(data(tennis.copy(repeatDays = 0b1111111)), at(20))
        assertEquals(day.plusDays(1), d)
        assertEquals(LocalTime.of(16, 0), items.single().start.toLocalTime())
        val (d2, items2) = ReminderPlanner.widgetItems(data(tennis), at(17))
        assertEquals(day, d2)
        assertEquals(1, items2.size) // zajęcia jeszcze trwają
    }

    @Test fun widgetEveningWithOnlyAllDayLeftShowsTomorrow() {
        val gala = EventEntity(id = 7, title = "Strój galowy", category = Category.IMPORTANT, startDay = day.toEpochDay(), allDay = true)
        val tomorrowTennis = tennis.copy(startDay = day.plusDays(1).toEpochDay())
        assertEquals(day, ReminderPlanner.widgetItems(data(gala, tomorrowTennis), at(9)).first)
        assertEquals(day.plusDays(1), ReminderPlanner.widgetItems(data(gala, tomorrowTennis), at(19)).first)
    }
}
