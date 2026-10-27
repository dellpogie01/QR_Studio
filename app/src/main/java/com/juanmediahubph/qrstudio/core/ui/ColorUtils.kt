package com.juanmediahubph.qrstudio.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.juanmediahubph.qrstudio.core.theme.QrPalette
import java.util.Locale

fun Long.toComposeColor(): Color = Color(this.toInt())

fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

/** Accepts `#RRGGBB`, `RRGGBB`, `#RGB` or `RGB` and returns an opaque ARGB value. */
fun parseHexColor(input: String): Long? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.any { it !in "0123456789abcdefABCDEF" }) return null
    return when (cleaned.length) {
        6 -> ("FF$cleaned").toLongOrNull(16)?.let(QrPalette::normalize)
        3 -> {
            val expanded = cleaned.flatMap { listOf(it, it) }.joinToString("")
            ("FF$expanded").toLongOrNull(16)?.let(QrPalette::normalize)
        }

        else -> null
    }
}

fun Long.toHexString(): String = String.format(Locale.ROOT, "#%06X", this and 0xFFFFFF)
