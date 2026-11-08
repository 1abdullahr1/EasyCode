package com.easycode.ide

import android.os.Bundle
import android.text.InputFilter
import androidx.core.widget.doAfterTextChanged
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.easycode.ide.data.model.FiddleEntity
import com.easycode.ide.databinding.ActivityMainBinding
import com.easycode.ide.engine.CodeCompiler
import com.easycode.ide.ui.EasyCodeViewModel
import com.easycode.ide.ui.LayoutMode
import com.easycode.ide.ui.MainPagerAdapter
import com.easycode.ide.ui.adapters.SavedFiddleAdapter
import com.easycode.ide.ui.dialogs.BoilerplatesBottomSheet
import com.easycode.ide.ui.dialogs.AboutBottomSheet
import com.easycode.ide.ui.dialogs.ResourcesBottomSheet
import com.easycode.ide.ui.dialogs.SettingsBottomSheet
import com.easycode.ide.ui.editor.EditorFragment
import com.easycode.ide.ui.result.ResultFragment
import com.easycode.ide.util.FileUtils
import com.easycode.ide.util.EditorPreferences
import com.easycode.ide.util.WorkspaceFiles
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EasyCodeViewModel by viewModels()

    private lateinit var pagerAdapter: MainPagerAdapter
    private lateinit var savedFiddleAdapter: SavedFiddleAdapter
    private lateinit var workspace: WorkspaceFiles

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            binding.drawerLayout.children.forEach { child ->
                child.updateLayoutParams<DrawerLayout.LayoutParams> {
                    setMargins(safe.left, safe.top, safe.right, safe.bottom)
                }
            }
            WindowInsetsCompat.CONSUMED
        }
        workspace = WorkspaceFiles(this, viewModel, ::activeEditorType, ::selectEditor, ::exportProjectHtml)

        setupToolbar()
        setupViewPagerAndTabs()
        setupBottomNavigation()
        setupKeyboardAccessoryBar()
        setupDrawer()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            handleMenuItemClick(item)
        }

        binding.tvProjectTitle.setOnClickListener {
            showRenameDialog()
        }
    }

    private fun setupViewPagerAndTabs() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 3
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                MainPagerAdapter.TAB_HTML -> getString(R.string.tab_html)
                MainPagerAdapter.TAB_CSS -> getString(R.string.tab_css)
                MainPagerAdapter.TAB_JS -> getString(R.string.tab_js)
                MainPagerAdapter.TAB_RESULT -> getString(R.string.tab_result)
                else -> ""
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val menuId = when (position) {
                    MainPagerAdapter.TAB_HTML -> R.id.nav_html
                    MainPagerAdapter.TAB_CSS -> R.id.nav_css
                    MainPagerAdapter.TAB_JS -> R.id.nav_js
                    MainPagerAdapter.TAB_RESULT -> R.id.nav_result
                    else -> R.id.nav_html
                }
                if (binding.bottomNavigation.selectedItemId != menuId) {
                    binding.bottomNavigation.selectedItemId = menuId
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val page = when (item.itemId) {
                R.id.nav_html -> MainPagerAdapter.TAB_HTML
                R.id.nav_css -> MainPagerAdapter.TAB_CSS
                R.id.nav_js -> MainPagerAdapter.TAB_JS
                R.id.nav_result -> MainPagerAdapter.TAB_RESULT
                else -> MainPagerAdapter.TAB_HTML
            }
            if (binding.viewPager.currentItem != page) {
                binding.viewPager.currentItem = page
            }
            true
        }
    }

    private fun setupKeyboardAccessoryBar() {
        val symbols = listOf(
            "Tab", "<", ">", "/", "=", "\"", "'", "{", "}", "(", ")",
            "[", "]", ";", ":", "!", "&", "|", "$", "#", ".", "+", "-", "*", "?"
        )

        binding.keyboardAccessoryContainer.removeAllViews()

        for (sym in symbols) {
            val btn = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
                text = sym
                setTextColor(getColor(R.color.editor_text))
                minWidth = 0
                minimumWidth = 0
                setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
                textSize = 13f
                isAllCaps = false
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(38)
                ).apply {
                    setMargins(dpToPx(3), dpToPx(4), dpToPx(3), dpToPx(4))
                }
                layoutParams = lp

                setOnClickListener {
                    val insertion = if (sym == "Tab") " ".repeat(EditorPreferences.indent(this@MainActivity)) else sym
                    insertIntoActiveEditor(insertion)
                }
            }
            binding.keyboardAccessoryContainer.addView(btn)
        }
    }

    private fun insertIntoActiveEditor(text: String) {
        val currentPosition = binding.viewPager.currentItem
        if (viewModel.layoutMode.value == LayoutMode.SPLIT) {
            (supportFragmentManager.findFragmentById(R.id.split_editor_container) as? EditorFragment)?.insertTextAtCursor(text)
            return
        }
        if (currentPosition in 0..2) {
            val fragment = supportFragmentManager.findFragmentByTag("f${pagerAdapter.getItemId(currentPosition)}")
                ?: pagerAdapter.getFragmentAt(currentPosition)
            if (fragment is EditorFragment) {
                fragment.insertTextAtCursor(text)
            }
        }
    }

    private fun setupDrawer() {
        savedFiddleAdapter = SavedFiddleAdapter(
            onFiddleClick = { fiddle ->
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                if (viewModel.currentFiddleId.value != fiddle.id) {
                    confirmProjectChange {
                        viewModel.loadFiddle(fiddle)
                        Toast.makeText(this, "Loaded ${fiddle.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDeleteClick = { fiddle ->
                confirmDeleteFiddle(fiddle)
            }
        )

        binding.rvSavedFiddles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = savedFiddleAdapter
            setHasFixedSize(true)
        }

        binding.btnDrawerNewFiddle.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            showNewFiddleDialog()
        }

        binding.btnDrawerBoilerplates.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            BoilerplatesBottomSheet.newInstance().show(supportFragmentManager, BoilerplatesBottomSheet.TAG)
        }

        binding.btnDrawerResources.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            ResourcesBottomSheet.newInstance().show(supportFragmentManager, ResourcesBottomSheet.TAG)
        }

        binding.btnDrawerSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            SettingsBottomSheet.newInstance().show(supportFragmentManager, SettingsBottomSheet.TAG)
        }

        binding.btnDrawerAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            AboutBottomSheet.newInstance().show(supportFragmentManager, AboutBottomSheet.TAG)
        }
    }

    private fun observeViewModel() {
        viewModel.title.observe(this) { titleText ->
            binding.tvProjectTitle.text = titleText.ifBlank { getString(R.string.fiddle_title_hint) }
        }

        viewModel.savedFiddles.observe(this) { fiddles ->
            savedFiddleAdapter.submitList(fiddles)
        }

        viewModel.layoutMode.observe(this) { mode ->
            updateLayoutMode(mode)
        }

        viewModel.isReady.observe(this) { ready ->
            binding.workspaceLoadingOverlay.visibility = if (ready) View.GONE else View.VISIBLE
        }

        viewModel.draftStatus.observe(this) { status ->
            binding.tvDraftStatus.text = status
            binding.tvDraftStatus.visibility = if (status.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun updateLayoutMode(mode: LayoutMode) {
        if (mode == LayoutMode.SPLIT) {
            binding.viewPager.visibility = View.GONE
            binding.tabLayout.visibility = View.GONE
            binding.splitContainer.visibility = View.VISIBLE

            // Attach editor to top and result to bottom
            val currentTab = binding.viewPager.currentItem
            val editorType = when (currentTab) {
                MainPagerAdapter.TAB_CSS -> EditorFragment.TYPE_CSS
                MainPagerAdapter.TAB_JS -> EditorFragment.TYPE_JS
                else -> EditorFragment.TYPE_HTML
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.split_editor_container, EditorFragment.newInstance(editorType))
                .replace(R.id.split_result_container, ResultFragment())
                .commit()
        } else {
            binding.splitContainer.visibility = View.GONE
            binding.tabLayout.visibility = View.VISIBLE
            binding.viewPager.visibility = View.VISIBLE
        }
    }

    private fun handleMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_run -> {
                viewModel.triggerRun()
                if (viewModel.layoutMode.value != LayoutMode.SPLIT) {
                    binding.viewPager.currentItem = MainPagerAdapter.TAB_RESULT
                }
                true
            }
            R.id.action_save -> {
                viewModel.saveCurrentFiddle(::showSaveError) {
                    Toast.makeText(this, getString(R.string.saved_success), Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_layout_toggle -> {
                viewModel.toggleLayoutMode()
                true
            }
            R.id.action_templates -> {
                BoilerplatesBottomSheet.newInstance().show(supportFragmentManager, BoilerplatesBottomSheet.TAG)
                true
            }
            R.id.action_resources -> {
                ResourcesBottomSheet.newInstance().show(supportFragmentManager, ResourcesBottomSheet.TAG)
                true
            }
            R.id.action_find_replace -> {
                activeEditorFragment()?.showFindReplaceDialog()
                true
            }
            R.id.action_go_to_line -> {
                activeEditorFragment()?.showGoToLineDialog()
                true
            }
            R.id.action_export_html -> {
                exportProjectHtml()
                true
            }
            R.id.action_files -> {
                workspace.showMenu()
                true
            }
            R.id.action_clear -> {
                confirmClearCode()
                true
            }
            R.id.action_settings -> {
                SettingsBottomSheet.newInstance().show(supportFragmentManager, SettingsBottomSheet.TAG)
                true
            }
            else -> false
        }
    }

    fun pickWorkspaceFolder() = workspace.pickFolder()

    fun disconnectWorkspaceFolder() = workspace.disconnectFolder()

    private fun activeEditorType(): String? = when (binding.viewPager.currentItem) {
        MainPagerAdapter.TAB_HTML -> EditorFragment.TYPE_HTML
        MainPagerAdapter.TAB_CSS -> EditorFragment.TYPE_CSS
        MainPagerAdapter.TAB_JS -> EditorFragment.TYPE_JS
        else -> if (viewModel.layoutMode.value == LayoutMode.SPLIT) EditorFragment.TYPE_HTML else null
    }

    private fun activeEditorFragment(): EditorFragment? {
        if (viewModel.layoutMode.value == LayoutMode.SPLIT) {
            return supportFragmentManager.findFragmentById(R.id.split_editor_container) as? EditorFragment
        }
        val position = binding.viewPager.currentItem
        if (position !in 0..2) return null
        val fragment = supportFragmentManager.findFragmentByTag("f${pagerAdapter.getItemId(position)}")
            ?: pagerAdapter.getFragmentAt(position)
        return fragment as? EditorFragment
    }

    private fun selectEditor(type: String) {
        binding.viewPager.setCurrentItem(when (type) {
            EditorFragment.TYPE_CSS -> MainPagerAdapter.TAB_CSS
            EditorFragment.TYPE_JS -> MainPagerAdapter.TAB_JS
            else -> MainPagerAdapter.TAB_HTML
        }, false)
        if (viewModel.layoutMode.value == LayoutMode.SPLIT) updateLayoutMode(LayoutMode.SPLIT)
    }

    private fun showSaveError() {
        if (!isDestroyed) Toast.makeText(this, "Could not save. Your editor contents are still available.", Toast.LENGTH_LONG).show()
    }

    fun confirmProjectChange(change: () -> Unit) {
        if (viewModel.isSaving) {
            Toast.makeText(this, "Wait for the current save to finish", Toast.LENGTH_SHORT).show()
            return
        }
        if (!viewModel.hasUnsavedChanges()) {
            change()
            return
        }
        AlertDialog.Builder(this).setTitle("Save your current fiddle?")
            .setMessage("Save your edits before switching, or discard them.")
            .setPositiveButton("Save and continue") { _, _ ->
                viewModel.saveCurrentFiddle(::showSaveError) {
                    if (!isDestroyed && !viewModel.hasUnsavedChanges()) change()
                }
            }
            .setNeutralButton("Discard edits") { _, _ -> change() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNewFiddleDialog() {
        val input = EditText(this).apply {
            hint = "Fiddle name"
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(80))
        }
        val dialog = AlertDialog.Builder(this).setTitle("New fiddle")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) confirmProjectChange {
                    viewModel.createNamedFiddle(name, ::showSaveError) { opened ->
                        if (!isDestroyed && opened) selectEditor(EditorFragment.TYPE_HTML)
                        if (!isDestroyed && !opened) {
                            Toast.makeText(this, "Fiddle created. Your current edits are still open.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            val create = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            create.isEnabled = false
            input.requestFocus()
            input.doAfterTextChanged { create.isEnabled = !it.isNullOrBlank() }
        }
        dialog.show()
    }

    private fun exportProjectHtml() {
        val html = viewModel.htmlCode.value.orEmpty()
        val css = viewModel.cssCode.value.orEmpty()
        val js = viewModel.jsCode.value.orEmpty()
        val resources = viewModel.externalResources.value.orEmpty()
        val title = viewModel.title.value.orEmpty().ifBlank { "easycode_project" }

        val compiled = CodeCompiler.compile(html, css, js, resources)
        FileUtils.exportAndShareHtml(this, title, compiled)
    }

    private fun showRenameDialog() {
        val currentTitle = viewModel.title.value.orEmpty()
        val input = EditText(this).apply {
            setText(currentTitle)
            setSelection(length())
            hint = getString(R.string.fiddle_title_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.fiddle_title_hint))
            .setView(input)
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                val newTitle = input.text.toString().trim()
                viewModel.updateTitle(newTitle.ifBlank { "Untitled fiddle" })
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun confirmDeleteFiddle(fiddle: FiddleEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.fiddle_delete_confirm))
            .setMessage(getString(R.string.fiddle_delete_msg))
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                viewModel.deleteFiddle(fiddle.id)
                Toast.makeText(this, "Deleted ${fiddle.title}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun confirmClearCode() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_clear))
            .setMessage("Are you sure you want to clear all code in HTML, CSS, and JavaScript?")
            .setPositiveButton(getString(R.string.action_clear)) { _, _ ->
                viewModel.clearAllCode()
                Toast.makeText(this, "Code cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onStop() {
        viewModel.saveDraftNow()
        super.onStop()
    }
}
