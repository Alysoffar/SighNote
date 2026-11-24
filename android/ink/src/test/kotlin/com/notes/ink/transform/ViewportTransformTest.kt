package com.notes.ink.transform

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportTransformTest {

    @Test
    fun testIdentityTransformRoundTrip() {
        val transform = ViewportTransform(scale = 1.0f, panX = 0f, panY = 0f)
        val originalX = 150.0f
        val originalY = 320.0f

        val (sx, sy) = transform.pageToScreen(originalX, originalY)
        assertEquals(originalX, sx, 0.001f)
        assertEquals(originalY, sy, 0.001f)

        val (px, py) = transform.screenToPage(sx, sy)
        assertEquals(originalX, px, 0.001f)
        assertEquals(originalY, py, 0.001f)
    }

    @Test
    fun testScaledAndPagedTransformRoundTrip() {
        val transform = ViewportTransform(scale = 2.5f, panX = 120.0f, panY = -340.0f)
        val originalPageX = 245.5f
        val originalPageY = 678.9f

        val (screenX, screenY) = transform.pageToScreen(originalPageX, originalPageY)
        val (backToPageX, backToPageY) = transform.screenToPage(screenX, screenY)

        assertEquals(originalPageX, backToPageX, 0.001f)
        assertEquals(originalPageY, backToPageY, 0.001f)
    }

    @Test
    fun testZoomInIncreasesScreenCoordinates() {
        val zoomedOut = ViewportTransform(scale = 1.0f, panX = 0f, panY = 0f)
        val zoomedIn = ViewportTransform(scale = 2.0f, panX = 0f, panY = 0f)

        val (sx1, sy1) = zoomedOut.pageToScreen(100f, 100f)
        val (sx2, sy2) = zoomedIn.pageToScreen(100f, 100f)

        assertEquals(100f, sx1, 0.001f)
        assertEquals(200f, sx2, 0.001f)
        assertEquals(100f, sy1, 0.001f)
        assertEquals(200f, sy2, 0.001f)
    }
}
