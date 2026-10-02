package com.easycode.ide.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun exportAndShareHtml(context: Context, filename: String, htmlContent: String) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val sanitizedFilename = filename.replace(Regex("[^a-zA-Z0-9_.-]"), "_").ifBlank { "easycode_project" } + ".html"
            val file = File(exportDir, sanitizedFilename)

            FileOutputStream(file).use { fos ->
                fos.write(htmlContent.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, filename)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share / Export HTML"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
