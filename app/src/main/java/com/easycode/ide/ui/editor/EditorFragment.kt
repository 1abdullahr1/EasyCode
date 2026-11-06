package com.easycode.ide.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
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
import com.easycode.ide.util.EditorPreferences
import com.google.android.material.snackbar.Snackbar

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EasyCodeViewModel by activityViewModels()
    private val undoRedoManager get() = viewModel.editorHistory.getOrPut(editorType) { UndoRedoManager() }
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> applyPreferences() }
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
        EditorPreferences.store(requireContext()).registerOnSharedPreferenceChangeListener(preferenceListener)
        applyPreferences()
    }

    private fun setupEditor() {
        binding.etCode.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateLineNumbers() }
        binding.etCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromViewModel || s == null) return

                val text = s.toString()
                undoRedoManager.recordSnapshot(text)
                binding.etCode.post { updateLineNumbers() }

                // Sync code with ViewModel
                syncCode(text)

                // Debounce syntax highlighting
                highlightRunnable?.let { highlightHandler.removeCallbacks(it) }
                highlightRunnable = Runnable {
                    val currentEditable = binding.etCode.text ?: return@Runnable
                    when (editorType) {
                        TYPE_HTML -> SyntaxHighlighter.highlightHtml(currentEditable, resources)
                        TYPE_CSS -> SyntaxHighlighter.highlightCss(currentEditable, resources)
                        TYPE_JS -> SyntaxHighlighter.highlightJs(currentEditable, resources)
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
                applyEditorAction(previous)
            }
        }

        binding.btnRedo.setOnClickListener {
            val next = undoRedoManager.redo()
            if (next != null) {
                applyEditorAction(next)
            }
        }

        binding.btnFormat.setOnClickListener {
            formatCode()
        }

        val language = if (editorType == TYPE_JS) "JavaScript" else editorType
        binding.btnClearTab.contentDescription = "Clear $language"
        binding.btnClearTab.setOnClickListener {
            val original = viewModel.code(editorType)
            if (original.isEmpty()) return@setOnClickListener
            val revision = viewModel.projectRevision.value
            undoRedoManager.recordSnapshot(original)
            undoRedoManager.recordSnapshot("")
            applyEditorAction("")
            val undo = Snackbar.make(binding.root, "$language cleared", Snackbar.LENGTH_LONG)
                .setBackgroundTint(requireContext().getColor(R.color.surfaceVariant))
                .setTextColor(requireContext().getColor(R.color.onSurface))
                .setActionTextColor(requireContext().getColor(R.color.primary))
            activity?.findViewById<View>(R.id.keyboard_accessory_scroll)?.let { undo.setAnchorView(it) }
            undo.setAction("Undo") {
                if (revision == viewModel.projectRevision.value && viewModel.code(editorType).isEmpty()) {
                    undoRedoManager.undo("")?.let { viewModel.replaceCode(editorType, it) }
                }
            }.show()
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
            }
            undoRedoManager.recordSnapshot(text.orEmpty())
        }
    }

    private fun applyTextSilently(text: String) {
        isUpdatingFromViewModel = true
        binding.etCode.setText(text)
        binding.etCode.setSelection(binding.etCode.length())
        binding.etCode.post { updateLineNumbers() }
        isUpdatingFromViewModel = false

        // Highlight immediately
        val editable = binding.etCode.text
        if (editable != null) {
            when (editorType) {
                TYPE_HTML -> SyntaxHighlighter.highlightHtml(editable, resources)
                TYPE_CSS -> SyntaxHighlighter.highlightCss(editable, resources)
                TYPE_JS -> SyntaxHighlighter.highlightJs(editable, resources)
            }
        }
    }

    private fun syncCode(text: String) {
        when (editorType) {
            TYPE_HTML -> viewModel.updateHtml(text)
            TYPE_CSS -> viewModel.updateCss(text)
            TYPE_JS -> viewModel.updateJs(text)
        }
    }

    private fun applyEditorAction(text: String) {
        applyTextSilently(text)
        syncCode(text)
    }

    private fun applyPreferences() {
        val editor = _binding ?: return
        val prefs = EditorPreferences.store(requireContext())
        val font = prefs.getInt(EditorPreferences.FONT, EditorPreferences.DEFAULT_FONT).toFloat()
        editor.etCode.textSize = font
        editor.tvLineNumbers.textSize = font
        editor.etCode.setHorizontallyScrolling(!prefs.getBoolean(EditorPreferences.WRAP, true))
        editor.tvLineNumbers.visibility = if (prefs.getBoolean(EditorPreferences.NUMBERS, true)) View.VISIBLE else View.GONE
        editor.etCode.post { updateLineNumbers() }
    }

    private fun updateLineNumbers() {
        val editor = _binding ?: return
        if (editor.tvLineNumbers.visibility != View.VISIBLE) return
        val layout = editor.etCode.layout ?: return
        val text = editor.etCode.text
        var number = 0
        val numbers = buildString {
            for (line in 0 until layout.lineCount) {
                if (line > 0) append('\n')
                val start = layout.getLineStart(line)
                if (start == 0 || text.getOrNull(start - 1) == '\n') append(++number)
            }
        }
        if (editor.tvLineNumbers.text.toString() != numbers) editor.tvLineNumbers.text = numbers
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
        undoRedoManager.recordSnapshot(formatted)
        applyEditorAction(formatted)
        Toast.makeText(requireContext(), getString(R.string.action_format), Toast.LENGTH_SHORT).show()
    }

    private fun simpleIndentFormat(input: String): String {
        val indent = " ".repeat(EditorPreferences.indent(requireContext()))
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

            result.append(indent.repeat(indentLevel)).append(line).append("\n")

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
            val insertionStart = if (end >= 0) minOf(start, end) else start
            text.replace(insertionStart, maxOf(start, end), insertion)
            binding.etCode.setSelection(insertionStart + insertion.length)
        } else {
            text.append(insertion)
        }
    }

    override fun onDestroyView() {
        EditorPreferences.store(requireContext()).unregisterOnSharedPreferenceChangeListener(preferenceListener)
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
