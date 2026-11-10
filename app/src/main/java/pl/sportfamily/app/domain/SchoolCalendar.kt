package pl.sportfamily.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class Voivodeship(val label: String) {
    DOLNOSLASKIE("dolnośląskie"), KUJAWSKO_POMORSKIE("kujawsko-pomorskie"), LUBELSKIE("lubelskie"),
    LUBUSKIE("lubuskie"), LODZKIE("łódzkie"), MALOPOLSKIE("małopolskie"), MAZOWIECKIE("mazowieckie"),
    OPOLSKIE("opolskie"), PODKARPACKIE("podkarpackie"), PODLASKIE("podlaskie"), POMORSKIE("pomorskie"),
    SLASKIE("śląskie"), SWIETOKRZYSKIE("świętokrzyskie"), WARMINSKO_MAZURSKIE("warmińsko-mazurskie"),
    WIELKOPOLSKIE("wielkopolskie"), ZACHODNIOPOMORSKIE("zachodniopomorskie"),
}

/** Dzień wolny od szkoły dodany ręcznie (np. dzień dyrektorski). */
data class CustomDayOff(val day: LocalDate, val label: String)

/**
 * Dni wolne od zajęć szkolnych w Polsce.
 * Reguły z rozporządzenia o organizacji roku szkolnego: przerwa zimowa 23 do 31 grudnia, przerwa wiosenna
 * od czwartku przed Wielkanocą do wtorku po Wielkanocy, koniec zajęć w ostatni piątek czerwca.
 * Ferie zimowe co roku ogłasza MEN osobno dla grup województw, dlatego są w tabeli [ferie].
 */
object SchoolCalendar {

    /** Ferie zimowe wg komunikatu MEN, klucz = rok rozpoczęcia roku szkolnego. Uzupełniać co roku. */
    private val ferie: Map<Int, List<Pair<ClosedRange<LocalDate>, Set<Voivodeship>>>> = mapOf(
        2026 to listOf(
            LocalDate.of(2027, 1, 18)..LocalDate.of(2027, 1, 31) to setOf(
                Voivodeship.PODKARPACKIE, Voivodeship.PODLASKIE, Voivodeship.DOLNOSLASKIE,
                Voivodeship.LODZKIE, Voivodeship.SLASKIE, Voivodeship.OPOLSKIE,
            ),
            LocalDate.of(2027, 2, 1)..LocalDate.of(2027, 2, 14) to setOf(
                Voivodeship.MAZOWIECKIE, Voivodeship.POMORSKIE, Voivodeship.SWIETOKRZYSKIE, Voivodeship.LUBELSKIE,
            ),
            LocalDate.of(2027, 2, 15)..LocalDate.of(2027, 2, 28) to setOf(
                Voivodeship.LUBUSKIE, Voivodeship.KUJAWSKO_POMORSKIE, Voivodeship.WARMINSKO_MAZURSKIE,
                Voivodeship.WIELKOPOLSKIE, Voivodeship.ZACHODNIOPOMORSKIE, Voivodeship.MALOPOLSKIE,
            ),
        ),
    )

    /** Czy znamy termin ferii dla roku szkolnego, w którym jest ta data. */
    fun ferieKnown(date: LocalDate): Boolean = ferie.containsKey(schoolYearStart(date))

    fun schoolYearStart(date: LocalDate): Int = if (date.monthValue >= 9) date.year else date.year - 1

    /** Niedziela Wielkanocna (algorytm Meeusa/Jonesa/Butchera, kalendarz gregoriański). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /** Święta ustawowo wolne od pracy (z Wigilią, wolną od 2025 r.). */
    fun publicHolidays(year: Int): Map<LocalDate, String> {
        val e = easter(year)
        return buildMap {
            put(LocalDate.of(year, 1, 1), "Nowy Rok")
            put(LocalDate.of(year, 1, 6), "Trzech Króli")
            put(e, "Wielkanoc")
            put(e.plusDays(1), "Poniedziałek Wielkanocny")
            put(LocalDate.of(year, 5, 1), "Święto Pracy")
            put(LocalDate.of(year, 5, 3), "Święto Konstytucji 3 Maja")
            put(e.plusDays(49), "Zielone Świątki")
            put(e.plusDays(60), "Boże Ciało")
            put(LocalDate.of(year, 8, 15), "Wniebowzięcie NMP")
            put(LocalDate.of(year, 11, 1), "Wszystkich Świętych")
            put(LocalDate.of(year, 11, 11), "Święto Niepodległości")
            if (year >= 2025) put(LocalDate.of(year, 12, 24), "Wigilia")
            put(LocalDate.of(year, 12, 25), "Boże Narodzenie")
            put(LocalDate.of(year, 12, 26), "Drugi dzień świąt")
        }
    }

    fun lastFridayOfJune(year: Int): LocalDate =
        LocalDate.of(year, 6, 30).with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))

    /**
     * Dni bez lekcji w przedziale [from, to], z opisem. Weekendy pomijamy (lekcji w nie i tak nie ma).
     * Ferie tylko, gdy wybrano województwo i znamy ich termin.
     */
    fun daysOff(from: LocalDate, to: LocalDate, voivodeship: Voivodeship?, custom: List<CustomDayOff> = emptyList()): Map<LocalDate, String> {
        if (to.isBefore(from)) return emptyMap()
        val out = LinkedHashMap<LocalDate, String>()
        fun mark(d: LocalDate, label: String) {
            if (!d.isBefore(from) && !d.isAfter(to) && d !in out) out[d] = label
        }
        fun markRange(a: LocalDate, b: LocalDate, label: String) {
            var d = maxOf(a, from)
            val end = minOf(b, to)
            while (!d.isAfter(end)) { mark(d, label); d = d.plusDays(1) }
        }
        // Najpierw dni dodane ręcznie i święta (dokładniejszy opis), potem przerwy.
        custom.forEach { mark(it.day, it.label) }
        for (y in from.year - 1..to.year) {
            publicHolidays(y).forEach { (d, l) -> mark(d, l) }
            markRange(LocalDate.of(y, 12, 23), LocalDate.of(y, 12, 31), "Zimowa przerwa świąteczna")
            val e = easter(y)
            markRange(e.minusDays(3), e.plusDays(2), "Wiosenna przerwa świąteczna")
            // Wakacje: od soboty po zakończeniu zajęć do 31 sierpnia.
            markRange(lastFridayOfJune(y).plusDays(1), LocalDate.of(y, 8, 31), "Wakacje")
            if (voivodeship != null) {
                ferie[y]?.forEach { (range, group) ->
                    if (voivodeship in group) markRange(range.start, range.endInclusive, "Ferie zimowe")
                }
            }
        }
        return out
    }
}
