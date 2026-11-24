package com.notes.net

import com.notes.model.BoundingBox
import com.notes.model.Line
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

val ProtocolJson = Json {
    ignoreUnknownKeys = false
    encodeDefaults = true
    prettyPrint = false
}

val TolerantProtocolJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class MessageEnvelope(
    val v: Int = 1,
    val type: String,
    val id: String,
    val session_id: String,
    val page_rev: Long,
    val payload: JsonElement
)

@Serializable
data class Point2D(
    val x: Float,
    val y: Float
)

@Serializable
data class RegionSubmitPayload(
    val lines: List<Line>,
    val bounding_box: BoundingBox,
    val trigger: String? = "manual"
)

@Serializable
data class LassoSubmitPayload(
    val line_ids: List<String>,
    val bounding_box: BoundingBox,
    val polygon_points: List<Point2D>? = null
)

@Serializable
data class CancelPayload(
    val target_id: String,
    val reason: String? = null
)

@Serializable
data class PingPayload(
    val timestamp: Long? = null
)

@Serializable
data class StatusPayload(
    val code: String,
    val message: String
)

@Serializable
data class ActionUnderlinePayload(
    val action_id: String,
    val page_rev: Long,
    val line_id: String,
    val fallback_bbox: BoundingBox,
    val color: String,
    val style: String,
    val explanation: String? = null
)

@Serializable
data class ActionStickyNotePayload(
    val action_id: String,
    val page_rev: Long,
    val line_id: String,
    val fallback_bbox: BoundingBox,
    val content: String,
    val color: String? = null
)

@Serializable
data class ActionDismissPayload(
    val action_id: String
)

@Serializable
data class ErrorPayload(
    val code: String,
    val message: String,
    val details: JsonElement? = null
)

@Serializable
data class PongPayload(
    val timestamp: Long? = null
)

sealed interface ServerMessage {
    data class Status(val envelope: MessageEnvelope, val payload: StatusPayload) : ServerMessage
    data class Underline(val envelope: MessageEnvelope, val payload: ActionUnderlinePayload) : ServerMessage
    data class StickyNote(val envelope: MessageEnvelope, val payload: ActionStickyNotePayload) : ServerMessage
    data class Dismiss(val envelope: MessageEnvelope, val payload: ActionDismissPayload) : ServerMessage
    data class Error(val envelope: MessageEnvelope, val payload: ErrorPayload) : ServerMessage
    data class Pong(val envelope: MessageEnvelope, val payload: PongPayload) : ServerMessage
    data class Unknown(val envelope: MessageEnvelope) : ServerMessage
}
