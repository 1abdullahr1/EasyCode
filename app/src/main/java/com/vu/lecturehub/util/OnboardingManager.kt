package com.vu.lecturehub.util

import android.content.Context
import android.content.SharedPreferences

object OnboardingManager {
    private const val PREFS_NAME = "vu_onboarding_prefs"
    private const val KEY_COMPLETED = "has_completed_onboarding"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_COMPLETED, true).apply()
    }
}
