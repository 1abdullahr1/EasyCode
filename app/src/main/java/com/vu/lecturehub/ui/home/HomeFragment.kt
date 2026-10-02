package com.vu.lecturehub.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.vu.lecturehub.MainActivity
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Lecture
import com.vu.lecturehub.databinding.FragmentHomeBinding
import com.vu.lecturehub.ui.MainViewModel
import com.vu.lecturehub.ui.adapters.CompactCourseAdapter
import com.vu.lecturehub.ui.detail.CourseDetailActivity
import com.vu.lecturehub.ui.player.PlayerActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var compactAdapter: CompactCourseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEnrolledRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupEnrolledRecyclerView() {
        compactAdapter = CompactCourseAdapter(emptyList()) { course ->
            openCourseDetail(course)
        }
        binding.rvEnrolledCourses.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = compactAdapter
        }
    }

    private fun setupListeners() {
        // "Find a Course" button in Empty State -> switches to Discover tab
        binding.btnFindCourse.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.nav_courses)
        }

        // "View All My Courses >" header -> switches to Saved tab
        binding.btnViewAllCourses.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.nav_saved)
        }

        // "Courses" dropdown selector -> shows Coming Soon
        binding.btnCoursesDropdown.setOnClickListener {
            android.widget.Toast.makeText(context, "Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Notification icon -> shows Coming Soon
        binding.btnNotifications.setOnClickListener {
            android.widget.Toast.makeText(context, "Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        // Observe both recently watched and bookmarked courses to detect active courses
        viewModel.recentlyWatchedCourses.observe(viewLifecycleOwner) { recent ->
            updateUI(recent, viewModel.bookmarkedCourses.value ?: emptyList())
        }

        viewModel.bookmarkedCourses.observe(viewLifecycleOwner) { bookmarked ->
            updateUI(viewModel.recentlyWatchedCourses.value ?: emptyList(), bookmarked)
        }
    }

    private fun updateUI(recent: List<Course>, bookmarked: List<Course>) {
        val enrolled = (recent + bookmarked).distinctBy { it.playlistId }

        if (enrolled.isEmpty()) {
            // STATE A: New User Empty State (neuser.jpeg)
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.layoutActiveLearning.visibility = View.GONE
        } else {
            // STATE B: Active Learning Dashboard (learn user courses.jpeg)
            binding.layoutEmptyState.visibility = View.GONE
            binding.layoutActiveLearning.visibility = View.VISIBLE

            val heroCourse = recent.firstOrNull() ?: bookmarked.firstOrNull() ?: enrolled.first()
            bindHeroCourse(heroCourse)

            binding.tvMyCoursesCountHeader.text = getString(R.string.view_all_my_courses, enrolled.size)
            compactAdapter.updateCourses(enrolled)
        }
    }

    private fun bindHeroCourse(course: Course) {
        binding.ivHeroThumbnail.load(course.thumbnailUrl) {
            crossfade(true)
            placeholder(R.drawable.playlist_placeholder)
            error(R.drawable.playlist_placeholder)
        }

        binding.tvHeroDuration.text = getString(R.string.lectures_count, course.videoCount)
        binding.tvHeroDepartment.text = "${course.department} • Virtual University"
        binding.tvHeroTitle.text = course.title

        val currentLectureIndex = if (course.lastWatchedLectureIndex > 0) course.lastWatchedLectureIndex else 1
        binding.tvHeroProgress.text = "Progress: Lecture $currentLectureIndex of ${course.videoCount}"
        binding.tvResumeLectureTitle.text = "Lecture ${String.format("%02d", currentLectureIndex)} - ${course.courseCode ?: course.title}"

        // Clicking the resume bar starts the player directly
        binding.btnResumeCourse.setOnClickListener {
            val lecture = Lecture(
                playlistId = course.playlistId,
                lectureIndex = currentLectureIndex,
                title = "Lecture ${String.format("%02d", currentLectureIndex)} - ${course.title}",
                videoId = if (currentLectureIndex == 1) course.firstVideoId else null,
                thumbnailUrl = course.thumbnailUrl
            )
            val mainAct = activity as? MainActivity
            if (mainAct != null) {
                mainAct.playCourse(course, lecture)
            } else {
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("EXTRA_COURSE", course)
                    putExtra("EXTRA_LECTURE", lecture)
                }
                startActivity(intent)
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        // Clicking the card itself opens the course syllabus/detail page
        binding.cardHeroCourse.setOnClickListener {
            openCourseDetail(course)
        }
    }

    private fun openCourseDetail(course: Course) {
        val intent = Intent(requireContext(), CourseDetailActivity::class.java).apply {
            putExtra("EXTRA_COURSE", course)
        }
        startActivity(intent)
        activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
