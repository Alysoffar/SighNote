package com.notes.recognition

import com.notes.model.BoundingBox
import com.notes.model.Stroke
import com.notes.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineGrouperTest {

    private val grouper = LineGrouper()

    private fun createStroke(id: String, minX: Float, minY: Float, width: Float, height: Float): Stroke {
        val bbox = BoundingBox(minX, minY, minX + width, minY + height)
        val points = listOf(
            StrokePoint(minX, minY, 0L, 0.5f),
            StrokePoint(minX + width, minY + height, 10L, 0.5f)
        )
        return Stroke(
            id = id,
            brush_id = "pen_black",
            bounds = bbox,
            points = points
        )
    }

    @Test
    fun testEmptyStrokesReturnsEmpty() {
        val lines = grouper.groupIntoLines(emptyList())
        assertTrue(lines.isEmpty())
    }

    @Test
    fun testSingleLineHorizontalLettersGroupedTogether() {
        // "x + 2 = 5" on line y = 100, height = 30
        val strokeX = createStroke("s1", 100f, 100f, 25f, 30f)
        val strokePlus = createStroke("s2", 135f, 105f, 20f, 20f)
        val strokeTwo = createStroke("s3", 165f, 100f, 20f, 30f)
        val strokeEq = createStroke("s4", 195f, 108f, 25f, 15f)
        val strokeFive = createStroke("s5", 230f, 100f, 20f, 30f)

        val lines = grouper.groupIntoLines(listOf(strokeX, strokePlus, strokeTwo, strokeEq, strokeFive))

        assertEquals(1, lines.size)
        assertEquals(5, lines[0].strokes.size)
    }

    @Test
    fun testFractionsGroupedIntoSingleLine() {
        // Fraction: numerator "1" above fraction bar, denominator "2" below fraction bar
        val numerator = createStroke("num", 200f, 80f, 20f, 25f)
        val bar = createStroke("bar", 190f, 115f, 40f, 5f)
        val denominator = createStroke("den", 200f, 130f, 20f, 25f)

        // Beside it: "= 0.5"
        val eq = createStroke("eq", 245f, 110f, 25f, 15f)
        val half = createStroke("half", 280f, 105f, 45f, 25f)

        val lines = grouper.groupIntoLines(listOf(numerator, bar, denominator, eq, half))

        assertEquals(1, lines.size)
        assertEquals(5, lines[0].strokes.size)
    }

    @Test
    fun testSuperscriptExponentsGroupedWithBase() {
        val base = createStroke("base_x", 100f, 200f, 25f, 30f)
        val exponent = createStroke("exp_2", 130f, 185f, 15f, 18f)

        val lines = grouper.groupIntoLines(listOf(base, exponent))

        assertEquals(1, lines.size)
        assertEquals(2, lines[0].strokes.size)
    }

    @Test
    fun testTwoCloselyWrittenLinesRemainSeparate() {
        val line1Stroke1 = createStroke("l1_1", 100f, 100f, 20f, 30f)
        val line1Stroke2 = createStroke("l1_2", 130f, 105f, 20f, 20f)
        val line1Stroke3 = createStroke("l1_3", 160f, 100f, 40f, 30f)

        val line2Stroke1 = createStroke("l2_1", 100f, 150f, 20f, 30f)
        val line2Stroke2 = createStroke("l2_2", 130f, 155f, 20f, 20f)
        val line2Stroke3 = createStroke("l2_3", 160f, 150f, 40f, 30f)

        val lines = grouper.groupIntoLines(
            listOf(line1Stroke1, line1Stroke2, line1Stroke3, line2Stroke1, line2Stroke2, line2Stroke3)
        )

        assertEquals(2, lines.size)
        assertEquals(3, lines[0].strokes.size)
        assertEquals(3, lines[1].strokes.size)
        assertTrue(lines[0].boundingBox.min_y < lines[1].boundingBox.min_y)
    }
}
