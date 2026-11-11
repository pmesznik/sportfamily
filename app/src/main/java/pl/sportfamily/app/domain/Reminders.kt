package pl.sportfamily.app.domain

import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.SchoolKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Ustawienia przypomnień (zapisane na telefonie). Domyślnie wszystko włączone. */
data class ReminderPrefs(
    /** Przypomnienie o wyjeździe na zajęcia i o wyjeździe po dziecko. */
    val travel: Boolean = true,
    /** Ile minut przed wyjazdem przypomnieć. */
    val travelLead: Int = 15,
    /** Przypomnienie o zajęciach bez dojazdu (np. zajęcia w szkole, online). */
    val start: Boolean = true,
    val startLead: Int = 30,
    /** Wieczorne przypomnienie o pakowaniu rzeczy na jutro. */
    val packing: Boolean = true,
    val packingHour: Int = 20,
    /** Poranne przypomnienie o całodniowych terminach (sprawdzian, wycieczka, strój galowy). */
    val morning: Boolean = true,
    val morningHour: Int = 7,
    /** Kartkówki, sprawdziany, prace domowe i projekty: przypomnienie wieczorem dzień wcześniej (o godzinie pakowania). */
    val schoolTasks: Boolean = true,
    /** Osoba, która używa tego telefonu. Ustawiona: o wyjazdach i odbiorach przypominamy tylko jej. */
    val me: Long? = null,
)

enum class ReminderKind { DEPARTURE, PICKUP, START, PACKING, MORNING, SCHOOL_TASK }

/**
 * Jedno przypomnienie. [at] to chwila pokazania, [deadline] to moment, po którym nie ma sensu go pokazywać
 * (np. spóźniony alarm po rozpoczęciu zajęć).
 */
data class Reminder(
    val key: String,
    val kind: ReminderKind,
    val at: LocalDateTime,
    val deadline: LocalDateTime,
    val occ: Occurrence,
    val title: String,
    val text: String,
)

object ReminderPlanner {
    private val HM = DateTimeFormatter.ofPattern("H:mm")

    /** Wszystkie przypomnienia dla terminów od wczoraj do [days] dni naprzód. */
    fun plan(data: PlannerData, prefs: ReminderPrefs, now: LocalDateTime, days: Long = 2): List<Reminder> {
        val today = now.toLocalDate()
        val occ = Planner.occurrences(data, today.minusDays(1), today.plusDays(days))
        val out = ArrayList<Reminder>()
        for (o in occ) {
            if (isLessons(o)) continue // zwykłe lekcje są codziennie, nie przypominamy
            val forMe = { id: Long? -> prefs.me == null || id == prefs.me }
            val title = o.event.title
            val kids = o.children.joinToString(", ") { it.name }.let { if (it.isBlank()) "" else " ($it)" }

            if (o.allDay) {
                // Terminy całodniowe: rano w dniu terminu.
                if (prefs.morning) {
                    val at = o.date.atTime(prefs.morningHour, 0)
                    out += Reminder(key(o, ReminderKind.MORNING), ReminderKind.MORNING, at, o.date.atTime(LocalTime.MAX),
                        o, "Dziś: $title$kids", listOfNotNull(o.event.placeName.ifBlank { null }, o.alsoTake.ifBlank { null }?.let { "Zabierz też: $it" }).joinToString(" · ").ifBlank { categoryText(o) })
                }
            } else {
                val dep = o.departure
                if (prefs.travel && dep != null && forMe(o.dropOff?.id)) {
                    val at = dep.minusMinutes(prefs.travelLead.toLong())
                    out += Reminder(key(o, ReminderKind.DEPARTURE), ReminderKind.DEPARTURE, at, o.start,
                        o, "Wyjazd o ${dep.format(HM)}: $title$kids", departureText(o))
                } else if (prefs.start && dep == null && o.stayFrom == null && !SchoolTasks.isTask(o.event)) {
                    val at = o.start.minusMinutes(prefs.startLead.toLong())
                    out += Reminder(key(o, ReminderKind.START), ReminderKind.START, at, o.start,
                        o, "O ${o.start.format(HM)}: $title$kids", listOfNotNull(o.event.placeName.ifBlank { null }, o.alsoTake.ifBlank { null }?.let { "Zabierz też: $it" }).joinToString(" · ").ifBlank { "Za ${prefs.startLead} min" })
                }
                // Odbiór: osoba odbierająca musi wyjechać tak, żeby być na końcu zajęć.
                if (prefs.travel && o.needsPickUp && o.event.travelMinutes > 0 && forMe(o.pickUp?.id)) {
                    val leave = o.end.minusMinutes(o.event.travelMinutes.toLong())
                    val at = leave.minusMinutes(prefs.travelLead.toLong())
                    out += Reminder(key(o, ReminderKind.PICKUP), ReminderKind.PICKUP, at, o.end,
                        o, "Odbiór o ${o.end.format(HM)}: $title$kids",
                        "Wyjazd po dziecko o ${leave.format(HM)}" + (o.pickUp?.let { " · odbiera ${it.name}" } ?: " · nikt nie odbiera!") +
                            (o.event.placeName.ifBlank { null }?.let { " · $it" } ?: ""))
                }
            }

            // Kartkówka, sprawdzian, praca domowa, projekt: wieczorem dzień wcześniej, z zakresem materiału.
            if (prefs.schoolTasks && SchoolTasks.isTask(o.event)) {
                val at = o.date.minusDays(1).atTime(prefs.packingHour, 0)
                val kind = SchoolTasks.label(o.event.schoolKind!!).lowercase()
                val subject = o.event.title.substringAfter(':', o.event.title).trim()
                out += Reminder(key(o, ReminderKind.SCHOOL_TASK), ReminderKind.SCHOOL_TASK, at, o.start,
                    o, "Jutro $kind: $subject$kids",
                    listOfNotNull(o.event.notes.ifBlank { null }, if (o.allDay) null else "lekcja o ${o.start.format(HM)}").joinToString(" · "))
            }

            // Pakowanie wieczorem dzień wcześniej, gdy lista rzeczy nie jest jeszcze odhaczona.
            if (prefs.packing && o.checklist.isNotEmpty() && o.checklistDone < o.checklist.size) {
                val at = o.date.minusDays(1).atTime(prefs.packingHour, 0)
                val left = o.checklist.filter { it.id !in o.checkedIds }
                out += Reminder(key(o, ReminderKind.PACKING), ReminderKind.PACKING, at, o.start,
                    o, "Spakuj na jutro: $title$kids",
                    "Zostało ${left.size} z ${o.checklist.size}: " + left.take(5).joinToString(", ") { it.text } + if (left.size > 5) "…" else "")
            }
        }
        return out.sortedBy { it.at }
    }

