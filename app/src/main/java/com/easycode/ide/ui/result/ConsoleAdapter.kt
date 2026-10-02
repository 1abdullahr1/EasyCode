package com.easycode.ide.ui.result

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.easycode.ide.R
import com.easycode.ide.data.model.ConsoleLevel
import com.easycode.ide.data.model.ConsoleMessage
import com.easycode.ide.databinding.ItemConsoleMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsoleAdapter : ListAdapter<ConsoleMessage, ConsoleAdapter.ConsoleViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsoleViewHolder {
        val binding = ItemConsoleMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConsoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConsoleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConsoleViewHolder(private val binding: ItemConsoleMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ConsoleMessage) {
            binding.tvMessage.text = item.message
            binding.tvTime.text = timeFormat.format(Date(item.timestamp))

            if (item.lineNumber > 0) {
                binding.tvLine.visibility = View.VISIBLE
                binding.tvLine.text = ":${item.lineNumber}"
            } else {
                binding.tvLine.visibility = View.GONE
            }

            when (item.level) {
                ConsoleLevel.ERROR -> {
                    binding.tvBadge.text = "ERR"
                    binding.tvBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.console_error))
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_console_badge_error)
                    binding.tvMessage.setTextColor(Color.parseColor("#FCA5A5"))
                }
                ConsoleLevel.WARN -> {
                    binding.tvBadge.text = "WRN"
                    binding.tvBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.console_warn))
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_console_badge_warn)
                    binding.tvMessage.setTextColor(Color.parseColor("#FDE68A"))
                }
                ConsoleLevel.INFO -> {
                    binding.tvBadge.text = "INF"
                    binding.tvBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.console_info))
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_console_badge_info)
                    binding.tvMessage.setTextColor(Color.parseColor("#BAE6FD"))
                }
                ConsoleLevel.LOG -> {
                    binding.tvBadge.text = "LOG"
                    binding.tvBadge.setTextColor(Color.parseColor("#A1A1AA"))
                    binding.tvBadge.setBackgroundResource(R.drawable.bg_console_badge_info)
                    binding.tvMessage.setTextColor(Color.parseColor("#E4E4E7"))
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ConsoleMessage>() {
            override fun areItemsTheSame(oldItem: ConsoleMessage, newItem: ConsoleMessage): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ConsoleMessage, newItem: ConsoleMessage): Boolean {
                return oldItem == newItem
            }
        }
    }
}
