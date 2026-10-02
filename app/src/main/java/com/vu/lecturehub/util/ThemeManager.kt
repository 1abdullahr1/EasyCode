package com.vu.lecturehub.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"

    const val THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    const val THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO
    const val THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES

    fun applySavedTheme(context: Context) {
        val mode = getSavedThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun setThemeMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, mode)
            .apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getSavedThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, THEME_LIGHT)
    }
}
