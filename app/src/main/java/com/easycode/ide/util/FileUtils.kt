package com.easycode.ide.util

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun exportAndShareHtml(context: Context, filename: String, htmlContent: String) =
        shareSource(context, filename, "HTML", htmlContent)

    fun shareSource(context: Context, filename: String, type: String, contents: String) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val extension = NativeFiles.extension(type)
            val base = filename.removeSuffix(".$extension").replace(Regex("[^a-zA-Z0-9_.-]"), "_")
                .take(80).ifBlank { "easycode_project" }
            val file = File.createTempFile("source-", ".$extension", exportDir)
            val namedFile = File(exportDir, file.nameWithoutExtension + "-$base.$extension")
            if (!file.renameTo(namedFile)) {
                file.delete()
                error("Could not prepare shared file")
            }

            FileOutputStream(namedFile).use { fos ->
                fos.write(contents.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                namedFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = NativeFiles.mime(type)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, filename)
                clipData = ClipData.newRawUri(filename, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share file"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share the file. Try again or choose another app.", Toast.LENGTH_LONG).show()
        }
    }
}
