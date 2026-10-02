package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.databinding.ItemCompactCourseCardBinding

class CompactCourseAdapter(
    initialCourses: List<Course>,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<CompactCourseAdapter.CompactCourseViewHolder>() {

    private val courses = initialCourses.toMutableList()

    fun updateCourses(newCourses: List<Course>) {
        courses.clear()
        courses.addAll(newCourses)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompactCourseViewHolder {
        val binding = ItemCompactCourseCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompactCourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompactCourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size

    inner class CompactCourseViewHolder(
        private val binding: ItemCompactCourseCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.tvCompactCode.text = course.courseCode ?: "VU"
            binding.tvCompactTitle.text = course.title
            binding.ivCompactThumbnail.load(course.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.playlist_placeholder)
                error(R.drawable.playlist_placeholder)
            }
            binding.root.setOnClickListener {
                onCourseClick(course)
            }
        }
    }
}
