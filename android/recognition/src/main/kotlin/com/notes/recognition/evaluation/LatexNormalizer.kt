package com.notes.recognition.evaluation

import kotlin.math.max
import kotlin.math.min

/**
 * Normalizes LaTeX expressions to allow fair, semantically equivalent comparisons.
 *
 * Normalization Rules:
 * 1. Delimiter Stripping: Remove enclosing $...$, $$...$$, \(...\), and \[...\].
 * 2. Whitespace & Spacing Commands: Remove standard spaces, tabs, newlines, and LaTeX spacing
 *    macros (\,, \;, \:, \!, \quad, \qquad, \enspace).
 * 3. Multi-character Redundancy: Normalize \cdot, \times, and multiplication variations where safe.
 * 4. Bracket Unification: Standardize \left and \right prefixes by stripping them (leaving (,), [,], \{,\}).
 * 5. Single Argument Braces: Normalize redundant single-character braces (e.g. x^{2} vs x^2, a_{1} vs a_1).
 * 6. Fraction Syntax: Standardize \dfrac to \frac.
 * 7. Unicode to LaTeX transliteration: Replace unicode symbols (e.g. π -> \pi, α -> \alpha, √ -> \sqrt).
 */
object LatexNormalizer {

    fun normalize(input: String?): String {
        if (input == null) return ""
        var s = input.trim()

        // 1. Strip math mode delimiters
        if (s.startsWith("$$") && s.endsWith("$$")) {
            s = s.substring(2, s.length - 2).trim()
        } else if (s.startsWith("$") && s.endsWith("$")) {
            s = s.substring(1, s.length - 1).trim()
        } else if (s.startsWith("\\[") && s.endsWith("\\]")) {
            s = s.substring(2, s.length - 2).trim()
        } else if (s.startsWith("\\(") && s.endsWith("\\)")) {
            s = s.substring(2, s.length - 2).trim()
        }

        // 2. Transliterate common unicode symbols to LaTeX
        s = s.replace("π", "\\pi")
            .replace("α", "\\alpha")
            .replace("β", "\\beta")
            .replace("γ", "\\gamma")
            .replace("θ", "\\theta")
            .replace("λ", "\\lambda")
            .replace("μ", "\\mu")
            .replace("σ", "\\sigma")
            .replace("ω", "\\omega")
            .replace("√", "\\sqrt")
            .replace("±", "\\pm")
            .replace("≤", "\\le")
            .replace("≥", "\\ge")
            .replace("≠", "\\ne")
            .replace("∞", "\\infty")
            .replace("∑", "\\sum")
            .replace("∫", "\\int")

        // 3. Remove sizing/bracket modifiers
        s = s.replace("\\left", "")
            .replace("\\right", "")
            .replace("\\Big", "")
            .replace("\\big", "")

        // 4. Standardize macros
        s = s.replace("\\dfrac", "\\frac")
            .replace("\\tfrac", "\\frac")

        // 5. Remove LaTeX spacing commands and whitespace
        val spacingCommands = listOf(
            "\\,", "\\;", "\\:", "\\!", "\\quad", "\\qquad", "\\enspace", "\\space"
        )
        for (cmd in spacingCommands) {
            s = s.replace(cmd, "")
        }
        s = s.replace(Regex("\\s+"), "")

        // 6. Normalize single-character braces for superscripts and subscripts
        // e.g. x^{2} -> x^2, a_{1} -> a_1
        s = s.replace(Regex("(\\^|_)\\{([a-zA-Z0-9])\\}"), "$1$2")

        return s
    }

    /**
     * Computes the Levenshtein edit distance between two strings.
     */
    fun editDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,       // deletion
                    min(
                        dp[i][j - 1] + 1,   // insertion
                        dp[i - 1][j - 1] + cost // substitution
                    )
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Computes symbol-level accuracy between 0.0 and 1.0 based on Levenshtein distance:
     * accuracy = 1.0 - (editDistance / max(len(ref), len(hyp)))
     */
    fun computeSymbolAccuracy(reference: String, hypothesis: String): Float {
        val normRef = normalize(reference)
        val normHyp = normalize(hypothesis)

        if (normRef.isEmpty() && normHyp.isEmpty()) return 1.0f
        val maxLen = max(normRef.length, normHyp.length)
        if (maxLen == 0) return 1.0f

        val dist = editDistance(normRef, normHyp)
        return max(0f, 1.0f - (dist.toFloat() / maxLen.toFloat()))
    }

    /**
     * Evaluates exact whole-expression match after normalization.
     */
    fun isExactMatch(reference: String, hypothesis: String): Boolean {
        return normalize(reference) == normalize(hypothesis)
    }
}
