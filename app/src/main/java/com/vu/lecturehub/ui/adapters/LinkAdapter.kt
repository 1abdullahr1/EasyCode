package com.vu.lecturehub.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.data.model.ResourceLink
import com.vu.lecturehub.databinding.ItemLinkRowBinding

class LinkAdapter(
    initialItems: List<ResourceLink> = emptyList(),
    private val onItemClick: (ResourceLink) -> Unit
) : RecyclerView.Adapter<LinkAdapter.ViewHolder>() {

    private val items = initialItems.toMutableList()

    fun updateItems(newItems: List<ResourceLink>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLinkRowBinding.inflate(
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
        private val binding: ItemLinkRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ResourceLink) {
            binding.tvLinkTitle.text = item.title
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
