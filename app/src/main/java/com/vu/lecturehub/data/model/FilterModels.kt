package com.vu.lecturehub.data.model

data class FilterItem(
    val id: String,
    val displayName: String,
    val count: Int = 0,
    var isSelected: Boolean = false
)

data class FilterState(
    val selectedSubjects: Set<String> = emptySet(),
    val selectedSkills: Set<String> = emptySet()
) {
    val totalActiveCount: Int get() = selectedSubjects.size + selectedSkills.size
    val isAnyActive: Boolean get() = totalActiveCount > 0
}
