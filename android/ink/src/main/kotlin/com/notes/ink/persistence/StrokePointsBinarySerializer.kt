package com.notes.ink.persistence

import com.notes.model.StrokePoint
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Compact binary serializer for List<StrokePoint> into ByteArray.
 *
 * Binary layout:
 * - 4 bytes: Int32 point count N
 * - Per point:
 *   - 4 bytes: Float32 x
 *   - 4 bytes: Float32 y
 *   - 8 bytes: Int64 timestamp t
 *   - 4 bytes: Float32 pressure
 *   - 4 bytes: Float32 tilt
 * Total per point: 24 bytes.
 */
object StrokePointsBinarySerializer {

    private const val BYTES_PER_POINT = 24

    fun serialize(points: List<StrokePoint>): ByteArray {
        val buffer = ByteBuffer.allocate(4 + points.size * BYTES_PER_POINT)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(points.size)
        for (p in points) {
            buffer.putFloat(p.x)
            buffer.putFloat(p.y)
            buffer.putLong(p.t)
            buffer.putFloat(p.pressure)
            buffer.putFloat(p.tilt)
        }
        return buffer.array()
    }

    fun deserialize(bytes: ByteArray): List<StrokePoint> {
        if (bytes.size < 4) return emptyList()
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val count = buffer.int
        if (count <= 0) return emptyList()

        val points = ArrayList<StrokePoint>(count)
        for (i in 0 until count) {
            if (buffer.remaining() < BYTES_PER_POINT) break
            val x = buffer.float
            val y = buffer.float
            val t = buffer.long
            val pressure = buffer.float
            val tilt = buffer.float
            points.add(StrokePoint(x = x, y = y, t = t, pressure = pressure, tilt = tilt))
        }
        return points
    }
}
