package com.notes.ink.undo

import com.notes.model.BoundingBox
import com.notes.model.Stroke
import com.notes.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UndoRedoManagerTest {

    private fun createDummyStroke(id: String = UUID.randomUUID().toString()): Stroke {
        return Stroke(
            id = id,
            brush_id = "test_pen",
            points = listOf(
                StrokePoint(10f, 20f, 1000L, 0.5f, 0f),
                StrokePoint(30f, 40f, 1016L, 0.7f, 0f)
            ),
            bounds = BoundingBox(10f, 20f, 30f, 40f)
        )
    }

    @Test
    fun testAddStrokeUndoRedo() {
        val manager = UndoRedoManager()
        val stroke = createDummyStroke()

        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)

        manager.recordOperation(InkOperation.AddStroke(stroke))
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)

        val undone = manager.popUndo()
        assertTrue(undone is InkOperation.AddStroke)
        assertEquals(stroke.id, (undone as InkOperation.AddStroke).stroke.id)
        assertFalse(manager.canUndo)
        assertTrue(manager.canRedo)

        val redone = manager.popRedo()
        assertTrue(redone is InkOperation.AddStroke)
        assertEquals(stroke.id, (redone as InkOperation.AddStroke).stroke.id)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
    }

    @Test
    fun testNewActionClearsRedoStack() {
        val manager = UndoRedoManager()
        val s1 = createDummyStroke("s1")
        val s2 = createDummyStroke("s2")
        val s3 = createDummyStroke("s3")

        manager.recordOperation(InkOperation.AddStroke(s1))
        manager.recordOperation(InkOperation.AddStroke(s2))
        manager.popUndo() // Undoes s2, redo stack has s2
        assertTrue(manager.canRedo)

        manager.recordOperation(InkOperation.AddStroke(s3))
        assertFalse("New operation must clear redo stack", manager.canRedo)
        assertEquals(2, manager.undoCount) // s1 and s3
    }

    @Test
    fun testMaxStackSizeIsBounded() {
        val manager = UndoRedoManager(maxStackSize = 50)
        for (i in 0 until 100) {
            manager.recordOperation(InkOperation.AddStroke(createDummyStroke("s_$i")))
        }
        assertEquals(50, manager.undoCount)
    }

    @Test
    fun testClearPageOperation() {
        val manager = UndoRedoManager()
        val strokes = listOf(createDummyStroke("1"), createDummyStroke("2"))

        manager.recordOperation(InkOperation.ClearPage(strokes))
        assertTrue(manager.canUndo)

        val popped = manager.popUndo()
        assertTrue(popped is InkOperation.ClearPage)
        assertEquals(2, (popped as InkOperation.ClearPage).previousStrokes.size)
    }
}
