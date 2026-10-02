package com.vu.lecturehub.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vu.lecturehub.R
import com.vu.lecturehub.databinding.LayoutSettingsBottomSheetBinding
import com.vu.lecturehub.util.ThemeManager

class SettingsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutSettingsBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutSettingsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSelector()
        setupAboutButton()

        binding.btnCloseSettings.setOnClickListener {
            dismiss()
        }
    }

    private fun setupThemeSelector() {
        val currentMode = ThemeManager.getSavedThemeMode(requireContext())
        when (currentMode) {
            ThemeManager.THEME_LIGHT -> binding.rbThemeLight.isChecked = true
            ThemeManager.THEME_DARK -> binding.rbThemeDark.isChecked = true
            else -> binding.rbThemeSystem.isChecked = true
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val newMode = when (checkedId) {
                R.id.rb_theme_light -> ThemeManager.THEME_LIGHT
                R.id.rb_theme_dark -> ThemeManager.THEME_DARK
                else -> ThemeManager.THEME_SYSTEM
            }
            if (newMode != currentMode) {
                ThemeManager.setThemeMode(requireContext(), newMode)
                dismiss()
            }
        }
    }

    private fun setupAboutButton() {
        binding.cardAboutApp.setOnClickListener {
            AboutBottomSheetDialogFragment.newInstance()
                .show(parentFragmentManager, AboutBottomSheetDialogFragment.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingsBottomSheetDialogFragment"

        fun newInstance(): SettingsBottomSheetDialogFragment = SettingsBottomSheetDialogFragment()
    }
}
