package com.notes.ink.perf

import com.notes.model.BoundingBox
import com.notes.model.Stroke
import com.notes.model.StrokePoint
import java.util.UUID
import kotlin.math.sin

object StressStrokeGenerator {

    /**
     * Generates count (default 2000) synthetic strokes spread along an infinite vertical page.
     */
    fun generateStrokes(count: Int = 2000, startY: Float = 100f): List<Stroke> {
        val strokes = ArrayList<Stroke>(count)
        val brushIds = listOf("color_black_size_8", "color_blue_size_4", "color_red_size_16")

        for (i in 0 until count) {
            val strokeId = UUID.randomUUID().toString()
            val brushId = brushIds[i % brushIds.size]

            // Lay strokes out in lines across a 900-pixel wide page
            val col = i % 4
            val row = i / 4
            val baseX = 60f + col * 220f
            val baseY = startY + row * 80f

            val pointCount = 10
            val points = ArrayList<StrokePoint>(pointCount)
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE

            for (p in 0 until pointCount) {
                val px = baseX + p * 15f + (sin(p * 0.8) * 10f).toFloat()
                val py = baseY + (sin(p * 0.5) * 15f).toFloat()
                val pressure = 0.4f + (sin(p * 0.4).toFloat() * 0.3f).coerceIn(0f, 0.5f)
                val t = i * 50L + p * 16L

                minX = minOf(minX, px)
                minY = minOf(minY, py)
                maxX = maxOf(maxX, px)
                maxY = maxOf(maxY, py)

                points.add(
                    StrokePoint(
                        x = px,
                        y = py,
                        t = t,
                        pressure = pressure,
                        tilt = 15f
                    )
                )
            }

            strokes.add(
                Stroke(
                    id = strokeId,
                    brush_id = brushId,
                    points = points,
                    bounds = BoundingBox(minX, minY, maxX, maxY)
                )
            )
        }
        return strokes
    }
}
