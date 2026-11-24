package com.notes.recognition

import com.notes.model.BoundingBox
import com.notes.model.Stroke
import kotlin.math.max
import kotlin.math.min

/**
 * Line grouping result containing the strokes in a line and their combined bounding box.
 */
data class LineGroup(
    val strokes: List<Stroke>,
    val boundingBox: BoundingBox
)

/**
 * Configurable thresholds for clustering freeform handwritten strokes into mathematical lines.
 *
 * @param verticalOverlapThreshold Fraction of vertical overlap (relative to smaller height)
 *                                  required to merge two stroke groups into the same line.
 * @param verticalGapToleranceFactor Multiplied by average stroke height: if two strokes have no vertical
 *                                   overlap, they can still merge if their vertical gap is below this threshold
 *                                   AND their horizontal proximity is within horizontalProximityFactor.
 * @param horizontalProximityFactor Maximum horizontal distance (as a multiple of line height) to merge horizontally
 *                                  adjacent strokes into the same line.
 */
data class LineGrouperConfig(
    val verticalOverlapThreshold: Float = 0.25f,
    val verticalGapToleranceFactor: Float = 0.35f,
    val horizontalProximityFactor: Float = 2.5f
)

/**
 * Clusters freeform page-space [Stroke]s into logical lines using vertical overlap,
 * proximity, and bounding box geometry.
 */
class LineGrouper(
    private val config: LineGrouperConfig = LineGrouperConfig()
) {

    fun groupIntoLines(strokes: List<Stroke>): List<LineGroup> {
        if (strokes.isEmpty()) return emptyList()

        // Wrap each stroke in its own initial group
        val groups = strokes.map { stroke ->
            MutableGroup(mutableListOf(stroke), computeBoundingBox(listOf(stroke)))
        }.toMutableList()

        var merged = true
        while (merged) {
            merged = false
            var i = 0
            while (i < groups.size) {
                var j = i + 1
                while (j < groups.size) {
                    if (shouldMerge(groups[i], groups[j])) {
                        groups[i].strokes.addAll(groups[j].strokes)
                        groups[i].boundingBox = computeBoundingBox(groups[i].strokes)
                        groups.removeAt(j)
                        merged = true
                    } else {
                        j++
                    }
                }
                i++
            }
        }

        // Sort groups vertically from top to bottom, then horizontally
        return groups
            .map { LineGroup(it.strokes, it.boundingBox) }
            .sortedWith(compareBy({ it.boundingBox.min_y }, { it.boundingBox.min_x }))
    }

    private fun shouldMerge(a: MutableGroup, b: MutableGroup): Boolean {
        val boxA = a.boundingBox
        val boxB = b.boundingBox

        val topA = boxA.min_y
        val bottomA = boxA.max_y
        val topB = boxB.min_y
        val bottomB = boxB.max_y

        val leftA = boxA.min_x
        val rightA = boxA.max_x
        val leftB = boxB.min_x
        val rightB = boxB.max_x

        val heightA = (bottomA - topA).coerceAtLeast(1f)
        val heightB = (bottomB - topB).coerceAtLeast(1f)
        val widthA = (rightA - leftA).coerceAtLeast(1f)
        val widthB = (rightB - leftB).coerceAtLeast(1f)

        // Vertical overlap calculation
        val overlapTop = max(topA, topB)
        val overlapBottom = min(bottomA, bottomB)
        val verticalOverlap = max(0f, overlapBottom - overlapTop)

        val minHeight = min(heightA, heightB)
        val avgHeight = (heightA + heightB) / 2f
        val overlapRatio = verticalOverlap / minHeight

        // Horizontal distance calculation
        val horizontalGap = max(0f, max(leftA, leftB) - min(rightA, rightB))

        // Horizontal overlap (e.g. fraction numerator over denominator)
        val overlapLeft = max(leftA, leftB)
        val overlapRight = min(rightA, rightB)
        val horizontalOverlap = max(0f, overlapRight - overlapLeft)
        val minWidth = min(widthA, widthB)
        val horizontalOverlapRatio = horizontalOverlap / minWidth

        // 1. Fractions & Radicals: Significant horizontal overlap with vertical proximity
        // Require horizontal containment or significant mutual overlap, and vertical gap small enough
        // to be fraction components (e.g. numerator directly over bar over denominator).
        // To distinguish from two separate equation lines written one above another:
        // A fraction has a horizontal bar or stacked sub-expressions within a single line's typical height range.
        val verticalGap = max(0f, max(topA, topB) - min(bottomA, bottomB))
        val maxBoxHeight = max(heightA, heightB)

        // For fractions: one of the elements is usually a thin fraction bar (height <= 10f) OR
        // the vertical gap is small and the elements are compact (not full-height separate lines)
        val isFractionLike = (heightA <= 12f || heightB <= 12f) || (maxBoxHeight < 35f && verticalGap <= 15f && horizontalOverlapRatio > 0.6f)
        if (isFractionLike && horizontalOverlapRatio > 0.4f) {
            if (verticalGap <= avgHeight * 0.8f) {
                return true
            }
        }

        // 2. Direct Vertical Overlap (Characters in standard horizontal sequence)
        if (overlapRatio >= config.verticalOverlapThreshold) {
            if (horizontalGap <= avgHeight * config.horizontalProximityFactor) {
                return true
            }
        }

        // 3. Superscripts / Subscripts: Slight vertical gap but tightly coupled horizontally
        if (verticalGap <= avgHeight * config.verticalGapToleranceFactor) {
            if (horizontalGap <= avgHeight * 1.0f) {
                return true
            }
        }

        return false
    }

    private fun computeBoundingBox(strokes: List<Stroke>): BoundingBox {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (stroke in strokes) {
            minX = min(minX, stroke.bounds.min_x)
            minY = min(minY, stroke.bounds.min_y)
            maxX = max(maxX, stroke.bounds.max_x)
            maxY = max(maxY, stroke.bounds.max_y)
        }

        return BoundingBox(
            min_x = if (minX == Float.MAX_VALUE) 0f else minX,
            min_y = if (minY == Float.MAX_VALUE) 0f else minY,
            max_x = if (maxX == -Float.MAX_VALUE) 0f else maxX,
            max_y = if (maxY == -Float.MAX_VALUE) 0f else maxY
        )
    }

    private class MutableGroup(
        val strokes: MutableList<Stroke>,
        var boundingBox: BoundingBox
    )
}
