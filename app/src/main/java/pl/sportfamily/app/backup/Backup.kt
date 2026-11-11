package pl.sportfamily.app.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pl.sportfamily.app.BuildConfig
import pl.sportfamily.app.SportFamilyApp
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Jak często robić kopię. */
enum class BackupFrequency(val label: String, val days: Long) {
    OFF("Wyłączona", 0), DAILY("Codziennie", 1), EVERY_3_DAYS("Co 3 dni", 3), WEEKLY("Co tydzień", 7)
}

data class BackupState(
    val target: Uri? = null,
    val targetName: String = "",
    val frequency: BackupFrequency = BackupFrequency.OFF,
    val lastOk: Long = 0,
    val lastError: String? = null,
)

/**
 * Kopia zapasowa: plik ZIP z bazą danych zapisywany do miejsca wybranego przez użytkownika
 * (np. folder na Dysku Google). Ten sam plik jest nadpisywany przy każdej kopii.
 */
object Backup {
    const val FILE_NAME = "SportFamily_kopia.zip"
    private const val DB = "sportfamily.db"
    private const val PREFS = "backup"
    private const val WORK = "sportfamily-backup"

    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state

    fun load(context: Context): BackupState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val s = BackupState(
            target = p.getString("target", null)?.let(Uri::parse),
            targetName = p.getString("targetName", "").orEmpty(),
            frequency = BackupFrequency.entries.firstOrNull { it.name == p.getString("frequency", null) } ?: BackupFrequency.OFF,
            lastOk = p.getLong("lastOk", 0),
            lastError = p.getString("lastError", null),
        )
        _state.value = s
        return s
    }

    private fun save(context: Context, s: BackupState) {
        _state.value = s
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("target", s.target?.toString()).putString("targetName", s.targetName)
            .putString("frequency", s.frequency.name).putLong("lastOk", s.lastOk).putString("lastError", s.lastError)
            .apply()
    }

    /** Zapamiętuje miejsce kopii z prawem zapisu na stałe. Zwraca komunikat błędu albo null. */
    fun setTarget(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val s = load(context)
            // Poprzednie miejsce zwalniamy, żeby nie zbierać uprawnień.
            s.target?.takeIf { it != uri }?.let { old ->
                runCatching { context.contentResolver.releasePersistableUriPermission(old, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            }
            save(context, s.copy(target = uri, targetName = describe(context, uri), lastError = null))
            null
        } catch (e: SecurityException) {
            "To miejsce nie pozwala na automatyczny zapis. Wybierz inny folder (np. na Dysku Google albo w pamięci telefonu)."
        }
    }

    fun setFrequency(context: Context, f: BackupFrequency) {
        save(context, load(context).copy(frequency = f))
        schedule(context)
    }

    /** Ustawia albo usuwa cykliczne zadanie kopii. */
    fun schedule(context: Context) {
        val s = load(context)
        val wm = WorkManager.getInstance(context)
        if (s.frequency == BackupFrequency.OFF || s.target == null) { wm.cancelUniqueWork(WORK); return }
        val req = PeriodicWorkRequestBuilder<BackupWorker>(s.frequency.days, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** Kopia teraz do zapamiętanego miejsca. Zwraca komunikat błędu albo null. */
    suspend fun backupNow(context: Context): String? = withContext(Dispatchers.IO) {
        val s = load(context)
        val target = s.target ?: return@withContext "Najpierw wybierz miejsce kopii."
        val err = runCatching {
            val out = runCatching { context.contentResolver.openOutputStream(target, "wt") }.getOrNull()
                ?: context.contentResolver.openOutputStream(target, "w")
                ?: error("Nie można otworzyć pliku kopii")
            out.use { write(context, it) }
        }.exceptionOrNull()?.let { "Kopia nie powiodła się: ${it.message ?: it.javaClass.simpleName}" }
        save(context, if (err == null) s.copy(lastOk = System.currentTimeMillis(), lastError = null) else s.copy(lastError = err))
        err
    }

    /** Zapisuje ZIP: baza danych i krótki opis kopii. */
    fun write(context: Context, out: OutputStream) {
        val app = context.applicationContext as SportFamilyApp
        val db = app.database.openHelper.writableDatabase
        // Wszystkie zmiany z dziennika WAL do głównego pliku, żeby kopia była kompletna.
        db.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val file = context.getDatabasePath(DB)
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(DB))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("info.json"))
            val info = JSONObject()
                .put("app", "SportFamily Kalendarz")
                .put("version", BuildConfig.VERSION_NAME)
                .put("dbVersion", db.version)
                .put("created", System.currentTimeMillis())
            zip.write(info.toString(2).toByteArray())
            zip.closeEntry()
        }
    }

    /**
     * Przywraca dane z kopii: sprawdza plik, zastępuje bazę i uruchamia aplikację od nowa.
     * Zwraca komunikat błędu albo nie wraca (restart).
     */
    suspend fun restore(context: Context, input: InputStream): String? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as SportFamilyApp
        val tmp = File(context.cacheDir, "restore.db")
        try {
            var found = false
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { e ->
                    if (e.name == DB) { tmp.outputStream().use { zip.copyTo(it) }; found = true }
                }
            }
            if (!found) return@withContext "To nie jest plik kopii SportFamily."
            val current = app.database.openHelper.readableDatabase.version
            val check = SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY)
            val ok = check.use { d ->
                val v = d.version
                if (v > current) return@withContext "Kopia pochodzi z nowszej wersji aplikacji. Najpierw zaktualizuj aplikację."
                d.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='events'", null).use { c -> c.moveToFirst() && c.getInt(0) == 1 }
            }
            if (!ok) return@withContext "Plik kopii jest uszkodzony."

            // Zamykamy bazę, podmieniamy pliki i startujemy od nowa (starsza kopia przejdzie migracje).
            app.database.close()
            val dbFile = context.getDatabasePath(DB)
            listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
            tmp.copyTo(dbFile, overwrite = true)
            tmp.delete()
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(launch)
            Runtime.getRuntime().exit(0)
            null
        } catch (e: Exception) {
            tmp.delete()
            "Nie udało się przywrócić kopii: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    /** Czytelna nazwa miejsca, np. "Dysk Google: SportFamily_kopia.zip". */
    private fun describe(context: Context, uri: Uri): String {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: FILE_NAME
        val where = when {
            "com.google.android.apps.docs" in (uri.authority ?: "") -> "Dysk Google"
            "externalstorage" in (uri.authority ?: "") -> "Pamięć telefonu"
            "downloads" in (uri.authority ?: "") -> "Pobrane"
            else -> uri.authority ?: ""
        }
        return "$where: $name"
    }
}

/** Cykliczna kopia w tle (WorkManager uruchamia ją nawet po restarcie telefonu). */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val err = Backup.backupNow(applicationContext)
        return when {
            err == null -> Result.success()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }
}
