package com.easycode.ide.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.easycode.ide.R
import com.easycode.ide.databinding.BottomSheetSettingsBinding
import com.easycode.ide.util.ThemeManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeOptions()
    }

    private fun setupThemeOptions() {
        when (ThemeManager.getSavedTheme(requireContext())) {
            ThemeManager.THEME_LIGHT -> binding.rbThemeLight.isChecked = true
            ThemeManager.THEME_SYSTEM -> binding.rbThemeSystem.isChecked = true
            else -> binding.rbThemeDark.isChecked = true
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.rb_theme_light -> ThemeManager.THEME_LIGHT
                R.id.rb_theme_system -> ThemeManager.THEME_SYSTEM
                else -> ThemeManager.THEME_DARK
            }
            ThemeManager.setTheme(requireContext(), themeMode)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "SettingsBottomSheet"
        fun newInstance() = SettingsBottomSheet()
    }
}
