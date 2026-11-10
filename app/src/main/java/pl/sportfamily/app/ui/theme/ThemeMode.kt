package pl.sportfamily.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode(val label: String) { SYSTEM("Systemowy"), LIGHT("Jasny"), DARK("Ciemny") }

/** Wybór motywu zapisany na telefonie. Domyślnie motyw systemowy. */
object ThemeSettings {
    private const val PREFS = "settings"
    private const val KEY = "theme_mode"
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode

    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        _mode.value = ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun set(context: Context, mode: ThemeMode) {
        _mode.value = mode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
        // Widżet ma ten sam motyw co aplikacja.
        pl.sportfamily.app.notify.ReminderScheduler.requestRefresh(context)
    }
}
