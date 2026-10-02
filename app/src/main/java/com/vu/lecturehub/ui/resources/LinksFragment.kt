package com.vu.lecturehub.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.vu.lecturehub.data.repository.ResourcesRepository
import com.vu.lecturehub.databinding.FragmentResourceLinksBinding
import com.vu.lecturehub.ui.adapters.LinkAdapter

class LinksFragment : Fragment() {

    private var _binding: FragmentResourceLinksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourceLinksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LinkAdapter(ResourcesRepository.links) { item ->
            ResourcesRepository.openUrlInApp(requireContext(), item.url, item.title)
        }

        binding.rvLinks.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
            setHasFixedSize(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
