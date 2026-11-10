package pl.sportfamily.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.EventEntity
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceAndLessonsTest {
    private val wed = LocalDate.of(2026, 11, 11)

    private fun event(every: Int, days: Int) = EventEntity(
        id = 1, title = "Basen", category = Category.SPORT, startDay = wed.toEpochDay(),
        startMinute = 17 * 60, endMinute = 18 * 60, repeatDays = days, repeatEveryWeeks = every,
    )

    @Test fun everyTwoWeeksCountsFromStartWeek() {
        val dates = Planner.datesFor(event(2, Planner.dayBit(DayOfWeek.WEDNESDAY)), wed, wed.plusDays(35))
        assertEquals(listOf(11, 25, 9), dates.map { it.dayOfMonth })
    }

    @Test fun severalDaysEveryThreeWeeks() {
        val days = Planner.dayBit(DayOfWeek.MONDAY) or Planner.dayBit(DayOfWeek.FRIDAY)
        // Seria zaczyna się w środę 11.11: w tym tygodniu tylko piątek, potem co 3 tygodnie pn i pt.
        val dates = Planner.datesFor(event(3, days), wed, wed.plusDays(28))
        assertEquals(listOf(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 4)), dates)
    }

    @Test fun weeklyIsDefault() {
        val dates = Planner.datesFor(event(1, Planner.dayBit(DayOfWeek.WEDNESDAY)), wed, wed.plusDays(14))
        assertEquals(3, dates.size)
    }

    private val notes = """
        1. 8:15-9:00 Plastyka
        2. 9:10-9:55 Geografia
        3. 10:05-10:50 Język angielski
    """.trimIndent()

    @Test fun parsesLessonsAndFindsCurrentOne() {
        val l = Lessons.parse(notes)
        assertEquals(3, l.size)
        assertEquals("Język angielski", l[2].subject)
        assertEquals(LessonState.BeforeSchool, Lessons.state(l, 8 * 60))
        val during = Lessons.state(l, 9 * 60 + 20) as LessonState.During
        assertEquals("Geografia", during.lesson.subject)
        assertEquals("Język angielski", during.next?.subject)
        val brk = Lessons.state(l, 10 * 60) as LessonState.Break
        assertEquals("Język angielski", brk.next.subject)
        assertEquals(LessonState.AfterSchool, Lessons.state(l, 11 * 60))
        assertTrue(Lessons.parse("Zwykła notatka bez lekcji").isEmpty())
    }
}
