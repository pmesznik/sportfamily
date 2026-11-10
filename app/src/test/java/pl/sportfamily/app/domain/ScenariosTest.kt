package pl.sportfamily.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventChild
import pl.sportfamily.app.data.EventDayRule
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.EventException
import pl.sportfamily.app.data.NOBODY
import pl.sportfamily.app.data.OccurrenceOverride
import pl.sportfamily.app.data.Person
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Scenariusze z życia rodziny. Testy oznaczone PROBLEM opisują lukę w aplikacji (oczekiwane zachowanie). */
class ScenariosTest {
    private val mon = LocalDate.of(2026, 11, 16)
    private val mama = Person(1, "Mama")
    private val tata = Person(2, "Tata")
    private val babcia = Person(3, "Babcia")
    private val kid = Child(1, "Stas", 0)
    private val kid2 = Child(2, "Ola", 0)
    private fun bits(vararg d: DayOfWeek) = d.fold(0) { a, x -> a or Planner.dayBit(x) }
    private fun m(h: Int, min: Int = 0) = h * 60 + min

    private fun ev(
        id: Long, title: String, days: Int = 0, start: Int = m(16), end: Int = m(17, 30), travel: Int = 20,
        drop: Long? = 1, pick: Long? = 2, place: String = "Kort", day: LocalDate = mon,
        every: Int = 1, until: LocalDate? = null, cat: Category = Category.SPORT,
    ) = EventEntity(
        id = id, title = title, category = cat, startDay = day.toEpochDay(), startMinute = start, endMinute = end,
        repeatDays = days, repeatEveryWeeks = every, repeatUntilDay = until?.toEpochDay(), travelMinutes = travel,
        placeName = place, dropOffPersonId = drop, pickUpPersonId = pick,
    )

    private fun data(
        events: List<EventEntity>, rules: List<EventDayRule> = emptyList(), ov: List<OccurrenceOverride> = emptyList(),
        ex: List<EventException> = emptyList(), links: List<EventChild> = events.map { EventChild(it.id, 1) },
    ) = PlannerData(
        children = listOf(kid, kid2), persons = listOf(mama, tata, babcia), events = events, eventChildren = links,
        exceptions = ex, dayRules = rules, overrides = ov,
    )

    private fun week(d: PlannerData, weeks: Int = 1) = Planner.occurrences(d, mon, mon.plusDays(7L * weeks - 1))

