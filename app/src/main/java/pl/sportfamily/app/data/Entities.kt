package pl.sportfamily.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Category { SPORT, SCHOOL, EXTRA, FAMILY, IMPORTANT }

/** Rodzaj terminu szkolnego, tylko dla kategorii SCHOOL. */
enum class SchoolKind { LESSONS, TEST, QUIZ, HOMEWORK, PROJECT, SCHOOL_EVENT }

@Entity(tableName = "children")
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val note: String = "",
    val sortOrder: Int = 0,
)

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/**
 * Wydarzenie lub seria. Daty jako epochDay, godziny jako minuty od północy (czas lokalny).
 * repeatDays: maska dni tygodnia, poniedziałek = 1, wtorek = 2, ..., niedziela = 64. Zero oznacza brak powtarzania.
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: Category,
    val schoolKind: SchoolKind? = null,
    val startDay: Long,
    val endDay: Long = startDay,
    val allDay: Boolean = false,
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val repeatDays: Int = 0,
    val repeatUntilDay: Long? = null,
    val placeName: String = "",
    val address: String = "",
    val travelMinutes: Int = 0,
    val bufferMinutes: Int = 10,
    val dropOffPersonId: Long? = null,
    val pickUpPersonId: Long? = null,
    val notes: String = "",
    /** Znacznik wydarzeń z importu planu (ponowny import zastępuje je zamiast dublować). */
    val importTag: String? = null,
)

@Entity(
    tableName = "event_children",
    primaryKeys = ["eventId", "childId"],
    foreignKeys = [
        ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Child::class, ["id"], ["childId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("childId")],
)
data class EventChild(val eventId: Long, val childId: Long)

/** Pojedynczy termin serii odwołany. */
@Entity(
    tableName = "event_exceptions",
    primaryKeys = ["eventId", "day"],
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE)],
)
data class EventException(val eventId: Long, val day: Long)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("eventId")],
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val text: String,
    val position: Int = 0,
)

/** Odhaczenie pozycji listy dla konkretnego dnia (seria treningów ma osobne pakowanie na każdy termin). */
@Entity(
    tableName = "checklist_checks",
    primaryKeys = ["itemId", "day"],
    foreignKeys = [ForeignKey(ChecklistItem::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE)],
)
data class ChecklistCheck(val itemId: Long, val day: Long)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(Child::class, ["id"], ["childId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("childId")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val dueDay: Long? = null,
    val childId: Long? = null,
    val done: Boolean = false,
)
