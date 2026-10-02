package com.vu.lecturehub.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey
    val playlistId: String,
    val title: String,
    val courseCode: String?,
    val department: String,
    val isAcademicCourse: Boolean,
    val videoCount: Int,
    val videoCountText: String?,
    val thumbnailUrl: String?,
    val playlistUrl: String?,
    val firstVideoId: String? = null,
    var isBookmarked: Boolean = false,
    var lastWatchedTimestamp: Long = 0L,
    var lastWatchedLectureIndex: Int = 1
) : Serializable
