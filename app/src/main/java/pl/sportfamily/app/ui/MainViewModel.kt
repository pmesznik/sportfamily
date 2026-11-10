package pl.sportfamily.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import pl.sportfamily.app.data.xlsx.Xlsx
import pl.sportfamily.app.domain.ImportOptions
import pl.sportfamily.app.domain.ParsedPlan
import pl.sportfamily.app.domain.PlanExport
import pl.sportfamily.app.domain.PlanImport
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.sportfamily.app.SportFamilyApp
import pl.sportfamily.app.data.ChecklistItem
import pl.sportfamily.app.data.Child
import pl.sportfamily.app.data.EventEntity
import pl.sportfamily.app.data.Person
import pl.sportfamily.app.data.TaskEntity
import pl.sportfamily.app.domain.PlannerData
import java.time.LocalDate
import java.time.LocalDateTime

/** null w stanie oznacza "jeszcze się ładuje". */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    val repo = (app as SportFamilyApp).repository

    val data: StateFlow<PlannerData?> = repo.data
        .map<PlannerData, PlannerData?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Bieżący czas, odświeżany co 30 s, żeby oś dnia i odliczanie były aktualne. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now().withSecond(0).withNano(0))
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now())

    fun toggleCheck(itemId: Long, day: LocalDate) = viewModelScope.launch { repo.toggleCheck(itemId, day) }
    fun cancelOccurrence(eventId: Long, day: LocalDate) = viewModelScope.launch { repo.cancelOccurrence(eventId, day) }
    fun deleteEvent(eventId: Long) = viewModelScope.launch { repo.deleteEvent(eventId) }

    fun saveChild(child: Child) = viewModelScope.launch { repo.saveChild(child) }
    fun deleteChild(child: Child) = viewModelScope.launch { repo.deleteChild(child) }
    fun savePerson(person: Person) = viewModelScope.launch { repo.savePerson(person) }
    fun deletePerson(person: Person) = viewModelScope.launch { repo.deletePerson(person) }

    fun saveTask(task: TaskEntity) = viewModelScope.launch { repo.saveTask(task) }
    fun deleteTask(task: TaskEntity) = viewModelScope.launch { repo.deleteTask(task) }
    fun toggleTask(task: TaskEntity) = saveTask(task.copy(done = !task.done))

    fun seedDemo() = viewModelScope.launch { repo.seedDemo() }

    // ---------- Import i eksport planu z Excela ----------

    /** Wczytany plan czekający na zatwierdzenie w podglądzie. */
    val importDraft = MutableStateFlow<ImportDraft?>(null)
    /** Krótki komunikat dla użytkownika (wynik importu lub eksportu). */
    val message = MutableStateFlow<String?>(null)

    fun readImport(uri: Uri) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val sheets = ctx.contentResolver.openInputStream(uri)!!.use { Xlsx.read(it) }
                val parsed = sheets.map { it.name to PlanImport.parse(it.rows) }
                parsed.maxByOrNull { (_, p) -> p.sections.sumOf { it.items.size } }
                    ?: error("Plik nie ma żadnego arkusza")
            }
        }
        result.onSuccess { (sheet, plan) ->
            if (plan.sections.isEmpty()) message.value = plan.warnings.firstOrNull() ?: "W pliku nie ma planu do zaimportowania."
            else importDraft.value = ImportDraft(plan, sheet)
        }.onFailure { message.value = "Nie udało się odczytać pliku: ${it.message ?: "nieznany błąd"}" }
    }

    fun confirmImport(options: ImportOptions, replace: Boolean) = viewModelScope.launch {
        val plan = importDraft.value?.plan ?: return@launch
        val events = PlanImport.toEvents(plan, options).map { it.event to it.childIds }
        val n = repo.importPlan(options.childId, replace, events)
        importDraft.value = null
        message.value = "Zaimportowano $n zajęć do kalendarza."
    }

    fun export(uri: Uri) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        val d = data.value ?: return@launch
        val r = withContext(Dispatchers.IO) {
            runCatching { ctx.contentResolver.openOutputStream(uri, "wt")!!.use { Xlsx.write(it, PlanExport.sheets(d, LocalDate.now())) } }
        }
        message.value = if (r.isSuccess) "Plan zapisany do pliku Excel." else "Nie udało się zapisać pliku: ${r.exceptionOrNull()?.message}"
    }

    suspend fun loadEvent(id: Long): EventDraftSource? {
        val e = repo.getEvent(id) ?: return null
        return EventDraftSource(e, repo.getEventChildIds(id), repo.getChecklist(id))
    }

    fun saveEvent(event: EventEntity, childIds: List<Long>, checklist: List<ChecklistItem>) =
        viewModelScope.launch { repo.saveEvent(event, childIds, checklist) }
}

data class EventDraftSource(val event: EventEntity, val childIds: List<Long>, val checklist: List<ChecklistItem>)

data class ImportDraft(val plan: ParsedPlan, val sheetName: String)
