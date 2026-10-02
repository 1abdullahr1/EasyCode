package com.easycode.ide.ui.result

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.easycode.ide.R
import com.easycode.ide.data.model.ConsoleLevel
import com.easycode.ide.data.model.ConsoleMessage
import com.easycode.ide.data.repository.BoilerplateRepository
import com.easycode.ide.databinding.FragmentResultBinding
import com.easycode.ide.engine.CodeCompiler
import com.easycode.ide.engine.ConsoleBridge
import com.easycode.ide.ui.EasyCodeViewModel
import com.google.android.material.chip.Chip

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EasyCodeViewModel by activityViewModels()
    private val consoleAdapter = ConsoleAdapter()

    private var isConsoleExpanded = false
    private var currentFilter: ConsoleLevel? = null
    private var allMessages = listOf<ConsoleMessage>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
        setupConsoleUi()
        setupBoilerplateChips()
        observeViewModel()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = binding.wvResult.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        binding.wvResult.addJavascriptInterface(
            ConsoleBridge { msg ->
                viewModel.addConsoleMessage(msg)
            },
            "EasyCodeBridge"
        )

        binding.wvResult.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                _binding?.progressLoading?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                _binding?.progressLoading?.visibility = View.GONE
            }
        }

        binding.wvResult.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 100) {
                    _binding?.progressLoading?.visibility = View.GONE
                }
            }
        }
    }

    private fun setupConsoleUi() {
        binding.rvConsoleLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = consoleAdapter
            setHasFixedSize(true)
        }

        binding.consoleHeader.setOnClickListener {
            toggleConsole()
        }

        binding.btnClearConsole.setOnClickListener {
            viewModel.clearConsole()
        }

        binding.filterAll.setOnClickListener { setConsoleFilter(null) }
        binding.filterErrors.setOnClickListener { setConsoleFilter(ConsoleLevel.ERROR) }
        binding.filterWarnings.setOnClickListener { setConsoleFilter(ConsoleLevel.WARN) }
        binding.filterLogs.setOnClickListener { setConsoleFilter(ConsoleLevel.LOG) }
    }

    private fun toggleConsole() {
        isConsoleExpanded = !isConsoleExpanded
        binding.consoleBody.visibility = if (isConsoleExpanded) View.VISIBLE else View.GONE
        binding.ivConsoleChevron.animate().rotation(if (isConsoleExpanded) 180f else 0f).setDuration(200).start()
    }

    private fun setConsoleFilter(level: ConsoleLevel?) {
        currentFilter = level

        binding.filterAll.setBackgroundResource(if (level == null) R.drawable.bg_filter_pill_active else R.drawable.bg_filter_pill)
        binding.filterErrors.setBackgroundResource(if (level == ConsoleLevel.ERROR) R.drawable.bg_filter_pill_active else R.drawable.bg_filter_pill)
        binding.filterWarnings.setBackgroundResource(if (level == ConsoleLevel.WARN) R.drawable.bg_filter_pill_active else R.drawable.bg_filter_pill)
        binding.filterLogs.setBackgroundResource(if (level == ConsoleLevel.LOG) R.drawable.bg_filter_pill_active else R.drawable.bg_filter_pill)

        applyFilteredMessages()
    }

    private fun applyFilteredMessages() {
        val filtered = when (currentFilter) {
            null -> allMessages
            ConsoleLevel.ERROR -> allMessages.filter { it.level == ConsoleLevel.ERROR }
            ConsoleLevel.WARN -> allMessages.filter { it.level == ConsoleLevel.WARN }
            ConsoleLevel.LOG -> allMessages.filter { it.level == ConsoleLevel.LOG || it.level == ConsoleLevel.INFO }
            ConsoleLevel.INFO -> allMessages.filter { it.level == ConsoleLevel.INFO }
        }

        consoleAdapter.submitList(filtered)
        val isEmpty = filtered.isEmpty()
        binding.tvConsoleEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvConsoleLogs.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (filtered.isNotEmpty()) {
            binding.rvConsoleLogs.scrollToPosition(filtered.size - 1)
        }
    }

    private fun setupBoilerplateChips() {
        val boilerplates = BoilerplateRepository.boilerplates
        binding.chipGroupBoilerplates.removeAllViews()

        for (bp in boilerplates) {
            val chip = Chip(requireContext()).apply {
                text = bp.title
                isCheckable = false
                isClickable = true
                setChipBackgroundColorResource(R.color.surfaceVariant)
                setTextColor(resources.getColor(R.color.onSurface, null))
                setOnClickListener {
                    viewModel.loadBoilerplate(bp)
                    runCode()
                }
            }
            binding.chipGroupBoilerplates.addView(chip)
        }
    }

    private fun observeViewModel() {
        viewModel.consoleMessages.observe(viewLifecycleOwner) { messages ->
            allMessages = messages
            applyFilteredMessages()

            val errors = messages.count { it.level == ConsoleLevel.ERROR }
            val warns = messages.count { it.level == ConsoleLevel.WARN }
            val logs = messages.count { it.level == ConsoleLevel.LOG || it.level == ConsoleLevel.INFO }

            binding.tvCountError.text = errors.toString()
            binding.tvCountWarn.text = warns.toString()
            binding.tvCountLog.text = logs.toString()
        }

        viewModel.runEvent.observe(viewLifecycleOwner) {
            runCode()
        }
    }

    fun runCode() {
        val html = viewModel.htmlCode.value.orEmpty()
        val css = viewModel.cssCode.value.orEmpty()
        val js = viewModel.jsCode.value.orEmpty()
        val resources = viewModel.externalResources.value.orEmpty()

        val isProjectEmpty = html.isBlank() && css.isBlank() && js.isBlank()
        if (isProjectEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.wvResult.visibility = View.GONE
            return
        }

        binding.layoutEmptyState.visibility = View.GONE
        binding.wvResult.visibility = View.VISIBLE

        val compiledHtml = CodeCompiler.compile(html, css, js, resources)
        binding.wvResult.loadDataWithBaseURL("https://localhost/", compiledHtml, "text/html", "UTF-8", null)
    }

    override fun onDestroyView() {
        binding.wvResult.stopLoading()
        _binding = null
        super.onDestroyView()
    }
}
