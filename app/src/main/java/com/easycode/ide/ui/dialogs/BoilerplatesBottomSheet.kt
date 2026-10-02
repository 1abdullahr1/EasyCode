package com.easycode.ide.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.easycode.ide.data.repository.BoilerplateRepository
import com.easycode.ide.databinding.BottomSheetBoilerplatesBinding
import com.easycode.ide.ui.EasyCodeViewModel
import com.easycode.ide.ui.adapters.BoilerplateCardAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BoilerplatesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBoilerplatesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EasyCodeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBoilerplatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvBoilerplates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = BoilerplateCardAdapter(BoilerplateRepository.boilerplates) { bp ->
                viewModel.loadBoilerplate(bp)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "BoilerplatesBottomSheet"
        fun newInstance() = BoilerplatesBottomSheet()
    }
}
