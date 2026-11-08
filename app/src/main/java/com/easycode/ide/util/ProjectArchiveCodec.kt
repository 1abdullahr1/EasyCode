package com.easycode.ide.util

import com.easycode.ide.data.model.ExternalResource
import com.easycode.ide.data.model.ProjectArchive
import com.google.gson.Gson
import com.google.gson.JsonParser

object ProjectArchiveCodec {
    private val gson = Gson()
    const val MAX_ARCHIVE_BYTES = 2 * 1024 * 1024
    private const val MAX_RESOURCES = 50
    private const val MAX_SOURCE_LENGTH = MAX_ARCHIVE_BYTES

    fun encode(title: String, html: String, css: String, javascript: String, resources: List<ExternalResource>): String {
        val archive = ProjectArchive(
            title = title.take(120).ifBlank { "Untitled fiddle" },
            html = html,
            css = css,
            javascript = javascript,
            resources = resources.filter { it.hasSafeHttpsUrl() }.take(MAX_RESOURCES)
        )
        val json = gson.toJson(archive)
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_ARCHIVE_BYTES) { "This project is too large to export (maximum 2 MB)." }
        return json
    }

    fun decode(json: String): ProjectArchive {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_ARCHIVE_BYTES) { "Choose a project backup smaller than 2 MB." }
        val root = try {
            JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject
        } catch (_: Exception) {
            null
        } ?: error("This file is not a valid EasyCode project backup.")
        fun requiredString(key: String): String = root.get(key)
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString ?: error("This project backup is missing a valid $key field.")
        val version = root.get("schemaVersion")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asInt ?: error("This project backup is missing its version.")
        require(root.get("resources")?.isJsonArray == true) { "This project backup has an invalid resources field." }
        val archive = try {
            gson.fromJson(json, ProjectArchive::class.java)
        } catch (_: Exception) {
            null
        } ?: error("This file is not a valid EasyCode project backup.")

        require(version == ProjectArchive.CURRENT_VERSION && archive.schemaVersion == version) { "This project backup version is not supported." }
        val title = requiredString("title")
        val html = requiredString("html")
        val css = requiredString("css")
        val javascript = requiredString("javascript")
        require(title.isNotBlank() && title.length <= 120) { "The project name in this backup is invalid." }
        require(html.length <= MAX_SOURCE_LENGTH && css.length <= MAX_SOURCE_LENGTH && javascript.length <= MAX_SOURCE_LENGTH) {
            "A source panel in this backup is too large."
        }
        require(archive.resources.size <= MAX_RESOURCES && archive.resources.all { it?.hasSafeHttpsUrl() == true }) {
            "This backup contains an invalid or unsupported external library URL."
        }
        require(listOf(html, css, javascript).none { '\u0000' in it }) {
            "This backup contains invalid source text."
        }
        return archive
    }
}
