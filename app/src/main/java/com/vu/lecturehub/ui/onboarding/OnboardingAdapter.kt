package com.vu.lecturehub.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.databinding.ItemOnboardingSlideBinding

class OnboardingAdapter(
    private val slides: List<OnboardingSlide>
) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnboardingSlideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    inner class ViewHolder(
        private val binding: ItemOnboardingSlideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(slide: OnboardingSlide) {
            binding.tvTitle.text = slide.title
            binding.tvDescription.text = slide.description
            binding.ivIllustration.setImageResource(slide.illustrationRes)
        }
    }
}
