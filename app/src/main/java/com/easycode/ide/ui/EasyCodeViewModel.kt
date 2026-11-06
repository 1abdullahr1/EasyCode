package com.easycode.ide.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.easycode.ide.EasyCodeApp
import com.easycode.ide.data.model.Boilerplate
import com.easycode.ide.data.model.ConsoleMessage
import com.easycode.ide.data.model.ExternalResource
import com.easycode.ide.data.model.FiddleEntity
import com.easycode.ide.ui.editor.UndoRedoManager
import com.easycode.ide.util.SourceFile
import com.easycode.ide.util.NativeFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

enum class LayoutMode {
    TABS,
    SPLIT
}

class EasyCodeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as EasyCodeApp).fiddleRepository
    private val gson = Gson()

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
    var isSaving = false
        private set
    private var savedSnapshot = FiddleEntity(title = "Untitled fiddle", updatedAt = 0)

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
    }

    fun updateHtml(code: String) {
        _htmlCode.value = code
    }

    fun updateCss(code: String) {
        _cssCode.value = code
    }

    fun updateJs(code: String) {
        _jsCode.value = code
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
        current.add(message)
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

        val resources: List<ExternalResource> = try {
            val type = object : TypeToken<List<ExternalResource>>() {}.type
            gson.fromJson(fiddle.resourcesJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        _externalResources.value = resources
        savedSnapshot = fiddle.copy(resourcesJson = gson.toJson(resources), updatedAt = 0)
        clearConsole()
        triggerRun()
    }

    fun saveCurrentFiddle(onError: () -> Unit, onComplete: () -> Unit) {
        val project = snapshot()
        storeFiddle(project, onError) { savedId ->
            _currentFiddleId.value = savedId
            savedSnapshot = project.copy(id = savedId)
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
            }
        }
    }

    fun addExternalResource(resource: ExternalResource) {
        val current = _externalResources.value.orEmpty().toMutableList()
        if (current.none { it.url.equals(resource.url, ignoreCase = true) }) {
            current.add(resource)
            _externalResources.value = current
            triggerRun()
        }
    }

    fun removeExternalResource(id: String) {
        val current = _externalResources.value.orEmpty().toMutableList()
        current.removeAll { it.id == id }
        _externalResources.value = current
        triggerRun()
    }
}
