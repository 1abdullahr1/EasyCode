package com.easycode.ide.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.easycode.ide.ui.editor.EditorFragment
import com.easycode.ide.ui.result.ResultFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragments = listOf(
        EditorFragment.newInstance(EditorFragment.TYPE_HTML),
        EditorFragment.newInstance(EditorFragment.TYPE_CSS),
        EditorFragment.newInstance(EditorFragment.TYPE_JS),
        ResultFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getFragmentAt(position: Int): Fragment? {
        return if (position in fragments.indices) fragments[position] else null
    }

    companion object {
        const val TAB_HTML = 0
        const val TAB_CSS = 1
        const val TAB_JS = 2
        const val TAB_RESULT = 3
    }
}
