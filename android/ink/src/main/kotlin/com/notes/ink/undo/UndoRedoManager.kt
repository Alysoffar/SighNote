package com.notes.ink.undo

import com.notes.model.Stroke

sealed interface InkOperation {
    data class AddStroke(val stroke: Stroke) : InkOperation
    data class EraseStroke(val stroke: Stroke) : InkOperation
    data class ClearPage(val previousStrokes: List<Stroke>) : InkOperation
}

class UndoRedoManager(
    private val maxStackSize: Int = 500
) {
    private val undoStack = ArrayDeque<InkOperation>()
    private val redoStack = ArrayDeque<InkOperation>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val undoCount: Int
        get() = undoStack.size

    val redoCount: Int
        get() = redoStack.size

    fun recordOperation(op: InkOperation) {
        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(op)
        redoStack.clear()
    }

    /**
     * Pops an operation from the undo stack and moves it to redo.
     * Returns the operation that needs to be undone, or null if stack is empty.
     */
    fun popUndo(): InkOperation? {
        if (undoStack.isEmpty()) return null
        val op = undoStack.removeLast()
        redoStack.addLast(op)
        return op
    }

    /**
     * Pops an operation from the redo stack and moves it to undo.
     * Returns the operation that needs to be reapplied, or null if stack is empty.
     */
    fun popRedo(): InkOperation? {
        if (redoStack.isEmpty()) return null
        val op = redoStack.removeLast()
        undoStack.addLast(op)
        return op
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
