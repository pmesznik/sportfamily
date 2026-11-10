package pl.sportfamily.app.widget

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Wygląd widżetu: krycie tła (1 = pełne, 0.2 = prawie przezroczyste). */
object WidgetSettings {
    private const val PREFS = "settings"
    private const val KEY = "widget_alpha"
    const val MIN = 0.2f
    private val _alpha = MutableStateFlow(1f)
    val alpha: StateFlow<Float> = _alpha

    fun load(context: Context) {
        _alpha.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY, 1f).coerceIn(MIN, 1f)
    }

    /** Podgląd w trakcie przesuwania suwaka (bez zapisu). */
    fun preview(value: Float) { _alpha.value = value.coerceIn(MIN, 1f) }

    fun save(context: Context, value: Float) {
        _alpha.value = value.coerceIn(MIN, 1f)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY, _alpha.value).apply()
        pl.sportfamily.app.notify.ReminderScheduler.requestRefresh(context)
    }
}
