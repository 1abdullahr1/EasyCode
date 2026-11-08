package com.easycode.ide.ui.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.easycode.ide.R
import com.easycode.ide.databinding.BottomSheetAboutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AboutBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val version = runCatching {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        }.getOrNull()
        binding.aboutVersion.text = version?.let { getString(R.string.about_version, it) }.orEmpty()
        binding.aboutWebsite.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URL))
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.about_open_link_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "AboutBottomSheet"
        private const val WEBSITE_URL = "https://ophiralabs.pages.dev/"

        fun newInstance() = AboutBottomSheet()
    }
}
