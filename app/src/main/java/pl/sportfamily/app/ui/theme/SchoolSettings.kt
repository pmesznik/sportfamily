package pl.sportfamily.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.sportfamily.app.domain.Voivodeship

/** Województwo dziecka (termin ferii zimowych), zapisane na telefonie. */
object SchoolSettings {
    private const val PREFS = "settings"
    private const val KEY = "voivodeship"
    private val _voivodeship = MutableStateFlow<Voivodeship?>(null)
    val voivodeship: StateFlow<Voivodeship?> = _voivodeship

    fun load(context: Context) {
        val v = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        _voivodeship.value = Voivodeship.entries.firstOrNull { it.name == v }
    }

    fun set(context: Context, v: Voivodeship?) {
        _voivodeship.value = v
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, v?.name).apply()
    }
}
