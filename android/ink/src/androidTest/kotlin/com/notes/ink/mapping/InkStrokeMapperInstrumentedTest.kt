package com.notes.ink.mapping

import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import com.notes.ink.transform.ViewportTransform
import com.notes.model.BoundingBox
import com.notes.model.Stroke
import com.notes.model.StrokePoint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class InkStrokeMapperInstrumentedTest {

    @Test
    fun testModelToInkStrokeAndBack() {
        val originalPoints = listOf(
            StrokePoint(x = 100f, y = 150f, t = 1000L, pressure = 0.6f, tilt = 25f),
            StrokePoint(x = 120f, y = 180f, t = 1016L, pressure = 0.85f, tilt = 30f),
            StrokePoint(x = 160f, y = 220f, t = 1032L, pressure = 0.4f, tilt = 20f)
        )
        val originalStroke = Stroke(
            id = UUID.randomUUID().toString(),
            brush_id = "color_black_size_8",
            points = originalPoints,
            bounds = BoundingBox(100f, 150f, 160f, 220f)
        )

        val brush = Brush.createWithColorIntArgb(
            family = StockBrushes.pressurePen(),
            colorIntArgb = android.graphics.Color.BLACK,
            size = 8f,
            epsilon = 0.1f
        )

        // 1. Convert to InkStroke
        val inkStroke = InkStrokeMapper.toInkStroke(originalStroke, brush)
        assertNotNull(inkStroke)
        assertEquals(3, inkStroke.inputs.size)

        // 2. Convert back to ModelStroke with identity transform
        val roundTripStroke = InkStrokeMapper.toModelStroke(
            inkStroke = inkStroke,
            transform = ViewportTransform(),
            id = originalStroke.id,
            brushId = originalStroke.brush_id
        )

        assertEquals(originalStroke.id, roundTripStroke.id)
        assertEquals(originalStroke.brush_id, roundTripStroke.brush_id)
        assertEquals(3, roundTripStroke.points.size)

        for (i in 0 until 3) {
            val origPt = originalPoints[i]
            val mappedPt = roundTripStroke.points[i]

            assertEquals("X mismatch at index $i", origPt.x, mappedPt.x, 0.01f)
            assertEquals("Y mismatch at index $i", origPt.y, mappedPt.y, 0.01f)
            assertEquals("Timestamp mismatch at index $i", origPt.t, mappedPt.t)
            assertEquals("Pressure mismatch at index $i", origPt.pressure, mappedPt.pressure, 0.05f)
            assertEquals("Tilt mismatch at index $i", origPt.tilt, mappedPt.tilt, 0.5f)
        }

        // Verify bounds
        assertEquals(100f, roundTripStroke.bounds.min_x, 0.01f)
        assertEquals(150f, roundTripStroke.bounds.min_y, 0.01f)
        assertEquals(160f, roundTripStroke.bounds.max_x, 0.01f)
        assertEquals(220f, roundTripStroke.bounds.max_y, 0.01f)
    }

    @Test
    fun testScreenToPageTransformAppliedDuringMapping() {
        val originalPoints = listOf(
            StrokePoint(x = 200f, y = 400f, t = 1000L, pressure = 0.5f, tilt = 0f)
        )
        val stroke = Stroke(
            id = "test-transform",
            brush_id = "pen",
            points = originalPoints,
            bounds = BoundingBox(200f, 400f, 200f, 400f)
        )
        val brush = Brush.createWithColorIntArgb(
            family = StockBrushes.pressurePen(),
            colorIntArgb = android.graphics.Color.BLACK,
            size = 8f,
            epsilon = 0.1f
        )
        val inkStroke = InkStrokeMapper.toInkStroke(stroke, brush)

        // Transform: panX = 100, scale = 2.0 -> pageX = (screenX - 100) / 2
        val transform = ViewportTransform(scale = 2.0f, panX = 100f, panY = 0f)
        val mapped = InkStrokeMapper.toModelStroke(inkStroke, transform)

        // Expected pageX = (200 - 100) / 2 = 50.0
        assertEquals(50.0f, mapped.points[0].x, 0.01f)
        // Expected pageY = (400 - 0) / 2 = 200.0
        assertEquals(200.0f, mapped.points[0].y, 0.01f)
    }
}
