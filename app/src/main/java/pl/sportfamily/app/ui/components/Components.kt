package pl.sportfamily.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.sportfamily.app.data.Category
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.SchoolKind
import pl.sportfamily.app.ui.theme.Sf
import pl.sportfamily.app.ui.theme.Tone

/** Karta: w jasnym motywie delikatny cień, w ciemnym jaśniejsza powierzchnia z obrysem. */
@Composable
fun SfCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shadow = Sf.colors.cardShadow
    val border = if (shadow) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val body: @Composable () -> Unit = { Column(Modifier.padding(padding), content = content) }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color,
            shadowElevation = if (shadow) 1.dp else 0.dp, border = border, content = body)
    } else {
        Surface(modifier = modifier, shape = shape, color = color,
            shadowElevation = if (shadow) 1.dp else 0.dp, border = border, content = body)
    }
}

@Composable
fun KidDot(child: Child, size: Dp = 24.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(child.colorArgb))
            .semantics { contentDescription = child.name },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            child.name.take(1).uppercase(),
            color = Color.White,
            fontSize = (size.value * 0.46f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun KidChip(child: Child) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KidDot(child, 20.dp)
        Text(child.name, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun KidDots(children: List<Child>, size: Dp = 22.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
        children.forEach { KidDot(it, size) }
    }
}

@Composable
fun ToneChip(text: String, tone: Tone, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(tone.container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) SfIcon(icon, null, Modifier.size(16.dp), tone.onContainer)
        Text(text, color = tone.onContainer, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/** Kwadratowy checkbox z obszarem dotyku 48 dp. */
@Composable
fun SquareCheck(checked: Boolean, onToggle: () -> Unit, label: String) {
    val sf = Sf.colors
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox, onClickLabel = label, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .then(
                    if (checked) Modifier.background(sf.school.main)
                    else Modifier.border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(7.dp))
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) SfIcon(AppIcons.zaznaczone, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Box(
        modifier
            .height(6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Sf.colors.brand.container),
            contentAlignment = Alignment.Center,
        ) { SfIcon(icon, null, tint = Sf.colors.brand.onContainer, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        actions()
    }
}

@Composable
fun IconBadge(icon: ImageVector, tone: Tone, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tone.container),
        contentAlignment = Alignment.Center,
    ) { SfIcon(icon, null, tint = tone.onContainer, modifier = Modifier.size(size * 0.55f)) }
}

fun categoryLabel(c: Category): String = when (c) {
    Category.SPORT -> "Sport"
    Category.SCHOOL -> "Szkoła"
    Category.EXTRA -> "Zajęcia"
    Category.FAMILY -> "Rodzina"
    Category.IMPORTANT -> "Ważne"
}

fun schoolKindLabel(k: SchoolKind): String = when (k) {
    SchoolKind.LESSONS -> "Lekcje"
    SchoolKind.TEST -> "Sprawdzian"
    SchoolKind.QUIZ -> "Kartkówka"
    SchoolKind.HOMEWORK -> "Praca domowa"
    SchoolKind.PROJECT -> "Projekt"
    SchoolKind.SCHOOL_EVENT -> "Wydarzenie szkolne"
}

/** Ikona kategorii (linia z paczki; kropkę dorysowuje [SfIcon]). */
fun categoryIcon(c: Category): ImageVector = AppIcons.icon(when (c) {
    Category.SPORT -> "kat-sport"
    Category.SCHOOL -> "kat-szkola"
    Category.EXTRA -> "kat-zajecia"
    Category.FAMILY -> "kat-rodzina"
    Category.IMPORTANT -> "kat-wazne"
}).line

private val GenericWords = setOf("nauka", "nauki", "zajęcia", "kurs", "lekcja", "lekcje", "warsztaty", "sekcja", "kółko", "koło", "grupa", "gry", "trening", "treningi")

/** Słowa słabe: wybierają ikonę dopiero, gdy żadne mocniejsze nie pasuje ("Mecz siatkówki" to siatkówka). */
private val WeakKeys = setOf("mecz", "nauk", "pomoc", "piłk", "futbol", "walk", "skoki")

/** Słowo kluczowe pasuje, gdy któreś słowo tytułu od niego się zaczyna (dłuższe frazy: gdziekolwiek). */
private fun matches(title: String, words: List<String>, keys: List<String>) =
    keys.any { k -> if (' ' in k) k in title else words.any { it.startsWith(k) } }

/** Terminy, które wygrywają z nazwą zajęć: "Opłata za basen" to opłata, "Urodziny na kręgielni" to urodziny. */
private val FirstAppIcons = listOf("oplata", "urodziny", "lekarz", "dentysta", "zebranie", "fryzjer")

/** Terminy rodzinne i szkolne rozpoznawane po słowach, w kolejności sprawdzania wobec ikon zajęć. */
private val KeywordAppIcons: Pair<List<ActivityIcon>, List<ActivityIcon>> by lazy {
    val withKeys = AppIconPack.filterValues { it.keywords.isNotEmpty() }
    FirstAppIcons.mapNotNull { withKeys[it] } to withKeys.filterKeys { it !in FirstAppIcons }.values.toList()
}

/** Ikona z paczek pasująca do nazwy albo null: najpierw zajęcia i sporty, potem terminy rodzinne i szkolne. */
fun matchActivityIcon(title: String): ActivityIcon? {
    val t = title.lowercase()
    val words = t.split(' ', ',', '.', '-', '/', '(', ')', ':').filter { it.isNotBlank() }
    // Słowa ogólne ("nauka", "zajęcia", "kurs") nie wybierają ikony: "Nauka jazdy konnej" to jazda konna, nie nauka.
    val specific = words.filter { it !in GenericWords }
    val all = KeywordAppIcons.first + ActivityIcons + KeywordAppIcons.second
    return all.firstOrNull { matches(t, specific, it.keywords - WeakKeys) }
        ?: all.firstOrNull { matches(t, specific, it.keywords.filter { k -> k in WeakKeys }) }
}

/** Ikona bloku lekcji: książka z paczki zajęć. */
val LessonsIcon: ActivityIcon get() = ActivityIcons.first { it.name == "Czytanie" }

/** Ikona wydarzenia: termin szkolny po rodzaju, inne po nazwie, na końcu ikona kategorii. */
fun eventSfIcon(e: EventEntity): ActivityIcon {
    // Lekcje: otwarta książka (jabłko z paczki nie kojarzyło się ze szkołą).
    if (e.category == Category.SCHOOL && (e.schoolKind == null || e.schoolKind == SchoolKind.LESSONS)) return LessonsIcon
    if (e.category == Category.SCHOOL) return AppIcons.icon(when (e.schoolKind) {
        SchoolKind.TEST -> "szkola-sprawdzian"
        SchoolKind.QUIZ -> "szkola-kartkowka"
        SchoolKind.HOMEWORK -> "szkola-praca-domowa"
        SchoolKind.PROJECT -> "szkola-projekt"
        SchoolKind.SCHOOL_EVENT -> "szkola-wydarzenie"
        else -> "szkola-lekcje"
    })
    return matchActivityIcon(e.title) ?: AppIconPack.values.first { it.line == categoryIcon(e.category) }
}

/** Ikona wydarzenia jako ImageVector (linia); [SfIcon] dorysuje kropkę. */
fun eventIcon(e: EventEntity): ImageVector = eventSfIcon(e).line

/** Ikona wydarzenia: linia w kolorze kategorii i pomarańczowa kropka. */
@Composable
fun EventIcon(e: EventEntity, tint: Color, modifier: Modifier = Modifier) {
    val v = remember(e.title, e.category, e.schoolKind) { eventIcon(e) }
    SfIcon(v, null, modifier, tint)
}

/** Logo z napisem i podpisem "Kalendarz rodzinny" (pełna nazwa aplikacji). */
@Composable
fun BrandLockup(modifier: Modifier = Modifier, logoHeight: Dp = 52.dp, centered: Boolean = true) {
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    Column(modifier, horizontalAlignment = align) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(Sf.logoHorizontal),
            contentDescription = androidx.compose.ui.res.stringResource(pl.sportfamily.app.R.string.app_name),
            modifier = Modifier.height(logoHeight),
        )
        Text(
            androidx.compose.ui.res.stringResource(pl.sportfamily.app.R.string.app_tagline).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = if (centered) 0.dp else (logoHeight.value * 0.36f).dp),
        )
    }
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

