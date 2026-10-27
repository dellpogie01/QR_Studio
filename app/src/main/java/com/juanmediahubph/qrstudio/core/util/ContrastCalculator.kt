package com.juanmediahubph.qrstudio.core.util

data class ContrastReport(
    val ratio: Double,
    val isInverted: Boolean,
    val isLowContrast: Boolean,
)

object ContrastCalculator {
    const val MIN_SCANNABLE_RATIO = 3.0

    fun relativeLuminance(argb: Long): Double {
        val red = ((argb shr 16) and 0xFF) / 255.0
        val green = ((argb shr 8) and 0xFF) / 255.0
        val blue = (argb and 0xFF) / 255.0
        val r = linearize(red)
        val g = linearize(green)
        val b = linearize(blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrastRatio(foreground: Long, background: Long): Double {
        val fg = relativeLuminance(foreground)
        val bg = relativeLuminance(background)
        val lighter = maxOf(fg, bg)
        val darker = minOf(fg, bg)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun evaluate(foreground: Long, background: Long): ContrastReport {
        val fgLuminance = relativeLuminance(foreground)
        val bgLuminance = relativeLuminance(background)
        val ratio = contrastRatio(foreground, background)
        return ContrastReport(
            ratio = ratio,
            isInverted = fgLuminance > bgLuminance,
            isLowContrast = ratio < MIN_SCANNABLE_RATIO,
        )
    }

    private fun linearize(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    private fun Double.pow(exponent: Double): Double = Math.pow(this, exponent)
}
