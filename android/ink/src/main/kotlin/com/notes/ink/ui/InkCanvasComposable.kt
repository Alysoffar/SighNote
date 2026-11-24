package com.notes.ink.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor

/**
 * Compose canvas integrating the native stylus inking pipeline, palm rejection,
 * infinite vertical scroll, faint ruled notebook lines, and real-time performance HUD.
 */
@Composable
fun InkCanvasComposable(
    viewModel: InkCanvasViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Active brush: pressure-sensitive pen configured from toolbar state
    val currentBrush = remember(uiState.color, uiState.thickness, uiState.tool) {
        val brushColor = if (uiState.tool == CanvasTool.HIGHLIGHTER) {
            when (uiState.color) {
                InkColor.BLACK -> Color(0x66FFEB3B)
                InkColor.BLUE -> Color(0x6680D8FF)
                InkColor.RED -> Color(0x66FF8A80)
                InkColor.YELLOW -> Color(0x66FFEB3B)
                InkColor.GREEN -> Color(0x66CCFF90)
            }
        } else {
            uiState.color.composeColor
        }

        val brushSize = if (uiState.tool == CanvasTool.HIGHLIGHTER) 24f else uiState.thickness.sizePx

        Brush.createWithComposeColor(
            family = StockBrushes.pressurePen(),
            color = brushColor,
            size = brushSize,
            epsilon = 0.1f
        )
    }

    // Measure frame time for performance monitoring
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { nowNanos ->
                if (lastFrameNanos != 0L) {
                    val frameMs = (nowNanos - lastFrameNanos) / 1_000_000f
                    viewModel.updateFrameTime(frameMs)
                }
                lastFrameNanos = nowNanos
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Native Stylus Inking & Gesture Canvas View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                InkCanvasView(context).apply {
                    this.currentBrush = currentBrush
                    this.currentTool = uiState.tool
                    this.transform = uiState.transform
                    this.strokes = uiState.strokes

                    this.onStrokeFinished = { inkStroke ->
                        viewModel.onStrokeFinished(inkStroke)
                    }
                    this.onEraseAt = { sx, sy ->
                        viewModel.onEraseAt(sx, sy)
                    }
                    this.onPanAndZoom = { dx, dy, zoom ->
                        viewModel.updatePanAndZoom(
                            deltaPanX = dx,
                            deltaPanY = dy,
                            zoomMultiplier = zoom
                        )
                    }
                }
            },
            update = { view ->
                view.currentBrush = currentBrush
                view.currentTool = uiState.tool
                view.transform = uiState.transform
                view.strokes = uiState.strokes
                view.pdfPath = uiState.pdfPath
                view.pdfPageIndex = uiState.pdfPageIndex
            }
        )

        // Performance Debug Overlay (HUD)
        if (uiState.showDebugOverlay) {
            DebugOverlay(
                strokeCount = uiState.strokeCount,
                frameTimeMs = uiState.frameTimeMs,
                pageRevision = uiState.pageRevision,
                scale = uiState.transform.scale,
                panX = uiState.transform.panX,
                panY = uiState.transform.panY
            )
        }
    }
}
