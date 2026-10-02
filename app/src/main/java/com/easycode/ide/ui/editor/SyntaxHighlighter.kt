package com.easycode.ide.ui.editor

import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

object SyntaxHighlighter {

    // HTML Patterns
    private val PATTERN_HTML_TAG = Pattern.compile("</?[a-zA-Z0-9]+|/?>")
    private val PATTERN_HTML_ATTR = Pattern.compile("\\s+([a-zA-Z0-9_-]+)(?==)")
    private val PATTERN_HTML_STRING = Pattern.compile("\"[^\"]*\"|'[^']*'")
    private val PATTERN_HTML_COMMENT = Pattern.compile("<!--[\\s\\S]*?-->")

    // CSS Patterns
    private val PATTERN_CSS_PROP = Pattern.compile("(?<=[{\\s;])([a-zA-Z0-9_-]+)(?=\\s*:)")
    private val PATTERN_CSS_VAL = Pattern.compile("(?<=:)\\s*([^;{}]+)(?=;)")
    private val PATTERN_CSS_SELECTOR = Pattern.compile("(^|[{},\\s])([.#]?[a-zA-Z0-9_:-]+)(?=\\s*\\{)")
    private val PATTERN_CSS_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/")

    // JavaScript Patterns
    private val PATTERN_JS_KEYWORDS = Pattern.compile(
        "\\b(function|const|let|var|if|else|for|while|do|switch|case|break|continue|return|new|delete|typeof|instanceof|void|this|class|extends|super|import|export|from|default|async|await|try|catch|finally|throw|true|false|null|undefined)\\b"
    )
    private val PATTERN_JS_STRING = Pattern.compile("\"[^\"]*\"|'[^']*'|`[^`]*`")
    private val PATTERN_JS_NUMBER = Pattern.compile("\\b(\\d+(\\.\\d+)?)\\b")
    private val PATTERN_JS_LINE_COMMENT = Pattern.compile("//.*")
    private val PATTERN_JS_BLOCK_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/")
    private val PATTERN_JS_FUNCTION = Pattern.compile("\\b([a-zA-Z0-9_$]+)(?=\\s*\\()")

    // Syntax Color Scheme
    private val COLOR_TAG = Color.parseColor("#F43F5E")      // Rose
    private val COLOR_ATTR = Color.parseColor("#FBBF24")     // Amber
    private val COLOR_STRING = Color.parseColor("#34D399")   // Emerald
    private val COLOR_KEYWORD = Color.parseColor("#A78BFA")  // Purple
    private val COLOR_FUNCTION = Color.parseColor("#38BDF8") // Sky Blue
    private val COLOR_COMMENT = Color.parseColor("#71717A")  // Zinc-500
    private val COLOR_NUMBER = Color.parseColor("#FB923C")   // Orange
    private val COLOR_CSS_PROP = Color.parseColor("#38BDF8") // Sky Blue
    private val COLOR_CSS_VAL = Color.parseColor("#F472B6")  // Pink
    private val COLOR_SELECTOR = Color.parseColor("#FBBF24") // Amber

    fun highlightHtml(spannable: Spannable) {
        clearSpans(spannable)
        val text = spannable.toString()

        applyPattern(spannable, text, PATTERN_HTML_TAG, COLOR_TAG)
        applyPattern(spannable, text, PATTERN_HTML_ATTR, COLOR_ATTR)
        applyPattern(spannable, text, PATTERN_HTML_STRING, COLOR_STRING)
        applyPattern(spannable, text, PATTERN_HTML_COMMENT, COLOR_COMMENT)
    }

    fun highlightCss(spannable: Spannable) {
        clearSpans(spannable)
        val text = spannable.toString()

        applyPattern(spannable, text, PATTERN_CSS_SELECTOR, COLOR_SELECTOR)
        applyPattern(spannable, text, PATTERN_CSS_PROP, COLOR_CSS_PROP)
        applyPattern(spannable, text, PATTERN_CSS_VAL, COLOR_CSS_VAL)
        applyPattern(spannable, text, PATTERN_CSS_COMMENT, COLOR_COMMENT)
    }

    fun highlightJs(spannable: Spannable) {
        clearSpans(spannable)
        val text = spannable.toString()

        applyPattern(spannable, text, PATTERN_JS_KEYWORDS, COLOR_KEYWORD)
        applyPattern(spannable, text, PATTERN_JS_FUNCTION, COLOR_FUNCTION)
        applyPattern(spannable, text, PATTERN_JS_NUMBER, COLOR_NUMBER)
        applyPattern(spannable, text, PATTERN_JS_STRING, COLOR_STRING)
        applyPattern(spannable, text, PATTERN_JS_LINE_COMMENT, COLOR_COMMENT)
        applyPattern(spannable, text, PATTERN_JS_BLOCK_COMMENT, COLOR_COMMENT)
    }

    private fun clearSpans(spannable: Spannable) {
        val spans = spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
        for (span in spans) {
            spannable.removeSpan(span)
        }
    }

    private fun applyPattern(spannable: Spannable, text: String, pattern: Pattern, color: Int) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            spannable.setSpan(
                ForegroundColorSpan(color),
                matcher.start(),
                matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
