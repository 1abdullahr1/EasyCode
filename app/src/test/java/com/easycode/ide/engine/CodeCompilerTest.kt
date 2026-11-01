package com.easycode.ide.engine

import com.easycode.ide.data.model.ExternalResource
import org.junit.Assert.*
import org.junit.Test

class CodeCompilerTest {
    @Test fun composesAllPanelsAndOnlyEnabledResources() {
        val result = CodeCompiler.compile(
            "<h1>Hello</h1>", "h1 { color: red; }", "console.log('ready')",
            listOf(
                ExternalResource(name = "styles", url = "https://example.com/a.css", isCss = true),
                ExternalResource(name = "disabled", url = "https://example.com/disabled.js", isCss = false, isEnabled = false)
            )
        )
        assertTrue(result.contains("<h1>Hello</h1>"))
        assertTrue(result.contains("h1 { color: red; }"))
        assertTrue(result.contains("console.log('ready')"))
        assertTrue(result.contains("https://example.com/a.css"))
        assertFalse(result.contains("disabled.js"))
    }

    @Test fun babelResourceEnablesJsxExecutionMode() {
        val result = CodeCompiler.compile("", "", "const element = <h1 />;", listOf(
            ExternalResource(name = "Babel", url = "https://example.com/babel.min.js", isCss = false)
        ))
        assertTrue(result.contains("<script type=\"text/babel\">"))
    }
}
