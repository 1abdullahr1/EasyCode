package com.easycode.ide.ui.editor

import org.junit.Assert.*
import org.junit.Test

class UndoRedoManagerTest {
    @Test fun undoRedoAndNewEditKeepCorrectHistory() {
        val history = UndoRedoManager()
        history.recordSnapshot("")
        history.recordSnapshot("a")
        history.recordSnapshot("ab")
        assertEquals("a", history.undo("ab"))
        assertEquals("ab", history.redo())
        assertEquals("a", history.undo("ab"))
        history.recordSnapshot("ac")
        assertNull(history.redo())
        assertEquals("a", history.undo("ac"))
    }

    @Test fun clearPreventsUndoIntoAnotherProject() {
        val history = UndoRedoManager()
        history.recordSnapshot("old project")
        history.clear()
        history.recordSnapshot("new project")
        assertNull(history.undo("new project"))
    }
}
