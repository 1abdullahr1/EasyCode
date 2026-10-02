package com.vu.lecturehub.ui.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.Department
import com.vu.lecturehub.databinding.ItemDepartmentChipBinding

class DepartmentAdapter(
    private var departments: List<Department>,
    private val onDepartmentSelected: (Department) -> Unit
) : RecyclerView.Adapter<DepartmentAdapter.DepartmentViewHolder>() {

    private var selectedIndex = 0

    fun updateDepartments(newDepartments: List<Department>) {
        departments = newDepartments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val binding = ItemDepartmentChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DepartmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        holder.bind(departments[position], position)
    }

    override fun getItemCount(): Int = departments.size

    inner class DepartmentViewHolder(
        private val binding: ItemDepartmentChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(department: Department, position: Int) {
            val isSelected = position == selectedIndex
            binding.departmentName.text = if (department.name == "All") "All Courses" else department.name

            val context = binding.root.context
            if (isSelected) {
                binding.cardDepartment.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary))
                binding.departmentName.setTextColor(Color.WHITE)
            } else {
                binding.cardDepartment.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                binding.departmentName.setTextColor(ContextCompat.getColor(context, R.color.onSurface))
            }

            binding.root.setOnClickListener {
                val previousIndex = selectedIndex
                @Suppress("DEPRECATION")
                val currentPos = adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    selectedIndex = currentPos
                    notifyItemChanged(previousIndex)
                    notifyItemChanged(selectedIndex)
                    onDepartmentSelected(department)
                }
            }
        }
    }
}
