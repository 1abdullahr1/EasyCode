package com.easycode.ide.engine

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import com.easycode.ide.data.model.ConsoleLevel
import com.easycode.ide.data.model.ConsoleMessage

class ConsoleBridge(private val onMessageReceived: (ConsoleMessage) -> Unit) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(levelStr: String, message: String, line: Int) {
        val level = try {
            ConsoleLevel.valueOf(levelStr.uppercase())
        } catch (_: Exception) {
            ConsoleLevel.LOG
        }

        val consoleMsg = ConsoleMessage(
            level = level,
            message = message,
            lineNumber = line,
            timestamp = System.currentTimeMillis()
        )

        mainHandler.post {
            onMessageReceived(consoleMsg)
        }
    }
}
