package com.easycode.ide.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.easycode.ide.data.model.ExternalResource
import com.easycode.ide.databinding.ItemResourceRowBinding

class ResourceRowAdapter(
    private val onDeleteResource: (ExternalResource) -> Unit
) : ListAdapter<ExternalResource, ResourceRowAdapter.ResourceViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val binding = ItemResourceRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ResourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ResourceViewHolder(private val binding: ItemResourceRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(res: ExternalResource) {
            binding.tvResName.text = res.name
            binding.tvResUrl.text = res.url
            binding.tvResType.text = if (res.isCss) "CSS" else "JS"

            binding.btnDeleteRes.setOnClickListener {
                onDeleteResource(res)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ExternalResource>() {
            override fun areItemsTheSame(oldItem: ExternalResource, newItem: ExternalResource): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ExternalResource, newItem: ExternalResource): Boolean {
                return oldItem == newItem
            }
        }
    }
}
