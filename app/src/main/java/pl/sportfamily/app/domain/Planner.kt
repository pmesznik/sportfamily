package pl.sportfamily.app.domain

import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.ChecklistCheck
import pl.sportfamily.app.data.ChecklistItem
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventChild
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.EventException
import pl.sportfamily.app.data.Person
import pl.sportfamily.app.data.SchoolKind
import pl.sportfamily.app.data.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Komplet danych planu, z którego wyliczamy wystąpienia. */
data class PlannerData(
    val children: List<Child> = emptyList(),
    val persons: List<Person> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val eventChildren: List<EventChild> = emptyList(),
    val exceptions: List<EventException> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
    val checks: List<ChecklistCheck> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    /** Ręcznie dodane dni wolne od lekcji. */
    val customDaysOff: List<CustomDayOff> = emptyList(),
    /** Województwo do terminu ferii; null = ferie nieuwzględniane. */
    val voivodeship: Voivodeship? = null,
) {
    /** Dni bez lekcji w przedziale, z opisem (święta, przerwy, ferie, dni dodane ręcznie). */
    fun daysOff(from: LocalDate, to: LocalDate): Map<LocalDate, String> =
        SchoolCalendar.daysOff(from, to, voivodeship, customDaysOff)
}

data class Occurrence(
    val event: EventEntity,
    val date: LocalDate,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val children: List<Child>,
    val dropOff: Person?,
    val pickUp: Person?,
    val checklist: List<ChecklistItem>,
    val checkedIds: Set<Long>,
) {
    val key: String get() = "${event.id}_${date.toEpochDay()}"
    val allDay: Boolean get() = event.allDay
    val multiDay: Boolean get() = event.endDay > event.startDay

    /** Godzina wyjazdu: start minus dojazd minus zapas. Brak, gdy dojazd nieustawiony. */
    val departure: LocalDateTime?
        get() = if (event.travelMinutes > 0 && !event.allDay)
            start.minusMinutes((event.travelMinutes + event.bufferMinutes).toLong()) else null

    val checklistDone: Int get() = checklist.count { it.id in checkedIds }
    val needsTransport: Boolean
        get() = event.travelMinutes > 0 && !event.allDay && event.category != Category.SCHOOL

    fun overlapsDay(day: LocalDate): Boolean =
        !start.toLocalDate().isAfter(day) && !end.toLocalDate().isBefore(day)
}

enum class ConflictKind { NO_DRIVER, CHILD_OVERLAP, DRIVER_OVERLAP }

data class Conflict(val kind: ConflictKind, val a: Occurrence, val b: Occurrence? = null)

object Planner {

    fun dayBit(dow: DayOfWeek): Int = 1 shl (dow.value - 1)

    /** Daty wystąpień wydarzenia w przedziale [from, to] (włącznie). */
    fun datesFor(e: EventEntity, from: LocalDate, to: LocalDate, cancelledDays: Set<Long> = emptySet()): List<LocalDate> {
        if (e.repeatDays == 0) {
            val s = LocalDate.ofEpochDay(e.startDay)
            val en = LocalDate.ofEpochDay(maxOf(e.endDay, e.startDay))
            return if (!en.isBefore(from) && !s.isAfter(to) && e.startDay !in cancelledDays) listOf(s) else emptyList()
        }
        val first = maxOf(from.toEpochDay(), e.startDay)
        val last = minOf(to.toEpochDay(), e.repeatUntilDay ?: Long.MAX_VALUE)
        if (first > last) return emptyList()
        val out = ArrayList<LocalDate>()
        // Co N tygodni: liczymy tygodnie od poniedziałku tygodnia, w którym seria się zaczyna.
        val every = e.repeatEveryWeeks.coerceAtLeast(1)
        val startMonday = LocalDate.ofEpochDay(e.startDay).let { it.minusDays(it.dayOfWeek.value - 1L) }.toEpochDay()
        var d = first
        while (d <= last) {
            val date = LocalDate.ofEpochDay(d)
            val week = Math.floorDiv(d - startMonday, 7L)
            if (e.repeatDays and dayBit(date.dayOfWeek) != 0 && week % every == 0L && d !in cancelledDays) out += date
            d++
        }
        return out
    }

