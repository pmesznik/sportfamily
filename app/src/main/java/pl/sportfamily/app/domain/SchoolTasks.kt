package pl.sportfamily.app.domain

import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.SchoolKind
import java.time.LocalDate

/** Kartkówki, sprawdziany, prace domowe i projekty przypięte do lekcji z planu. */
object SchoolTasks {
    /** Rodzaje, które można dodać z menu przedmiotu, w kolejności w menu. */
    val kinds = listOf(SchoolKind.QUIZ, SchoolKind.TEST, SchoolKind.HOMEWORK, SchoolKind.PROJECT)

    fun label(k: SchoolKind): String = when (k) {
        SchoolKind.QUIZ -> "Kartkówka"
        SchoolKind.TEST -> "Sprawdzian"
        SchoolKind.HOMEWORK -> "Praca domowa"
        SchoolKind.PROJECT -> "Projekt"
        SchoolKind.SCHOOL_EVENT -> "Wydarzenie szkolne"
        SchoolKind.LESSONS -> "Lekcje"
    }

    fun isTask(e: EventEntity) = e.category == Category.SCHOOL && e.schoolKind in kinds

    /** Tytuł wpisu, np. "Kartkówka: Matematyka". Przedmiot po dwukropku służy do dopasowania do lekcji. */
    fun title(kind: SchoolKind, subject: String) = "${label(kind)}: ${subject.trim()}"

    /** Nowy wpis na czas trwania lekcji (nie cały dzień), bez dojazdu. */
    fun newTask(kind: SchoolKind, lesson: Lesson, day: LocalDate, scope: String): EventEntity = EventEntity(
        title = title(kind, lesson.subject),
        category = Category.SCHOOL,
        schoolKind = kind,
        startDay = day.toEpochDay(),
        startMinute = lesson.startMinute,
        endMinute = lesson.endMinute,
        travelMinutes = 0,
        notes = scope.trim(),
    )

    /**
     * Wpisy przypięte do tej lekcji: ten sam dzień, to samo dziecko (albo brak dzieci) i ten sam przedmiot
     * w tytule; gdy tytuł jest inny (wpis dodany ręcznie), wystarczy ta sama godzina rozpoczęcia.
     */
    fun forLesson(lesson: Lesson, day: LocalDate, childIds: Set<Long>, dayOccurrences: List<Occurrence>): List<Occurrence> =
        dayOccurrences.filter { o ->
            isTask(o.event) && o.date == day &&
                (childIds.isEmpty() || o.children.isEmpty() || o.children.any { it.id in childIds }) &&
                (subjectOf(o.event.title).equals(lesson.subject.trim(), ignoreCase = true) ||
                    (!o.allDay && o.event.startMinute == lesson.startMinute))
        }

    private fun subjectOf(title: String) = title.substringAfter(':', title).trim()
}