/**
 * Lista lekcji z bloku "Szkoła". Gdy podano [nowMinute] (dzisiejszy dzień), bieżąca lekcja jest wyróżniona,
 * minione przygaszone, a w przerwie pojawia się wiersz "Przerwa, następna o ...".
 */
@Composable
fun LessonList(
    lessons: List<pl.sportfamily.app.domain.Lesson>,
    nowMinute: Int?,
    modifier: Modifier = Modifier,
    /** Plakietki przy przedmiocie, np. "Kartkówka". */
    badges: (pl.sportfamily.app.domain.Lesson) -> List<String> = { emptyList() },
    /** Stuknięcie w lekcję (null = lista tylko do odczytu). */
    onLessonClick: ((pl.sportfamily.app.domain.Lesson) -> Unit)? = null,
) {
    val sf = Sf.colors
    val state = nowMinute?.let { pl.sportfamily.app.domain.Lessons.state(lessons, it) }
    fun hm(m: Int) = "%d:%02d".format(m / 60, m % 60)
    Column(modifier) {
        lessons.forEach { l ->
            if (state is pl.sportfamily.app.domain.LessonState.Break && state.next == l) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(26.dp))
                    Text("Przerwa, następna o ${hm(l.startMinute)}", style = MaterialTheme.typography.labelMedium, color = sf.school.onContainer)
                }
            }
            val current = state is pl.sportfamily.app.domain.LessonState.During && state.lesson == l
            val past = nowMinute != null && l.endMinute <= nowMinute
            val textColor = when {
                current -> sf.school.onContainer
                past -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (current) sf.school.container else Color.Transparent)
                    .then(if (onLessonClick != null) Modifier.clickable(onClickLabel = "Dodaj kartkówkę lub sprawdzian") { onLessonClick(l) }.heightIn(min = 40.dp) else Modifier)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${l.number}.", style = MaterialTheme.typography.labelMedium, color = textColor, modifier = Modifier.width(20.dp))
                Text(
                    "${hm(l.startMinute)}-${hm(l.endMinute)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = textColor,
                    modifier = Modifier.width(80.dp),
                )
                Text(
                    l.subject, color = textColor, modifier = Modifier.weight(1f).padding(end = 4.dp), maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = if (current) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
                )
                badges(l).forEach { ToneChip(it, sf.important, modifier = Modifier.padding(start = 4.dp)) }
                if (current) ToneChip("TERAZ", sf.school)
            }
        }
    }
}
