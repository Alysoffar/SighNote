package com.notes.ink.persistence

import com.notes.model.BoundingBox
import com.notes.model.Stroke
import com.notes.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PersistenceRoundTripTest {

    @Test
    fun testStrokePointsBinarySerialization() {
        val points = listOf(
            StrokePoint(x = 12.34f, y = 56.78f, t = 1000L, pressure = 0.45f, tilt = 15.0f),
            StrokePoint(x = 90.12f, y = 34.56f, t = 1016L, pressure = 0.89f, tilt = 22.5f),
            StrokePoint(x = -45.0f, y = -12.5f, t = 1032L, pressure = 0.12f, tilt = 0.0f)
        )

        val bytes = StrokePointsBinarySerializer.serialize(points)
        assertTrue("Serialized byte array should be non-empty", bytes.isNotEmpty())
        assertEquals(4 + points.size * 24, bytes.size)

        val deserialized = StrokePointsBinarySerializer.deserialize(bytes)
        assertEquals(points.size, deserialized.size)

        for (i in points.indices) {
            val expected = points[i]
            val actual = deserialized[i]
            assertEquals("X at $i", expected.x, actual.x, 0.0001f)
            assertEquals("Y at $i", expected.y, actual.y, 0.0001f)
            assertEquals("t at $i", expected.t, actual.t)
            assertEquals("pressure at $i", expected.pressure, actual.pressure, 0.0001f)
            assertEquals("tilt at $i", expected.tilt, actual.tilt, 0.0001f)
        }
    }

    @Test
    fun testStrokeEntityMappingRoundTrip() {
        val originalStroke = Stroke(
            id = UUID.randomUUID().toString(),
            brush_id = "color_blue_size_8",
            points = listOf(
                StrokePoint(x = 10f, y = 20f, t = 500L, pressure = 0.6f, tilt = 10f),
                StrokePoint(x = 15f, y = 25f, t = 516L, pressure = 0.7f, tilt = 12f)
            ),
            bounds = BoundingBox(10f, 20f, 15f, 25f)
        )

        val entity = StrokeEntity.fromDomainStroke(originalStroke, pageId = "page-42")
        assertEquals(originalStroke.id, entity.id)
        assertEquals("page-42", entity.pageId)

        val restoredStroke = entity.toDomainStroke()
        assertEquals(originalStroke.id, restoredStroke.id)
        assertEquals(originalStroke.brush_id, restoredStroke.brush_id)
        assertEquals(originalStroke.bounds.min_x, restoredStroke.bounds.min_x, 0.001f)
        assertEquals(originalStroke.bounds.max_y, restoredStroke.bounds.max_y, 0.001f)
        assertEquals(originalStroke.points.size, restoredStroke.points.size)
    }

    @Test
    fun testEmptyPointsSerialization() {
        val bytes = StrokePointsBinarySerializer.serialize(emptyList())
        val result = StrokePointsBinarySerializer.deserialize(bytes)
        assertTrue("Deserialized list from empty should be empty", result.isEmpty())
    }
}
