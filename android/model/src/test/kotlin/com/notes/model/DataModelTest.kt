package com.notes.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testStrokeSerializationRoundTrip() {
        val original = Stroke(
            id = "11111111-1111-1111-1111-111111111111",
            brush_id = "pen_black_medium",
            points = listOf(
                StrokePoint(x = 10.5f, y = 20.0f, t = 1000L, pressure = 0.8f, tilt = 12.0f),
                StrokePoint(x = 15.0f, y = 25.5f, t = 1016L, pressure = 0.9f, tilt = 14.5f)
            ),
            bounds = BoundingBox(min_x = 10.5f, min_y = 20.0f, max_x = 15.0f, max_y = 25.5f)
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Stroke>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun testPageRevisionCounterIncrement() {
        val page = Page(
            id = "page-1",
            notebook_id = "notebook-1",
            revision_counter = 0L
        )
        val editedPage = page.copy(revision_counter = page.revision_counter + 1)
        assertEquals(1L, editedPage.revision_counter)
        assertTrue(editedPage.revision_counter > page.revision_counter)
    }
}
