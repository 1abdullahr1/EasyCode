package com.easycode.ide.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.easycode.ide.R
import com.easycode.ide.databinding.FragmentEditorBinding
import com.easycode.ide.ui.EasyCodeViewModel

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EasyCodeViewModel by activityViewModels()
    private val undoRedoManager = UndoRedoManager()
    private val highlightHandler = Handler(Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null

    private var editorType: String = TYPE_HTML
    private var isUpdatingFromViewModel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorType = arguments?.getString(ARG_EDITOR_TYPE) ?: TYPE_HTML
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEditor()
        setupActions()
        observeViewModel()
    }

    private fun setupEditor() {
        binding.etCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromViewModel || s == null) return

                val text = s.toString()
                undoRedoManager.recordSnapshot(text)
                updateLineNumbers(text)

                // Sync code with ViewModel
                when (editorType) {
                    TYPE_HTML -> viewModel.updateHtml(text)
                    TYPE_CSS -> viewModel.updateCss(text)
                    TYPE_JS -> viewModel.updateJs(text)
                }

                // Debounce syntax highlighting
                highlightRunnable?.let { highlightHandler.removeCallbacks(it) }
                highlightRunnable = Runnable {
                    val currentEditable = binding.etCode.text ?: return@Runnable
                    when (editorType) {
                        TYPE_HTML -> SyntaxHighlighter.highlightHtml(currentEditable)
                        TYPE_CSS -> SyntaxHighlighter.highlightCss(currentEditable)
                        TYPE_JS -> SyntaxHighlighter.highlightJs(currentEditable)
                    }
                }
                highlightHandler.postDelayed(highlightRunnable!!, 250)
            }
        })
    }

    private fun setupActions() {
        binding.btnUndo.setOnClickListener {
            val currentText = binding.etCode.text?.toString().orEmpty()
            val previous = undoRedoManager.undo(currentText)
            if (previous != null) {
                applyTextSilently(previous)
            }
        }

        binding.btnRedo.setOnClickListener {
            val next = undoRedoManager.redo()
            if (next != null) {
                applyTextSilently(next)
            }
        }

        binding.btnFormat.setOnClickListener {
            formatCode()
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.etCode.text?.toString().orEmpty()
            if (text.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("EasyCode Snippet", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        val liveData = when (editorType) {
            TYPE_HTML -> viewModel.htmlCode
            TYPE_CSS -> viewModel.cssCode
            else -> viewModel.jsCode
        }

        liveData.observe(viewLifecycleOwner) { text ->
            if (binding.etCode.text?.toString() != text) {
                applyTextSilently(text.orEmpty())
                undoRedoManager.recordSnapshot(text.orEmpty())
            }
        }
    }

    private fun applyTextSilently(text: String) {
        isUpdatingFromViewModel = true
        binding.etCode.setText(text)
        binding.etCode.setSelection(binding.etCode.length())
        updateLineNumbers(text)
        isUpdatingFromViewModel = false

        // Highlight immediately
        val editable = binding.etCode.text
        if (editable != null) {
            when (editorType) {
                TYPE_HTML -> SyntaxHighlighter.highlightHtml(editable)
                TYPE_CSS -> SyntaxHighlighter.highlightCss(editable)
                TYPE_JS -> SyntaxHighlighter.highlightJs(editable)
            }
        }
    }

    private fun updateLineNumbers(text: String) {
        val lineCount = text.count { it == '\n' } + 1
        val sb = StringBuilder()
        for (i in 1..lineCount) {
            sb.append(i).append("\n")
        }
        binding.tvLineNumbers.text = sb.toString().trimEnd()
    }

    private fun formatCode() {
        val text = binding.etCode.text?.toString().orEmpty()
        if (text.isBlank()) return

        val formatted = when (editorType) {
            TYPE_HTML -> simpleIndentFormat(text)
            TYPE_CSS -> simpleIndentFormat(text)
            TYPE_JS -> simpleIndentFormat(text)
            else -> text
        }
        applyTextSilently(formatted)
        Toast.makeText(requireContext(), getString(R.string.action_format), Toast.LENGTH_SHORT).show()
    }

    private fun simpleIndentFormat(input: String): String {
        val lines = input.lines()
        val result = StringBuilder()
        var indentLevel = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                result.append("\n")
                continue
            }

            // Decrease indent if line closes a block
            if (line.startsWith("}") || line.startsWith("</") || line.startsWith("]")) {
                indentLevel = maxOf(0, indentLevel - 1)
            }

            result.append("  ".repeat(indentLevel)).append(line).append("\n")

            // Increase indent if line opens a block
            if (line.endsWith("{") || (line.startsWith("<") && !line.startsWith("</") && !line.endsWith("/>") && !line.contains("</")) || line.endsWith("[")) {
                indentLevel++
            }
        }

        return result.toString().trimEnd()
    }

    fun insertTextAtCursor(insertion: String) {
        val start = binding.etCode.selectionStart
        val end = binding.etCode.selectionEnd
        val text = binding.etCode.text ?: return

        if (start >= 0) {
            text.replace(minOf(start, end), maxOf(start, end), insertion)
            binding.etCode.setSelection(start + insertion.length)
        } else {
            text.append(insertion)
        }
    }

    override fun onDestroyView() {
        highlightRunnable?.let { highlightHandler.removeCallbacks(it) }
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_EDITOR_TYPE = "arg_editor_type"
        const val TYPE_HTML = "HTML"
        const val TYPE_CSS = "CSS"
        const val TYPE_JS = "JS"

        fun newInstance(type: String): EditorFragment {
            return EditorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EDITOR_TYPE, type)
                }
            }
        }
    }
}