    // ---- Odwolania ----
    @Test fun cancelOneTermRemovesOnlyThatDay() {
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))), ex = listOf(EventException(1, mon.toEpochDay())))
        assertEquals(listOf(mon.plusDays(2)), week(d).map { it.date })
    }

    @Test fun cancelWholeWeekNeedsManyExceptions() {
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))),
            ex = listOf(0L, 2L, 4L).map { EventException(1, mon.toEpochDay() + it) })
        assertTrue(week(d).isEmpty())
        assertEquals(3, week(d, 2).size) // kolejny tydzien wraca normalnie
    }

    @Test fun cancelledTermKeepsOverrideHarmlessly() {
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY))),
            ov = listOf(OccurrenceOverride(1, mon.toEpochDay(), dropOffPersonId = 3)), ex = listOf(EventException(1, mon.toEpochDay())))
        assertTrue(week(d).isEmpty())
    }

    // ---- Zamiana godzin ----
    @Test fun oneDayTimeChangeViaOverrideWorks() {
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY))),
            ov = listOf(OccurrenceOverride(1, mon.toEpochDay(), startMinute = m(18), endMinute = m(19, 30))))
        val o = week(d).single()
        assertEquals(LocalTime.of(18, 0), o.start.toLocalTime())
        assertEquals(LocalTime.of(17, 30), o.departure!!.toLocalTime())
        assertTrue(o.changedThisDay)
    }

    @Test fun ruleStartLaterThanSeriesEndSilentlyGetsOneHour() {
        // Regula dnia zmienia tylko poczatek na 18:00, koniec serii 17:30 -> koniec = start + 1 h
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY))), rules = listOf(EventDayRule(1, 1, startMinute = m(18))))
        assertEquals(LocalTime.of(19, 0), week(d).single().end.toLocalTime())
    }

    @Test fun eventPastMidnightKeepsRealEnd() {
        // PROBLEM: 23:00-01:00 (nocny turniej, oboz) powinno trwac 2 h
        val d = data(listOf(ev(1, "Impreza", day = mon, start = m(23), end = m(1))))
        val o = week(d).single()
        assertEquals(2L, java.time.Duration.between(o.start, o.end).toHours())
    }

    @Test fun seriesChangeFromDateByEndingOldSeriesAndStartingNew() {
        val old = ev(1, "Tenis", bits(DayOfWeek.MONDAY), until = LocalDate.of(2026, 11, 30))
        val new = ev(2, "Tenis", bits(DayOfWeek.MONDAY), start = m(17), end = m(18, 30), day = LocalDate.of(2026, 12, 7))
        val occ = Planner.occurrences(data(listOf(old, new)), mon, LocalDate.of(2026, 12, 14))
        assertEquals(listOf(16, 16, 16, 17, 17), occ.map { it.start.hour })
    }

    // ---- Osoby ----
    @Test fun differentDriverEachWeekday() {
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.FRIDAY))),
            rules = listOf(EventDayRule(1, 2, dropOffPersonId = 2, pickUpPersonId = 3), EventDayRule(1, 5, dropOffPersonId = NOBODY)))
        val o = week(d).associateBy { it.date.dayOfWeek }
        assertEquals(mama, o[DayOfWeek.MONDAY]!!.dropOff)
        assertEquals(tata, o[DayOfWeek.TUESDAY]!!.dropOff)
        assertEquals(babcia, o[DayOfWeek.TUESDAY]!!.pickUp)
        assertNull(o[DayOfWeek.FRIDAY]!!.dropOff)
    }

    @Test fun nobodyPickingUpIsFlagged() {
        // PROBLEM: brak odbioru nie jest ostrzezeniem (tylko brak zawozenia)
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY), pick = null)))
        assertTrue(Planner.conflicts(week(d)).any { it.kind == ConflictKind.NO_PICKUP })
    }

    @Test fun noDriverFlaggedEvenWhenTravelIsZero() {
        // PROBLEM: bez wpisanego dojazdu nikt nie dostaje ostrzezenia o braku kierowcy
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY), travel = 0, drop = null)))
        assertTrue(Planner.conflicts(week(d)).any { it.kind == ConflictKind.NO_DRIVER })
    }

    @Test fun deletedPersonDoesNotFallBackToSeriesDriver() {
        val d = data(listOf(ev(1, "Tenis", bits(DayOfWeek.MONDAY))), rules = listOf(EventDayRule(1, 1, dropOffPersonId = 99)))
        val o = week(d).single()
        assertNull(o.dropOff)
        assertTrue(Planner.conflicts(listOf(o)).any { it.kind == ConflictKind.NO_DRIVER })
    }

    // ---- Konflikty kierowcy ----
    @Test fun samePersonCannotDriveTwoKidsToDifferentPlacesAtOnce() {
        val a = ev(1, "Tenis", day = mon, start = m(16), end = m(17), place = "Kort")
        val b = ev(2, "Basen", day = mon, start = m(16), end = m(17), place = "Basen")
        val d = data(listOf(a, b), links = listOf(EventChild(1, 1), EventChild(2, 2)))
        assertTrue(Planner.conflicts(week(d)).any { it.kind == ConflictKind.DRIVER_OVERLAP })
    }

    @Test fun siblingsSamePlaceSameTimeIsCarpoolNotConflict() {
        val a = ev(1, "Tenis Stas", day = mon, place = "Kort")
        val b = ev(2, "Tenis Ola", day = mon, place = "Kort")
        val d = data(listOf(a, b), links = listOf(EventChild(1, 1), EventChild(2, 2)))
        assertTrue(Planner.conflicts(week(d)).none { it.kind == ConflictKind.DRIVER_OVERLAP })
    }

    @Test fun pickUpThenDropOffForNextEventIsFlagged() {
        // PROBLEM: Mama odbiera Ole z basenu o 15:00 i ma zawiezc Stasia na kort (wyjazd 15:00).
        val a = ev(1, "Basen", day = mon, start = m(14), end = m(15), place = "Basen", drop = 2, pick = 1)
        val b = ev(2, "Tenis", day = mon, start = m(15, 30), end = m(17), place = "Kort", drop = 1, pick = 2)
        val d = data(listOf(a, b), links = listOf(EventChild(1, 2), EventChild(2, 1)))
        assertTrue(Planner.conflicts(week(d)).any { it.kind == ConflictKind.DRIVER_OVERLAP })
    }

    @Test fun oneKidTwoEventsAtOnceIsConflict() {
        val a = ev(1, "Tenis", day = mon, start = m(16), end = m(17))
        val b = ev(2, "Angielski", day = mon, start = m(16, 30), end = m(17, 30), cat = Category.EXTRA)
        assertTrue(Planner.conflicts(week(data(listOf(a, b)))).any { it.kind == ConflictKind.CHILD_OVERLAP })
    }

    @Test fun overrideDriverFixesConflict() {
        val a = ev(1, "Tenis", day = mon, start = m(16), end = m(17), place = "Kort")
        val b = ev(2, "Basen", day = mon, start = m(16), end = m(17), place = "Basen")
        val d = data(listOf(a, b), ov = listOf(OccurrenceOverride(2, mon.toEpochDay(), dropOffPersonId = 2, pickUpPersonId = 3)),
            links = listOf(EventChild(1, 1), EventChild(2, 2)))
        assertTrue(Planner.conflicts(week(d)).none { it.kind == ConflictKind.DRIVER_OVERLAP })
    }

    // ---- Powtarzanie ----
    @Test fun everyTwoWeeksSkipsOddWeeksAndRespectsUntil() {
        val e = ev(1, "Turniej klubowy", bits(DayOfWeek.SATURDAY), every = 2, until = mon.plusDays(5 + 14))
        val dates = Planner.occurrences(data(listOf(e)), mon, mon.plusDays(60)).map { it.date }
        assertEquals(listOf(mon.plusDays(5), mon.plusDays(19)), dates)
    }

    @Test fun seriesStartingMidWeekDoesNotBackfill() {
        val e = ev(1, "Tenis", bits(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), day = mon.plusDays(2))
        assertEquals(listOf(DayOfWeek.FRIDAY), week(data(listOf(e))).map { it.date.dayOfWeek })
    }

    @Test fun trainingOnHolidayStaysButLessonsVanish() {
        val hol = LocalDate.of(2026, 11, 11)
        val lessons = ev(1, "Szkola", bits(DayOfWeek.WEDNESDAY), day = hol, cat = Category.SCHOOL, travel = 0, drop = null, pick = null)
        val training = ev(2, "Trening", bits(DayOfWeek.WEDNESDAY), day = hol)
        val occ = Planner.occurrences(data(listOf(lessons, training)), hol, hol)
        assertEquals(listOf("Trening"), occ.map { it.event.title })
    }

    // ---- Dodatkowa osoba do zabrania ----
    @Test fun alsoTakeOverridesSeriesText() {
        val e = ev(1, "Tenis", bits(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)).copy(alsoTake = "Kuba")
        val ov = listOf(OccurrenceOverride(1, mon.toEpochDay(), alsoTake = "Ania z klasy"), OccurrenceOverride(1, mon.plusDays(1).toEpochDay(), alsoTake = ""))
        val o = week(data(listOf(e), ov = ov))
        assertEquals("Ania z klasy", o[0].alsoTake)
        assertEquals("", o[1].alsoTake)
        assertEquals("Kuba", week(data(listOf(e)), 1)[0].alsoTake)
    }
}
