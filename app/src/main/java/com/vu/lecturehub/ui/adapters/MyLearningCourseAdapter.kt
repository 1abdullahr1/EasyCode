package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.databinding.ItemMyLearningCourseBinding

class MyLearningCourseAdapter(
    private val onCourseClick: (Course) -> Unit,
    private val onPlayClick: (Course) -> Unit
) : RecyclerView.Adapter<MyLearningCourseAdapter.ViewHolder>() {

    private val courses = mutableListOf<Course>()
    var isExpanded = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submitList(list: List<Course>) {
        courses.clear()
        courses.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyLearningCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int {
        return if (!isExpanded && courses.size > 4) 4 else courses.size
    }

    inner class ViewHolder(
        private val binding: ItemMyLearningCourseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.courseTitle.text = course.title
            binding.courseCodeBadge.text = course.courseCode ?: "VU"

            val countText = if (course.videoCount > 0) "${course.videoCount} lectures" else "Course"
            binding.videoCount.text = countText

            val lastIdx = if (course.lastWatchedLectureIndex > 0) course.lastWatchedLectureIndex else 1
            binding.tvContinueLecture.text = itemView.context.getString(R.string.continue_lecture_format, lastIdx)

            val total = if (course.videoCount > 0) course.videoCount else 45
            val percent = ((lastIdx.toFloat() / total) * 100).toInt().coerceIn(1, 100)
            binding.pbCourseProgress.progress = percent
            binding.tvProgressPercent.text = itemView.context.getString(R.string.progress_format, percent)

            binding.thumbnail.load(course.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.playlist_placeholder)
                error(R.drawable.playlist_placeholder)
            }

            binding.cardThumbnail.setOnClickListener { onPlayClick(course) }
            binding.btnThumbnailPlay.setOnClickListener { onPlayClick(course) }
            binding.root.setOnClickListener { onCourseClick(course) }
        }
    }
}
