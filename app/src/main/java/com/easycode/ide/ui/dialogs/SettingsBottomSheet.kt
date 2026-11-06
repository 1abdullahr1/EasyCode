package com.easycode.ide.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.easycode.ide.MainActivity
import com.easycode.ide.R
import com.easycode.ide.databinding.BottomSheetSettingsBinding
import com.easycode.ide.util.ThemeManager
import com.easycode.ide.util.EditorPreferences
import com.easycode.ide.util.NativeFiles
import com.easycode.ide.ui.EasyCodeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!
    private val model: EasyCodeViewModel by activityViewModels()

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
        setupEditorOptions()
        setupFolderOptions()
    }

    private fun setupEditorOptions() {
        val prefs = EditorPreferences.store(requireContext())
        val sizes = mapOf(R.id.chip_font_12 to 12, R.id.chip_font_14 to 14, R.id.chip_font_16 to 16, R.id.chip_font_18 to 18)
        val widths = listOf(2, 4, 8)
        binding.indentWidth.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            widths.map { "$it spaces" })
        fun reflectPreferences() {
            val font = prefs.getInt(EditorPreferences.FONT, EditorPreferences.DEFAULT_FONT)
            binding.chipGroupFontSize.check(sizes.entries.firstOrNull { it.value == font }?.key ?: R.id.chip_font_14)
            binding.switchWordWrap.isChecked = prefs.getBoolean(EditorPreferences.WRAP, true)
            binding.switchLineNumbers.isChecked = prefs.getBoolean(EditorPreferences.NUMBERS, true)
            binding.indentWidth.setSelection(widths.indexOf(EditorPreferences.indent(requireContext())).coerceAtLeast(0))
        }
        reflectPreferences()
        binding.chipGroupFontSize.setOnCheckedStateChangeListener { _, checked ->
            sizes[checked.firstOrNull()]?.let { prefs.edit().putInt(EditorPreferences.FONT, it).apply() }
        }
        binding.switchWordWrap.setOnCheckedChangeListener { _, enabled -> prefs.edit().putBoolean(EditorPreferences.WRAP, enabled).apply() }
        binding.switchLineNumbers.setOnCheckedChangeListener { _, enabled -> prefs.edit().putBoolean(EditorPreferences.NUMBERS, enabled).apply() }
        binding.indentWidth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt(EditorPreferences.INDENT, widths[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.resetEditor.setOnClickListener {
            prefs.edit()
                .putInt(EditorPreferences.FONT, EditorPreferences.DEFAULT_FONT)
                .putBoolean(EditorPreferences.WRAP, true)
                .putBoolean(EditorPreferences.NUMBERS, true)
                .putInt(EditorPreferences.INDENT, EditorPreferences.DEFAULT_INDENT)
                .apply()
            reflectPreferences()
        }
    }

    private fun setupFolderOptions() {
        binding.changeFolder.setOnClickListener { (activity as? MainActivity)?.pickWorkspaceFolder() }
        binding.disconnectFolder.setOnClickListener { (activity as? MainActivity)?.disconnectWorkspaceFolder() }
        model.folderUri.observe(viewLifecycleOwner) { uri ->
            binding.changeFolder.text = if (uri == null) "Choose folder" else "Change folder"
            binding.disconnectFolder.visibility = if (uri == null) View.GONE else View.VISIBLE
            binding.selectedFolder.text = if (uri == null) "No folder selected" else "Selected folder"
            if (uri != null) {
                val resolver = requireContext().contentResolver
                viewLifecycleOwner.lifecycleScope.launch {
                    val name = withContext(Dispatchers.IO) {
                        runCatching { NativeFiles.folderName(resolver, uri) }.getOrNull()
                    }
                    if (model.folderUri.value == uri) {
                        _binding?.selectedFolder?.text = name?.let { "Folder: $it" } ?: "Folder access unavailable"
                    }
                }
            }
        }
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
