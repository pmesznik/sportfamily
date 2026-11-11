package pl.sportfamily.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventChild
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.SchoolKind
import java.time.LocalDate

class SchoolTasksTest {
    private val day = LocalDate.of(2026, 11, 17)
    private val kid = Child(1, "Staś", 0)
    private val kid2 = Child(2, "Ola", 0)
    private val math = Lesson(3, 10 * 60 + 5, 10 * 60 + 50, "Matematyka")
    private val polish = Lesson(4, 11 * 60, 11 * 60 + 45, "Język polski")

    private fun occ(vararg e: Pair<EventEntity, Long>) = Planner.occurrences(
        PlannerData(children = listOf(kid, kid2), events = e.map { it.first },
            eventChildren = e.map { EventChild(it.first.id, it.second) }),
        day, day,
    )

    @Test fun newTaskTakesLessonTimeAndScope() {
        val e = SchoolTasks.newTask(SchoolKind.QUIZ, math, day, "  ułamki, dział 3 ")
        assertEquals("Kartkówka: Matematyka", e.title)
        assertEquals(Category.SCHOOL, e.category)
        assertEquals(SchoolKind.QUIZ, e.schoolKind)
        assertFalse(e.allDay)
        assertEquals(math.startMinute, e.startMinute)
        assertEquals(math.endMinute, e.endMinute)
        assertEquals("ułamki, dział 3", e.notes)
        assertEquals(day.toEpochDay(), e.startDay)
    }

    @Test fun matchesBySubjectAndChild() {
        val quiz = SchoolTasks.newTask(SchoolKind.QUIZ, math, day, "").copy(id = 10)
        val test = SchoolTasks.newTask(SchoolKind.TEST, polish, day, "").copy(id = 11)
        val olaQuiz = SchoolTasks.newTask(SchoolKind.QUIZ, math, day, "").copy(id = 12)
        val all = occ(quiz to 1L, test to 1L, olaQuiz to 2L)
        val forStas = SchoolTasks.forLesson(math, day, setOf(1L), all)
        assertEquals(listOf(10L), forStas.map { it.event.id })
        assertEquals(listOf(11L), SchoolTasks.forLesson(polish, day, setOf(1L), all).map { it.event.id })
    }

    @Test fun manualEntryMatchesByStartTime() {
        val manual = EventEntity(id = 20, title = "Kartkówka z ułamków", category = Category.SCHOOL, schoolKind = SchoolKind.QUIZ,
            startDay = day.toEpochDay(), startMinute = math.startMinute, endMinute = math.endMinute)
        assertEquals(1, SchoolTasks.forLesson(math, day, setOf(1L), occ(manual to 1L)).size)
        assertTrue(SchoolTasks.forLesson(polish, day, setOf(1L), occ(manual to 1L)).isEmpty())
    }

    @Test fun reminderEveningBeforeWithScope() {
        val quiz = SchoolTasks.newTask(SchoolKind.QUIZ, math, day, "ułamki").copy(id = 30)
        val data = PlannerData(children = listOf(kid), events = listOf(quiz), eventChildren = listOf(EventChild(30, 1)))
        val r = ReminderPlanner.plan(data, ReminderPrefs(), day.minusDays(1).atTime(12, 0))
        val prep = r.single { it.kind == ReminderKind.SCHOOL_TASK }
        assertEquals(day.minusDays(1).atTime(20, 0), prep.at)
        assertEquals("Jutro kartkówka: Matematyka (Staś)", prep.title)
        assertTrue(prep.text.contains("ułamki"))
        // bez przypomnienia "za 30 min" w trakcie lekcji
        assertTrue(r.none { it.kind == ReminderKind.START })
    }
}
