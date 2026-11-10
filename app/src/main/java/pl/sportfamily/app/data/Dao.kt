package pl.sportfamily.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    // Odczyt
    @Query("SELECT * FROM children ORDER BY sortOrder, id")
    fun observeChildren(): Flow<List<Child>>

    @Query("SELECT * FROM persons ORDER BY id")
    fun observePersons(): Flow<List<Person>>

    @Query("SELECT * FROM events")
    fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM event_children")
    fun observeEventChildren(): Flow<List<EventChild>>

    @Query("SELECT * FROM event_exceptions")
    fun observeExceptions(): Flow<List<EventException>>

    @Query("SELECT * FROM checklist_items ORDER BY position, id")
    fun observeChecklistItems(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_checks")
    fun observeChecks(): Flow<List<ChecklistCheck>>

    @Query("SELECT * FROM tasks ORDER BY done, CASE WHEN dueDay IS NULL THEN 1 ELSE 0 END, dueDay, id")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEvent(id: Long): EventEntity?

    @Query("SELECT childId FROM event_children WHERE eventId = :eventId")
    suspend fun getEventChildIds(eventId: Long): List<Long>

    @Query("SELECT * FROM checklist_items WHERE eventId = :eventId ORDER BY position, id")
    suspend fun getChecklist(eventId: Long): List<ChecklistItem>

    @Query("SELECT * FROM persons ORDER BY id")
    suspend fun observePersonsOnce(): List<Person>

    @Query("SELECT COUNT(*) FROM persons")
    suspend fun personCount(): Int

    // Zapis: dzieci i osoby
    @Upsert suspend fun upsertChild(child: Child): Long
    @Delete suspend fun deleteChild(child: Child)
    @Upsert suspend fun upsertPerson(person: Person): Long
    @Delete suspend fun deletePerson(person: Person)

    // Zapis: wydarzenia
    @Insert suspend fun insertEvent(event: EventEntity): Long
    @androidx.room.Update suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    @Query("DELETE FROM event_children WHERE eventId = :eventId")
    suspend fun clearEventChildren(eventId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEventChildren(links: List<EventChild>)

    @Query("DELETE FROM checklist_items WHERE eventId = :eventId AND id NOT IN (:keepIds)")
    suspend fun deleteChecklistExcept(eventId: Long, keepIds: List<Long>)

    @Upsert suspend fun upsertChecklistItem(item: ChecklistItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertException(ex: EventException)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: ChecklistCheck)

    @Query("DELETE FROM checklist_checks WHERE itemId = :itemId AND day = :day")
    suspend fun deleteCheck(itemId: Long, day: Long)

    @Query("SELECT COUNT(*) FROM checklist_checks WHERE itemId = :itemId AND day = :day")
    suspend fun isChecked(itemId: Long, day: Long): Int

    /** Usuwa wcześniej zaimportowany plan danego dziecka. */
    @Query("DELETE FROM events WHERE importTag = :tag AND id IN (SELECT eventId FROM event_children WHERE childId = :childId)")
    suspend fun deleteImported(tag: String, childId: Long): Int

    /** Zapisuje import. Zastępuje tylko wydarzenia z tym samym znacznikiem (plan lekcji albo kalendarium). */
    @Transaction
    suspend fun importEvents(childId: Long, replace: Boolean, events: List<Pair<EventEntity, List<Long>>>, daysOff: List<SchoolDayOff>): Int {
        if (replace) events.mapNotNull { it.first.importTag }.toSet().forEach { deleteImported(it, childId) }
        events.forEach { (e, kids) -> saveEvent(e, kids, emptyList()) }
        daysOff.forEach { upsertSchoolDayOff(it) }
        return events.size
    }

    // Dni wolne od lekcji (ręczne)
    @Query("SELECT * FROM school_days_off ORDER BY day")
    fun observeSchoolDaysOff(): Flow<List<SchoolDayOff>>

    @Upsert suspend fun upsertSchoolDayOff(d: SchoolDayOff)
    @Delete suspend fun deleteSchoolDayOff(d: SchoolDayOff)

    // Zadania
    @Upsert suspend fun upsertTask(task: TaskEntity): Long
    @Delete suspend fun deleteTask(task: TaskEntity)

    @Transaction
    suspend fun saveEvent(event: EventEntity, childIds: List<Long>, checklist: List<ChecklistItem>): Long {
        val id = if (event.id == 0L) insertEvent(event) else {
            updateEvent(event); event.id
        }
        clearEventChildren(id)
        insertEventChildren(childIds.map { EventChild(id, it) })
        val kept = checklist.filter { it.id != 0L }.map { it.id }
        deleteChecklistExcept(id, kept.ifEmpty { listOf(-1L) })
        checklist.forEachIndexed { i, item -> upsertChecklistItem(item.copy(eventId = id, position = i)) }
        return id
    }

    @Transaction
    suspend fun toggleCheck(itemId: Long, day: Long) {
        if (isChecked(itemId, day) > 0) deleteCheck(itemId, day) else insertCheck(ChecklistCheck(itemId, day))
    }
}
