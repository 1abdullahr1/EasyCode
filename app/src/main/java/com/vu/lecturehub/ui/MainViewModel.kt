package com.vu.lecturehub.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Department
import com.vu.lecturehub.data.model.FilterItem
import com.vu.lecturehub.data.model.FilterState
import com.vu.lecturehub.data.model.SkillDefinitions
import com.vu.lecturehub.data.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.vu.lecturehub.data.model.WatchHistoryItem
import com.vu.lecturehub.util.WatchHistoryManager

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CourseRepository(application)

    val allCourses: LiveData<List<Course>> = repository.allCourses.asLiveData()
    val bookmarkedCourses: LiveData<List<Course>> = repository.bookmarkedCourses.asLiveData()
    val recentlyWatchedCourses: LiveData<List<Course>> = repository.recentlyWatchedCourses.asLiveData()
    val recentVideos: LiveData<List<WatchHistoryItem>> = WatchHistoryManager.historyFlow.asLiveData()

    private val _departments = MutableLiveData<List<Department>>(emptyList())
    val departments: LiveData<List<Department>> = _departments

    private val _filteredCourses = MutableLiveData<List<Course>>(emptyList())
    val filteredCourses: LiveData<List<Course>> = _filteredCourses

    private val _filterState = MutableLiveData(FilterState())
    val filterState: LiveData<FilterState> = _filterState

    private val _availableSubjects = MutableLiveData<List<FilterItem>>(emptyList())
    val availableSubjects: LiveData<List<FilterItem>> = _availableSubjects

    private val _availableSkills = MutableLiveData<List<FilterItem>>(emptyList())
    val availableSkills: LiveData<List<FilterItem>> = _availableSkills

    var currentSearchQuery = ""
    private var rawCoursesList: List<Course> = emptyList()

    init {
        WatchHistoryManager.init(application)
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded()
        }

        viewModelScope.launch(Dispatchers.Default) {
            repository.allCourses.collectLatest { list ->
                rawCoursesList = list
                if (_departments.value.isNullOrEmpty() && list.isNotEmpty()) {
                    _departments.postValue(repository.extractDepartments(list))
                }
                computeAvailableFilters(list)
                applyFilters()
            }
        }
    }

    private fun computeAvailableFilters(courses: List<Course>) {
        if (courses.isEmpty()) return

        // 1. Compute Subject counts
        val subjectCounts = mutableMapOf<String, Int>()
        for (c in courses) {
            val dept = c.department.trim()
            if (dept.isNotEmpty() && !dept.equals("All", ignoreCase = true)) {
                subjectCounts[dept] = (subjectCounts[dept] ?: 0) + 1
            }
        }
        val subjects = subjectCounts.map { (name, count) ->
            FilterItem(
                id = name,
                displayName = name,
                count = count,
                isSelected = _filterState.value?.selectedSubjects?.contains(name) == true
            )
        }.sortedByDescending { it.count }
        _availableSubjects.postValue(subjects)

        // 2. Compute Skill counts based on keyword matching
        val skills = SkillDefinitions.SKILLS.map { skillDef ->
            val count = courses.count { course ->
                val fullText = "${course.title} ${course.department} ${course.courseCode ?: ""}".lowercase()
                skillDef.keywords.any { kw -> fullText.contains(kw) }
            }
            FilterItem(
                id = skillDef.id,
                displayName = skillDef.name,
                count = count,
                isSelected = _filterState.value?.selectedSkills?.contains(skillDef.id) == true
            )
        }.filter { it.count > 0 }
        _availableSkills.postValue(skills)
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query.trim()
        applyFilters()
    }

    fun selectDepartment(dept: Department) {
        if (dept.name == "All") {
            clearAllFilters()
        } else {
            updateFilters(selectedSubjects = setOf(dept.name), selectedSkills = emptySet())
        }
    }

    fun updateFilters(selectedSubjects: Set<String>, selectedSkills: Set<String>) {
        val newState = FilterState(
            selectedSubjects = selectedSubjects,
            selectedSkills = selectedSkills
        )
        _filterState.value = newState

        // Refresh selection state in available lists
        _availableSubjects.value?.forEach { it.isSelected = it.id in selectedSubjects }
        _availableSkills.value?.forEach { it.isSelected = it.id in selectedSkills }

        applyFilters()
    }

    fun clearAllFilters() {
        _filterState.value = FilterState()
        _availableSubjects.value?.forEach { it.isSelected = false }
        _availableSkills.value?.forEach { it.isSelected = false }
        applyFilters()
    }

    fun toggleBookmark(course: Course) {
        viewModelScope.launch {
            repository.toggleBookmark(course)
        }
    }

    fun clearWatchHistory() {
        WatchHistoryManager.clearHistory(getApplication())
    }

    private fun applyFilters() {
        var result = rawCoursesList
        val state = _filterState.value ?: FilterState()

        // 1. Subject / Department filter (multi-select)
        if (state.selectedSubjects.isNotEmpty()) {
            result = result.filter { course ->
                state.selectedSubjects.any { dept ->
                    course.department.equals(dept, ignoreCase = true)
                }
            }
        }

        // 2. Skills filter (multi-select)
        if (state.selectedSkills.isNotEmpty()) {
            val selectedSkillDefs = SkillDefinitions.SKILLS.filter { it.id in state.selectedSkills }
            result = result.filter { course ->
                val fullText = "${course.title} ${course.department} ${course.courseCode ?: ""}".lowercase()
                selectedSkillDefs.any { skillDef ->
                    skillDef.keywords.any { kw -> fullText.contains(kw) }
                }
            }
        }

        // 3. Search query filter
        if (currentSearchQuery.isNotEmpty()) {
            val q = currentSearchQuery.lowercase()
            result = result.filter {
                (it.courseCode?.lowercase()?.contains(q) == true) ||
                it.title.lowercase().contains(q) ||
                it.department.lowercase().contains(q)
            }
        }

        _filteredCourses.postValue(result)
    }
}
