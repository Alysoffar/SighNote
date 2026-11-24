package com.notes.ink.mapping

import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke as InkStroke
import com.notes.ink.transform.ViewportTransform
import com.notes.model.BoundingBox
import com.notes.model.Stroke as ModelStroke
import com.notes.model.StrokePoint
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Isolates all conversions between androidx.ink types and com.notes.model types.
 *
 * This ensures no androidx.ink types leak outside the :ink module.
 */
object InkStrokeMapper {

    /**
     * Maps an androidx.ink finalized stroke into the domain ModelStroke in page-space coordinates.
     */
    fun toModelStroke(
        inkStroke: InkStroke,
        transform: ViewportTransform,
        id: String = UUID.randomUUID().toString(),
        brushId: String = "pressure_pen"
    ): ModelStroke {
        val inputs = inkStroke.inputs
        val points = ArrayList<StrokePoint>(inputs.size)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (i in 0 until inputs.size) {
            val input = inputs.get(i)
            // Stroke was generated with motionEventToWorldTransform converting screenToPage,
            // so input.x and input.y are ALREADY in page space!
            val px = input.x
            val py = input.y

            minX = min(minX, px)
            minY = min(minY, py)
            maxX = max(maxX, px)
            maxY = max(maxY, py)

            val pressure = if (input.hasPressure) {
                input.pressure.coerceIn(0f, 1f)
            } else {
                0.5f
            }

            val tiltDeg = if (input.hasTilt) {
                Math.toDegrees(input.tiltRadians.toDouble()).toFloat()
            } else {
                0f
            }

            points.add(
                StrokePoint(
                    x = px,
                    y = py,
                    t = input.elapsedTimeMillis,
                    pressure = pressure,
                    tilt = tiltDeg
                )
            )
        }

        val bounds = if (points.isEmpty()) {
            BoundingBox(0f, 0f, 0f, 0f)
        } else {
            BoundingBox(minX, minY, maxX, maxY)
        }

        return ModelStroke(
            id = id,
            brush_id = brushId,
            points = points,
            bounds = bounds
        )
    }

    /**
     * Reconstructs an androidx.ink.strokes.Stroke from a domain ModelStroke.
     * Useful for rendering cached or persisted strokes and synthetic stress strokes.
     */
    fun toInkStroke(
        modelStroke: ModelStroke,
        brush: Brush
    ): InkStroke {
        val batch = MutableStrokeInputBatch()
        for (pt in modelStroke.points) {
            val tiltRad = Math.toRadians(pt.tilt.toDouble()).toFloat()
            batch.add(
                InputToolType.STYLUS,
                pt.x,
                pt.y,
                pt.t,
                1.0f, // strokeUnitLengthCm
                pt.pressure.coerceIn(0f, 1f),
                tiltRad,
                0f   // orientationRadians
            )
        }
        return InkStroke(brush, batch.toImmutable())
    }
}
