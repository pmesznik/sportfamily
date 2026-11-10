package pl.sportfamily.app.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
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

/** Pasek u góry ekranu, widoczny tylko gdy jest nowsza wersja. Sprawdza przy powrocie do aplikacji, najwyżej co godzinę. */
@Composable
fun UpdateBanner() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val info by Updater.available.collectAsState()
    var dismissedCode by remember { mutableStateOf(-1) }
    var progress by remember { mutableFloatStateOf(-1f) }
    var error by remember { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        if (Updater.enabled) scope.launch { Updater.refresh() }
        onPauseOrDispose { }
    }

    val tone = Sf.colors.brand
    AnimatedVisibility(visible = info != null && info?.versionCode != dismissedCode) {
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
                    TextButton(onClick = { dismissedCode = u.versionCode }) { Text("Później") }
                    Button(onClick = {
                        error = null
                        progress = 0f
                        scope.launch {
                            runCatching { Updater.downloadApk(context, u) { progress = it } }
                                .onSuccess { apk ->
                                    progress = -1f
                                    // Android sam zapyta o zgodę na instalację z tego źródła, jeśli jej jeszcze nie ma.
                                    runCatching { Updater.install(context, apk) }.onFailure {
                                        error = "Zezwól SportFamily na instalowanie aplikacji i spróbuj ponownie."
                                        Updater.openInstallPermission(context)
                                    }
                                }
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
    return status to {
        status = "Sprawdzam..."
        scope.launch {
            status = if (!Updater.enabled) "Aktualizacje nie są skonfigurowane"
            else Updater.refresh(force = true)?.let { "Dostępna wersja ${it.versionName}. Pasek instalacji jest u góry ekranu." }
                ?: "Masz najnowszą wersję"
        }
    }
}
