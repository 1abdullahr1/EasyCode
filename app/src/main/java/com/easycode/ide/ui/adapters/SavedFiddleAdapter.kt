package com.easycode.ide.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.easycode.ide.data.model.FiddleEntity
import com.easycode.ide.databinding.ItemSavedFiddleBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedFiddleAdapter(
    private val onFiddleClick: (FiddleEntity) -> Unit,
    private val onDeleteClick: (FiddleEntity) -> Unit
) : ListAdapter<FiddleEntity, SavedFiddleAdapter.FiddleViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FiddleViewHolder {
        val binding = ItemSavedFiddleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FiddleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FiddleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FiddleViewHolder(private val binding: ItemSavedFiddleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fiddle: FiddleEntity) {
            binding.tvFiddleTitle.text = fiddle.title.ifBlank { "Untitled fiddle" }
            binding.tvFiddleDate.text = dateFormat.format(Date(fiddle.updatedAt))

            binding.root.setOnClickListener {
                onFiddleClick(fiddle)
            }

            binding.btnDeleteFiddle.setOnClickListener {
                onDeleteClick(fiddle)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<FiddleEntity>() {
            override fun areItemsTheSame(oldItem: FiddleEntity, newItem: FiddleEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FiddleEntity, newItem: FiddleEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
