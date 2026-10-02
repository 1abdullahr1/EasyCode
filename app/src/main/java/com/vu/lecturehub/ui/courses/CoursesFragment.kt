package com.vu.lecturehub.ui.courses

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Department
import com.vu.lecturehub.data.model.Lecture
import com.vu.lecturehub.data.model.SkillDefinitions
import com.vu.lecturehub.data.repository.CourseRepository
import com.vu.lecturehub.databinding.FragmentCoursesBinding
import com.vu.lecturehub.ui.MainViewModel
import com.vu.lecturehub.ui.adapters.CourseAdapter
import com.vu.lecturehub.ui.adapters.DiscoverHeaderAdapter
import com.vu.lecturehub.ui.adapters.SearchSuggestionAdapter
import com.vu.lecturehub.ui.adapters.SubjectAdapter
import com.vu.lecturehub.ui.detail.CourseDetailActivity
import com.vu.lecturehub.ui.player.PlayerActivity

class CoursesFragment : Fragment() {

    private var _binding: FragmentCoursesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var courseAdapter: CourseAdapter
    private lateinit var subjectAdapter: SubjectAdapter
    private lateinit var headerAdapter: DiscoverHeaderAdapter
    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupSearchDropdown()
        setupKeyboardListener()
        observeData()
    }

    private fun setupAdapters() {
        // 1. Subject adapter for 2-row horizontal grid in header
        subjectAdapter = SubjectAdapter(emptyList()) { selectedDept ->
            filterByDepartment(selectedDept)
        }

        // 2. Header adapter containing Hero banner, Search box, Popular Subjects, and Filter Bar
        headerAdapter = DiscoverHeaderAdapter(
            subjectAdapter = subjectAdapter,
            onSearchQueryChanged = { query ->
                viewModel.setSearchQuery(query)
                if (query.isNotEmpty()) {
                    hideSearchDropdown()
                }
            },
            onSearchSubmitted = { query ->
                viewModel.setSearchQuery(query)
                hideSearchDropdown()
                hideKeyboard()
            },
            onSearchFocused = {
                showSearchDropdown()
            },
            onTrendingChipClicked = { tag ->
                applySearchQuery(tag)
            },
            onResetFilterClicked = {
                resetDepartmentFilter()
            },
            onOpenFiltersClicked = {
                openFilterBottomSheet()
            },
            onClearFiltersClicked = {
                viewModel.clearAllFilters()
            }
        )

        // 3. Course list adapter for catalog items
        courseAdapter = CourseAdapter(
            onCourseClick = { course -> openCourseDetail(course) },
            onPlayClick = { course -> openPlayerDirectly(course) },
            onBookmarkClick = { course -> viewModel.toggleBookmark(course) }
        )

        // 4. Combine header and courses using ConcatAdapter (isolateViewTypes = true by default)
        val concatAdapter = ConcatAdapter(headerAdapter, courseAdapter)

        binding.rvCourses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = concatAdapter
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState != RecyclerView.SCROLL_STATE_IDLE && binding.cardSearchDropdown.visibility == View.VISIBLE) {
                        hideSearchDropdown()
                        hideKeyboard()
                    }
                }
            })
        }
    }

    private fun openFilterBottomSheet() {
        hideKeyboard()
        hideSearchDropdown()
        CourseFilterBottomSheet.newInstance().show(childFragmentManager, CourseFilterBottomSheet.TAG)
    }

    private fun filterByDepartment(dept: Department) {
        viewModel.selectDepartment(dept)
        hideSearchDropdown()

        // Smooth scroll to catalog items (just below header)
        binding.rvCourses.post {
            val total = binding.rvCourses.adapter?.itemCount ?: 0
            if (total > 1) {
                (binding.rvCourses.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(1, 0)
            }
        }
    }

    private fun resetDepartmentFilter() {
        viewModel.clearAllFilters()
    }

    private fun setupSearchDropdown() {
        searchSuggestionAdapter = SearchSuggestionAdapter(emptyList()) { course ->
            hideSearchDropdown()
            openCourseDetail(course)
        }
        binding.rvSearchPopular.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchSuggestionAdapter
        }

        // Trending now rows in dropdown
        binding.itemTrendCs.setOnClickListener { applySearchQuery("CS") }
        binding.itemTrendData.setOnClickListener { applySearchQuery("Data") }
        binding.itemTrendMath.setOnClickListener { applySearchQuery("MTH") }
        binding.itemTrendBusiness.setOnClickListener { applySearchQuery("MGT") }
    }

    private fun applySearchQuery(query: String) {
        headerAdapter.setSearchQueryText(query)
        viewModel.setSearchQuery(query)
        hideSearchDropdown()
        hideKeyboard()

        // Smooth scroll to catalog items
        binding.rvCourses.post {
            val total = binding.rvCourses.adapter?.itemCount ?: 0
            if (total > 1) {
                (binding.rvCourses.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(1, 0)
            }
        }
    }

    private fun setupKeyboardListener() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (isImeVisible) {
                if (viewModel.currentSearchQuery.isEmpty()) {
                    showSearchDropdown()
                }
            } else {
                hideSearchDropdownInstantly()
            }
            insets
        }
    }

    private fun showSearchDropdown() {
        if (binding.cardSearchDropdown.visibility != View.VISIBLE) {
            binding.cardSearchDropdown.alpha = 0f
            binding.cardSearchDropdown.visibility = View.VISIBLE
            binding.cardSearchDropdown.animate()
                .alpha(1f)
                .setDuration(180)
                .start()
        }
    }

    private fun hideSearchDropdownInstantly() {
        binding.cardSearchDropdown.animate().cancel()
        binding.cardSearchDropdown.visibility = View.GONE
        binding.cardSearchDropdown.alpha = 0f
    }

    private fun hideSearchDropdown() {
        hideSearchDropdownInstantly()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun observeData() {
        // Observe filtered courses
        viewModel.filteredCourses.observe(viewLifecycleOwner) { courses ->
            courseAdapter.submitList(courses)
            binding.emptyView.visibility = if (courses.isEmpty()) View.VISIBLE else View.GONE
        }

        // Observe filter state to update header status and catalog title
        viewModel.filterState.observe(viewLifecycleOwner) { state ->
            if (state.isAnyActive) {
                val activeTitle = when {
                    state.selectedSubjects.isNotEmpty() && state.selectedSkills.isEmpty() -> {
                        if (state.selectedSubjects.size == 1) state.selectedSubjects.first()
                        else "Subjects (${state.selectedSubjects.size})"
                    }
                    state.selectedSkills.isNotEmpty() && state.selectedSubjects.isEmpty() -> {
                        if (state.selectedSkills.size == 1) {
                            SkillDefinitions.SKILLS.find { it.id == state.selectedSkills.first() }?.name ?: "Skill"
                        } else "Skills (${state.selectedSkills.size})"
                    }
                    else -> "Filtered Courses (${state.totalActiveCount})"
                }
                headerAdapter.updateCatalogHeader(activeTitle, true, state.totalActiveCount)
            } else {
                headerAdapter.updateCatalogHeader(getString(R.string.explore_courses_and_programs), false, 0)
            }
        }

        // Observe departments for the 2-row horizontal grid in header
        viewModel.departments.observe(viewLifecycleOwner) { depts ->
            subjectAdapter.updateDepartments(depts)
        }

        // Populate "Most popular programs" in search dropdown from top courses
        viewModel.allCourses.observe(viewLifecycleOwner) { courses ->
            val topPopular = courses.filter { c ->
                val code = c.courseCode?.uppercase() ?: ""
                code in setOf("CS101", "CS201", "MTH101", "MGT211")
            }.ifEmpty { courses.take(4) }
            searchSuggestionAdapter.updateSuggestions(topPopular)
        }
    }

    private fun openCourseDetail(course: Course) {
        val intent = Intent(requireContext(), CourseDetailActivity::class.java).apply {
            putExtra("EXTRA_COURSE", course)
        }
        startActivity(intent)
        activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openPlayerDirectly(course: Course) {
        val lectureIndex = if (course.lastWatchedLectureIndex > 0) course.lastWatchedLectureIndex else 1
        val cached = CourseRepository.getCachedLectures(course.playlistId)
        val targetLecture = cached?.find { it.lectureIndex == lectureIndex }
            ?: Lecture(
                playlistId = course.playlistId,
                lectureIndex = lectureIndex,
                title = "Lecture ${String.format("%02d", lectureIndex)} - ${course.title}",
                videoId = if (lectureIndex == 1) course.firstVideoId else null,
                thumbnailUrl = course.thumbnailUrl
            )
        val mainAct = activity as? com.vu.lecturehub.MainActivity
        if (mainAct != null) {
            mainAct.playCourse(course, targetLecture)
        } else {
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("EXTRA_COURSE", course)
                putExtra("EXTRA_LECTURE", targetLecture)
            }
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
