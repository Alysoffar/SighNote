package com.notes.recognition.evaluation

import com.notes.model.Stroke
import kotlinx.serialization.Serializable

/**
 * Category classification for evaluation expressions.
 */
@Serializable
enum class MathCategory(val displayName: String) {
    FRACTIONS("Fractions"),
    EXPONENTS("Exponents"),
    ROOTS("Roots & Radicals"),
    SUBSCRIPTS("Subscripts"),
    GREEK_LETTERS("Greek Letters"),
    INTEGRALS("Integrals"),
    SUMMATIONS("Summations & Limits"),
    SYSTEMS_OF_EQUATIONS("Systems of Equations"),
    MATRICES("Matrices"),
    MESSY_WRITING("Messy / Rapid Writing")
}

/**
 * A mathematical prompt presented to the user during sample collection.
 */
@Serializable
data class MathPrompt(
    val id: String,
    val category: MathCategory,
    val groundTruthLatex: String,
    val description: String = ""
)

/**
 * Collected handwritten sample saved as JSON.
 */
@Serializable
data class HandwrittenSample(
    val id: String,
    val timestamp: Long,
    val category: MathCategory,
    val groundTruthLatex: String,
    val strokes: List<Stroke>
)
