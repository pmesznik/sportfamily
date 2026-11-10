package pl.sportfamily.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    }
}
