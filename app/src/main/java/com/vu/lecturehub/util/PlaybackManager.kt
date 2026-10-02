package com.vu.lecturehub.util

import android.content.Context
import android.content.SharedPreferences

object PlaybackManager {

    private const val PREFS_NAME = "vu_playback_prefs"
    private const val KEY_PREFIX_POS = "pos_"
    private const val KEY_PREFIX_DUR = "dur_"
    private const val KEY_PREFIX_LAST_LECTURE = "last_lec_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun savePosition(
        context: Context,
        playlistId: String,
        lectureIndex: Int,
        positionSeconds: Float,
        durationSeconds: Float = 0f
    ) {
        if (playlistId.isEmpty() || positionSeconds < 0) return
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putFloat("${KEY_PREFIX_POS}${playlistId}_$lectureIndex", positionSeconds)
            putFloat("${KEY_PREFIX_POS}${playlistId}_last", positionSeconds)
            putInt("${KEY_PREFIX_LAST_LECTURE}$playlistId", lectureIndex)
            if (durationSeconds > 0) {
                putFloat("${KEY_PREFIX_DUR}${playlistId}_$lectureIndex", durationSeconds)
            }
            apply()
        }
    }

    fun getPosition(context: Context, playlistId: String, lectureIndex: Int): Float {
        if (playlistId.isEmpty()) return 0f
        val prefs = getPrefs(context)
        return prefs.getFloat("${KEY_PREFIX_POS}${playlistId}_$lectureIndex", 0f)
    }

    fun getLastPositionForCourse(context: Context, playlistId: String): Float {
        if (playlistId.isEmpty()) return 0f
        val prefs = getPrefs(context)
        return prefs.getFloat("${KEY_PREFIX_POS}${playlistId}_last", 0f)
    }

    fun getLastLectureIndex(context: Context, playlistId: String): Int {
        if (playlistId.isEmpty()) return 1
        val prefs = getPrefs(context)
        return prefs.getInt("${KEY_PREFIX_LAST_LECTURE}$playlistId", 1)
    }

    fun getDuration(context: Context, playlistId: String, lectureIndex: Int): Float {
        if (playlistId.isEmpty()) return 0f
        val prefs = getPrefs(context)
        return prefs.getFloat("${KEY_PREFIX_DUR}${playlistId}_$lectureIndex", 0f)
    }
}
