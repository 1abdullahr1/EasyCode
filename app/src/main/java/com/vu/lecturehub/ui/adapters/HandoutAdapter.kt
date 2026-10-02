package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.data.model.HandoutCourse
import com.vu.lecturehub.databinding.ItemHandoutRowBinding

class HandoutAdapter(
    initialItems: List<HandoutCourse> = emptyList(),
    private val onItemClick: (HandoutCourse) -> Unit
) : RecyclerView.Adapter<HandoutAdapter.ViewHolder>() {

    private val items = initialItems.toMutableList()

    fun updateItems(newItems: List<HandoutCourse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHandoutRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemHandoutRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HandoutCourse) {
            binding.tvCourseCode.text = item.courseCode
            binding.tvCourseTitle.text = item.title
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
