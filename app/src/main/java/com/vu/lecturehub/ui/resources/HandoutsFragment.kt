package com.vu.lecturehub.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vu.lecturehub.data.repository.ResourcesRepository
import com.vu.lecturehub.databinding.FragmentResourceHandoutsBinding
import com.vu.lecturehub.ui.adapters.HandoutAdapter
import com.vu.lecturehub.ui.adapters.SubjectChipAdapter

class HandoutsFragment : Fragment() {

    private var _binding: FragmentResourceHandoutsBinding? = null
    private val binding get() = _binding!!

    private lateinit var subjectAdapter: SubjectChipAdapter
    private lateinit var handoutAdapter: HandoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourceHandoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val departments = ResourcesRepository.getDepartments(context)
        val initialHandouts = ResourcesRepository.getHandouts(context)

        subjectAdapter = SubjectChipAdapter(departments) { selectedDept ->
            val filtered = ResourcesRepository.getHandoutsByDepartment(context, selectedDept)
            handoutAdapter.updateItems(filtered)
            binding.layoutEmptyHandouts.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }

        handoutAdapter = HandoutAdapter(initialHandouts) { item ->
            ResourcesRepository.openHandoutInBrowser(requireContext(), item.courseCode, item.title)
        }

        binding.rvSubjects.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = subjectAdapter
            setHasFixedSize(true)
            addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    rv.parent?.requestDisallowInterceptTouchEvent(true)
                    return false
                }
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }

        binding.rvHandouts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = handoutAdapter
            setHasFixedSize(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
