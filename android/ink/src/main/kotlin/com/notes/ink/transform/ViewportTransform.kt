package com.notes.ink.transform

import android.graphics.Matrix as AndroidMatrix
import androidx.compose.ui.graphics.Matrix as ComposeMatrix

/**
 * Manages pan, zoom, and coordinate mapping between screen pixels and infinite page-space.
 *
 * All stored stroke data is in page-space units.
 * Screen coordinates = Page coordinates * scale + pan
 * Page coordinates = (Screen coordinates - pan) / scale
 */
data class ViewportTransform(
    val scale: Float = 1.0f,
    val panX: Float = 0.0f,
    val panY: Float = 0.0f
) {
    fun screenToPage(screenX: Float, screenY: Float): Pair<Float, Float> {
        val px = (screenX - panX) / scale
        val py = (screenY - panY) / scale
        return Pair(px, py)
    }

    fun pageToScreen(pageX: Float, pageY: Float): Pair<Float, Float> {
        val sx = pageX * scale + panX
        val sy = pageY * scale + panY
        return Pair(sx, sy)
    }

    /**
     * Converts page-space -> physical screen coordinates (for CanvasStrokeRenderer.draw).
     */
    fun toPageToScreenAndroidMatrix(): AndroidMatrix {
        val matrix = AndroidMatrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(panX, panY)
        return matrix
    }

    /**
     * Converts physical screen -> page-space coordinates.
     */
    fun toScreenToPageAndroidMatrix(): AndroidMatrix {
        val matrix = AndroidMatrix()
        matrix.postTranslate(-panX, -panY)
        matrix.postScale(1f / scale, 1f / scale)
        return matrix
    }

    fun toComposePageToScreenMatrix(): ComposeMatrix {
        val m = ComposeMatrix()
        m.translate(panX, panY)
        m.scale(scale, scale, 1f)
        return m
    }

    fun toComposeScreenToPageMatrix(): ComposeMatrix {
        val m = ComposeMatrix()
        m.scale(1f / scale, 1f / scale, 1f)
        m.translate(-panX, -panY)
        return m
    }
}
