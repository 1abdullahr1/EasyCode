package com.vu.lecturehub.data.model

data class Department(
    val name: String,
    val courseCount: Int = 0,
    var isSelected: Boolean = false
)
