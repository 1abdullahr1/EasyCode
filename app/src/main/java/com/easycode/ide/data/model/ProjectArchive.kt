package com.easycode.ide.data.model

data class ProjectArchive(
    val schemaVersion: Int = CURRENT_VERSION,
    val title: String,
    val html: String,
    val css: String,
    val javascript: String,
    val resources: List<ExternalResource> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
