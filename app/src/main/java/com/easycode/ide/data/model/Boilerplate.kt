package com.easycode.ide.data.model

data class Boilerplate(
    val id: String,
    val title: String,
    val tag: String,
    val description: String,
    val htmlCode: String,
    val cssCode: String,
    val jsCode: String,
    val resources: List<ExternalResource> = emptyList()
)
