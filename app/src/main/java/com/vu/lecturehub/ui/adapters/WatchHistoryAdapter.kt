package com.vu.lecturehub.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vu.lecturehub.R
import com.vu.lecturehub.data.model.WatchHistoryItem
import com.vu.lecturehub.databinding.ItemHistoryLectureRowBinding
import com.vu.lecturehub.util.PlaybackManager

class WatchHistoryAdapter(
    private val onItemClick: (WatchHistoryItem) -> Unit
) : RecyclerView.Adapter<WatchHistoryAdapter.ViewHolder>() {

    private val history = mutableListOf<WatchHistoryItem>()
    var isExpanded = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submitList(list: List<WatchHistoryItem>) {
        history.clear()
        history.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryLectureRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(history[position])
    }

    override fun getItemCount(): Int {
        return if (!isExpanded && history.size > 4) 4 else history.size
    }

    inner class ViewHolder(
        private val binding: ItemHistoryLectureRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchHistoryItem) {
            val context = itemView.context
            binding.tvLectureTitle.text = item.lectureTitle
            binding.tvCourseTitle.text = if (!item.courseCode.isNullOrEmpty()) {
                "${item.courseTitle} • ${item.courseCode}"
            } else {
                item.courseTitle
            }

            val relativeTime = DateUtils.getRelativeTimeSpanString(
                item.watchedTimestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.tvRelativeTime.text = relativeTime

            val thumbUrl = item.thumbnailUrl
                ?: if (!item.videoId.isNullOrEmpty()) "https://i.ytimg.com/vi/${item.videoId}/hqdefault.jpg" else null

            binding.thumbnail.load(thumbUrl) {
                crossfade(true)
                placeholder(R.drawable.playlist_placeholder)
                error(R.drawable.playlist_placeholder)
            }

            val pos = PlaybackManager.getPosition(context, item.playlistId, item.lectureIndex)
            val dur = PlaybackManager.getDuration(context, item.playlistId, item.lectureIndex)
            if (dur > 0f && pos > 0f) {
                val pct = ((pos / dur) * 100).toInt().coerceIn(1, 100)
                binding.pbHistoryProgress.progress = pct
                binding.pbHistoryProgress.visibility = View.VISIBLE
            } else {
                binding.pbHistoryProgress.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnPlay.setOnClickListener { onItemClick(item) }
        }
    }
}
