package com.easycode.ide.data.model

data class ExternalResource(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val isCss: Boolean,
    val isEnabled: Boolean = true
)
