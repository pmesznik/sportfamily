package pl.sportfamily.app.ui.more

import pl.sportfamily.app.ui.components.AppIcons
import pl.sportfamily.app.ui.components.SfIcon

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import pl.sportfamily.app.data.Person
import pl.sportfamily.app.domain.Planner
import pl.sportfamily.app.domain.ReminderPrefs
import pl.sportfamily.app.notify.Notifier
import pl.sportfamily.app.notify.ReminderSettings
import pl.sportfamily.app.ui.components.SectionHeader
import pl.sportfamily.app.ui.components.SfCard
import pl.sportfamily.app.ui.event.PersonChoice
import pl.sportfamily.app.ui.theme.Sf
import pl.sportfamily.app.widget.TodayWidgetReceiver

/** Przypomnienia (powiadomienia) i widżet na ekran główny. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersSection(persons: List<Person>) {
    val context = LocalContext.current
    val prefs by ReminderSettings.prefs.collectAsState()
    fun save(p: ReminderPrefs) = ReminderSettings.save(context, p)

    // Stan zgody sprawdzamy przy każdym powrocie na ekran (użytkownik mógł ją zmienić w ustawieniach).
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val allowed = remember(tick) { Notifier.canNotify(context) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    SectionHeader("Przypomnienia")
    SfCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!allowed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SfIcon(AppIcons.przypomnienie, null, tint = Sf.colors.error.main)
                    Text("  Powiadomienia są wyłączone, przypomnienia nie dotrą.", style = MaterialTheme.typography.bodyMedium, color = Sf.colors.error.onContainer)
                }
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !ReminderSettings.askedForPermission(context)) {
                        ReminderSettings.setAskedForPermission(context)
                        ask.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Po odmowie Android już nie pyta: otwieramy ustawienia powiadomień aplikacji.
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }
                }, shape = RoundedCornerShape(12.dp), modifier = Modifier.heightIn(min = 48.dp)) { Text("Włącz powiadomienia") }
            }

            ToggleRow("Wyjazdy i odbiory", "Kiedy wyjechać na zajęcia i po dziecko", prefs.travel) { save(prefs.copy(travel = it)) }
            if (prefs.travel) Choices(listOf(10, 15, 20, 30), prefs.travelLead, { "$it min przed" }) { save(prefs.copy(travelLead = it)) }

            ToggleRow("Zajęcia bez dojazdu", "Np. zajęcia w szkole albo online", prefs.start) { save(prefs.copy(start = it)) }
            if (prefs.start) Choices(listOf(15, 30, 60), prefs.startLead, { "$it min przed" }) { save(prefs.copy(startLead = it)) }

            ToggleRow("Pakowanie na jutro", "Wieczorem, gdy lista rzeczy nie jest odhaczona", prefs.packing) { save(prefs.copy(packing = it)) }
            if (prefs.packing) Choices(listOf(19, 20, 21), prefs.packingHour, { "o $it:00" }) { save(prefs.copy(packingHour = it)) }

            ToggleRow("Kartkówki i sprawdziany", "Wieczorem dzień wcześniej (o godzinie pakowania), z zakresem materiału", prefs.schoolTasks) { save(prefs.copy(schoolTasks = it)) }

            ToggleRow("Rano: terminy całodniowe", "Sprawdziany, wycieczki, strój galowy", prefs.morning) { save(prefs.copy(morning = it)) }
            if (prefs.morning) Choices(listOf(6, 7, 8), prefs.morningHour, { "o $it:00" }) { save(prefs.copy(morningHour = it)) }

            if (persons.isNotEmpty()) {
                Text("Kto używa tego telefonu", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Gdy wybierzesz osobę, przypomnienia o wyjazdach i odbiorach przyjdą tylko wtedy, gdy to ona wiezie lub odbiera.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PersonChoice("To jestem ja", prefs.me, persons.filter { it.id != pl.sportfamily.app.data.NOBODY }, "wszyscy (przypominaj o każdym kursie)",
                    Modifier.fillMaxWidth().padding(top = 4.dp)) { save(prefs.copy(me = it?.takeIf { id -> id != pl.sportfamily.app.data.NOBODY })) }
            }

            if (allowed) OutlinedButton(
                onClick = { Notifier.showTest(context) },
                shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 6.dp).heightIn(min = 48.dp),
            ) { Text("Wyślij próbne powiadomienie") }
        }
    }

    SectionHeader("Widżet")
    SfCard(Modifier.fillMaxWidth()) {
        Text(
            "Na ekranie głównym: najbliższe zajęcia, godzina wyjazdu, kto wiezie i plan dnia. Gdy dziś już nic, pokazuje jutro.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val alpha by pl.sportfamily.app.widget.WidgetSettings.alpha.collectAsState()
        Text("Przezroczystość tła: ${((1f - alpha) * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
        androidx.compose.material3.Slider(
            value = 1f - alpha,
            onValueChange = { pl.sportfamily.app.widget.WidgetSettings.preview(1f - it) },
            onValueChangeFinished = { pl.sportfamily.app.widget.WidgetSettings.save(context, alpha) },
            valueRange = 0f..(1f - pl.sportfamily.app.widget.WidgetSettings.MIN),
            steps = 7,
        )
        val awm = AppWidgetManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && awm.isRequestPinAppWidgetSupported) {
            OutlinedButton(
                onClick = { awm.requestPinAppWidget(ComponentName(context, TodayWidgetReceiver::class.java), null, null) },
                shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp),
            ) { SfIcon(AppIcons.widzet, null); Text("  Dodaj widżet na ekran główny") }
        } else {
            Text("Przytrzymaj palec na ekranie głównym, wybierz Widżety i znajdź SportFamily.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choices(values: List<Int>, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label(v)) }, modifier = Modifier.heightIn(min = 40.dp))
        }
    }
}
