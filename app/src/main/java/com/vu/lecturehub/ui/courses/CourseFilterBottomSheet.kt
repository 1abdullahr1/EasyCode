package com.vu.lecturehub.ui.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vu.lecturehub.databinding.LayoutFilterBottomSheetBinding
import com.vu.lecturehub.ui.MainViewModel
import com.vu.lecturehub.ui.adapters.FilterCheckboxAdapter

class CourseFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var subjectAdapter: FilterCheckboxAdapter
    private lateinit var skillAdapter: FilterCheckboxAdapter

    private val tempSelectedSubjects = mutableSetOf<String>()
    private val tempSelectedSkills = mutableSetOf<String>()

    private var isSubjectExpanded = true
    private var isSkillExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Expand sheet by default
        dialog?.let { d ->
            val sheet = d as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            sheet.behavior.skipCollapsed = true
        }

        // Initialize temporary selection state from ViewModel
        val currentFilterState = viewModel.filterState.value
        currentFilterState?.let {
            tempSelectedSubjects.addAll(it.selectedSubjects)
            tempSelectedSkills.addAll(it.selectedSkills)
        }

        setupAdapters()
        setupAccordionListeners()
        setupActionButtons()
        observeData()
        updateHeaderAndCounts()
    }

    private fun setupAdapters() {
        subjectAdapter = FilterCheckboxAdapter(emptyList()) { item, isChecked ->
            if (isChecked) {
                tempSelectedSubjects.add(item.id)
            } else {
                tempSelectedSubjects.remove(item.id)
            }
            updateHeaderAndCounts()
        }
        binding.rvFilterSubjects.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subjectAdapter
        }

        skillAdapter = FilterCheckboxAdapter(emptyList()) { item, isChecked ->
            if (isChecked) {
                tempSelectedSkills.add(item.id)
            } else {
                tempSelectedSkills.remove(item.id)
            }
            updateHeaderAndCounts()
        }
        binding.rvFilterSkills.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = skillAdapter
        }
    }

    private fun setupAccordionListeners() {
        // 1. Subject Accordion Toggle
        binding.layoutSubjectHeader.setOnClickListener {
            isSubjectExpanded = !isSubjectExpanded
            binding.layoutSubjectContent.visibility = if (isSubjectExpanded) View.VISIBLE else View.GONE
            binding.ivSubjectChevron.animate()
                .rotation(if (isSubjectExpanded) 180f else 0f)
                .setDuration(200)
                .start()
        }

        // 2. Skills Accordion Toggle
        binding.layoutSkillHeader.setOnClickListener {
            isSkillExpanded = !isSkillExpanded
            binding.layoutSkillContent.visibility = if (isSkillExpanded) View.VISIBLE else View.GONE
            binding.ivSkillChevron.animate()
                .rotation(if (isSkillExpanded) 180f else 0f)
                .setDuration(200)
                .start()
        }
    }

    private fun setupActionButtons() {
        binding.btnCloseSheet.setOnClickListener {
            dismiss()
        }

        binding.btnSheetClearAll.setOnClickListener {
            tempSelectedSubjects.clear()
            tempSelectedSkills.clear()

            subjectAdapter.uncheckAll()
            skillAdapter.uncheckAll()

            updateHeaderAndCounts()
        }

        binding.btnSheetApply.setOnClickListener {
            viewModel.updateFilters(
                selectedSubjects = tempSelectedSubjects.toSet(),
                selectedSkills = tempSelectedSkills.toSet()
            )
            dismiss()
        }
    }

    private fun observeData() {
        viewModel.availableSubjects.observe(viewLifecycleOwner) { subjects ->
            // Clone items and apply temporary selection state
            val cloned = subjects.map { item ->
                item.copy(isSelected = item.id in tempSelectedSubjects)
            }
            subjectAdapter.updateItems(cloned)
            updateHeaderAndCounts()
        }

        viewModel.availableSkills.observe(viewLifecycleOwner) { skills ->
            val cloned = skills.map { item ->
                item.copy(isSelected = item.id in tempSelectedSkills)
            }
            skillAdapter.updateItems(cloned)
            updateHeaderAndCounts()
        }
    }

    private fun updateHeaderAndCounts() {
        val subjectCount = tempSelectedSubjects.size
        val skillCount = tempSelectedSkills.size
        val totalCount = subjectCount + skillCount

        // Subject accordion indicator
        if (subjectCount > 0) {
            binding.tvSubjectSelectedCount.visibility = View.VISIBLE
            binding.tvSubjectSelectedCount.text = "$subjectCount selected"
        } else {
            binding.tvSubjectSelectedCount.visibility = View.GONE
        }

        // Skills accordion indicator
        if (skillCount > 0) {
            binding.tvSkillSelectedCount.visibility = View.VISIBLE
            binding.tvSkillSelectedCount.text = "$skillCount selected"
        } else {
            binding.tvSkillSelectedCount.visibility = View.GONE
        }

        // Header active badge
        if (totalCount > 0) {
            binding.tvSheetActiveBadge.visibility = View.VISIBLE
            binding.tvSheetActiveBadge.text = "$totalCount selected"
            binding.btnSheetApply.text = "Apply ($totalCount)"
        } else {
            binding.tvSheetActiveBadge.visibility = View.GONE
            binding.btnSheetApply.text = "Apply"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CourseFilterBottomSheet"

        fun newInstance(): CourseFilterBottomSheet = CourseFilterBottomSheet()
    }
}
