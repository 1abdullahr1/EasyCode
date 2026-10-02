package com.vu.lecturehub.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.vu.lecturehub.ui.courses.CoursesFragment
import com.vu.lecturehub.ui.home.HomeFragment
import com.vu.lecturehub.ui.saved.SavedFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val TAB_HOME = 0
        const val TAB_COURSES = 1
        const val TAB_SAVED = 2
        const val TAB_COUNT = 3
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_COURSES -> CoursesFragment()
            TAB_SAVED -> SavedFragment()
            else -> HomeFragment()
        }
    }
}
