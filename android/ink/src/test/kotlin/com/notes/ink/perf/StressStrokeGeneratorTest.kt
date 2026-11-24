package com.notes.ink.perf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StressStrokeGeneratorTest {

    @Test
    fun testGenerateTwoThousandStrokes() {
        val strokes = StressStrokeGenerator.generateStrokes(count = 2000)
        assertEquals(2000, strokes.size)

        for (stroke in strokes) {
            assertTrue("Stroke points must not be empty", stroke.points.isNotEmpty())
            assertTrue("Bounds minX <= maxX", stroke.bounds.min_x <= stroke.bounds.max_x)
            assertTrue("Bounds minY <= maxY", stroke.bounds.min_y <= stroke.bounds.max_y)

            for (pt in stroke.points) {
                assertTrue("Pressure must be in [0, 1]", pt.pressure in 0f..1f)
            }
        }

        // Verify vertical distribution across infinite scroll area
        val minY = strokes.minOf { it.bounds.min_y }
        val maxY = strokes.maxOf { it.bounds.max_y }
        assertTrue("Strokes must span a large vertical scroll region (>10,000)", (maxY - minY) > 10000f)
    }
}
