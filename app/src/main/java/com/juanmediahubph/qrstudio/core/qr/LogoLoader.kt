package com.juanmediahubph.qrstudio.core.qr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import java.io.File

object LogoLoader {
    private const val MAX_DIMENSION = 512

    fun load(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.isFile) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAX_DIMENSION * 2 ||
                bounds.outHeight / sampleSize > MAX_DIMENSION * 2
            ) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeFile(path, options) ?: return null
            val largest = maxOf(decoded.width, decoded.height)
            if (largest > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / largest
                val scaled = decoded.scale(
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                )
                if (scaled !== decoded) decoded.recycle()
                scaled
            } else {
                decoded
            }
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
