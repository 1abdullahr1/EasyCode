package com.vu.lecturehub.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.R
import com.vu.lecturehub.databinding.ItemDiscoverHeaderBinding

class DiscoverHeaderAdapter(
    private val subjectAdapter: SubjectAdapter,
    private val onSearchQueryChanged: (String) -> Unit,
    private val onSearchSubmitted: (String) -> Unit,
    private val onSearchFocused: () -> Unit,
    private val onTrendingChipClicked: (String) -> Unit,
    private val onResetFilterClicked: () -> Unit,
    private val onOpenFiltersClicked: () -> Unit,
    private val onClearFiltersClicked: () -> Unit
) : RecyclerView.Adapter<DiscoverHeaderAdapter.HeaderViewHolder>() {

    private var currentCatalogTitle = "Explore all courses"
    private var isFilterActive = false
    private var activeFilterCount = 0
    private var currentSearchQuery = ""
    private var activeViewHolder: HeaderViewHolder? = null

    fun updateCatalogHeader(title: String, isFiltered: Boolean, filterCount: Int = 0) {
        currentCatalogTitle = title
        isFilterActive = isFiltered
        activeFilterCount = filterCount
        activeViewHolder?.updateHeaderState() ?: notifyItemChanged(0)
    }

    fun setSearchQueryText(text: String) {
        currentSearchQuery = text
        activeViewHolder?.let { vh ->
            if (vh.b.etHeaderSearch.text.toString() != text) {
                vh.b.etHeaderSearch.setText(text)
                vh.b.etHeaderSearch.setSelection(text.length)
            }
        } ?: notifyItemChanged(0)
    }

    fun clearSearchInput() {
        currentSearchQuery = ""
        activeViewHolder?.b?.etHeaderSearch?.text?.clear() ?: notifyItemChanged(0)
    }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val b = ItemDiscoverHeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HeaderViewHolder(b)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        activeViewHolder = holder
        holder.bind()
    }

    override fun onViewRecycled(holder: HeaderViewHolder) {
        super.onViewRecycled(holder)
        if (activeViewHolder == holder) {
            activeViewHolder = null
        }
    }

    inner class HeaderViewHolder(
        val b: ItemDiscoverHeaderBinding
    ) : RecyclerView.ViewHolder(b.root) {

        init {
            // Setup Popular Subjects 2-Row Horizontal Scrolling Grid
            b.rvPopularSubjects.apply {
                layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
                adapter = subjectAdapter
                setHasFixedSize(true)
                isNestedScrollingEnabled = false

                addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                        when (e.action) {
                            MotionEvent.ACTION_DOWN -> rv.parent.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> rv.parent.requestDisallowInterceptTouchEvent(false)
                        }
                        return false
                    }
                    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                })
            }

            // Filter Trigger Bar Listeners
            b.btnOpenFilters.setOnClickListener {
                onOpenFiltersClicked()
            }

            b.btnClearAllFilters.setOnClickListener {
                onClearFiltersClicked()
            }

            b.btnResetSubjectFilter.setOnClickListener {
                onResetFilterClicked()
            }

            // Search EditText Listeners
            b.etHeaderSearch.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    onSearchFocused()
                }
            }

            b.etHeaderSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s?.toString() ?: ""
                    currentSearchQuery = query
                    b.btnHeaderClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                    onSearchQueryChanged(query)
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            b.etHeaderSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val query = b.etHeaderSearch.text.toString().trim()
                    onSearchSubmitted(query)
                    true
                } else {
                    false
                }
            }

            b.btnHeaderSearchAction.setOnClickListener {
                val query = b.etHeaderSearch.text.toString().trim()
                onSearchSubmitted(query)
            }

            b.btnHeaderClearSearch.setOnClickListener {
                b.etHeaderSearch.text.clear()
                onSearchQueryChanged("")
            }

            // Trending Quick Search Chips
            b.chipTrendCs101.setOnClickListener { onTrendingChipClicked("CS101") }
            b.chipTrendCs201.setOnClickListener { onTrendingChipClicked("CS201") }
            b.chipTrendPython.setOnClickListener { onTrendingChipClicked("Python") }
            b.chipTrendData.setOnClickListener { onTrendingChipClicked("Data") }
            b.chipTrendCalculus.setOnClickListener { onTrendingChipClicked("Calculus") }
            b.chipTrendAccounting.setOnClickListener { onTrendingChipClicked("Accounting") }
        }

        fun bind() {
            updateHeaderState()
            if (b.etHeaderSearch.text.toString() != currentSearchQuery) {
                b.etHeaderSearch.setText(currentSearchQuery)
                b.etHeaderSearch.setSelection(currentSearchQuery.length)
            }
            b.btnHeaderClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
        }

        fun updateHeaderState() {
            b.tvCatalogSectionTitle.text = currentCatalogTitle
            b.btnResetSubjectFilter.visibility = if (isFilterActive) View.VISIBLE else View.GONE

            // Filter Bar Status
            if (activeFilterCount > 0) {
                b.tvFilterStatus.text = "Filters ($activeFilterCount selected)"
                b.btnOpenFilters.setBackgroundResource(R.drawable.bg_filter_pill_active)
                b.btnClearAllFilters.visibility = View.VISIBLE
            } else {
                b.tvFilterStatus.text = "All filters"
                b.btnOpenFilters.setBackgroundResource(R.drawable.bg_filter_pill)
                b.btnClearAllFilters.visibility = View.GONE
            }
        }
    }
}
