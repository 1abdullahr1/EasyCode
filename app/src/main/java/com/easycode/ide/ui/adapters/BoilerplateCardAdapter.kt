package com.easycode.ide.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.easycode.ide.data.model.Boilerplate
import com.easycode.ide.databinding.ItemBoilerplateCardBinding

class BoilerplateCardAdapter(
    private val boilerplates: List<Boilerplate>,
    private val onBoilerplateSelected: (Boilerplate) -> Unit
) : RecyclerView.Adapter<BoilerplateCardAdapter.BoilerplateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoilerplateViewHolder {
        val binding = ItemBoilerplateCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BoilerplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoilerplateViewHolder, position: Int) {
        holder.bind(boilerplates[position])
    }

    override fun getItemCount(): Int = boilerplates.size

    inner class BoilerplateViewHolder(private val binding: ItemBoilerplateCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bp: Boilerplate) {
            binding.tvTitle.text = bp.title
            binding.tvTag.text = bp.tag
            binding.tvDescription.text = bp.description

            binding.root.setOnClickListener {
                onBoilerplateSelected(bp)
            }
        }
    }
}
