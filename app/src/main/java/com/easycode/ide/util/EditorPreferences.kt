package com.easycode.ide.util

import android.content.Context

object EditorPreferences {
    const val FONT = "font_size"
    const val WRAP = "word_wrap"
    const val NUMBERS = "line_numbers"
    const val INDENT = "indent_width"
    const val DEFAULT_FONT = 14
    const val DEFAULT_INDENT = 2

    fun store(context: Context) = context.getSharedPreferences("editor_preferences", Context.MODE_PRIVATE)
    fun indent(context: Context) = store(context).getInt(INDENT, DEFAULT_INDENT).coerceIn(2, 8)
}
