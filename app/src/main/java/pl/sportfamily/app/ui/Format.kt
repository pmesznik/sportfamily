package pl.sportfamily.app.ui

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val PL: Locale = Locale.forLanguageTag("pl-PL")

private val dayLong = DateTimeFormatter.ofPattern("EEEE, d MMMM", PL)
private val dayShort = DateTimeFormatter.ofPattern("EEE d.MM", PL)
private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", PL)
private val monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", PL)
private val hm = DateTimeFormatter.ofPattern("HH:mm")

fun String.cap() = replaceFirstChar { it.titlecase(PL) }

fun LocalDate.longLabel(): String = format(dayLong).cap()
fun LocalDate.shortLabel(): String = format(dayShort)
fun LocalDate.dayMonthLabel(): String = format(dayMonth)
fun LocalDate.monthYearLabel(): String = format(monthYear).cap()
fun LocalTime.hm(): String = format(hm)
fun LocalDateTime.hm(): String = format(hm)
fun minutesLabel(m: Int): String = "%02d:%02d".format(m / 60, m % 60)

val WeekdayShort = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")

/** "dziś", "jutro", "pojutrze" albo nazwa dnia. */
fun LocalDate.relativeLabel(today: LocalDate): String = when (toEpochDay() - today.toEpochDay()) {
    0L -> "dziś"
    1L -> "jutro"
    2L -> "pojutrze"
    -1L -> "wczoraj"
    else -> shortLabel()
}

/** "za 1 h 20 min", "za 12 min", "teraz". */
fun untilLabel(now: LocalDateTime, t: LocalDateTime): String {
    val min = Duration.between(now, t).toMinutes()
    return when {
        min <= 0 -> "teraz"
        min < 60 -> "za $min min"
        min < 24 * 60 -> {
            val h = min / 60
            val m = min % 60
            if (m == 0L) "za $h h" else "za $h h $m min"
        }
        else -> {
            val d = Duration.between(now.toLocalDate().atStartOfDay(), t.toLocalDate().atStartOfDay()).toDays()
            if (d == 1L) "jutro" else "za $d dni"
        }
    }
}
