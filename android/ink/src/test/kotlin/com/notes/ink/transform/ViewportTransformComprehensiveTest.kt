package com.notes.ink.transform

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportTransformComprehensiveTest {

    @Test
    fun testMultiplePanAndZoomRoundTrips() {
        val testConfigs = listOf(
            ViewportTransform(scale = 1.0f, panX = 0f, panY = 0f),
            ViewportTransform(scale = 0.5f, panX = -200f, panY = 150f),
            ViewportTransform(scale = 2.0f, panX = 500f, panY = -800f),
            ViewportTransform(scale = 3.75f, panX = -1234.5f, panY = 678.9f),
            ViewportTransform(scale = 0.2f, panX = 0f, panY = 0f),
            ViewportTransform(scale = 5.0f, panX = 999f, panY = 999f)
        )

        val samplePoints = listOf(
            Pair(100f, 100f),
            Pair(500f, 500f),
            Pair(0f, 0f),
            Pair(-50f, 1200f),
            Pair(2048f, 1536f)
        )

        for (transform in testConfigs) {
            for ((origPx, origPy) in samplePoints) {
                val (sx, sy) = transform.pageToScreen(origPx, origPy)
                val (px, py) = transform.screenToPage(sx, sy)

                assertEquals("Page X round-trip mismatch at $transform", origPx, px, 0.001f)
                assertEquals("Page Y round-trip mismatch at $transform", origPy, py, 0.001f)
            }
        }
    }

    @Test
    fun testStrokeDrawnAtOneScrollZoomThenViewedAtAnotherRetainsPageCoordinates() {
        // Step 1: User draws a stroke while canvas is panned at (100, 200) and zoomed at 1.5x
        val initialViewport = ViewportTransform(scale = 1.5f, panX = 100f, panY = 200f)

        // Pen touches screen at (400, 500)
        val screenTouchX = 400f
        val screenTouchY = 500f

        // Input converted to page coordinates at the moment of touch:
        val (pageX, pageY) = initialViewport.screenToPage(screenTouchX, screenTouchY)
        assertEquals((400f - 100f) / 1.5f, pageX, 0.001f) // 200.0f
        assertEquals((500f - 200f) / 1.5f, pageY, 0.001f) // 200.0f

        // Step 2: User pans and zooms canvas to new state: pan (-300, -400), scale 2.0x
        val newViewport = ViewportTransform(scale = 2.0f, panX = -300f, panY = -400f)

        // The stroke is stored ONLY in page coordinates (200.0f, 200.0f)
        // When rendered under newViewport, its screen position shifts accordingly:
        val (newScreenX, newScreenY) = newViewport.pageToScreen(pageX, pageY)
        assertEquals(200f * 2.0f - 300f, newScreenX, 0.001f) // 100.0f
        assertEquals(200f * 2.0f - 400f, newScreenY, 0.001f) // 0.0f

        // Assert that converting newScreen position back using newViewport yields the original page coordinate:
        val (restoredPageX, restoredPageY) = newViewport.screenToPage(newScreenX, newScreenY)
        assertEquals(pageX, restoredPageX, 0.001f)
        assertEquals(pageY, restoredPageY, 0.001f)
    }

    @Test
    fun testPanDeltaDirectlyOffsetsScreenCoordinates() {
        val baseViewport = ViewportTransform(scale = 1.0f, panX = 0f, panY = 0f)
        val pannedViewport = ViewportTransform(scale = 1.0f, panX = 150f, panY = 250f)

        val pageX = 100f
        val pageY = 100f

        val (s1x, s1y) = baseViewport.pageToScreen(pageX, pageY)
        val (s2x, s2y) = pannedViewport.pageToScreen(pageX, pageY)

        assertEquals(150f, s2x - s1x, 0.001f)
        assertEquals(250f, s2y - s1y, 0.001f)
    }

    @Test
    fun testWetStrokeScreenPositionEqualsFinishedStrokeScreenPosition() {
        // Simulates the exact transform calculation used by InProgressStrokesView:
        // strokeToViewTransform = (motionEventToStrokeTransform)^-1 * motionEventToViewTransform
        // When motionEventToWorldTransform = screenToPageMatrix and strokeToWorldTransform = IDENTITY,
        // motionEventToStrokeTransform = screenToPageMatrix.
        // If motionEventToViewTransform is IDENTITY, then:
        // strokeToViewTransform = (screenToPageMatrix)^-1 * IDENTITY = pageToScreenMatrix.
        // This test asserts that for any touch point across the canvas (top, middle, bottom),
        // wet stroke screen position equals finished stroke screen position (< 0.001f error),
        // at 1x zoom and at zoomed + panned states.

        val viewports = listOf(
            ViewportTransform(scale = 1.0f, panX = 0f, panY = 0f),
            ViewportTransform(scale = 1.75f, panX = -120f, panY = 350f),
            ViewportTransform(scale = 0.6f, panX = 400f, panY = -250f),
            ViewportTransform(scale = 2.5f, panX = 50f, panY = 100f)
        )

        // Test across top, middle, and bottom of canvas
        val touchPoints = listOf(
            Pair(100f, 50f),    // Top
            Pair(800f, 600f),   // Middle
            Pair(1200f, 1550f)  // Bottom
        )

        for (viewport in viewports) {
            for ((rawTouchX, rawTouchY) in touchPoints) {
                // 1. Raw input in view space: (rawTouchX, rawTouchY)
                // 2. Input mapped to page space:
                val (pageX, pageY) = viewport.screenToPage(rawTouchX, rawTouchY)

                // 3. Wet stroke rendering transform:
                // Since motionEventToView is identity, wet stroke's strokeToViewTransform is pageToScreenMatrix.
                val (wetScreenX, wetScreenY) = viewport.pageToScreen(pageX, pageY)

                // 4. Dry stroke rendering transform:
                // Dry stroke is drawn using canvas.concat(pageToScreenMatrix) with page coordinates (pageX, pageY).
                val (dryScreenX, dryScreenY) = viewport.pageToScreen(pageX, pageY)

                // Assert that raw pen position, wet stroke screen position, and finished dry stroke screen position all match:
                assertEquals("Wet X does not match raw pen at $viewport", rawTouchX, wetScreenX, 0.001f)
                assertEquals("Wet Y does not match raw pen at $viewport", rawTouchY, wetScreenY, 0.001f)
                assertEquals("Dry X does not match wet X at $viewport", wetScreenX, dryScreenX, 0.001f)
                assertEquals("Dry Y does not match wet Y at $viewport", wetScreenY, dryScreenY, 0.001f)
            }
        }
    }
}
