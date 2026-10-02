package com.vu.lecturehub.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vu.lecturehub.data.model.WatchHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WatchHistoryManager {

    private const val PREFS_NAME = "vu_watch_history_prefs"
    private const val KEY_HISTORY = "watch_history_list"
    private const val MAX_HISTORY = 30
    private val gson = Gson()

    private val _historyFlow = MutableStateFlow<List<WatchHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<WatchHistoryItem>> = _historyFlow.asStateFlow()

    fun init(context: Context) {
        _historyFlow.value = loadHistoryFromPrefs(context)
    }

    fun getRecentVideos(context: Context): List<WatchHistoryItem> {
        val list = loadHistoryFromPrefs(context)
        _historyFlow.value = list
        return list
    }

    private fun loadHistoryFromPrefs(context: Context): List<WatchHistoryItem> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WatchHistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordVideoWatch(
        context: Context,
        playlistId: String,
        courseCode: String?,
        courseTitle: String,
        lectureIndex: Int,
        lectureTitle: String,
        videoId: String?,
        thumbnailUrl: String?,
        positionSeconds: Float = 0f,
        durationSeconds: Float = 0f
    ) {
        if (playlistId.isEmpty()) return
        val current = loadHistoryFromPrefs(context).toMutableList()

        current.removeAll { it.playlistId == playlistId && it.lectureIndex == lectureIndex }

        val newItem = WatchHistoryItem(
            playlistId = playlistId,
            courseCode = courseCode,
            courseTitle = courseTitle,
            lectureIndex = lectureIndex,
            lectureTitle = lectureTitle,
            videoId = videoId,
            thumbnailUrl = thumbnailUrl,
            positionSeconds = positionSeconds,
            durationSeconds = durationSeconds,
            watchedTimestamp = System.currentTimeMillis()
        )

        current.add(0, newItem)
        if (current.size > MAX_HISTORY) {
            current.removeAt(current.size - 1)
        }

        saveHistoryToPrefs(context, current)
        _historyFlow.value = current
    }

    private fun saveHistoryToPrefs(context: Context, list: List<WatchHistoryItem>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    fun clearHistory(context: Context) {
        saveHistoryToPrefs(context, emptyList())
        _historyFlow.value = emptyList()
    }
}
