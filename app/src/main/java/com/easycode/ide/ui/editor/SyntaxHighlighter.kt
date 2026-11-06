package com.easycode.ide.ui.editor

import android.content.res.Resources
import com.easycode.ide.R
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

    fun highlightHtml(spannable: Spannable, resources: Resources) {
        clearSpans(spannable)
        val text = spannable.toString()

        applyPattern(spannable, text, PATTERN_HTML_TAG, resources.getColor(R.color.syntax_tag, null))
        applyPattern(spannable, text, PATTERN_HTML_ATTR, resources.getColor(R.color.syntax_attr, null))
        applyPattern(spannable, text, PATTERN_HTML_STRING, resources.getColor(R.color.syntax_string, null))
        applyPattern(spannable, text, PATTERN_HTML_COMMENT, resources.getColor(R.color.syntax_comment, null))
    }

    fun highlightCss(spannable: Spannable, resources: Resources) {
        clearSpans(spannable)
        val text = spannable.toString()

        applyPattern(spannable, text, PATTERN_CSS_SELECTOR, resources.getColor(R.color.syntax_attr, null))
        applyPattern(spannable, text, PATTERN_CSS_PROP, resources.getColor(R.color.syntax_css_prop, null))
        applyPattern(spannable, text, PATTERN_CSS_VAL, resources.getColor(R.color.syntax_css_val, null))
        applyPattern(spannable, text, PATTERN_CSS_COMMENT, resources.getColor(R.color.syntax_comment, null))
    }

    fun highlightJs(spannable: Spannable, resources: Resources) {
        clearSpans(spannable)
        val text = spannable.toString()

        applyPattern(spannable, text, PATTERN_JS_KEYWORDS, resources.getColor(R.color.syntax_keyword, null))
        applyPattern(spannable, text, PATTERN_JS_FUNCTION, resources.getColor(R.color.syntax_function, null))
        applyPattern(spannable, text, PATTERN_JS_NUMBER, resources.getColor(R.color.syntax_number, null))
        applyPattern(spannable, text, PATTERN_JS_STRING, resources.getColor(R.color.syntax_string, null))
        applyPattern(spannable, text, PATTERN_JS_LINE_COMMENT, resources.getColor(R.color.syntax_comment, null))
        applyPattern(spannable, text, PATTERN_JS_BLOCK_COMMENT, resources.getColor(R.color.syntax_comment, null))
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
