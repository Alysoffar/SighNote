package com.notes.recognition

import com.notes.model.Stroke
import kotlinx.serialization.Serializable

/**
 * Result of recognizing a list of strokes.
 *
 * @param latex The recognized LaTeX expression string.
 * @param confidence Confidence score for the overall expression (0.0 to 1.0), or null if not reported.
 * @param symbolConfidences Per-symbol confidence scores if available from the recognizer.
 * @param latencyMs Latency measured by the evaluation harness or adapter in milliseconds.
 * @param rawResponse Optional raw text or JSON response from the underlying engine (useful for debugging).
 */
@Serializable
data class RecognitionResult(
    val latex: String,
    val confidence: Float? = null,
    val symbolConfidences: List<SymbolConfidence> = emptyList(),
    val latencyMs: Long = 0L,
    val rawResponse: String? = null
)

@Serializable
data class SymbolConfidence(
    val symbol: String,
    val confidence: Float
)

/**
 * Capability descriptor for candidate recognizers.
 */
data class RecognizerDescriptor(
    val id: String,
    val displayName: String,
    val isOfflineCapable: Boolean,
    val notes: String
)

/**
 * Core swappable interface for handwriting recognizers.
 * Operates purely on domain [Stroke] objects in logical page-space.
 */
interface Recognizer {
    val descriptor: RecognizerDescriptor

    /**
     * Checks if this recognizer is available and ready for inference
     * (e.g. models downloaded, API keys configured).
     */
    suspend fun isAvailable(): Boolean

    /**
     * Recognizes a list of page-space strokes into a LaTeX expression.
     */
    suspend fun recognize(strokes: List<Stroke>): RecognitionResult
}
