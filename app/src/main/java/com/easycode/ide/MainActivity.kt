package com.easycode.ide

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
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
import com.easycode.ide.ui.dialogs.ResourcesBottomSheet
import com.easycode.ide.ui.dialogs.SettingsBottomSheet
import com.easycode.ide.ui.editor.EditorFragment
import com.easycode.ide.ui.result.ResultFragment
import com.easycode.ide.util.FileUtils
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EasyCodeViewModel by viewModels()

    private lateinit var pagerAdapter: MainPagerAdapter
    private lateinit var savedFiddleAdapter: SavedFiddleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            val btn = Button(this, null, 0, com.google.android.material.R.style.Widget_Material3_Button_TonalButton).apply {
                text = sym
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
                    val insertion = if (sym == "Tab") "  " else sym
                    insertIntoActiveEditor(insertion)
                }
            }
            binding.keyboardAccessoryContainer.addView(btn)
        }
    }

    private fun insertIntoActiveEditor(text: String) {
        val currentPosition = binding.viewPager.currentItem
        if (currentPosition in 0..2) {
            val fragment = pagerAdapter.getFragmentAt(currentPosition)
            if (fragment is EditorFragment) {
                fragment.insertTextAtCursor(text)
            }
        }
    }

    private fun setupDrawer() {
        savedFiddleAdapter = SavedFiddleAdapter(
            onFiddleClick = { fiddle ->
                viewModel.loadFiddle(fiddle)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                Toast.makeText(this, "Loaded ${fiddle.title}", Toast.LENGTH_SHORT).show()
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
            viewModel.newFiddle()
            binding.viewPager.currentItem = MainPagerAdapter.TAB_HTML
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "New fiddle started", Toast.LENGTH_SHORT).show()
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
                viewModel.saveCurrentFiddle {
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
            R.id.action_export_html -> {
                exportProjectHtml()
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
}
