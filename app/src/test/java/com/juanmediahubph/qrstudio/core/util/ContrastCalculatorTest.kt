package com.juanmediahubph.qrstudio.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastCalculatorTest {
    private val black = 0xFF000000L
    private val white = 0xFFFFFFFFL

    @Test
    fun `black on white has a 21 to 1 ratio`() {
        assertEquals(21.0, ContrastCalculator.contrastRatio(black, white), 0.001)
        assertEquals(21.0, ContrastCalculator.contrastRatio(white, black), 0.001)
    }

    @Test
    fun `identical colors have a 1 to 1 ratio`() {
        assertEquals(1.0, ContrastCalculator.contrastRatio(black, black), 0.0001)
    }

    @Test
    fun `detects inversion`() {
        val normal = ContrastCalculator.evaluate(black, white)
        assertFalse(normal.isInverted)
        assertFalse(normal.isLowContrast)

        val inverted = ContrastCalculator.evaluate(white, black)
        assertTrue(inverted.isInverted)
        assertFalse(inverted.isLowContrast)
    }

    @Test
    fun `flags low contrast below three to one`() {
        val greyOnGrey = ContrastCalculator.evaluate(0xFF777777, 0xFF888888)
        assertTrue(greyOnGrey.isLowContrast)
        assertTrue(greyOnGrey.ratio < 3.0)

        val brandOnWhite = ContrastCalculator.evaluate(0xFF0B6B57, white)
        assertFalse(brandOnWhite.isLowContrast)
        assertTrue(brandOnWhite.ratio > 3.0)
    }

    @Test
    fun `luminance ignores alpha`() {
        assertEquals(
            ContrastCalculator.relativeLuminance(black),
            ContrastCalculator.relativeLuminance(0x00000000L),
            1e-9,
        )
    }
}
