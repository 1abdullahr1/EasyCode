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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Button
import android.text.InputType
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.easycode.ide.R
import com.easycode.ide.databinding.FragmentEditorBinding
import com.easycode.ide.ui.EasyCodeViewModel
import com.easycode.ide.util.EditorPreferences
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AlertDialog

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

    fun showFindReplaceDialog() {
        val context = requireContext()
        val findInput = EditText(context).apply {
            hint = "Find in ${editorType.lowercase()}"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
        }
        val replaceInput = EditText(context).apply {
            hint = "Replace with"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
        }
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(context).apply {
                text = "Find next"
                setOnClickListener { findNext(findInput.text.toString()) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(Button(context).apply {
                text = "Replace"
                setOnClickListener { replaceNext(findInput.text.toString(), replaceInput.text.toString()) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(Button(context).apply {
                text = "Replace all"
                setOnClickListener { replaceAll(findInput.text.toString(), replaceInput.text.toString()) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = (20 * resources.displayMetrics.density).toInt()
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(findInput)
            addView(replaceInput)
            addView(actions)
        }
        AlertDialog.Builder(context)
            .setTitle("Find and replace")
            .setView(content)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun findNext(query: String) {
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Enter text to find", Toast.LENGTH_SHORT).show()
            return
        }
        val text = binding.etCode.text?.toString().orEmpty()
        val from = binding.etCode.selectionEnd.coerceAtLeast(0).coerceAtMost(text.length)
        val found = text.indexOf(query, from).takeIf { it >= 0 } ?: text.indexOf(query)
        if (found < 0) {
            Toast.makeText(requireContext(), "No match found", Toast.LENGTH_SHORT).show()
            return
        }
        binding.etCode.requestFocus()
        binding.etCode.setSelection(found, found + query.length)
        binding.editorScroll.smoothScrollTo(0, binding.etCode.top + (binding.etCode.layout?.getLineTop(binding.etCode.layout?.getLineForOffset(found) ?: 0) ?: 0))
    }

    private fun replaceNext(query: String, replacement: String) {
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Enter text to find", Toast.LENGTH_SHORT).show()
            return
        }
        val text = binding.etCode.text?.toString().orEmpty()
        val start = binding.etCode.selectionStart
        val end = binding.etCode.selectionEnd
        val selectedMatch = start >= 0 && end - start == query.length && text.regionMatches(start, query, 0, query.length)
        val from = if (selectedMatch) start else binding.etCode.selectionEnd.coerceAtLeast(0).coerceAtMost(text.length)
        val match = if (selectedMatch) start else text.indexOf(query, from).takeIf { it >= 0 } ?: text.indexOf(query)
        if (match < 0) {
            Toast.makeText(requireContext(), "No match found", Toast.LENGTH_SHORT).show()
            return
        }
        undoRedoManager.recordSnapshot(text)
        val updated = text.replaceRange(match, match + query.length, replacement)
        undoRedoManager.recordSnapshot(updated)
        applyEditorAction(updated)
        val next = (match + replacement.length).coerceAtMost(updated.length)
        binding.etCode.setSelection(next)
    }

    private fun replaceAll(query: String, replacement: String) {
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Enter text to find", Toast.LENGTH_SHORT).show()
            return
        }
        val text = binding.etCode.text?.toString().orEmpty()
        val updated = text.replace(query, replacement)
        if (updated == text) {
            Toast.makeText(requireContext(), "No matches found", Toast.LENGTH_SHORT).show()
            return
        }
        undoRedoManager.recordSnapshot(text)
        undoRedoManager.recordSnapshot(updated)
        applyEditorAction(updated)
        Toast.makeText(requireContext(), "Text replaced", Toast.LENGTH_SHORT).show()
    }

    fun showGoToLineDialog() {
        val text = binding.etCode.text?.toString().orEmpty()
        val lineCount = text.count { it == '\n' } + 1
        val input = EditText(requireContext()).apply {
            hint = "Line number (1–$lineCount)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Go to line")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Go") { _, _ ->
                val requested = input.text.toString().toIntOrNull()
                if (requested == null || requested !in 1..lineCount) {
                    Toast.makeText(requireContext(), "Choose a line from 1 to $lineCount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val offset = text.lineSequence().take(requested - 1).sumOf { it.length + 1 }.coerceAtMost(text.length)
                binding.etCode.requestFocus()
                binding.etCode.setSelection(offset)
                val layout = binding.etCode.layout
                if (layout != null) {
                    val line = layout.getLineForOffset(offset)
                    binding.editorScroll.smoothScrollTo(0, binding.etCode.top + layout.getLineTop(line))
                }
            }
            .show()
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
