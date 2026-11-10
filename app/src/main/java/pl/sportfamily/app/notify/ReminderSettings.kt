package pl.sportfamily.app.notify

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.sportfamily.app.domain.ReminderPrefs

/** Ustawienia przypomnień zapisane na telefonie. Każdy telefon ma własne (np. "to jestem ja"). */
object ReminderSettings {
    private const val PREFS = "reminders"
    private val _prefs = MutableStateFlow(ReminderPrefs())
    val prefs: StateFlow<ReminderPrefs> = _prefs

    fun load(context: Context): ReminderPrefs {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = ReminderPrefs()
        val v = ReminderPrefs(
            travel = p.getBoolean("travel", d.travel),
            travelLead = p.getInt("travelLead", d.travelLead),
            start = p.getBoolean("start", d.start),
            startLead = p.getInt("startLead", d.startLead),
            packing = p.getBoolean("packing", d.packing),
            packingHour = p.getInt("packingHour", d.packingHour),
            morning = p.getBoolean("morning", d.morning),
            morningHour = p.getInt("morningHour", d.morningHour),
            me = p.getLong("me", -1L).takeIf { it > 0 },
        )
        _prefs.value = v
        return v
    }

    fun save(context: Context, v: ReminderPrefs) {
        _prefs.value = v
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("travel", v.travel).putInt("travelLead", v.travelLead)
            .putBoolean("start", v.start).putInt("startLead", v.startLead)
            .putBoolean("packing", v.packing).putInt("packingHour", v.packingHour)
            .putBoolean("morning", v.morning).putInt("morningHour", v.morningHour)
            .putLong("me", v.me ?: -1L)
            .apply()
        ReminderScheduler.requestRefresh(context)
    }

    // ---- Pokazane przypomnienia (żeby nie dublować po restarcie lub ponownym przeliczeniu) ----

    private const val SHOWN = "shown"

    /** Klucze pokazanych przypomnień, z datą pokazania w epochDay (do sprzątania starych). */
    fun shown(context: Context): Map<String, Long> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(SHOWN, emptySet())!!
            .mapNotNull { e -> e.substringBeforeLast('|').takeIf { e.contains('|') }?.let { it to (e.substringAfterLast('|').toLongOrNull() ?: 0L) } }
            .toMap()

    fun markShown(context: Context, keys: Collection<String>, today: Long) {
        if (keys.isEmpty()) return
        val keep = shown(context).filterValues { it >= today - 3 } + keys.associateWith { today }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(SHOWN, keep.map { (k, d) -> "$k|$d" }.toSet()).apply()
    }

    /** Czy aplikacja już raz pytała o zgodę na powiadomienia (żeby nie pytać przy każdym starcie). */
    fun askedForPermission(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("asked", false)

    fun setAskedForPermission(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("asked", true).apply()
}
