package com.vu.lecturehub.ui.saved

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vu.lecturehub.MainActivity
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Lecture
import com.vu.lecturehub.data.model.WatchHistoryItem
import com.vu.lecturehub.data.repository.CourseRepository
import com.vu.lecturehub.databinding.FragmentSavedBinding
import com.vu.lecturehub.ui.MainViewModel
import com.vu.lecturehub.ui.adapters.BookmarkedCourseAdapter
import com.vu.lecturehub.ui.adapters.MyLearningCourseAdapter
import com.vu.lecturehub.ui.adapters.WatchHistoryAdapter
import com.vu.lecturehub.ui.detail.CourseDetailActivity
import com.vu.lecturehub.ui.player.PlayerActivity
import com.vu.lecturehub.ui.settings.SettingsBottomSheetDialogFragment

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var yourCoursesAdapter: MyLearningCourseAdapter
    private lateinit var bookmarkedAdapter: BookmarkedCourseAdapter
    private lateinit var historyAdapter: WatchHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupListeners()
        observeData()
    }

    private fun setupAdapters() {
        yourCoursesAdapter = MyLearningCourseAdapter(
            onCourseClick = { course -> openCourseDetail(course) },
            onPlayClick = { course -> openPlayerDirectly(course) }
        )

        bookmarkedAdapter = BookmarkedCourseAdapter(
            onCourseClick = { course -> openCourseDetail(course) },
            onPlayClick = { course -> openPlayerDirectly(course) },
            onBookmarkClick = { course -> viewModel.toggleBookmark(course) }
        )

        historyAdapter = WatchHistoryAdapter(
            onItemClick = { item -> playHistoryLecture(item) }
        )

        binding.rvYourCourses.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = yourCoursesAdapter
            setHasFixedSize(true)
        }

        binding.rvBookmarkedCourses.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = bookmarkedAdapter
            setHasFixedSize(true)
        }

        binding.rvRecentlyWatched.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.btnResources.setOnClickListener {
            val intent = Intent(requireContext(), com.vu.lecturehub.ui.resources.ResourcesActivity::class.java)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnSettings.setOnClickListener {
            SettingsBottomSheetDialogFragment.newInstance()
                .show(childFragmentManager, SettingsBottomSheetDialogFragment.TAG)
        }

        binding.btnExploreCourses.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.nav_courses)
        }

        binding.btnSeeAllYourCourses.setOnClickListener {
            val expanded = !yourCoursesAdapter.isExpanded
            yourCoursesAdapter.isExpanded = expanded
            binding.btnSeeAllYourCourses.text = if (expanded) {
                getString(R.string.show_less)
            } else {
                getString(R.string.see_all)
            }
        }

        binding.btnSeeAllBookmarked.setOnClickListener {
            val expanded = !bookmarkedAdapter.isExpanded
            bookmarkedAdapter.isExpanded = expanded
            binding.btnSeeAllBookmarked.text = if (expanded) {
                getString(R.string.show_less)
            } else {
                getString(R.string.see_all)
            }
        }

        binding.btnSeeAllHistory.setOnClickListener {
            val expanded = !historyAdapter.isExpanded
            historyAdapter.isExpanded = expanded
            binding.btnSeeAllHistory.text = if (expanded) {
                getString(R.string.show_less)
            } else {
                getString(R.string.see_all)
            }
        }

        binding.btnClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }
    }

    private fun observeData() {
        viewModel.recentlyWatchedCourses.observe(viewLifecycleOwner) { courses ->
            yourCoursesAdapter.submitList(courses)
            binding.sectionYourCourses.visibility = if (courses.isEmpty()) View.GONE else View.VISIBLE
            binding.btnSeeAllYourCourses.visibility = if (courses.size > 4) View.VISIBLE else View.GONE
            updateEmptyState()
        }

        viewModel.bookmarkedCourses.observe(viewLifecycleOwner) { bookmarked ->
            bookmarkedAdapter.submitList(bookmarked)
            binding.sectionBookmarked.visibility = if (bookmarked.isEmpty()) View.GONE else View.VISIBLE
            binding.btnSeeAllBookmarked.visibility = if (bookmarked.size > 4) View.VISIBLE else View.GONE
            updateEmptyState()
        }

        viewModel.recentVideos.observe(viewLifecycleOwner) { history ->
            historyAdapter.submitList(history)
            binding.sectionRecentlyWatched.visibility = if (history.isEmpty()) View.GONE else View.VISIBLE
            binding.btnSeeAllHistory.visibility = if (history.size > 4) View.VISIBLE else View.GONE
            binding.btnClearHistory.visibility = if (history.isNotEmpty()) View.VISIBLE else View.GONE
            updateEmptyState()
        }
    }

    private fun updateEmptyState() {
        val yourCoursesEmpty = viewModel.recentlyWatchedCourses.value.isNullOrEmpty()
        val bookmarkedEmpty = viewModel.bookmarkedCourses.value.isNullOrEmpty()
        val historyEmpty = viewModel.recentVideos.value.isNullOrEmpty()
        val allEmpty = yourCoursesEmpty && bookmarkedEmpty && historyEmpty

        binding.layoutEmptyMyLearning.visibility = if (allEmpty) View.VISIBLE else View.GONE
        binding.scrollContent.visibility = if (allEmpty) View.GONE else View.VISIBLE
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_history_title)
            .setMessage(R.string.clear_history_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearWatchHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
        val mainAct = activity as? MainActivity
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

    private fun playHistoryLecture(item: WatchHistoryItem) {
        val all = viewModel.allCourses.value ?: emptyList()
        val course = all.find { it.playlistId == item.playlistId }
            ?: Course(
                playlistId = item.playlistId,
                title = item.courseTitle,
                courseCode = item.courseCode,
                department = "Virtual University",
                isAcademicCourse = true,
                videoCount = 45,
                videoCountText = "45 lectures",
                thumbnailUrl = item.thumbnailUrl,
                playlistUrl = "https://www.youtube.com/playlist?list=${item.playlistId}",
                firstVideoId = item.videoId,
                lastWatchedLectureIndex = item.lectureIndex,
                lastWatchedTimestamp = item.watchedTimestamp
            )

        val cached = CourseRepository.getCachedLectures(item.playlistId)
        val targetLecture = cached?.find { it.lectureIndex == item.lectureIndex }
            ?: Lecture(
                playlistId = item.playlistId,
                lectureIndex = item.lectureIndex,
                title = item.lectureTitle,
                videoId = item.videoId,
                thumbnailUrl = item.thumbnailUrl ?: course.thumbnailUrl
            )

        val mainAct = activity as? MainActivity
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
