package com.juanmediahubph.qrstudio.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ModuleStyle {
    SQUARE,
    ROUNDED,
    DOTS,
}

@Serializable
enum class QrSize(val px: Int) {
    PX512(512),
    PX1024(1024),
    PX2048(2048);

    companion object {
        val minimumExportSize: Int = PX512.px

        fun fromPx(px: Int): QrSize = entries.minBy { kotlin.math.abs(it.px - px) }
    }
}

@Serializable
enum class EcLevel {
    L,
    M,
    Q,
    H,
}

@Serializable
data class QrStyle(
    val foreground: Long = 0xFF000000L,
    val background: Long = 0xFFFFFFFFL,
    val moduleStyle: ModuleStyle = ModuleStyle.SQUARE,
    val size: QrSize = QrSize.PX512,
    val errorCorrection: EcLevel = EcLevel.M,
    val logoPath: String? = null,
    val logoScale: Float = 0.18f,
    val quietZone: Int = 4,
) {
    /** A logo hides modules, so the encoder always uses H while one is present. */
    val effectiveErrorCorrection: EcLevel
        get() = if (logoPath.isNullOrBlank()) errorCorrection else EcLevel.H

    fun sanitized(): QrStyle = copy(
        foreground = foreground or 0xFF000000L,
        background = background or 0xFF000000L,
        logoScale = logoScale.coerceIn(MIN_LOGO_SCALE, MAX_LOGO_SCALE),
        quietZone = quietZone.coerceIn(MIN_QUIET_ZONE, MAX_QUIET_ZONE),
        logoPath = logoPath?.takeIf { it.isNotBlank() },
    )

    companion object {
        const val MIN_LOGO_SCALE = 0.12f
        const val MAX_LOGO_SCALE = 0.20f
        const val MIN_QUIET_ZONE = 2
        const val MAX_QUIET_ZONE = 10
    }
}
