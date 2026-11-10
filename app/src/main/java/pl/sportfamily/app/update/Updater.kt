package pl.sportfamily.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pl.sportfamily.app.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Auto-aktualizacja bez sklepu Play.
 * Aplikacja pobiera mały plik update.json (BuildConfig.UPDATE_MANIFEST_URL):
 * { "versionCode": 2, "versionName": "0.2", "apkUrl": "https://...", "notes": "Co nowego" }
 * Gdy versionCode jest większy od zainstalowanego, pobiera APK i otwiera systemowy instalator.
 * Wysyłane jest tylko zapytanie o ten plik, żadne dane z aplikacji nie opuszczają telefonu.
 */
data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String, val notes: String)

object Updater {
    val enabled: Boolean get() = BuildConfig.UPDATE_MANIFEST_URL.isNotBlank()

    /** Wspólny stan: pasek na ekranie i ręczne sprawdzenie w "Więcej" widzą to samo. */
    val available = kotlinx.coroutines.flow.MutableStateFlow<UpdateInfo?>(null)
    private var lastCheck = 0L

    /** Sprawdza, jeśli od ostatniego razu minęła godzina (albo zawsze, gdy force). Wynik trafia do [available]. */
    suspend fun refresh(force: Boolean = false): UpdateInfo? {
        val now = System.currentTimeMillis()
        if (!force && now - lastCheck < 3600_000L) return available.value
        lastCheck = now
        val info = check()
        available.value = info
        return info
    }

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        runCatching {
            val json = JSONObject(download(BuildConfig.UPDATE_MANIFEST_URL).decodeToString())
            UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.optString("versionName"),
                apkUrl = json.getString("apkUrl"),
                notes = json.optString("notes"),
            )
        }.getOrNull()?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
    }

    /** Pobiera APK do pamięci podręcznej. Zwraca plik albo rzuca wyjątek. */
    suspend fun downloadApk(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "sportfamily-${info.versionCode}.apk")
        val conn = open(info.apkUrl)
        try {
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(read / total.toFloat())
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        // Zabezpieczenie: Google Drive potrafi zwrócić stronę HTML zamiast pliku.
        val head = out.inputStream().use { s -> ByteArray(2).also { s.read(it) } }
        check(head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) { "Pobrany plik nie jest aplikacją (APK)" }
        out
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Otwiera ustawienie "Instaluj nieznane aplikacje" dla SportFamily. */
    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun download(url: String): ByteArray {
        val conn = open(url)
        return try { conn.inputStream.use { it.readBytes() } } finally { conn.disconnect() }
    }

    /** Otwiera połączenie i ręcznie podąża za przekierowaniami (GitHub i Google Drive przekierowują między domenami). */
    private fun open(url: String): HttpURLConnection {
        var current = url
        repeat(6) {
            val c = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "SportFamily-Updater")
            }
            when (c.responseCode) {
                in 300..399 -> {
                    val loc = c.getHeaderField("Location") ?: error("Przekierowanie bez adresu")
                    c.disconnect()
                    current = URL(URL(current), loc).toString()
                }
                in 200..299 -> return c
                else -> { val code = c.responseCode; c.disconnect(); error("Serwer odpowiedział kodem $code") }
            }
        }
        error("Zbyt wiele przekierowań")
    }
}
