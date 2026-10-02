package com.vu.lecturehub.data.model

import java.io.Serializable

data class WatchHistoryItem(
    val playlistId: String,
    val courseCode: String?,
    val courseTitle: String,
    val lectureIndex: Int,
    val lectureTitle: String,
    val videoId: String?,
    val thumbnailUrl: String?,
    val positionSeconds: Float = 0f,
    val durationSeconds: Float = 0f,
    val watchedTimestamp: Long = System.currentTimeMillis()
) : Serializable
