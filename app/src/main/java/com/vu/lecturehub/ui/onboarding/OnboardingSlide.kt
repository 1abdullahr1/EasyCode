package com.vu.lecturehub.ui.onboarding

import androidx.annotation.DrawableRes

data class OnboardingSlide(
    val title: String,
    val description: String,
    @DrawableRes val illustrationRes: Int
)
