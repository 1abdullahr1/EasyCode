package com.vu.lecturehub.ui.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.R
import com.vu.lecturehub.databinding.ItemSubjectChipBinding

class SubjectChipAdapter(
    private val subjects: List<String>,
    private val onSubjectSelected: (String) -> Unit
) : RecyclerView.Adapter<SubjectChipAdapter.ViewHolder>() {

    var selectedSubject: String = "All"
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(subjects[position])
    }

    override fun getItemCount(): Int = subjects.size

    inner class ViewHolder(
        private val binding: ItemSubjectChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: String) {
            binding.tvSubjectName.text = subject
            val isSelected = subject.equals(selectedSubject, ignoreCase = true)

            if (isSelected) {
                binding.tvSubjectName.setBackgroundResource(R.drawable.bg_filter_pill_active)
                binding.tvSubjectName.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
            } else {
                binding.tvSubjectName.setBackgroundResource(R.drawable.bg_filter_pill)
                val typedValue = TypedValue()
                itemView.context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
                binding.tvSubjectName.setTextColor(typedValue.data)
            }

            binding.root.setOnClickListener {
                selectedSubject = subject
                onSubjectSelected(subject)
            }
        }
    }
}
