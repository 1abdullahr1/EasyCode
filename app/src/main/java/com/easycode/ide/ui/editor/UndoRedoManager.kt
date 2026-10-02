package com.easycode.ide.ui.editor

import java.util.Stack

class UndoRedoManager(private val maxHistory: Int = 50) {

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var isPerformingUndoOrRedo = false

    fun recordSnapshot(text: String) {
        if (isPerformingUndoOrRedo) return

        if (undoStack.isNotEmpty() && undoStack.peek() == text) {
            return
        }

        undoStack.push(text)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.size > 1

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(currentText: String): String? {
        if (!canUndo()) return null

        isPerformingUndoOrRedo = true
        val current = undoStack.pop()
        redoStack.push(current)
        val previous = undoStack.peek()
        isPerformingUndoOrRedo = false
        return previous
    }

    fun redo(): String? {
        if (!canRedo()) return null

        isPerformingUndoOrRedo = true
        val next = redoStack.pop()
        undoStack.push(next)
        isPerformingUndoOrRedo = false
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        isPerformingUndoOrRedo = false
    }
}
