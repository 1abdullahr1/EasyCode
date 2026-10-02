package com.vu.lecturehub.data.model

import java.io.Serializable

data class HandoutCourse(
    val courseCode: String,
    val title: String,
    val department: String
) : Serializable
