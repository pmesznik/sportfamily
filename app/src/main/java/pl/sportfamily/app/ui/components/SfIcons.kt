package pl.sportfamily.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pl.sportfamily.app.ui.theme.Sf
import java.util.Collections
import java.util.IdentityHashMap

/** Rodzaj części ikony: L linia, F wypełnienie (obie w kolorze ikony), D pomarańczowa kropka, C i CS wycięcie w kolorze tła. */
enum class K { L, F, D, C, CS }

/** Jedna ścieżka ikony z paczki SVG (siatka 24, linia 2). */
class P(val d: String, val kind: K, val w: Float = 2f, val rot: Float = 0f, val px: Float = 0f, val py: Float = 0f)

/** Ikona SportFamily w trzech warstwach: linia (kolor kategorii), kropka (pomarańczowa), wycięcie (kolor tła). */
class ActivityIcon(val name: String, val group: String, val keywords: List<String>, private val parts: List<P>) {
    val line: ImageVector by lazy { build(K.L, K.F).also { byLine[it] = this } }
    val dot: ImageVector by lazy { build(K.D) }
    val cut: ImageVector? by lazy { if (parts.any { it.kind == K.C || it.kind == K.CS }) build(K.C, K.CS) else null }

    private fun build(vararg kinds: K): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        val ink = SolidColor(Color.Black)
        parts.filter { it.kind in kinds }.forEach { p ->
            val add: ImageVector.Builder.() -> Unit = {
                if (p.kind == K.L || p.kind == K.CS) addPath(addPathNodes(p.d), stroke = ink, strokeLineWidth = p.w,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
                else addPath(addPathNodes(p.d), fill = ink)
            }
            if (p.rot != 0f) group(rotate = p.rot, pivotX = p.px, pivotY = p.py) { add() } else add()
        }
    }.build()

    companion object {
        /** Linia ikony -> ikona, żeby zwykłe miejsca przyjmujące ImageVector rysowały też kropkę. */
        internal val byLine: MutableMap<ImageVector, ActivityIcon> = Collections.synchronizedMap(IdentityHashMap())
    }
}

/**
 * Jak [Icon], ale ikona SportFamily dostaje pomarańczową kropkę i wycięcia w kolorze [cut].
 * Dla zwykłych ikon Material działa dokładnie jak [Icon].
 */
@Composable
fun SfIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    cut: Color = Color.Unspecified,
) {
    val icon = ActivityIcon.byLine[imageVector]
    if (icon == null) { Icon(imageVector, contentDescription, modifier, tint); return }
    Box(modifier.size(24.dp).then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)) {
        Icon(icon.line, null, Modifier.matchParentSize(), tint)
        icon.cut?.let { Icon(it, null, Modifier.matchParentSize(), if (cut.isSpecified) cut else Color.Transparent) }
        Icon(icon.dot, null, Modifier.matchParentSize(), Sf.colors.accent.copy(alpha = tint.alpha))
    }
}

private val Color.isSpecified get() = this != Color.Unspecified

/** Ikony aplikacji z paczki "brakujące" (szkoła, kategorie, terminy rodzinne, interfejs). */
object AppIcons {
    fun icon(key: String): ActivityIcon = AppIconPack[key] ?: AppIconPack2[key] ?: error("Brak ikony $key")
    private fun v(key: String): ImageVector = icon(key).line

    val transport get() = v("ui-transport")
    val wyjazd get() = v("ui-wyjazd")
    val miejsce get() = v("ui-miejsce")
    val nawigacja get() = v("ui-nawigacja")
    val telefon get() = v("ui-telefon")
    val trener get() = v("ui-trener")
    val zabierzTez get() = v("ui-zabierz-tez")
    val listaRzeczy get() = v("ui-lista-rzeczy")
    val zadanie get() = v("ui-zadanie")
    val powtarzanie get() = v("ui-powtarzanie")
    val odwolane get() = v("ui-odwolane")
    val konflikt get() = v("ui-konflikt")
    val przypomnienie get() = v("ui-przypomnienie")
    val widzet get() = v("ui-widzet")
    val importuj get() = v("ui-import")
    val eksportuj get() = v("ui-eksport")
    val dzienWolny get() = v("szkola-dzien-wolny")

    val tabDzisiaj get() = v("tab-dzisiaj")
    val tabDzisiajOn get() = v("tab-dzisiaj-wybrana")
    val tabKalendarz get() = v("tab-kalendarz")
    val tabKalendarzOn get() = v("tab-kalendarz-wybrana")
    val tabDzieci get() = v("tab-dzieci")
    val tabDzieciOn get() = v("tab-dzieci-wybrana")
    val tabZadania get() = v("tab-zadania")
    val tabZadaniaOn get() = v("tab-zadania-wybrana")
    val tabWiecej get() = v("tab-wiecej")

    val data get() = v("ui-data")
    val notatki get() = v("ui-notatki")
    val zmianaTerminu get() = v("ui-zmiana-terminu")
    val osoba get() = v("ui-osoba")
    val kontakty get() = v("ui-kontakty")
    val aktualizacja get() = v("ui-aktualizacja")
    val uwaga get() = v("ui-uwaga")
    val przyklad get() = v("ui-przyklad")
    val motywAuto get() = v("motyw-auto")
    val motywJasny get() = v("motyw-jasny")
    val motywCiemny get() = v("motyw-ciemny")
    val dodaj get() = v("akcja-dodaj")
    val zamknij get() = v("akcja-zamknij")
    val usun get() = v("akcja-usun")
    val edytuj get() = v("akcja-edytuj")
    val wstecz get() = v("akcja-wstecz")
    val poprzedni get() = v("akcja-poprzedni")
    val nastepny get() = v("akcja-nastepny")
    val zaznaczone get() = v("akcja-zaznaczone")
}
