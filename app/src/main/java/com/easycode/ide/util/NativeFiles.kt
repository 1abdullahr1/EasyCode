package com.easycode.ide.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream

data class SourceFile(val uri: Uri, val name: String, val contents: String)

object NativeFiles {
    private const val MAX_IMPORT = 512 * 1024
    private const val FOLDER = "folder_uri"

    fun preferences(context: Context) = context.getSharedPreferences("workspace_files", Context.MODE_PRIVATE)
    fun savedFolder(context: Context): Uri? = preferences(context).getString(FOLDER, null)?.let(Uri::parse)

    fun rememberFolder(context: Context, uri: Uri): Boolean {
        val resolver = context.contentResolver
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val write = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val persisted = runCatching { resolver.takePersistableUriPermission(uri, read or write) }.isSuccess ||
            runCatching { resolver.takePersistableUriPermission(uri, read) }.isSuccess
        val previous = savedFolder(context)
        if (previous != uri) releaseFolder(resolver, previous)
        preferences(context).edit().putString(FOLDER, if (persisted) uri.toString() else null).apply()
        return persisted
    }

    fun disconnect(context: Context) {
        releaseFolder(context.contentResolver, savedFolder(context))
        preferences(context).edit().remove(FOLDER).apply()
    }

    private fun releaseFolder(resolver: ContentResolver, uri: Uri?) {
        val grant = resolver.persistedUriPermissions.firstOrNull { it.uri == uri } ?: return
        val flags = (if (grant.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
            (if (grant.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
        runCatching { resolver.releasePersistableUriPermission(grant.uri, flags) }
    }

    fun name(resolver: ContentResolver, uri: Uri): String =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "Selected file"

    fun folderName(resolver: ContentResolver, tree: Uri): String = name(resolver, directory(tree))

    private fun directory(tree: Uri) =
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    fun children(resolver: ContentResolver, tree: Uri): List<Pair<Uri, String>> {
        val base = DocumentsContract.getTreeDocumentId(tree)
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, base)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return resolver.query(uri, columns, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)) to cursor.getString(1))
                }
            }.sortedBy { it.second.lowercase() }
        } ?: error("Could not read the selected folder.")
    }

    fun read(resolver: ContentResolver, uri: Uri, maxBytes: Int = MAX_IMPORT): String {
        require(maxBytes > 0) { "Invalid import size limit." }
        val stream = resolver.openInputStream(uri) ?: error("Could not open the file.")
        return stream.use {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = it.read(buffer)
                if (count == -1) break
                require(output.size() + count <= maxBytes) { "This file is larger than the supported import limit." }
                output.write(buffer, 0, count)
            }
            val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(output.toByteArray())).toString()
            require('\u0000' !in text) { "Choose a plain-text source file." }
            text
        }
    }

    fun write(resolver: ContentResolver, uri: Uri, text: String) {
        val stream = resolver.openOutputStream(uri, "wt") ?: error("Could not write the file.")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    fun create(resolver: ContentResolver, folder: Uri, name: String, mime: String): Uri {
        require(children(resolver, folder).none { it.second.equals(name, ignoreCase = true) }) {
            "A file with this name already exists. Choose another name."
        }
        return DocumentsContract.createDocument(resolver, directory(folder), mime, name)
            ?: error("Could not create the file in this folder.")
    }

    fun type(name: String): String? = when (name.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "HTML"
        "css" -> "CSS"
        "js", "mjs" -> "JS"
        else -> null
    }

    fun extension(type: String) = type.lowercase(java.util.Locale.ROOT)
    fun mime(type: String) = when (type) {
        "HTML" -> "text/html"
        "CSS" -> "text/css"
        else -> "application/javascript"
    }
}
