package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Course
import com.vu.lecturehub.databinding.ItemSearchSuggestionBinding

class SearchSuggestionAdapter(
    initialCourses: List<Course>,
    private val onSuggestionClick: (Course) -> Unit
) : RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder>() {

    private val courses = initialCourses.toMutableList()

    fun updateSuggestions(newCourses: List<Course>) {
        courses.clear()
        courses.addAll(newCourses)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ItemSearchSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size

    inner class SuggestionViewHolder(
        private val binding: ItemSearchSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.tvSuggestionTitle.text = course.title
            binding.tvSuggestionSubtitle.text = "${course.department} • ${course.courseCode ?: "VU"}"
            binding.ivSuggestionThumb.load(course.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.playlist_placeholder)
                error(R.drawable.playlist_placeholder)
            }
            binding.root.setOnClickListener {
                onSuggestionClick(course)
            }
        }
    }
}
