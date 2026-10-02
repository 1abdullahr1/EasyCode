package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Lecture
import com.vu.lecturehub.databinding.ItemLectureRowBinding

class LectureAdapter(
    initialLectures: List<Lecture>,
    private val onLectureClick: (Lecture) -> Unit
) : RecyclerView.Adapter<LectureAdapter.LectureViewHolder>() {

    private val lectures = initialLectures.toMutableList()

    fun updateLectures(newLectures: List<Lecture>) {
        lectures.clear()
        lectures.addAll(newLectures)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LectureViewHolder {
        val binding = ItemLectureRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LectureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LectureViewHolder, position: Int) {
        holder.bind(lectures[position])
    }

    override fun getItemCount(): Int = lectures.size

    inner class LectureViewHolder(
        private val binding: ItemLectureRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(lecture: Lecture) {
            val formattedNum = String.format("#%02d", lecture.lectureIndex)
            binding.lectureNumberBadge.text = formattedNum
            binding.lectureTitle.text = lecture.title

            if (lecture.isCompleted) {
                binding.lectureStatus.text = "✓ Completed"
                binding.lectureStatus.setTextColor(binding.root.context.getColor(R.color.secondary))
            } else {
                binding.lectureStatus.text = "Tap to play"
                binding.lectureStatus.setTextColor(binding.root.context.getColor(R.color.primary))
            }

            binding.lectureThumbnail.load(lecture.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.playlist_placeholder)
                error(R.drawable.playlist_placeholder)
            }

            binding.root.setOnClickListener {
                onLectureClick(lecture)
            }
        }
    }
}
