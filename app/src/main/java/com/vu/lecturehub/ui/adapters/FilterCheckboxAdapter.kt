package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.data.model.FilterItem
import com.vu.lecturehub.databinding.ItemFilterCheckboxBinding

class FilterCheckboxAdapter(
    initialItems: List<FilterItem>,
    private val onItemToggled: (FilterItem, Boolean) -> Unit
) : RecyclerView.Adapter<FilterCheckboxAdapter.CheckboxViewHolder>() {

    private val items = initialItems.toMutableList()

    fun updateItems(newItems: List<FilterItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun uncheckAll() {
        items.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckboxViewHolder {
        val binding = ItemFilterCheckboxBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CheckboxViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CheckboxViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CheckboxViewHolder(
        private val binding: ItemFilterCheckboxBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterItem) {
            binding.cbFilterItem.text = "${item.displayName} (${item.count})"
            binding.cbFilterItem.isChecked = item.isSelected

            binding.root.setOnClickListener {
                val newChecked = !item.isSelected
                item.isSelected = newChecked
                binding.cbFilterItem.isChecked = newChecked
                onItemToggled(item, newChecked)
            }
        }
    }
}
