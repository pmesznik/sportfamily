package pl.sportfamily.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColor
import kotlinx.coroutines.flow.first
import pl.sportfamily.app.MainActivity
import pl.sportfamily.app.R
import pl.sportfamily.app.SportFamilyApp
import pl.sportfamily.app.domain.Occurrence
import pl.sportfamily.app.domain.ReminderPlanner
import pl.sportfamily.app.notify.Notifier
import pl.sportfamily.app.ui.hm
import pl.sportfamily.app.ui.longLabel
import pl.sportfamily.app.ui.shortLabel
import pl.sportfamily.app.ui.theme.SchoolSettings
import pl.sportfamily.app.ui.theme.ThemeMode
import pl.sportfamily.app.ui.theme.ThemeSettings
import java.time.LocalDate
import java.time.LocalDateTime

/** Widżet "Dziś": najbliższe zajęcia z godziną wyjazdu i lista na dziś (albo na jutro, gdy dziś już nic). */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as SportFamilyApp
        ThemeSettings.load(context)
        WidgetSettings.load(context)
        val first = runCatching { app.repository.data.first() }.getOrNull()
        // Widżet żyje dłużej niż jedno odświeżenie: dane, motyw, przezroczystość i czas czytamy na bieżąco,
        // żeby każda zmiana (nowe zajęcia, motyw, suwak, kolejna minuta) od razu była widoczna.
        provideContent {
            val data by app.repository.data.collectAsState(initial = first)
            val mode by ThemeSettings.mode.collectAsState()
            val alpha by WidgetSettings.alpha.collectAsState()
            val tick by ticks.collectAsState()
            val now = remember(tick, data) { LocalDateTime.now() }
            val (day, items) = data?.let { ReminderPlanner.widgetItems(it.copy(voivodeship = SchoolSettings.voivodeship.value), now) }
                ?: (now.toLocalDate() to emptyList())
            androidx.compose.runtime.CompositionLocalProvider(LocalPal provides palette(mode, alpha)) { Content(context, now, day, items) }
        }
    }

    companion object {
        /** Zmienia się przy każdym odświeżeniu, żeby widżet przeliczył "teraz". */
        private val ticks = kotlinx.coroutines.flow.MutableStateFlow(0L)

        suspend fun refresh(context: Context) {
            ticks.value = System.currentTimeMillis()
            TodayWidget().updateAll(context)
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

// Kolory marki: granat i pomarańcz, osobno dla jasnego i ciemnego motywu.
private data class Pal(val bg: GlanceColor, val card: GlanceColor, val fg: GlanceColor, val muted: GlanceColor, val accent: GlanceColor, val warn: GlanceColor, val logo: Int)

private object Light { val bg = Color(0xFFF7F4EF); val card = Color(0xFFFFFFFF); val fg = Color(0xFF141B3D); val muted = Color(0xFF5B6280); val accent = Color(0xFFC2410C); val warn = Color(0xFFB3261E) }
private object Dark { val bg = Color(0xFF141B3D); val card = Color(0xFF1E2650); val fg = Color(0xFFF3F1EC); val muted = Color(0xFFB9BCCF); val accent = Color(0xFFFF7A35); val warn = Color(0xFFFFB4AB) }

/** Motyw widżetu taki jak w aplikacji: systemowy (dzień/noc telefonu), zawsze jasny albo zawsze ciemny. */
private fun palette(mode: ThemeMode, alpha: Float = 1f): Pal = when (mode) {
    ThemeMode.LIGHT -> Pal(GlanceColor(Light.bg.copy(alpha = alpha)), GlanceColor(Light.card.copy(alpha = cardAlpha(alpha))), GlanceColor(Light.fg), GlanceColor(Light.muted), GlanceColor(Light.accent), GlanceColor(Light.warn), R.drawable.logo_symbol)
    ThemeMode.DARK -> Pal(GlanceColor(Dark.bg.copy(alpha = alpha)), GlanceColor(Dark.card.copy(alpha = cardAlpha(alpha))), GlanceColor(Dark.fg), GlanceColor(Dark.muted), GlanceColor(Dark.accent), GlanceColor(Dark.warn), R.drawable.logo_symbol_dark)
    ThemeMode.SYSTEM -> Pal(
        ColorProvider(Light.bg.copy(alpha = alpha), Dark.bg.copy(alpha = alpha)), ColorProvider(Light.card.copy(alpha = cardAlpha(alpha)), Dark.card.copy(alpha = cardAlpha(alpha))), ColorProvider(Light.fg, Dark.fg),
        ColorProvider(Light.muted, Dark.muted), ColorProvider(Light.accent, Dark.accent), ColorProvider(Light.warn, Dark.warn),
        R.drawable.widget_logo,
    )
}

/** Karta z najbliższymi zajęciami zostaje trochę mniej przezroczysta, żeby tekst był czytelny. */
private fun cardAlpha(a: Float) = (a + 0.25f).coerceAtMost(1f)

private val LocalPal = androidx.compose.runtime.staticCompositionLocalOf { palette(ThemeMode.SYSTEM) }
private val Bg: GlanceColor @Composable get() = LocalPal.current.bg
private val Card: GlanceColor @Composable get() = LocalPal.current.card
private val Fg: GlanceColor @Composable get() = LocalPal.current.fg
private val Muted: GlanceColor @Composable get() = LocalPal.current.muted
private val Accent: GlanceColor @Composable get() = LocalPal.current.accent
private val Warn: GlanceColor @Composable get() = LocalPal.current.warn

private fun open(context: Context, o: Occurrence?): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (o != null) {
            action = "widget_${o.key}"
            putExtra(Notifier.EXTRA_EVENT, o.event.id)
            putExtra(Notifier.EXTRA_DAY, o.date.toEpochDay())
        }
    }

@Composable
private fun Content(context: Context, now: LocalDateTime, day: LocalDate, items: List<Occurrence>) {
    val today = now.toLocalDate()
    val next = items.firstOrNull { !it.allDay && it.end.isAfter(now) }
    // Niski widżet (2 rzędy): układ poziomy. Wyższy: karta i lista pod spodem.
    if (androidx.glance.LocalSize.current.height < 240.dp) { Compact(context, now, day, items, next); return }
    Column(
        GlanceModifier.fillMaxSize().background(Bg).cornerRadius(20.dp).padding(12.dp)
    ) {
        Row(GlanceModifier.fillMaxWidth().clickable(actionStartActivity(open(context, null))), verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(LocalPal.current.logo), contentDescription = null, modifier = GlanceModifier.size(20.dp))
            Spacer(GlanceModifier.width(8.dp))
            Text(
                (if (day == today) "Dziś · " else "Jutro · ") + day.longLabel(),
                style = TextStyle(color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        if (items.isEmpty()) {
            Text("Dziś i jutro nic nie zaplanowano.", style = TextStyle(color = Muted, fontSize = 13.sp))
            return@Column
        }
        if (next != null) NextCard(context, next, now)
        val rest = items.filter { it !== next }
        if (rest.isNotEmpty()) {
            Spacer(GlanceModifier.height(6.dp))
            LazyColumn(GlanceModifier.fillMaxWidth()) {
                items(rest, itemId = { it.key.hashCode().toLong() }) { o -> ItemRow(context, o, now) }
            }
        }
    }
}

/** Ikona zajęć w widżecie: linia w kolorze tekstu, kropka pomarańczowa (te same ikony co w aplikacji). */
@Composable
private fun WIcon(o: Occurrence, size: androidx.compose.ui.unit.Dp, tint: GlanceColor = Fg) {
    val key = remember(o.event.title, o.event.category, o.event.schoolKind) { pl.sportfamily.app.ui.components.eventSfIcon(o.event).key }
    val res = WidgetIcons[key] ?: return
    Box(GlanceModifier.size(size)) {
        Image(ImageProvider(res.first), contentDescription = null, modifier = GlanceModifier.size(size),
            colorFilter = androidx.glance.ColorFilter.tint(tint))
        Image(ImageProvider(res.second), contentDescription = null, modifier = GlanceModifier.size(size))
    }
}

@Composable
private fun Compact(context: Context, now: LocalDateTime, day: LocalDate, items: List<Occurrence>, next: Occurrence?) {
    val today = now.toLocalDate()
    Row(GlanceModifier.fillMaxSize().background(Bg).cornerRadius(20.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
        // Lewa strona: dzień i najbliższe zajęcia z wyjazdem.
        Column(GlanceModifier.defaultWeight().fillMaxHeight().clickable(actionStartActivity(open(context, next)))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(LocalPal.current.logo), contentDescription = null, modifier = GlanceModifier.size(16.dp))
                Spacer(GlanceModifier.width(6.dp))
                Text((if (day == today) "Dziś · " else "Jutro · ") + day.shortLabel(), style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            Spacer(GlanceModifier.height(4.dp))
            if (items.isEmpty()) {
                Text("Nic nie zaplanowano", style = TextStyle(color = Fg, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                return@Column
            }
            val o = next ?: items.first()
            val running = !o.allDay && !o.start.isAfter(now)
            Text(
                when { o.allDay -> "Cały dzień"; running -> "Teraz, do ${o.end.hm()}"; else -> o.start.hm() },
                style = TextStyle(color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                WIcon(o, 20.dp)
                Spacer(GlanceModifier.width(6.dp))
                Text(o.event.title, style = TextStyle(color = Fg, fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            val dep = o.departure
            val sub = when {
                running && o.stayFor != null -> "Zostaje na: ${o.stayFor}"
                running && o.needsPickUp -> "Odbiór ${o.end.hm()}: " + (o.pickUp?.name ?: "nikt!")
                dep != null && dep.isAfter(now) -> "Wyjazd ${dep.hm()}: " + (o.dropOff?.name ?: "nikt nie wiezie!")
                o.stayFrom != null -> "Na miejscu po: ${o.stayFrom}"
                else -> o.event.placeName
            }
            if (sub.isNotBlank()) Text(sub, style = TextStyle(color = if (sub.endsWith("!")) Warn else Muted, fontSize = 12.sp), maxLines = 1)
        }
        // Prawa strona: kolejne punkty dnia.
        val rest = items.filter { it !== next }.take(4)
        if (rest.isNotEmpty()) {
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight().fillMaxHeight().background(Card).cornerRadius(12.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
                rest.forEach { o ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 1.dp).clickable(actionStartActivity(open(context, o))), verticalAlignment = Alignment.CenterVertically) {
                        Box(GlanceModifier.width(3.dp).height(14.dp).cornerRadius(2.dp).background(o.children.firstOrNull()?.let { GlanceColor(Color(it.colorArgb)) } ?: Accent)) {}
                        Spacer(GlanceModifier.width(5.dp))
                        WIcon(o, 14.dp)
                        Spacer(GlanceModifier.width(5.dp))
                        Text((if (o.allDay) "" else o.start.hm() + " ") + o.event.title, style = TextStyle(color = Fg, fontSize = 12.sp), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun NextCard(context: Context, o: Occurrence, now: LocalDateTime) {
    val running = !o.start.isAfter(now)
    val dep = o.departure
    Column(
        GlanceModifier.fillMaxWidth().background(Card).cornerRadius(14.dp).padding(10.dp)
            .clickable(actionStartActivity(open(context, o)))
    ) {
        Text(
            if (running) "Teraz, do ${o.end.hm()}" else "Następne",
            style = TextStyle(color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            WIcon(o, 20.dp)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                (if (running) "" else o.start.hm() + "  ") + o.event.title,
                style = TextStyle(color = Fg, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        val sub = when {
            running && o.stayFor != null -> "Zostaje na: ${o.stayFor}"
            running && o.needsPickUp -> "Odbiór ${o.end.hm()}: " + (o.pickUp?.name ?: "nikt nie odbiera!")
            dep != null && dep.isAfter(now) -> "Wyjazd ${dep.hm()}: " + (o.dropOff?.name ?: "nikt nie wiezie!")
            o.stayFrom != null -> "Na miejscu po: ${o.stayFrom}"
            else -> listOfNotNull(o.children.joinToString { it.name }.ifBlank { null }, o.event.placeName.ifBlank { null }).joinToString(" · ")
        }
        if (sub.isNotBlank()) {
            val warn = sub.endsWith("!")
            Text(sub, style = TextStyle(color = if (warn) Warn else Muted, fontSize = 13.sp, fontWeight = if (warn) FontWeight.Bold else FontWeight.Normal), maxLines = 2)
        }
        if (o.alsoTake.isNotBlank()) Text("Zabierz też: ${o.alsoTake}", style = TextStyle(color = Muted, fontSize = 12.sp), maxLines = 1)
    }
}

@Composable
private fun ItemRow(context: Context, o: Occurrence, now: LocalDateTime) {
    val kid = o.children.firstOrNull()
    Row(
        GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(actionStartActivity(open(context, o))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (o.allDay) "cały" else o.start.hm(),
            style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.width(44.dp),
        )
        Box(
            GlanceModifier.width(4.dp).height(18.dp).cornerRadius(2.dp)
                .background(kid?.let { GlanceColor(Color(it.colorArgb)) } ?: Accent)
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        WIcon(o, 16.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(o.event.title, style = TextStyle(color = Fg, fontSize = 14.sp), maxLines = 1)
    }
}
