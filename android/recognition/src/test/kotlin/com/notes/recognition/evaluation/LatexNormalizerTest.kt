package com.notes.recognition.evaluation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexNormalizerTest {

    @Test
    fun testMathModeDelimitersStripped() {
        assertEquals("x+1", LatexNormalizer.normalize("$ x + 1 $"))
        assertEquals("x+1", LatexNormalizer.normalize("$$ x + 1 $$"))
        assertEquals("x+1", LatexNormalizer.normalize("\\[ x + 1 \\]"))
        assertEquals("x+1", LatexNormalizer.normalize("\\( x + 1 \\)"))
    }

    @Test
    fun testWhitespaceAndSpacingCommandsRemoved() {
        // Spaces, \quad, \;, \,
        assertEquals("a=b", LatexNormalizer.normalize("a \\quad = \\; b"))
        assertEquals("\\intxdx", LatexNormalizer.normalize("\\int x \\, dx"))
    }

    @Test
    fun testBracketModifiersRemoved() {
        // \left( and \right)
        assertEquals("(x+1)", LatexNormalizer.normalize("\\left( x + 1 \\right)"))
    }

    @Test
    fun testFractionSynonymsNormalized() {
        assertEquals("\\frac{1}{2}", LatexNormalizer.normalize("\\dfrac{1}{2}"))
        assertEquals("\\frac{1}{2}", LatexNormalizer.normalize("\\tfrac{1}{2}"))
    }

    @Test
    fun testSingleCharacterBracesNormalized() {
        assertEquals("x^2+y^3", LatexNormalizer.normalize("x^{2} + y^{3}"))
        assertEquals("a_1+b_2", LatexNormalizer.normalize("a_{1} + b_{2}"))
        // Multi-character braces remain intact
        assertEquals("x^{a+b}", LatexNormalizer.normalize("x^{a+b}"))
    }

    @Test
    fun testUnicodeTransliteratedToLatex() {
        assertEquals("\\pi", LatexNormalizer.normalize("π"))
        assertEquals("\\alpha+\\beta", LatexNormalizer.normalize("α + β"))
        assertEquals("\\sqrt{2}", LatexNormalizer.normalize("√{2}"))
    }

    @Test
    fun testExactMatchEquivalence() {
        assertTrue(LatexNormalizer.isExactMatch("x^{2} + 1", "x^2 + 1"))
        assertTrue(LatexNormalizer.isExactMatch("$\\dfrac{a}{b}$", "\\frac{a}{b}"))
        assertTrue(LatexNormalizer.isExactMatch("\\left( x \\right)", "(x)"))
        assertFalse(LatexNormalizer.isExactMatch("x^2", "x^3"))
    }

    @Test
    fun testSymbolAccuracyCalculation() {
        val accExact = LatexNormalizer.computeSymbolAccuracy("x + 1", "x + 1")
        assertEquals(1.0f, accExact, 0.001f)

        // 1 char difference: "x+1" (len 3) vs "x+2" (dist 1) -> 1 - 1/3 = 0.6667
        val accMinor = LatexNormalizer.computeSymbolAccuracy("x + 1", "x + 2")
        assertEquals(2f / 3f, accMinor, 0.01f)
    }
}