    /** Przypomnienia do pokazania teraz: czas minął, termin jeszcze aktualny, nie były pokazane. */
    fun due(all: List<Reminder>, now: LocalDateTime, shown: Set<String>): List<Reminder> =
        all.filter { !it.at.isAfter(now) && now.isBefore(it.deadline) && it.key !in shown }

    /** Najbliższa chwila, w której trzeba się obudzić: kolejne przypomnienie albo zmiana na widżecie. */
    fun nextWake(all: List<Reminder>, occ: List<Occurrence>, now: LocalDateTime): LocalDateTime {
        val reminder = all.map { it.at }.filter { it.isAfter(now) }.minOrNull()
        // Widżet: odświeżenie przy starcie i końcu zajęć oraz o północy.
        val boundary = occ.filter { !it.allDay }.flatMap { listOf(it.start, it.end) }.filter { it.isAfter(now) }.minOrNull()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(1)
        return listOfNotNull(reminder, boundary, midnight).min()
    }

    fun key(o: Occurrence, kind: ReminderKind) = "${kind.name}_${o.key}_${o.start.toLocalTime()}"

    private fun isLessons(o: Occurrence) =
        o.event.category == Category.SCHOOL && (o.event.schoolKind == null || o.event.schoolKind == SchoolKind.LESSONS)

    private fun departureText(o: Occurrence): String = listOfNotNull(
        "Start ${o.start.format(HM)}",
        o.dropOff?.let { "wiezie ${it.name}" } ?: "nikt nie wiezie!",
        o.event.placeName.ifBlank { null },
        o.alsoTake.ifBlank { null }?.let { "zabierz też: $it" },
    ).joinToString(" · ")

    private fun categoryText(o: Occurrence) = when (o.event.schoolKind) {
        SchoolKind.TEST -> "Sprawdzian"
        SchoolKind.QUIZ -> "Kartkówka"
        SchoolKind.HOMEWORK -> "Zadanie domowe"
        SchoolKind.PROJECT -> "Projekt"
        else -> "Cały dzień"
    }

    /** Wpisy na widżet: pozostałe dziś, a gdy dziś już nic, to jutro. */
    fun widgetItems(data: PlannerData, now: LocalDateTime): Pair<LocalDate, List<Occurrence>> {
        val today = now.toLocalDate()
        val todayLeft = Planner.occurrences(data, today, today).filter { it.allDay || it.end.isAfter(now) }
        // Wieczorem, gdy zostały już tylko terminy całodniowe (np. strój galowy), ważniejsze jest jutro.
        val timedLeft = todayLeft.any { !it.allDay }
        if (timedLeft || (todayLeft.isNotEmpty() && now.hour < 18)) return today to todayLeft
        val tomorrow = today.plusDays(1)
        return tomorrow to Planner.occurrences(data, tomorrow, tomorrow)
    }
}
