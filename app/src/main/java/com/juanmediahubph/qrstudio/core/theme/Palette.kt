package com.juanmediahubph.qrstudio.core.theme

/**
 * ARGB color values used by QR customization. Colors live here (rather than in UI code)
 * so the theme, settings and export layers share one source of truth.
 */
object QrPalette {
    const val Black = 0xFF000000L
    const val White = 0xFFFFFFFFL

    val swatches: List<Long> = listOf(
        0xFF000000L,
        0xFF1F2924L,
        0xFF37474FL,
        0xFF0B6B57L,
        0xFF00695CL,
        0xFF1565C0L,
        0xFF4527A0L,
        0xFFAD1457L,
        0xFFC62828L,
        0xFFEF6C00L,
        0xFF6D4C41L,
        0xFF7A8B84L,
        0xFFF1F8E9L,
        0xFFE3F2FDL,
        0xFFFCE4ECL,
        0xFFFFF8E1L,
        0xFFF3E5F5L,
        0xFFFFFFFFL,
    )

    fun normalize(argb: Long): Long = argb or 0xFF000000L
}

object BrandColors {
    const val Ink = 0xFF111815L
    const val Muted = 0xFF5B6661L
    const val Accent = 0xFF0B6B57L
    const val Paper = 0xFFFFFFFFL
    const val Hairline = 0xFFDCE5E0L
}