    fun occurrences(data: PlannerData, from: LocalDate, to: LocalDate): List<Occurrence> {
        val childById = data.children.associateBy { it.id }
        val personById = data.persons.associateBy { it.id }
        val childrenByEvent = data.eventChildren.groupBy({ it.eventId }, { it.childId })
        val cancelled = data.exceptions.groupBy({ it.eventId }, { it.day })
        val itemsByEvent = data.checklist.groupBy { it.eventId }
        val checksByDay = data.checks.groupBy({ it.day }, { it.itemId })
        val off = data.daysOff(from, to)

        return data.events.flatMap { e ->
            val lessons = e.category == Category.SCHOOL && (e.schoolKind == null || e.schoolKind == SchoolKind.LESSONS)
            datesFor(e, from, to, cancelled[e.id]?.toSet() ?: emptySet())
                // W dni wolne od szkoły nie ma lekcji. Sprawdziany, prace domowe i treningi zostają.
                .filter { !(lessons && e.repeatDays != 0 && it in off) }
                .map { date ->
                val start: LocalDateTime
                val end: LocalDateTime
                if (e.allDay) {
                    start = date.atStartOfDay()
                    end = date.plusDays(e.endDay - e.startDay).atTime(LocalTime.MAX)
                } else {
                    start = date.atTime(minuteToTime(e.startMinute))
                    val endDate = date.plusDays(e.endDay - e.startDay)
                    val rawEnd = endDate.atTime(minuteToTime(e.endMinute))
                    end = if (rawEnd.isAfter(start)) rawEnd else start.plusHours(1)
                }
                Occurrence(
                    event = e,
                    date = date,
                    start = start,
                    end = end,
                    children = childrenByEvent[e.id].orEmpty().mapNotNull { childById[it] }
                        .sortedBy { it.sortOrder },
                    dropOff = e.dropOffPersonId?.let { personById[it] },
                    pickUp = e.pickUpPersonId?.let { personById[it] },
                    checklist = itemsByEvent[e.id].orEmpty(),
                    checkedIds = checksByDay[date.toEpochDay()].orEmpty().toSet(),
                )
            }
        }.sortedWith(compareBy<Occurrence>({ !it.allDay }, { it.start }, { it.event.title }))
    }

    /**
     * Konflikty: brak kierowcy, to samo dziecko w dwóch miejscach naraz,
     * ta sama osoba wiezie lub odbiera w nakładających się oknach.
     */
    fun conflicts(occ: List<Occurrence>): List<Conflict> {
        val out = ArrayList<Conflict>()
        occ.filter { it.needsTransport && it.dropOff == null }.forEach { out += Conflict(ConflictKind.NO_DRIVER, it) }

        val timed = occ.filter { !it.allDay && !it.multiDay }
        for (i in timed.indices) for (j in i + 1 until timed.size) {
            val a = timed[i]
            val b = timed[j]
            if (a.event.id == b.event.id) continue
            val sharedChild = a.children.any { c -> b.children.any { it.id == c.id } }
            val schoolPair = a.event.category == Category.SCHOOL || b.event.category == Category.SCHOOL
            if (sharedChild && !schoolPair && overlaps(a.start, a.end, b.start, b.end)) {
                out += Conflict(ConflictKind.CHILD_OVERLAP, a, b)
                continue
            }
            if (a.dropOff != null && a.dropOff.id == b.dropOff?.id && a.departure != null && b.departure != null &&
                a.event.placeName != b.event.placeName &&
                overlaps(a.departure!!, a.start, b.departure!!, b.start)
            ) {
                out += Conflict(ConflictKind.DRIVER_OVERLAP, a, b)
                continue
            }
            if (a.pickUp != null && a.pickUp.id == b.pickUp?.id && a.event.placeName != b.event.placeName &&
                overlaps(a.end, a.end.plusMinutes(a.event.travelMinutes.toLong()), b.end, b.end.plusMinutes(b.event.travelMinutes.toLong()))
            ) {
                out += Conflict(ConflictKind.DRIVER_OVERLAP, a, b)
            }
        }
        return out.sortedBy { it.a.start }
    }

    private fun overlaps(s1: LocalDateTime, e1: LocalDateTime, s2: LocalDateTime, e2: LocalDateTime) =
        s1.isBefore(e2) && s2.isBefore(e1)

    fun minuteToTime(m: Int): LocalTime = LocalTime.of((m / 60).coerceIn(0, 23), m % 60)
}
