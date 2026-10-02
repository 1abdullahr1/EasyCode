package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.data.model.Lecture
import com.vu.lecturehub.databinding.ItemPlayerHeaderBinding

class PlayerHeaderAdapter(
    private var course: Course,
    private var currentLecture: Lecture,
    private val onOpenYoutubeClick: () -> Unit
) : RecyclerView.Adapter<PlayerHeaderAdapter.HeaderViewHolder>() {

    fun updateLecture(lecture: Lecture) {
        this.currentLecture = lecture
        notifyItemChanged(0)
    }

    fun updateCourse(course: Course, lecture: Lecture) {
        this.course = course
        this.currentLecture = lecture
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val binding = ItemPlayerHeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(course, currentLecture)
    }

    override fun getItemCount(): Int = 1

    inner class HeaderViewHolder(
        private val binding: ItemPlayerHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course, lecture: Lecture) {
            val formattedNum = String.format("%02d", lecture.lectureIndex)
            binding.tvPlayerCourseCode.text = "${course.courseCode ?: "VU"} - Lecture #$formattedNum"
            binding.tvPlayerLectureTitle.text = lecture.title
            binding.tvPlayerCourseTitle.text = "${course.department} • Virtual University of Pakistan"

            binding.btnOpenYoutube.setOnClickListener {
                onOpenYoutubeClick()
            }
        }
    }
}
