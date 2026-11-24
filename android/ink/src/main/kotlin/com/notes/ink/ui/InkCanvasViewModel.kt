package com.notes.ink.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.ink.strokes.Stroke as InkStroke
import com.notes.ink.mapping.InkStrokeMapper
import com.notes.ink.perf.StressStrokeGenerator
import com.notes.ink.persistence.InkRepository
import com.notes.ink.transform.ViewportTransform
import com.notes.ink.undo.InkOperation
import com.notes.ink.undo.UndoRedoManager
import com.notes.model.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.hypot

enum class CanvasTool {
    PEN,
    ERASER
}

enum class InkColor(val composeColor: Color, val hex: String) {
    BLACK(Color.Black, "#000000"),
    BLUE(Color(0xFF1E88E5), "#1E88E5"),
    RED(Color(0xFFE53935), "#E53935")
}

enum class InkThickness(val sizePx: Float) {
    THIN(4f),
    MEDIUM(8f),
    THICK(16f)
}

data class InkCanvasUiState(
    val pageId: String = "default-page",
    val pageRevision: Long = 0L,
    val tool: CanvasTool = CanvasTool.PEN,
    val color: InkColor = InkColor.BLACK,
    val thickness: InkThickness = InkThickness.MEDIUM,
    val strokes: List<Stroke> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showDebugOverlay: Boolean = true,
    val strokeCount: Int = 0,
    val frameTimeMs: Float = 0f,
    val transform: ViewportTransform = ViewportTransform()
)

