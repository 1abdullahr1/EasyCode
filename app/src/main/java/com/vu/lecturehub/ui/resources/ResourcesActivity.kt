package com.vu.lecturehub.ui.resources

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.vu.lecturehub.R
import com.vu.lecturehub.data.repository.ResourcesRepository
import com.vu.lecturehub.databinding.ActivityResourcesBinding
import com.vu.lecturehub.ui.adapters.HandoutAdapter
import com.vu.lecturehub.ui.adapters.LinkAdapter

class ResourcesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResourcesBinding
    private lateinit var searchHandoutAdapter: HandoutAdapter
    private lateinit var searchLinkAdapter: LinkAdapter
    private var isSearchActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResourcesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPager()
        setupSearch()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finishWithTransition()
        }

        binding.btnSearch.setOnClickListener {
            openSearchMode()
        }

        binding.btnCloseSearch.setOnClickListener {
            closeSearchMode()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchResources.text.clear()
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> HandoutsFragment()
                    1 -> LinksFragment()
                    2 -> ToolsFragment()
                    else -> HandoutsFragment()
                }
            }
        }

        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_handouts)
                1 -> getString(R.string.tab_links)
                2 -> getString(R.string.tab_tools)
                else -> ""
            }
        }.attach()
    }

    private fun setupSearch() {
        searchHandoutAdapter = HandoutAdapter(emptyList()) { item ->
            ResourcesRepository.openHandoutInBrowser(this@ResourcesActivity, item.courseCode, item.title)
        }

        searchLinkAdapter = LinkAdapter(emptyList()) { item ->
            ResourcesRepository.openUrlInApp(this@ResourcesActivity, item.url, item.title)
        }

        val concatAdapter = ConcatAdapter(searchHandoutAdapter, searchLinkAdapter)

        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(this@ResourcesActivity)
            adapter = concatAdapter
            setHasFixedSize(true)
        }

        binding.etSearchResources.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                performSearch(query)
            }
        })
    }

    private fun openSearchMode() {
        isSearchActive = true
        binding.layoutNormalHeader.visibility = View.GONE
        binding.layoutSearchHeader.visibility = View.VISIBLE
        binding.layoutTabsContent.visibility = View.GONE
        binding.layoutSearchContainer.visibility = View.VISIBLE

        binding.etSearchResources.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.etSearchResources, InputMethodManager.SHOW_IMPLICIT)

        val currentQuery = binding.etSearchResources.text.toString().trim()
        performSearch(currentQuery)
    }

    private fun closeSearchMode() {
        isSearchActive = false
        hideKeyboard()

        binding.etSearchResources.text.clear()
        binding.layoutSearchHeader.visibility = View.GONE
        binding.layoutNormalHeader.visibility = View.VISIBLE
        binding.layoutSearchContainer.visibility = View.GONE
        binding.layoutTabsContent.visibility = View.VISIBLE
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            searchHandoutAdapter.updateItems(emptyList())
            searchLinkAdapter.updateItems(emptyList())
            binding.layoutSearchEmpty.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
            return
        }

        val handouts = ResourcesRepository.searchHandouts(this@ResourcesActivity, query)
        val links = ResourcesRepository.searchLinks(query)

        searchHandoutAdapter.updateItems(handouts)
        searchLinkAdapter.updateItems(links)

        val totalEmpty = handouts.isEmpty() && links.isEmpty()
        if (totalEmpty) {
            binding.layoutSearchEmpty.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
        } else {
            binding.layoutSearchEmpty.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etSearchResources.windowToken, 0)
    }

    private fun finishWithTransition() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onBackPressed() {
        if (isSearchActive) {
            closeSearchMode()
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
