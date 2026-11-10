package pl.sportfamily.app.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import pl.sportfamily.app.ui.theme.Sf

/** Pasek u góry ekranu, widoczny tylko gdy jest nowsza wersja. Sprawdza przy każdym powrocie do aplikacji (najwyżej co 6 h). */
@Composable
fun UpdateBanner() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(-1f) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastCheck by remember { mutableStateOf(0L) }

    LifecycleResumeEffect(Unit) {
        val now = System.currentTimeMillis()
        if (Updater.enabled && now - lastCheck > 6 * 3600_000L) {
            lastCheck = now
            scope.launch { info = Updater.check() }
        }
        onPauseOrDispose { }
    }

    val tone = Sf.colors.sport
    AnimatedVisibility(visible = info != null && !dismissed) {
        val u = info ?: return@AnimatedVisibility
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tone.container)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = tone.onContainer)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Nowa wersja ${u.versionName}", style = MaterialTheme.typography.labelLarge, color = tone.onContainer)
                    Text(error ?: u.notes.ifBlank { "Gotowa do instalacji" }, style = MaterialTheme.typography.bodySmall, color = tone.onContainer)
                }
            }
            if (progress >= 0f) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            } else {
                Row(Modifier.padding(top = 6.dp)) {
                    TextButton(onClick = { dismissed = true }) { Text("Później") }
                    Button(onClick = {
                        error = null
                        if (!Updater.canInstall(context)) {
                            error = "Zezwól SportFamily na instalowanie aplikacji, potem wróć i stuknij Zainstaluj."
                            Updater.openInstallPermission(context)
                            return@Button
                        }
                        progress = 0f
                        scope.launch {
                            runCatching { Updater.downloadApk(context, u) { progress = it } }
                                .onSuccess { progress = -1f; Updater.install(context, it) }
                                .onFailure { progress = -1f; error = "Nie udało się pobrać: ${it.message}" }
                        }
                    }) { Text("Zainstaluj") }
                }
            }
        }
    }
}

/** Ręczne sprawdzenie, używane w zakładce Więcej. */
@Composable
fun rememberUpdateCheck(): Pair<String?, () -> Unit> {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { }
    return status to {
        status = "Sprawdzam..."
        scope.launch {
            status = if (!Updater.enabled) "Aktualizacje nie są jeszcze skonfigurowane"
            else Updater.check()?.let { "Dostępna wersja ${it.versionName}. Wróć do ekranu Dzisiaj, żeby zainstalować." }
                ?: "Masz najnowszą wersję"
        }
    }
}
