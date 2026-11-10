package pl.sportfamily.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.sportfamily.app.SportFamilyApp
import pl.sportfamily.app.domain.Planner
import pl.sportfamily.app.domain.ReminderPlanner
import pl.sportfamily.app.ui.theme.SchoolSettings
import pl.sportfamily.app.widget.TodayWidget
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Jeden alarm na raz: zawsze ustawiony na najbliższą chwilę, w której coś trzeba zrobić
 * (pokazać przypomnienie albo odświeżyć widżet). Po każdym alarmie i po każdej zmianie danych
 * przeliczamy wszystko od nowa, więc edycja lub odwołanie terminu od razu zmienia przypomnienia.
 */
object ReminderScheduler {
    const val ACTION_TICK = "pl.sportfamily.app.REMINDER_TICK"
    private val lock = Mutex()

    fun requestRefresh(context: Context) {
        val app = context.applicationContext as SportFamilyApp
        app.appScope.launch { run(app) }
    }

    suspend fun run(context: Context) = lock.withLock {
        val app = context.applicationContext as SportFamilyApp
        val data = app.repository.data.first().copy(voivodeship = SchoolSettings.voivodeship.value)
        val prefs = ReminderSettings.load(app)
        val now = LocalDateTime.now()

        val all = ReminderPlanner.plan(data, prefs, now)
        val due = ReminderPlanner.due(all, now.plusSeconds(30), ReminderSettings.shown(app).keys)
        due.forEach { Notifier.show(app, it) }
        ReminderSettings.markShown(app, due.map { it.key }, now.toLocalDate().toEpochDay())

        val occ = Planner.occurrences(data, now.toLocalDate(), now.toLocalDate().plusDays(1))
        schedule(app, ReminderPlanner.nextWake(all, occ, now.plusSeconds(30)))
        runCatching { TodayWidget.refresh(app) }
    }

    private fun schedule(context: Context, at: LocalDateTime) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Dokładny alarm, także w trybie uśpienia. Bez zgody na dokładne alarmy: przybliżony (może się spóźnić kilka minut).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }
}

/** Budzi się na alarm, po restarcie telefonu, zmianie czasu i po aktualizacji aplikacji. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SportFamilyApp
        val pending = goAsync()
        app.appScope.launch {
            try { ReminderScheduler.run(app) } finally { pending.finish() }
        }
    }
}
