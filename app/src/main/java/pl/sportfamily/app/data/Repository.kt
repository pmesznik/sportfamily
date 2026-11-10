package pl.sportfamily.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.sportfamily.app.domain.Planner
import pl.sportfamily.app.domain.PlannerData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class Repository(private val dao: PlannerDao) {

    val data: Flow<PlannerData> = combine(
        combine(dao.observeChildren(), dao.observePersons(), dao.observeEvents(), dao.observeEventChildren()) { c, p, e, ec ->
            PlannerData(children = c, persons = p, events = e, eventChildren = ec)
        },
        combine(dao.observeExceptions(), dao.observeChecklistItems(), dao.observeChecks(), dao.observeTasks(), dao.observeSchoolDaysOff()) { ex, it, ch, t, off ->
            PlannerData(
                exceptions = ex, checklist = it, checks = ch, tasks = t,
                customDaysOff = off.map { d -> pl.sportfamily.app.domain.CustomDayOff(LocalDate.ofEpochDay(d.day), d.label) },
            )
        },
    ) { a, b -> a.copy(exceptions = b.exceptions, checklist = b.checklist, checks = b.checks, tasks = b.tasks, customDaysOff = b.customDaysOff) }

    suspend fun ensureDefaults() {
        if (dao.personCount() == 0) {
            dao.upsertPerson(Person(name = "Mama"))
            dao.upsertPerson(Person(name = "Tata"))
        }
    }

    suspend fun saveChild(child: Child) = dao.upsertChild(child)
    suspend fun deleteChild(child: Child) = dao.deleteChild(child)
    suspend fun savePerson(person: Person) = dao.upsertPerson(person)
    suspend fun deletePerson(person: Person) = dao.deletePerson(person)

    suspend fun getEvent(id: Long) = dao.getEvent(id)
    suspend fun getEventChildIds(id: Long) = dao.getEventChildIds(id)
    suspend fun getChecklist(id: Long) = dao.getChecklist(id)

    suspend fun saveEvent(event: EventEntity, childIds: List<Long>, checklist: List<ChecklistItem>) =
        dao.saveEvent(event, childIds, checklist)

    suspend fun deleteEvent(id: Long) = dao.deleteEvent(id)
    suspend fun cancelOccurrence(eventId: Long, day: LocalDate) = dao.insertException(EventException(eventId, day.toEpochDay()))
    suspend fun toggleCheck(itemId: Long, day: LocalDate) = dao.toggleCheck(itemId, day.toEpochDay())

    suspend fun importPlan(childId: Long, replace: Boolean, events: List<Pair<EventEntity, List<Long>>>, daysOff: List<pl.sportfamily.app.domain.CustomDayOff>) =
        dao.importEvents(childId, replace, events, daysOff.map { SchoolDayOff(it.day.toEpochDay(), it.label) })

    suspend fun addSchoolDayOff(day: LocalDate, label: String) = dao.upsertSchoolDayOff(SchoolDayOff(day.toEpochDay(), label))
    suspend fun deleteSchoolDayOff(day: LocalDate) = dao.deleteSchoolDayOff(SchoolDayOff(day.toEpochDay(), ""))

    suspend fun saveTask(task: TaskEntity) = dao.upsertTask(task)
    suspend fun deleteTask(task: TaskEntity) = dao.deleteTask(task)

    /** Przykładowe dane do wypróbowania aplikacji: dwoje dzieci i typowy tydzień. */
    suspend fun seedDemo() {
        ensureDefaults()
        val persons = dao.observePersonsOnce()
        val mama = persons.getOrNull(0)?.id
        val tata = persons.getOrNull(1)?.id
        val zosia = dao.upsertChild(Child(name = "Zosia", colorArgb = 0xFF1F9EA8.toInt(), note = "klasa 5a", sortOrder = 0))
        val kuba = dao.upsertChild(Child(name = "Kuba", colorArgb = 0xFFE4572E.toInt(), note = "klasa 2b", sortOrder = 1))
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekdays = (1..5).sumOf { Planner.dayBit(DayOfWeek.of(it)) }
        fun bit(vararg d: DayOfWeek) = d.sumOf { Planner.dayBit(it) }
        val until = monday.plusWeeks(30).toEpochDay()

        dao.saveEvent(
            EventEntity(title = "Szkoła", category = Category.SCHOOL, schoolKind = SchoolKind.LESSONS,
                startDay = monday.toEpochDay(), startMinute = 8 * 60, endMinute = 14 * 60 + 20,
                repeatDays = weekdays, repeatUntilDay = until, placeName = "SP 12"),
            listOf(zosia, kuba), emptyList(),
        )
        val tennisList = listOf("Rakiety", "Buty na kort", "Strój", "Woda", "Ręcznik").map { ChecklistItem(eventId = 0, text = it) }
        dao.saveEvent(
            EventEntity(title = "Trening tenisa", category = Category.SPORT,
                startDay = monday.toEpochDay(), startMinute = 16 * 60 + 30, endMinute = 18 * 60,
                repeatDays = bit(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), repeatUntilDay = until,
                placeName = "Korty Orlik", address = "ul. Sportowa 12", travelMinutes = 25, bufferMinutes = 10,
                dropOffPersonId = mama, pickUpPersonId = tata),
            listOf(zosia), tennisList,
        )
        dao.saveEvent(
            EventEntity(title = "Trening piłki", category = Category.SPORT,
                startDay = monday.toEpochDay(), startMinute = 17 * 60, endMinute = 18 * 60 + 30,
                repeatDays = bit(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), repeatUntilDay = until,
                placeName = "Stadion MOSiR", address = "ul. Piłkarska 3", travelMinutes = 15, bufferMinutes = 10,
                dropOffPersonId = null, pickUpPersonId = tata),
            listOf(kuba), listOf("Korki", "Ochraniacze", "Strój", "Woda").map { ChecklistItem(eventId = 0, text = it) },
        )
        dao.saveEvent(
            EventEntity(title = "Gitara", category = Category.EXTRA,
                startDay = monday.toEpochDay(), startMinute = 17 * 60, endMinute = 17 * 60 + 45,
                repeatDays = bit(DayOfWeek.TUESDAY), repeatUntilDay = until,
                placeName = "Dom Kultury", address = "ul. Kulturalna 5", travelMinutes = 10, bufferMinutes = 5,
                dropOffPersonId = tata, pickUpPersonId = tata),
            listOf(kuba), listOf(ChecklistItem(eventId = 0, text = "Gitara i nuty")),
        )
        val friday = monday.plusDays(4)
        dao.saveEvent(
            EventEntity(title = "Sprawdzian: biologia", category = Category.SCHOOL, schoolKind = SchoolKind.TEST,
                startDay = friday.toEpochDay(), startMinute = 8 * 60 + 55, endMinute = 9 * 60 + 40,
                notes = "Dział 3: komórka, tkanki, narządy"),
            listOf(zosia), emptyList(),
        )
        val saturday = monday.plusDays(5)
        dao.saveEvent(
            EventEntity(title = "Turniej U12", category = Category.IMPORTANT,
                startDay = saturday.toEpochDay(), endDay = saturday.plusDays(1).toEpochDay(),
                startMinute = 9 * 60, endMinute = 16 * 60,
                placeName = "Hala Tenisowa Śląsk", address = "Katowice, ul. Kortowa 4",
                travelMinutes = 75, bufferMinutes = 15, dropOffPersonId = tata, pickUpPersonId = tata,
                notes = "Wpisowe 120 zł do czwartku. Nocleg: Hotel Park."),
            listOf(zosia),
            listOf("Rakiety (2)", "Buty na kort", "Strój x2", "Ręcznik", "Woda i przekąski", "Legitymacja zawodnika", "Piłki treningowe")
                .map { ChecklistItem(eventId = 0, text = it) },
        )
        dao.upsertTask(TaskEntity(title = "Opłacić wpisowe na turniej", note = "120 zł", dueDay = monday.plusDays(3).toEpochDay(), childId = zosia))
        dao.upsertTask(TaskEntity(title = "Podpisać zgodę na wycieczkę", dueDay = today.plusDays(1).toEpochDay(), childId = kuba))
        dao.upsertTask(TaskEntity(title = "Kupić piłki treningowe", childId = zosia))
    }
}