class InkCanvasViewModel(
    private val repository: InkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InkCanvasUiState())
    val uiState: StateFlow<InkCanvasUiState> = _uiState.asStateFlow()

    private val undoRedoManager = UndoRedoManager()

    fun loadPage(pageId: String) {
        _uiState.update { it.copy(pageId = pageId) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensurePageExists(pageId)
            val rev = repository.getPageRevision(pageId)
            val loadedStrokes = repository.getStrokesList(pageId)
            _uiState.update {
                it.copy(
                    pageRevision = rev,
                    strokes = loadedStrokes,
                    strokeCount = loadedStrokes.size,
                    canUndo = undoRedoManager.canUndo,
                    canRedo = undoRedoManager.canRedo
                )
            }
        }
    }

    /**
     * Called when a stylus stroke finishes drawing.
     * Maps to domain Stroke, updates UI state, logs operation for undo, and autosaves.
     */
    fun onStrokeFinished(inkStroke: InkStroke) {
        val currentTransform = _uiState.value.transform
        val currentColor = _uiState.value.color
        val currentThickness = _uiState.value.thickness
        val pageId = _uiState.value.pageId

        val brushId = "color_${currentColor.name.lowercase()}_size_${currentThickness.sizePx.toInt()}"
        val modelStroke = InkStrokeMapper.toModelStroke(
            inkStroke = inkStroke,
            transform = currentTransform,
            brushId = brushId
        )

        // Ignore zero-point strokes (e.g. cancelled taps)
        if (modelStroke.points.isEmpty()) return

        val updatedStrokes = _uiState.value.strokes + modelStroke
        undoRedoManager.recordOperation(InkOperation.AddStroke(modelStroke))

        _uiState.update {
            it.copy(
                strokes = updatedStrokes,
                strokeCount = updatedStrokes.size,
                pageRevision = it.pageRevision + 1L,
                canUndo = undoRedoManager.canUndo,
                canRedo = undoRedoManager.canRedo
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStroke(modelStroke, pageId)
        }
    }

    /**
     * Erases strokes intersected by eraser touch at (screenX, screenY).
     */
    fun onEraseAt(screenX: Float, screenY: Float) {
        val (px, py) = _uiState.value.transform.screenToPage(screenX, screenY)
        val hitRadius = 50f / _uiState.value.transform.scale

        val hitStrokes = _uiState.value.strokes.filter { stroke ->
            // First check bounding box with tolerance
            val b = stroke.bounds
            val inBounds = px >= b.min_x - hitRadius && px <= b.max_x + hitRadius &&
                    py >= b.min_y - hitRadius && py <= b.max_y + hitRadius

            if (!inBounds) return@filter false

            // Then check distance to sampled points
            stroke.points.any { pt ->
                hypot(pt.x - px, pt.y - py) <= hitRadius
            }
        }

        if (hitStrokes.isNotEmpty()) {
            for (stroke in hitStrokes) {
                undoRedoManager.recordOperation(InkOperation.EraseStroke(stroke))
            }
            val updated = _uiState.value.strokes - hitStrokes.toSet()

            _uiState.update {
                it.copy(
                    strokes = updated,
                    strokeCount = updated.size,
                    pageRevision = it.pageRevision + 1L,
                    canUndo = undoRedoManager.canUndo,
                    canRedo = undoRedoManager.canRedo
                )
            }

            viewModelScope.launch(Dispatchers.IO) {
                for (stroke in hitStrokes) {
                    repository.deleteStroke(stroke.id, _uiState.value.pageId)
                }
            }
        }
    }

    fun undo() {
        val op = undoRedoManager.popUndo() ?: return
        val pageId = _uiState.value.pageId

        when (op) {
            is InkOperation.AddStroke -> {
                val updated = _uiState.value.strokes.filterNot { it.id == op.stroke.id }
                _uiState.update {
                    it.copy(
                        strokes = updated,
                        strokeCount = updated.size,
                        pageRevision = it.pageRevision + 1L,
                        canUndo = undoRedoManager.canUndo,
                        canRedo = undoRedoManager.canRedo
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.deleteStroke(op.stroke.id, pageId)
                }
            }
            is InkOperation.EraseStroke -> {
                val updated = _uiState.value.strokes + op.stroke
                _uiState.update {
                    it.copy(
                        strokes = updated,
                        strokeCount = updated.size,
                        pageRevision = it.pageRevision + 1L,
                        canUndo = undoRedoManager.canUndo,
                        canRedo = undoRedoManager.canRedo
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveStroke(op.stroke, pageId)
                }
            }
            is InkOperation.ClearPage -> {
                val updated = op.previousStrokes
                _uiState.update {
                    it.copy(
                        strokes = updated,
                        strokeCount = updated.size,
                        pageRevision = it.pageRevision + 1L,
                        canUndo = undoRedoManager.canUndo,
                        canRedo = undoRedoManager.canRedo
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveStrokes(updated, pageId)
                }
            }
        }
    }

    fun redo() {
        val op = undoRedoManager.popRedo() ?: return
        val pageId = _uiState.value.pageId

        when (op) {
            is InkOperation.AddStroke -> {
                val updated = _uiState.value.strokes + op.stroke
                _uiState.update {
                    it.copy(
                        strokes = updated,
                        strokeCount = updated.size,
                        pageRevision = it.pageRevision + 1L,
                        canUndo = undoRedoManager.canUndo,
                        canRedo = undoRedoManager.canRedo
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveStroke(op.stroke, pageId)
                }
            }
            is InkOperation.EraseStroke -> {
                val updated = _uiState.value.strokes.filterNot { it.id == op.stroke.id }
                _uiState.update {
                    it.copy(
                        strokes = updated,
                        strokeCount = updated.size,
                        pageRevision = it.pageRevision + 1L,
                        canUndo = undoRedoManager.canUndo,
                        canRedo = undoRedoManager.canRedo
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.deleteStroke(op.stroke.id, pageId)
                }
            }
            is InkOperation.ClearPage -> {
                _uiState.update {
                    it.copy(
                        strokes = emptyList(),
                        strokeCount = 0,
                        pageRevision = it.pageRevision + 1L,
                        canUndo = undoRedoManager.canUndo,
                        canRedo = undoRedoManager.canRedo
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.clearPage(pageId)
                }
            }
        }
    }

    fun clearPage() {
        val currentStrokes = _uiState.value.strokes
        if (currentStrokes.isEmpty()) return

        undoRedoManager.recordOperation(InkOperation.ClearPage(currentStrokes))
        _uiState.update {
            it.copy(
                strokes = emptyList(),
                strokeCount = 0,
                pageRevision = it.pageRevision + 1L,
                canUndo = undoRedoManager.canUndo,
                canRedo = undoRedoManager.canRedo
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.clearPage(_uiState.value.pageId)
        }
    }

    fun selectTool(tool: CanvasTool) {
        _uiState.update { it.copy(tool = tool) }
    }

    fun selectColor(color: InkColor) {
        _uiState.update { it.copy(color = color, tool = CanvasTool.PEN) }
    }

    fun selectThickness(thickness: InkThickness) {
        _uiState.update { it.copy(thickness = thickness) }
    }

    fun toggleDebugOverlay() {
        _uiState.update { it.copy(showDebugOverlay = !it.showDebugOverlay) }
    }

    fun updateFrameTime(timeMs: Float) {
        _uiState.update { it.copy(frameTimeMs = timeMs) }
    }

    fun updatePanAndZoom(deltaPanX: Float, deltaPanY: Float, zoomMultiplier: Float) {
        val old = _uiState.value.transform
        val newScale = (old.scale * zoomMultiplier).coerceIn(0.2f, 5.0f)
        val newPanX = old.panX + deltaPanX
        val newPanY = old.panY + deltaPanY
        _uiState.update {
            it.copy(transform = ViewportTransform(scale = newScale, panX = newPanX, panY = newPanY))
        }
    }

    /**
     * Programmatically inserts count (default 2000) synthetic strokes to test rendering & scrolling.
     */
    fun triggerStressTest(count: Int = 2000) {
        viewModelScope.launch(Dispatchers.Default) {
            val synthetic = StressStrokeGenerator.generateStrokes(count = count)
            val updated = _uiState.value.strokes + synthetic

            _uiState.update {
                it.copy(
                    strokes = updated,
                    strokeCount = updated.size,
                    pageRevision = it.pageRevision + 1L
                )
            }

            viewModelScope.launch(Dispatchers.IO) {
                repository.saveStrokes(synthetic, _uiState.value.pageId)
            }
        }
    }
}
