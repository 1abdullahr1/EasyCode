package com.easycode.ide.data.model

enum class ConsoleLevel {
    LOG,
    INFO,
    WARN,
    ERROR
}

data class ConsoleMessage(
    val id: Long = System.nanoTime(),
    val level: ConsoleLevel,
    val message: String,
    val lineNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
