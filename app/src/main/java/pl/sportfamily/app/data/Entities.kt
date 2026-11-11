package pl.sportfamily.app.data

import androidx.room.ColumnInfo
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
    /** Numer telefonu (opcjonalny, np. wybrany z kontaktów) do szybkiego połączenia. */
    @ColumnInfo(defaultValue = "") val phone: String = "",
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
    /** Co ile tygodni powtarzać (1 = co tydzień, 2 = co dwa tygodnie...). */
    @ColumnInfo(defaultValue = "1") val repeatEveryWeeks: Int = 1,
    val placeName: String = "",
    val address: String = "",
    val travelMinutes: Int = 0,
    val bufferMinutes: Int = 10,
    val dropOffPersonId: Long? = null,
    val pickUpPersonId: Long? = null,
    val notes: String = "",
    /** Kogo jeszcze zabrać (np. kolega ze szkoły), wolny tekst. */
    @ColumnInfo(defaultValue = "") val alsoTake: String = "",
    /** Transport nie dotyczy (zajęcia w domu, online, dziecko idzie samo): bez wyjazdu, przypomnień i ostrzeżeń o kierowcy. */
    @ColumnInfo(defaultValue = "0") val noTransport: Boolean = false,
    /** Trener (dla treningów): imię i telefon do szybkiego połączenia. */
    @ColumnInfo(defaultValue = "") val coachName: String = "",
    @ColumnInfo(defaultValue = "") val coachPhone: String = "",
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

/** Dzień wolny od lekcji dodany ręcznie (np. dzień dyrektorski). Święta, przerwy i ferie liczy SchoolCalendar. */
@Entity(tableName = "school_days_off")
data class SchoolDayOff(
    @PrimaryKey val day: Long,
    val label: String,
)

/** Osoba "nikt" w ustawieniach dnia i zmianach terminu (null = jak w serii). */
const val NOBODY = 0L

/**
 * Ustawienia jednego dnia tygodnia w serii: inne godziny lub inne osoby niż domyślne.
 * weekday: 1 = poniedziałek ... 7 = niedziela. Null = jak w serii, [NOBODY] = nikt.
 */
@Entity(
    tableName = "event_day_rules",
    primaryKeys = ["eventId", "weekday"],
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE)],
)
data class EventDayRule(
    val eventId: Long,
    val weekday: Int,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val dropOffPersonId: Long? = null,
    val pickUpPersonId: Long? = null,
)

/** Zmiana jednego konkretnego terminu (np. w ten piątek wiezie Babcia). Null = bez zmiany, [NOBODY] = nikt. */
@Entity(
    tableName = "occurrence_overrides",
    primaryKeys = ["eventId", "day"],
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["eventId"], onDelete = ForeignKey.CASCADE)],
)
data class OccurrenceOverride(
    val eventId: Long,
    val day: Long,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val dropOffPersonId: Long? = null,
    val pickUpPersonId: Long? = null,
    /** Kogo zabrać tylko tego dnia; null = jak w serii, pusty tekst = nikogo. */
    val alsoTake: String? = null,
    /** Transport nie dotyczy tylko tego dnia; null = jak w serii. */
    val noTransport: Boolean? = null,
)
