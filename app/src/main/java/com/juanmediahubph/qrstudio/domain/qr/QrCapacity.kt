package com.juanmediahubph.qrstudio.domain.qr

import com.juanmediahubph.qrstudio.domain.model.EcLevel

object QrCapacity {
    /** Byte-mode payload capacity of the largest QR version (40) per EC level. */
    private val maxBytes = mapOf(
        EcLevel.L to 2953,
        EcLevel.M to 2331,
        EcLevel.Q to 1663,
        EcLevel.H to 1273,
    )

    fun byteCount(text: String): Int = text.toByteArray(Charsets.UTF_8).size

    fun maxBytes(level: EcLevel): Int = maxBytes.getValue(level)

    fun isOverCapacity(text: String, level: EcLevel): Boolean = byteCount(text) > maxBytes(level)

    fun isNearCapacity(text: String, level: EcLevel, threshold: Double = 0.85): Boolean {
        val limit = maxBytes(level)
        return byteCount(text) > limit * threshold
    }
}
