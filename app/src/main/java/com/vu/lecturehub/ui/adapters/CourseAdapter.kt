package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.databinding.ItemCourseCardBinding

class CourseAdapter(
    private val onCourseClick: (Course) -> Unit,
    private val onPlayClick: (Course) -> Unit,
    private val onBookmarkClick: (Course) -> Unit
) : ListAdapter<Course, CourseAdapter.CourseViewHolder>(CourseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CourseViewHolder(
        private val binding: ItemCourseCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.courseTitle.text = course.title
            binding.departmentName.text = course.department
            binding.courseCodeBadge.text = course.courseCode ?: "VU"

            val countText = if (course.videoCount > 0) "${course.videoCount} lectures" else "Course"
            binding.videoCount.text = countText

            // Bookmark icon state
            if (course.isBookmarked) {
                binding.btnBookmark.setImageResource(R.drawable.ic_bookmark)
            } else {
                binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_outlined)
            }

            // Thumbnail loading with Coil
            binding.thumbnail.load(course.thumbnailUrl) {
                crossfade(150)
                placeholder(R.drawable.playlist_placeholder)
                error(R.drawable.playlist_placeholder)
            }

            // Tap on thumbnail or play button -> plays video immediately
            binding.cardThumbnail.setOnClickListener {
                onPlayClick(course)
            }
            binding.btnThumbnailPlay.setOnClickListener {
                onPlayClick(course)
            }

            // Tap on card body -> opens course overview
            binding.root.setOnClickListener {
                onCourseClick(course)
            }

            // Bookmark toggle
            binding.btnBookmark.setOnClickListener {
                onBookmarkClick(course)
            }
        }
    }

    class CourseDiffCallback : DiffUtil.ItemCallback<Course>() {
        override fun areItemsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem.playlistId == newItem.playlistId
        }

        override fun areContentsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem == newItem
        }
    }
}
