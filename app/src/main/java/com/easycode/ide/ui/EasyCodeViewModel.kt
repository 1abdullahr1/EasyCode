package com.easycode.ide.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.easycode.ide.data.model.DraftEntity
import com.easycode.ide.EasyCodeApp
import com.easycode.ide.data.model.Boilerplate
import com.easycode.ide.data.model.ConsoleMessage
import com.easycode.ide.data.model.ExternalResource
import com.easycode.ide.data.model.FiddleEntity
import com.easycode.ide.data.model.ProjectArchive
import com.easycode.ide.ui.editor.UndoRedoManager
import com.easycode.ide.util.SourceFile
import com.easycode.ide.util.NativeFiles
import com.easycode.ide.util.ProjectArchiveCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LayoutMode {
    TABS,
    SPLIT
}

class EasyCodeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as EasyCodeApp).fiddleRepository
    private val gson = Gson()
    private var draftSaveJob: Job? = null
    private val draftMutex = Mutex()
    private var ready = false

    private val _isReady = MutableLiveData(false)
    val isReady: LiveData<Boolean> = _isReady
    private val _draftStatus = MutableLiveData("")
    val draftStatus: LiveData<String> = _draftStatus

    val savedFiddles: LiveData<List<FiddleEntity>> = repository.allFiddles.asLiveData()

    private val _currentFiddleId = MutableLiveData<Long?>(null)
    val currentFiddleId: LiveData<Long?> = _currentFiddleId

    private val _title = MutableLiveData("Untitled fiddle")
    val title: LiveData<String> = _title

    private val _htmlCode = MutableLiveData("")
    val htmlCode: LiveData<String> = _htmlCode

    private val _cssCode = MutableLiveData("")
    val cssCode: LiveData<String> = _cssCode

    private val _jsCode = MutableLiveData("")
    val jsCode: LiveData<String> = _jsCode

    private val _externalResources = MutableLiveData<List<ExternalResource>>(emptyList())
    val externalResources: LiveData<List<ExternalResource>> = _externalResources

    private val _consoleMessages = MutableLiveData<List<ConsoleMessage>>(emptyList())
    val consoleMessages: LiveData<List<ConsoleMessage>> = _consoleMessages

    private val _runEvent = MutableLiveData<Long>()
    val runEvent: LiveData<Long> = _runEvent

    private val _layoutMode = MutableLiveData(LayoutMode.TABS)
    val layoutMode: LiveData<LayoutMode> = _layoutMode

    private val _projectRevision = MutableLiveData(0L)
    val projectRevision: LiveData<Long> = _projectRevision
    val editorHistory = mutableMapOf<String, UndoRedoManager>()
    val sourceFiles = mutableMapOf<String, SourceFile>()
    val fileOperations = Mutex()
    val folderUri = MutableLiveData(NativeFiles.savedFolder(application))
    var pendingFileType: String? = null
    var pendingFileRevision: Long? = null
    var pendingFileContents: String? = null
    var pendingArchiveJson: String? = null
    var isSaving = false
        private set
    private var savedSnapshot = FiddleEntity(title = "Untitled fiddle", updatedAt = 0)

    init {
        viewModelScope.launch {
            val draftResult = runCatching { withContext(Dispatchers.IO) { repository.getDraft() } }
            val draft = draftResult.getOrNull()
            if (draftResult.isFailure) _draftStatus.value = "Draft could not be loaded"
            if (draft != null) {
                val restored = runCatching {
                    gson.fromJson(draft.stateJson, FiddleEntity::class.java) to
                        gson.fromJson(draft.savedSnapshotJson, FiddleEntity::class.java)
                }.getOrNull()
                if (restored != null && restored.first != null && restored.second != null &&
                    !restored.first.title.isNullOrBlank() && !restored.second.title.isNullOrBlank() &&
                    restored.first.htmlCode != null && restored.first.cssCode != null && restored.first.jsCode != null &&
                    restored.second.htmlCode != null && restored.second.cssCode != null && restored.second.jsCode != null
                ) {
                    restoreDraft(restored.first, restored.second)
                    _draftStatus.value = "Draft restored"
                } else {
                    runCatching { withContext(Dispatchers.IO) { repository.clearDraft() } }
                    _draftStatus.value = "Draft could not be restored"
                }
            }
            ready = true
            _isReady.value = true
        }
    }

    private fun restoreDraft(state: FiddleEntity, baseline: FiddleEntity) {
        _currentFiddleId.value = state.id.takeIf { it > 0 }
        _title.value = state.title
        _htmlCode.value = state.htmlCode
        _cssCode.value = state.cssCode
        _jsCode.value = state.jsCode
        _externalResources.value = parseResources(state.resourcesJson)
        savedSnapshot = baseline.copy(resourcesJson = gson.toJson(parseResources(baseline.resourcesJson)), updatedAt = 0)
        beginProject()
        clearConsole()
        triggerRun()
    }

    private fun parseResources(json: String): List<ExternalResource> = try {
        val type = object : TypeToken<List<ExternalResource>>() {}.type
        gson.fromJson<List<ExternalResource>>(json, type)?.filter { it?.hasSafeHttpsUrl() == true } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun scheduleDraftSave() {
        if (!ready) return
        _draftStatus.value = "Saving draft"
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(700)
            draftMutex.withLock {
                val state = snapshot()
                val baseline = savedSnapshot.copy(updatedAt = 0)
                try {
                    val stateJson = withContext(Dispatchers.Default) { gson.toJson(state) }
                    val baselineJson = withContext(Dispatchers.Default) { gson.toJson(baseline) }
                    withContext(Dispatchers.IO) {
                        repository.saveDraft(DraftEntity(stateJson = stateJson, savedSnapshotJson = baselineJson))
                    }
                    _draftStatus.postValue("Draft saved")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _draftStatus.postValue("Draft save failed")
                }
            }
        }
    }

    fun saveDraftNow() {
        if (!ready) return
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            draftMutex.withLock {
                try {
                    val state = snapshot()
                    val baseline = savedSnapshot.copy(updatedAt = 0)
                    val stateJson = withContext(Dispatchers.Default) { gson.toJson(state) }
                    val baselineJson = withContext(Dispatchers.Default) { gson.toJson(baseline) }
                    withContext(Dispatchers.IO) {
                        repository.saveDraft(DraftEntity(stateJson = stateJson, savedSnapshotJson = baselineJson))
                    }
                    _draftStatus.postValue("Draft saved")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _draftStatus.postValue("Draft save failed")
                }
            }
        }
    }

    private fun beginProject() {
        editorHistory.values.forEach { it.clear() }
        sourceFiles.clear()
        _projectRevision.value = (_projectRevision.value ?: 0L) + 1L
    }

    private fun snapshot() = FiddleEntity(
        id = _currentFiddleId.value ?: 0,
        title = _title.value.orEmpty(),
        htmlCode = _htmlCode.value.orEmpty(),
        cssCode = _cssCode.value.orEmpty(),
        jsCode = _jsCode.value.orEmpty(),
        resourcesJson = gson.toJson(_externalResources.value.orEmpty()),
        updatedAt = 0,
        description = savedSnapshot.description,
        isStarred = savedSnapshot.isStarred
    )

    fun hasUnsavedChanges() = snapshot() != savedSnapshot

    fun code(type: String): String = when (type) {
        "CSS" -> _cssCode.value.orEmpty()
        "JS" -> _jsCode.value.orEmpty()
        else -> _htmlCode.value.orEmpty()
    }

    fun replaceCode(type: String, value: String) {
        when (type) {
            "CSS" -> updateCss(value)
            "JS" -> updateJs(value)
            else -> updateHtml(value)
        }
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
        scheduleDraftSave()
    }

    fun updateHtml(code: String) {
        _htmlCode.value = code
        scheduleDraftSave()
    }

    fun updateCss(code: String) {
        _cssCode.value = code
        scheduleDraftSave()
    }

    fun updateJs(code: String) {
        _jsCode.value = code
        scheduleDraftSave()
    }

    fun triggerRun() {
        _runEvent.value = System.currentTimeMillis()
    }

    fun toggleLayoutMode() {
        val current = _layoutMode.value ?: LayoutMode.TABS
        _layoutMode.value = if (current == LayoutMode.TABS) LayoutMode.SPLIT else LayoutMode.TABS
    }

    fun addConsoleMessage(message: ConsoleMessage) {
        val current = _consoleMessages.value.orEmpty().toMutableList()
        current.add(message.copy(message = message.message.take(4_000)))
        if (current.size > 300) current.subList(0, current.size - 300).clear()
        _consoleMessages.value = current
    }

    fun clearConsole() {
        _consoleMessages.value = emptyList()
    }

    fun clearAllCode() {
        _htmlCode.value = ""
        _cssCode.value = ""
        _jsCode.value = ""
        clearConsole()
        triggerRun()
        scheduleDraftSave()
    }

    fun loadBoilerplate(bp: Boilerplate) {
        beginProject()
        savedSnapshot = FiddleEntity(title = "Untitled fiddle", updatedAt = 0)
        _title.value = bp.title
        _htmlCode.value = bp.htmlCode
        _cssCode.value = bp.cssCode
        _jsCode.value = bp.jsCode
        _externalResources.value = bp.resources
        _currentFiddleId.value = null
        clearConsole()
        triggerRun()
        scheduleDraftSave()
    }

    fun createNamedFiddle(name: String, onError: () -> Unit, onComplete: (Boolean) -> Unit) {
        val title = name.trim()
        if (title.isEmpty()) return
        val project = FiddleEntity(title = title)
        val previous = snapshot()
        val revision = _projectRevision.value
        storeFiddle(project, onError) { id ->
            val canSwitch = revision == _projectRevision.value && previous == snapshot()
            if (canSwitch) loadFiddle(project.copy(id = id))
            onComplete(canSwitch)
        }
    }

    fun loadFiddle(fiddle: FiddleEntity) {
        beginProject()
        _currentFiddleId.value = fiddle.id
        _title.value = fiddle.title
        _htmlCode.value = fiddle.htmlCode
        _cssCode.value = fiddle.cssCode
        _jsCode.value = fiddle.jsCode

        val resources = parseResources(fiddle.resourcesJson)
        _externalResources.value = resources
        savedSnapshot = fiddle.copy(resourcesJson = gson.toJson(resources), updatedAt = 0)
        clearConsole()
        triggerRun()
        scheduleDraftSave()
    }

    fun saveCurrentFiddle(onError: () -> Unit, onComplete: () -> Unit) {
        val project = snapshot()
        storeFiddle(project, onError) { savedId ->
            _currentFiddleId.value = savedId
            savedSnapshot = project.copy(id = savedId, updatedAt = 0)
            _draftStatus.value = "Saved"
            scheduleDraftSave()
            onComplete()
        }
    }

    private fun storeFiddle(project: FiddleEntity, onError: () -> Unit, onComplete: (Long) -> Unit) {
        if (isSaving) return
        isSaving = true
        viewModelScope.launch {
            val savedId = try {
                repository.saveFiddle(project.copy(updatedAt = System.currentTimeMillis()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onError()
                return@launch
            } finally {
                isSaving = false
            }
            onComplete(savedId)
        }
    }

    fun deleteFiddle(id: Long) {
        if (isSaving) return
        viewModelScope.launch {
            repository.deleteFiddleById(id)
            if (_currentFiddleId.value == id) {
                _currentFiddleId.value = null
                savedSnapshot = FiddleEntity(title = "Untitled fiddle", updatedAt = 0)
                scheduleDraftSave()
            }
        }
    }

    fun addExternalResource(resource: ExternalResource) {
        if (!resource.hasSafeHttpsUrl()) return
        val current = _externalResources.value.orEmpty().toMutableList()
        if (current.none { it.url.equals(resource.url, ignoreCase = true) }) {
            current.add(resource)
            _externalResources.value = current
            triggerRun()
            scheduleDraftSave()
        }
    }

    fun removeExternalResource(id: String) {
        val current = _externalResources.value.orEmpty().toMutableList()
        current.removeAll { it.id == id }
        _externalResources.value = current
        triggerRun()
        scheduleDraftSave()
    }

    fun exportProjectArchive(): String = ProjectArchiveCodec.encode(
        title = _title.value.orEmpty(),
        html = _htmlCode.value.orEmpty(),
        css = _cssCode.value.orEmpty(),
        javascript = _jsCode.value.orEmpty(),
        resources = _externalResources.value.orEmpty()
    )

    fun importProjectArchive(archive: ProjectArchive) {
        beginProject()
        _currentFiddleId.value = null
        _title.value = archive.title
        _htmlCode.value = archive.html
        _cssCode.value = archive.css
        _jsCode.value = archive.javascript
        _externalResources.value = archive.resources
        savedSnapshot = FiddleEntity(title = "Untitled fiddle", updatedAt = 0)
        clearConsole()
        triggerRun()
        scheduleDraftSave()
    }
}
