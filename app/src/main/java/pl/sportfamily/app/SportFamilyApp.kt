package pl.sportfamily.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pl.sportfamily.app.data.AppDatabase
import pl.sportfamily.app.data.Repository

class SportFamilyApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(AppDatabase.create(this).dao())
        pl.sportfamily.app.ui.theme.SchoolSettings.load(this)
        appScope.launch { repository.ensureDefaults() }
        pl.sportfamily.app.notify.Notifier.createChannels(this)
        pl.sportfamily.app.notify.ReminderSettings.load(this)
        pl.sportfamily.app.widget.WidgetSettings.load(this)
        pl.sportfamily.app.ui.theme.ThemeSettings.load(this)
        // Każda zmiana danych (nowe zajęcia, odwołanie, odhaczona rzecz) przelicza przypomnienia i widżet.
        appScope.launch {
            repository.data.collectLatest {
                kotlinx.coroutines.delay(800)
                runCatching { pl.sportfamily.app.notify.ReminderScheduler.run(this@SportFamilyApp) }
            }
        }
    }
}
