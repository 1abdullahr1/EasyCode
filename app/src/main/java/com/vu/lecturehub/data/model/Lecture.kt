package com.vu.lecturehub.data.model

import java.io.Serializable

data class Lecture(
    val playlistId: String,
    val lectureIndex: Int,
    val title: String,
    val videoId: String?,
    val thumbnailUrl: String?,
    var isCompleted: Boolean = false
) : Serializable
