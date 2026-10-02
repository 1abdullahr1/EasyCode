package com.vu.lecturehub.ui.detail

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Lecture
import com.vu.lecturehub.data.repository.CourseRepository
import com.vu.lecturehub.databinding.ActivityCourseDetailBinding
import com.vu.lecturehub.ui.adapters.CourseDetailHeaderAdapter
import com.vu.lecturehub.ui.adapters.LectureAdapter
import com.vu.lecturehub.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class CourseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailBinding
    private lateinit var repository: CourseRepository
    private var currentCourse: Course? = null
    private var currentLectures: List<Lecture> = emptyList()
    private lateinit var headerAdapter: CourseDetailHeaderAdapter
    private lateinit var lectureAdapter: LectureAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CourseRepository(this)

        @Suppress("DEPRECATION")
        currentCourse = intent.getSerializableExtra("EXTRA_COURSE") as? Course

        if (currentCourse == null) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView(currentCourse!!)
        loadRealLectures(currentCourse!!)

        lifecycleScope.launch {
            if (currentCourse!!.lastWatchedTimestamp == 0L) {
                repository.updateWatchProgress(currentCourse!!.playlistId, 1)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun setupRecyclerView(course: Course) {
        headerAdapter = CourseDetailHeaderAdapter(
            course = course,
            onPlayClick = {
                val lectureIndex = if (course.lastWatchedLectureIndex > 0) course.lastWatchedLectureIndex else 1
                val targetLecture = currentLectures.find { it.lectureIndex == lectureIndex }
                    ?: currentLectures.firstOrNull()
                    ?: Lecture(
                        playlistId = course.playlistId,
                        lectureIndex = 1,
                        title = "Lecture 01 - ${course.title}",
                        videoId = course.firstVideoId,
                        thumbnailUrl = course.thumbnailUrl
                    )
                playLecture(course, targetLecture)
            },
            onBookmarkClick = {
                lifecycleScope.launch {
                    repository.toggleBookmark(course)
                    course.isBookmarked = !course.isBookmarked
                    headerAdapter.updateBookmarkState(course.isBookmarked)
                }
            }
        )

        currentLectures = repository.generateLectures(course)
        lectureAdapter = LectureAdapter(currentLectures) { lecture ->
            playLecture(course, lecture)
        }

        val concatAdapter = ConcatAdapter(headerAdapter, lectureAdapter)

        binding.rvDetailContent.apply {
            layoutManager = LinearLayoutManager(this@CourseDetailActivity)
            adapter = concatAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadRealLectures(course: Course) {
        lifecycleScope.launch {
            val realLectures = repository.fetchPlaylistVideos(course)
            if (realLectures.isNotEmpty()) {
                currentLectures = realLectures
                lectureAdapter.updateLectures(realLectures)
            }
        }
    }

    private fun playLecture(course: Course, lecture: Lecture) {
        val intent = Intent(this, com.vu.lecturehub.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_PLAY_COURSE", course)
            putExtra("EXTRA_PLAY_LECTURE", lecture)
        }
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
