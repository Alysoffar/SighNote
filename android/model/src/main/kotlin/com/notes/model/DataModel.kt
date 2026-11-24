package com.notes.model

import kotlinx.serialization.Serializable

/**
 * Domain data model for math notebook in page-space coordinates.
 */

@Serializable
data class BoundingBox(
    val min_x: Float,
    val min_y: Float,
    val max_x: Float,
    val max_y: Float
)

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val t: Long,
    val pressure: Float,
    val tilt: Float = 0f
)

@Serializable
data class Stroke(
    val id: String,
    val brush_id: String,
    val points: List<StrokePoint>,
    val bounds: BoundingBox
)

@Serializable
data class SymbolConfidence(
    val symbol: String,
    val confidence: Float
)

@Serializable
data class RecognitionResult(
    val latex: String,
    val confidence: Float,
    val symbols: List<SymbolConfidence>? = null
)

@Serializable
data class Line(
    val id: String,
    val stroke_ids: List<String>,
    val bounding_box: BoundingBox,
    val recognition: RecognitionResult? = null
)

@Serializable
data class Problem(
    val id: String,
    val line_ids: List<String>
)

@Serializable
data class Page(
    val id: String,
    val notebook_id: String,
    val revision_counter: Long,
    val strokes: List<Stroke> = emptyList(),
    val lines: List<Line> = emptyList(),
    val problems: List<Problem> = emptyList()
)

@Serializable
data class Notebook(
    val id: String,
    val title: String,
    val pages: List<Page> = emptyList()
)
