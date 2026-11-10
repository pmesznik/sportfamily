package pl.sportfamily.app.domain

/** Lekcja zapisana w bloku "Szkoła" (notatka w formacie "1. 8:15-9:00 Plastyka"). */
data class Lesson(val number: Int, val startMinute: Int, val endMinute: Int, val subject: String)

/** Stan dnia szkolnego w danej chwili. */
sealed interface LessonState {
    data object BeforeSchool : LessonState
    data class During(val lesson: Lesson, val next: Lesson?) : LessonState
    data class Break(val next: Lesson, val untilMinute: Int) : LessonState
    data object AfterSchool : LessonState
}

object Lessons {
    private val line = Regex("""^\s*(\d+)\.\s*(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})\s+(.+)$""")

    fun parse(notes: String): List<Lesson> = notes.lines().mapNotNull { l ->
        line.find(l)?.destructured?.let { (n, h1, m1, h2, m2, s) ->
            Lesson(n.toInt(), h1.toInt() * 60 + m1.toInt(), h2.toInt() * 60 + m2.toInt(), s.trim())
        }
    }.sortedBy { it.startMinute }

    fun state(lessons: List<Lesson>, minute: Int): LessonState? {
        if (lessons.isEmpty()) return null
        if (minute < lessons.first().startMinute) return LessonState.BeforeSchool
        lessons.forEachIndexed { i, l ->
            if (minute in l.startMinute until l.endMinute) return LessonState.During(l, lessons.getOrNull(i + 1))
            val next = lessons.getOrNull(i + 1)
            if (next != null && minute >= l.endMinute && minute < next.startMinute) return LessonState.Break(next, next.startMinute)
        }
        return LessonState.AfterSchool
    }
}
