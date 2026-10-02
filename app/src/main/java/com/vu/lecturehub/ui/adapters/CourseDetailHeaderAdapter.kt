package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.databinding.ItemCourseDetailHeaderBinding

class CourseDetailHeaderAdapter(
    private var course: Course,
    private val onPlayClick: () -> Unit,
    private val onBookmarkClick: () -> Unit
) : RecyclerView.Adapter<CourseDetailHeaderAdapter.HeaderViewHolder>() {

    fun updateCourse(newCourse: Course) {
        this.course = newCourse
        notifyItemChanged(0)
    }

    fun updateBookmarkState(isBookmarked: Boolean) {
        this.course.isBookmarked = isBookmarked
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val binding = ItemCourseDetailHeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(course)
    }

    override fun getItemCount(): Int = 1

    inner class HeaderViewHolder(
        private val binding: ItemCourseDetailHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.detailTitle.text = course.title
            binding.detailCourseCode.text = course.courseCode ?: "VU"
            binding.detailDepartment.text = course.department

            val countText = if (course.videoCount > 0) "${course.videoCount} Lectures" else "Course Lectures"
            binding.detailLectureCount.text = countText

            updateBookmarkButton(course.isBookmarked)

            binding.detailThumbnail.load(course.thumbnailUrl) {
                crossfade(150)
                placeholder(R.drawable.playlist_placeholder)
                error(R.drawable.playlist_placeholder)
            }

            binding.btnPlayFirst.setOnClickListener {
                onPlayClick()
            }

            binding.btnDetailThumbPlay.setOnClickListener {
                onPlayClick()
            }

            binding.btnBookmarkToggle.setOnClickListener {
                onBookmarkClick()
            }
        }

        private fun updateBookmarkButton(isBookmarked: Boolean) {
            val context = binding.root.context
            if (isBookmarked) {
                binding.btnBookmarkToggle.text = context.getString(R.string.enrolled_course)
                binding.btnBookmarkToggle.setIconResource(R.drawable.ic_bookmark)
            } else {
                binding.btnBookmarkToggle.text = context.getString(R.string.enroll_course)
                binding.btnBookmarkToggle.setIconResource(R.drawable.ic_bookmark_outlined)
            }
        }
    }
}
