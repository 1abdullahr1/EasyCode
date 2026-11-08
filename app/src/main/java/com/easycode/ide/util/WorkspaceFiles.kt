package com.easycode.ide.util

import android.net.Uri
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.easycode.ide.MainActivity
import com.easycode.ide.ui.EasyCodeViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WorkspaceFiles(
    private val activity: AppCompatActivity,
    private val model: EasyCodeViewModel,
    private val activeType: () -> String?,
    private val selectEditor: (String) -> Unit,
    private val shareFiddle: () -> Unit
) {
    private val resolver get() = activity.contentResolver
    private val chooseFolder = activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            io("Could not connect the folder.") {
                val persisted = withContext(Dispatchers.IO) { NativeFiles.rememberFolder(activity, uri) }
                model.folderUri.value = uri
                message(if (persisted) "Folder connected" else "Folder connected for this session")
            }
        }
    }
    private val openDocument = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val revision = model.pendingFileRevision
        model.pendingFileRevision = null
        if (uri != null && revision != null) open(uri, revision)
    }
    private val importArchivePicker = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val revision = model.pendingFileRevision
        model.pendingFileRevision = null
        if (uri != null && revision != null) importArchive(uri, revision)
    }
    private val exportArchivePicker = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = model.pendingArchiveJson
        model.pendingArchiveJson = null
        if (uri != null && json != null) {
            io("Could not export this project backup.") {
                withContext(Dispatchers.IO) { NativeFiles.write(resolver, uri, json) }
                message("Project backup exported")
            }
        }
    }
    private val createPickers = listOf("HTML", "CSS", "JS").associateWith { type ->
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument(NativeFiles.mime(type))) { uri ->
            val contents = model.pendingFileContents
            val revision = model.pendingFileRevision
            val link = model.pendingFileType == type
            model.pendingFileContents = null
            model.pendingFileRevision = null
            model.pendingFileType = null
            if (uri != null && contents != null && revision != null) {
                io("Could not save the file. Your editor contents are still available.") {
                    val file = withContext(Dispatchers.IO) {
                        NativeFiles.write(resolver, uri, contents)
                        SourceFile(uri, NativeFiles.name(resolver, uri), contents)
                    }
                    if (link && model.projectRevision.value == revision) model.sourceFiles[type] = file
                    message("Saved ${file.name}")
                }
            }
        }
    }

    fun pickFolder() = chooseFolder.launch(model.folderUri.value)

    fun disconnectFolder() {
        NativeFiles.disconnect(activity)
        model.folderUri.value = null
        model.sourceFiles.entries.removeAll { android.provider.DocumentsContract.isTreeUri(it.value.uri) }
    }

    fun showMenu() {
        val type = activeType()
        val actions = mutableListOf<Pair<String, () -> Unit>>(
            "Choose folder" to ::pickFolder,
            "Open file" to {
                model.pendingFileRevision = model.projectRevision.value
                openDocument.launch(arrayOf("*/*"))
            },
            "New file" to ::chooseNewType
        )
        if (model.folderUri.value != null) actions.add(1, "Browse selected folder" to ::browseFolder)
        if (type != null) {
            model.sourceFiles[type]?.let { file ->
                actions.add("Save changes to ${file.name}" to { saveBack(type, file) })
            }
            actions.add("Save $type as file" to { create(type, model.code(type), link = true) })
            actions.add("Share $type file" to {
                val name = model.sourceFiles[type]?.name ?: model.title.value.orEmpty()
                FileUtils.shareSource(activity, name, type, model.code(type))
            })
        }
        actions.add("Share runnable HTML" to shareFiddle)
        actions.add("Export project backup" to ::exportArchive)
        actions.add("Import project backup" to {
            model.pendingFileRevision = model.projectRevision.value
            importArchivePicker.launch(arrayOf("application/json", "text/plain"))
        })
        AlertDialog.Builder(activity).setTitle("Files")
            .setItems(actions.map { it.first }.toTypedArray()) { _, index -> actions[index].second() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun exportArchive() {
        val json = try {
            model.exportProjectArchive()
        } catch (error: Exception) {
            message(error.message ?: "Could not prepare the project backup.")
            return
        }
        model.pendingArchiveJson = json
        val base = model.title.value.orEmpty().replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            .take(80).ifBlank { "easycode-project" }
        exportArchivePicker.launch("$base.easycode.json")
    }

    private fun importArchive(uri: Uri, revision: Long) {
        activity.lifecycleScope.launch {
            try {
                val archive = model.fileOperations.withLock {
                    val json = withContext(Dispatchers.IO) {
                        NativeFiles.read(resolver, uri, ProjectArchiveCodec.MAX_ARCHIVE_BYTES)
                    }
                    withContext(Dispatchers.Default) { ProjectArchiveCodec.decode(json) }
                }
                if (model.projectRevision.value != revision) {
                    message("The current project changed. Choose the backup again to import it.")
                    return@launch
                }
                val applyImport = {
                    if (model.projectRevision.value == revision) {
                        model.importProjectArchive(archive)
                        message("Imported ${archive.title}")
                    }
                }
                val main = activity as? MainActivity
                if (main != null) main.confirmProjectChange(applyImport) else applyImport()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val reason = if (error is IllegalArgumentException || error is IllegalStateException) error.message else null
                message(reason ?: "Could not import this project backup.")
            }
        }
    }

    private fun browseFolder() {
        val tree = model.folderUri.value ?: return
        val revision = model.projectRevision.value ?: return
        io("Could not read the folder. Choose it again if access has changed.") {
            val files = withContext(Dispatchers.IO) {
                NativeFiles.children(resolver, tree).filter { NativeFiles.type(it.second) != null }
            }
            if (files.isEmpty()) {
                message("No HTML, CSS, or JavaScript files in this folder")
            } else {
                AlertDialog.Builder(activity).setTitle("Open from selected folder")
                    .setItems(files.map { it.second }.toTypedArray()) { _, index -> open(files[index].first, revision) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun open(uri: Uri, revision: Long) {
        io("Could not open this source file.") {
            val (type, file) = withContext(Dispatchers.IO) {
                val name = NativeFiles.name(resolver, uri)
                val language = NativeFiles.type(name) ?: error("Choose an HTML, CSS, or JavaScript file.")
                language to SourceFile(uri, name, NativeFiles.read(resolver, uri))
            }
            if (model.projectRevision.value != revision) return@io
            val applyFile: () -> Unit = {
                if (model.projectRevision.value == revision) {
                    model.sourceFiles[type] = file
                    model.replaceCode(type, file.contents.removePrefix("\uFEFF"))
                    selectEditor(type)
                    message("Opened ${file.name}")
                }
            }
            if (model.code(type).isEmpty()) applyFile() else {
                AlertDialog.Builder(activity).setTitle("Replace $type code?")
                    .setMessage("Open ${file.name} in this editor. You can undo the replacement.")
                    .setPositiveButton("Replace") { _, _ -> applyFile() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun chooseNewType() {
        val types = arrayOf("HTML", "CSS", "JS")
        AlertDialog.Builder(activity).setTitle("New source file")
            .setItems(arrayOf("HTML", "CSS", "JavaScript")) { _, index -> create(types[index], "", link = false) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun create(type: String, contents: String, link: Boolean) {
        val tree = model.folderUri.value
        val extension = NativeFiles.extension(type)
        val revision = model.projectRevision.value ?: return
        val suggested = model.title.value.orEmpty().replace(Regex("[/\\\\]"), "_").take(80).ifBlank { "source" }
        val input = EditText(activity).apply {
            setSingleLine(true)
            setText("$suggested.$extension")
            selectAll()
        }
        val dialog = AlertDialog.Builder(activity).setTitle(if (link) "Save as" else "New file")
            .setView(input).setNegativeButton("Cancel", null)
            .setPositiveButton(if (link) "Save" else "Create", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isBlank() || name.length > 120 || '/' in name || '\\' in name ||
                    name in listOf(".", "..") || name.any { it.isISOControl() }) {
                    input.error = "Enter a valid file name"
                    return@setOnClickListener
                }
                val filename = if (NativeFiles.type(name) == type) name else "$name.$extension"
                dialog.dismiss()
                if (tree == null) {
                    model.pendingFileRevision = revision
                    model.pendingFileContents = contents
                    model.pendingFileType = if (link) type else null
                    createPickers.getValue(type).launch(filename)
                } else {
                    io("Could not create the file. Your editor contents are still available.") {
                        val file = withContext(Dispatchers.IO) {
                            val uri = NativeFiles.create(resolver, tree, filename, NativeFiles.mime(type))
                            if (contents.isNotEmpty()) NativeFiles.write(resolver, uri, contents)
                            SourceFile(uri, filename, contents)
                        }
                        if (link && model.projectRevision.value == revision) model.sourceFiles[type] = file
                        message("Saved $filename")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun saveBack(type: String, file: SourceFile) {
        val revision = model.projectRevision.value
        val contents = model.code(type)
        io("Could not save the original file. Use Save as to keep a copy.") {
            withContext(Dispatchers.IO) {
                check(NativeFiles.read(resolver, file.uri) == file.contents) {
                    "The original file changed outside EasyCode. Use Save as to keep both copies."
                }
                NativeFiles.write(resolver, file.uri, contents)
            }
            if (model.projectRevision.value == revision && model.sourceFiles[type] == file) {
                model.sourceFiles[type] = file.copy(contents = contents)
            }
            message("Saved ${file.name}")
        }
    }

    private fun io(fallback: String, action: suspend () -> Unit) {
        activity.lifecycleScope.launch {
            try {
                model.fileOperations.withLock { action() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val reason = if (error is IllegalArgumentException || error is IllegalStateException) error.message else null
                message(reason ?: fallback)
            }
        }
    }

    private fun message(text: String) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
}
