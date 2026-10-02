package com.easycode.ide.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.easycode.ide.R
import com.easycode.ide.data.model.ExternalResource
import com.easycode.ide.databinding.BottomSheetResourcesBinding
import com.easycode.ide.ui.EasyCodeViewModel
import com.easycode.ide.ui.adapters.ResourceRowAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class ResourcesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetResourcesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EasyCodeViewModel by activityViewModels()
    private val resourceAdapter = ResourceRowAdapter { resource ->
        viewModel.removeExternalResource(resource.id)
    }

    private val popularLibraries = listOf(
        ExternalResource(name = "Tailwind CSS", url = "https://cdn.tailwindcss.com", isCss = false),
        ExternalResource(name = "Bootstrap 5 CSS", url = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css", isCss = true),
        ExternalResource(name = "Bootstrap 5 JS", url = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js", isCss = false),
        ExternalResource(name = "Font Awesome 6", url = "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css", isCss = true),
        ExternalResource(name = "Animate.css", url = "https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css", isCss = true),
        ExternalResource(name = "jQuery 3.7", url = "https://code.jquery.com/jquery-3.7.1.min.js", isCss = false),
        ExternalResource(name = "Vue 3", url = "https://unpkg.com/vue@3/dist/vue.global.js", isCss = false),
        ExternalResource(name = "React 18", url = "https://unpkg.com/react@18/umd/react.production.min.js", isCss = false),
        ExternalResource(name = "ReactDOM 18", url = "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js", isCss = false),
        ExternalResource(name = "Babel", url = "https://unpkg.com/@babel/standalone/babel.min.js", isCss = false),
        ExternalResource(name = "Axios", url = "https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js", isCss = false),
        ExternalResource(name = "Lodash", url = "https://cdnjs.cloudflare.com/ajax/libs/lodash.js/4.17.21/lodash.min.js", isCss = false),
        ExternalResource(name = "Chart.js", url = "https://cdn.jsdelivr.net/npm/chart.js", isCss = false),
        ExternalResource(name = "Three.js", url = "https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js", isCss = false)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupPopularChips()
        setupAddResource()
        observeResources()
    }

    private fun setupRecyclerView() {
        binding.rvActiveResources.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = resourceAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupPopularChips() {
        binding.chipGroupPopularCdns.removeAllViews()
        for (lib in popularLibraries) {
            val chip = Chip(requireContext()).apply {
                text = lib.name
                isCheckable = false
                isClickable = true
                setChipBackgroundColorResource(R.color.surfaceVariant)
                setTextColor(resources.getColor(R.color.onSurface, null))
                setOnClickListener {
                    viewModel.addExternalResource(lib)
                    Toast.makeText(requireContext(), "Added ${lib.name}", Toast.LENGTH_SHORT).show()
                }
            }
            binding.chipGroupPopularCdns.addView(chip)
        }
    }

    private fun setupAddResource() {
        binding.btnAddResource.setOnClickListener {
            val url = binding.etResourceUrl.text?.toString()?.trim().orEmpty()
            if (url.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a valid CDN URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isCss = url.endsWith(".css") || url.contains("/css")
            val name = url.substringAfterLast("/").substringBefore("?").ifBlank { "External Library" }

            val newRes = ExternalResource(name = name, url = url, isCss = isCss)
            viewModel.addExternalResource(newRes)
            binding.etResourceUrl.setText("")
            Toast.makeText(requireContext(), "Added $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeResources() {
        viewModel.externalResources.observe(viewLifecycleOwner) { resources ->
            resourceAdapter.submitList(resources)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ResourcesBottomSheet"
        fun newInstance() = ResourcesBottomSheet()
    }
}
