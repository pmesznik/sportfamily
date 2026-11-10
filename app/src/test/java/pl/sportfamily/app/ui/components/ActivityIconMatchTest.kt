package pl.sportfamily.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Dobór ikony po nazwie zajęć, na prawdziwej paczce 95 ikon. */
class ActivityIconMatchTest {
    private fun icon(title: String) = matchActivityIcon(title)?.name

    @Test fun packHas95Icons() = assertEquals(95, ActivityIcons.size)

    @Test fun commonTitles() {
        val cases = mapOf(
            "Tenis" to "Tenis",
            "Trening tenisa" to "Tenis",
            "Tenis stołowy" to "Tenis stołowy",
            "Ping-pong" to "Tenis stołowy",
            "Piłka ręczna" to "Piłka ręczna",
            "Piłka nożna" to "Piłka nożna",
            "Mecz siatkówki" to "Siatkówka",
            "Mecz" to "Piłka nożna",
            "Nauka pływania" to "Pływanie",
            "Basen" to "Pływanie",
            "Nauka jazdy konnej" to "Jazda konna",
            "Nauka gry na gitarze" to "Gitara",
            "Szachy" to "Szachy",
            "Chór szkolny" to "Chór",
            "Śpiew" to "Śpiew",
            "Pieczenie ciast" to "Pieczenie",
            "Judo" to "Judo i karate",
            "Ogólnorozwojówka" to "Skakanka",
            "Koło robotyki" to "Robotyka",
            "Kurs pierwszej pomocy" to "Pierwsza pomoc",
            "Wycieczka klasowa" to "Wydarzenie szkolne",
            "Urodziny Kuby" to "Urodziny",
            "Dentysta" to "Dentysta",
            "Turniej halowy" to "Turniej",
            "Turniej tenisowy" to "Tenis",
            "Urodziny na kręgielni" to "Urodziny",
            "Wywiadówka" to "Zebranie",
            "Opłata za basen" to "Opłata",
        )
        val wrong = cases.filter { (t, want) -> icon(t) != want }.map { (t, want) -> "$t: ${icon(t)} zamiast $want" }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test fun appIconsAllResolve() {
        val icons = listOf(AppIcons.transport, AppIcons.wyjazd, AppIcons.miejsce, AppIcons.nawigacja, AppIcons.telefon, AppIcons.trener,
            AppIcons.zabierzTez, AppIcons.listaRzeczy, AppIcons.zadanie, AppIcons.powtarzanie, AppIcons.odwolane, AppIcons.konflikt,
            AppIcons.przypomnienie, AppIcons.widzet, AppIcons.importuj, AppIcons.eksportuj, AppIcons.dzienWolny,
            AppIcons.tabDzisiaj, AppIcons.tabDzisiajOn, AppIcons.tabKalendarz, AppIcons.tabKalendarzOn, AppIcons.tabDzieci,
            AppIcons.tabDzieciOn, AppIcons.tabZadania, AppIcons.tabZadaniaOn, AppIcons.tabWiecej)
        assertEquals(26, icons.toSet().size)
        pl.sportfamily.app.data.Category.entries.forEach { categoryIcon(it) }
        assertEquals(45, AppIconPack.size)
        assertEquals(22, AppIconPack2.size)
        listOf(AppIcons.data, AppIcons.notatki, AppIcons.zmianaTerminu, AppIcons.osoba, AppIcons.kontakty, AppIcons.aktualizacja,
            AppIcons.uwaga, AppIcons.przyklad, AppIcons.motywAuto, AppIcons.motywJasny, AppIcons.motywCiemny, AppIcons.dodaj,
            AppIcons.zamknij, AppIcons.usun, AppIcons.edytuj, AppIcons.wstecz, AppIcons.poprzedni, AppIcons.nastepny, AppIcons.zaznaczone)
            .let { assertEquals(19, it.toSet().size) }
    }

    @Test fun genericTitlesHaveNoPackIcon() {
        assertNull(icon("Trening"))
        assertNull(icon("Zajęcia dodatkowe"))
    }
}
