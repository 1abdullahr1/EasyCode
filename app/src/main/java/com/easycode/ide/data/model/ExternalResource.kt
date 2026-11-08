package com.easycode.ide.data.model

data class ExternalResource(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val isCss: Boolean,
    val isEnabled: Boolean = true
) {
    fun hasSafeHttpsUrl(): Boolean = try {
        val parsed = java.net.URI(url.trim())
        parsed.scheme.equals("https", ignoreCase = true) &&
            !parsed.host.isNullOrBlank() && parsed.userInfo == null && url.length <= 2048
    } catch (_: Exception) {
        false
    }
}
