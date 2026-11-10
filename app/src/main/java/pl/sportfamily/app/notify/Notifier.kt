package pl.sportfamily.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pl.sportfamily.app.MainActivity
import pl.sportfamily.app.R
import pl.sportfamily.app.domain.Reminder
import pl.sportfamily.app.domain.ReminderKind

/** Kanały i wysyłanie powiadomień. */
object Notifier {
    const val CH_TRAVEL = "travel"
    const val CH_EVENTS = "events"
    const val CH_PACKING = "packing"

    const val EXTRA_EVENT = "open_event"
    const val EXTRA_DAY = "open_day"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_TRAVEL, "Wyjazdy i odbiory", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Kiedy wyjechać na zajęcia i po dziecko"
                },
                NotificationChannel(CH_EVENTS, "Zajęcia i terminy", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Zajęcia bez dojazdu, sprawdziany, wydarzenia całodniowe"
                },
                NotificationChannel(CH_PACKING, "Pakowanie", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Wieczorem: co spakować na jutro"
                },
            )
        )
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Intencja otwierająca szczegóły terminu w aplikacji. */
    fun openEventIntent(context: Context, eventId: Long, day: Long, requestCode: Int): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
            .setAction("open_event_$eventId$day")
            .putExtra(EXTRA_EVENT, eventId).putExtra(EXTRA_DAY, day)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Próbne powiadomienie z ekranu ustawień: sprawdza, czy telefon je pokazuje. */
    fun showTest(context: Context) {
        if (!canNotify(context)) return
        val b = NotificationCompat.Builder(context, CH_TRAVEL)
            .setSmallIcon(R.drawable.ic_notif_wyjazd)
            .setColor(0xFFFF6A1F.toInt())
            .setContentTitle("Wyjazd o 15:30: Tenis")
            .setContentText("To próbne przypomnienie. Tak będą wyglądać prawdziwe.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        runCatching { NotificationManagerCompat.from(context).notify(1, b.build()) }
    }

    fun show(context: Context, r: Reminder) {
        if (!canNotify(context)) return
        val o = r.occ
        val id = r.key.hashCode()
        val channel = when (r.kind) {
            ReminderKind.DEPARTURE, ReminderKind.PICKUP -> CH_TRAVEL
            ReminderKind.PACKING -> CH_PACKING
            else -> CH_EVENTS
        }
        val small = when (r.kind) {
            ReminderKind.DEPARTURE, ReminderKind.PICKUP -> R.drawable.ic_notif_wyjazd
            ReminderKind.PACKING -> R.drawable.ic_notif_pakowanie
            else -> R.drawable.ic_notif_termin
        }
        val b = NotificationCompat.Builder(context, channel)
            .setSmallIcon(small)
            .setColor(0xFFFF6A1F.toInt())
            .setContentTitle(r.title)
            .setContentText(r.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.text))
            .setContentIntent(openEventIntent(context, o.event.id, o.date.toEpochDay(), id))
            .setAutoCancel(true)
            .setCategory(if (channel == CH_TRAVEL) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_EVENT)
            .setPriority(if (channel == CH_TRAVEL) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            // Znika samo, gdy przestaje być aktualne (start zajęć, koniec odbioru).
            .setTimeoutAfter(java.time.Duration.between(java.time.LocalDateTime.now(), r.deadline).toMillis().coerceAtLeast(60_000))

        // Nawigacja do miejsca zajęć przy wyjazdach i odbiorach.
        val place = o.event.address.ifBlank { o.event.placeName }
        if ((r.kind == ReminderKind.DEPARTURE || r.kind == ReminderKind.PICKUP) && place.isNotBlank()) {
            val nav = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(place)))
            b.addAction(0, "Nawiguj", PendingIntent.getActivity(context, id + 1, nav, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        // Telefon do trenera, gdy jest wpisany.
        if (o.event.coachPhone.isNotBlank() && r.kind != ReminderKind.PACKING) {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + o.event.coachPhone.filter { it.isDigit() || it == '+' }))
            b.addAction(0, "Trener", PendingIntent.getActivity(context, id + 2, dial, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        runCatching { NotificationManagerCompat.from(context).notify(id, b.build()) }
    }
}
