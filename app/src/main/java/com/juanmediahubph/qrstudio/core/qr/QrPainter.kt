package com.juanmediahubph.qrstudio.core.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.juanmediahubph.qrstudio.domain.model.ModuleStyle
import com.juanmediahubph.qrstudio.domain.model.QrStyle
import com.juanmediahubph.qrstudio.domain.qr.EncodedQr
import kotlin.math.max
import kotlin.math.roundToInt

object QrPainter {
    data class RenderResult(
        val bitmap: Bitmap?,
        val logoApplied: Boolean,
        val failed: Boolean,
    )

    fun render(
        encoded: EncodedQr,
        style: QrStyle,
        targetSizePx: Int = style.size.px,
        logoPath: String? = style.logoPath,
    ): RenderResult {
        val safeStyle = style.sanitized()
        val target = targetSizePx.coerceIn(64, 4096)
        val modules = encoded.matrix.width
        val margin = safeStyle.quietZone
        val totalModules = modules + margin * 2
        val moduleSize = max(1, target / totalModules)
        val actualSize = moduleSize * totalModules
        val offset = ((target - actualSize) / 2).toFloat()

        var bitmap: Bitmap? = null
        return try {
            bitmap = createBitmap(target, target)
            val canvas = Canvas(bitmap)
            val background = safeStyle.background.toInt()
            val foreground = safeStyle.foreground.toInt()
            canvas.drawColor(background)

            val paint = Paint().apply {
                isAntiAlias = safeStyle.moduleStyle != ModuleStyle.SQUARE
                color = foreground
            }
            val backgroundPaint = Paint().apply {
                isAntiAlias = paint.isAntiAlias
                color = background
            }

            val finderRegions = finderRegions(modules)
            for (row in 0 until modules) {
                for (column in 0 until modules) {
                    if (!encoded.matrix.get(column, row)) continue
                    if (finderRegions.any { it.contains(column, row) }) continue
                    val left = offset + (margin + column) * moduleSize
                    val top = offset + (margin + row) * moduleSize
                    when (safeStyle.moduleStyle) {
                        ModuleStyle.SQUARE -> canvas.drawRect(
                            left,
                            top,
                            left + moduleSize,
                            top + moduleSize,
                            paint,
                        )

                        ModuleStyle.ROUNDED -> canvas.drawRoundRect(
                            left,
                            top,
                            left + moduleSize,
                            top + moduleSize,
                            moduleSize * 0.3f,
                            moduleSize * 0.3f,
                            paint,
                        )

                        ModuleStyle.DOTS -> canvas.drawCircle(
                            left + moduleSize / 2f,
                            top + moduleSize / 2f,
                            moduleSize * 0.46f,
                            paint,
                        )
                    }
                }
            }

            finderRegions.forEach { region ->
                drawFinder(
                    canvas = canvas,
                    paint = paint,
                    backgroundPaint = backgroundPaint,
                    left = offset + (margin + region.left) * moduleSize,
                    top = offset + (margin + region.top) * moduleSize,
                    moduleSize = moduleSize.toFloat(),
                    style = safeStyle.moduleStyle,
                )
            }

            val logo = LogoLoader.load(logoPath)
            var logoApplied = false
            if (logo != null) {
                logoApplied = drawLogo(
                    canvas = canvas,
                    qrPixelWidth = modules * moduleSize,
                    logo = logo,
                    style = safeStyle,
                )
                logo.recycle()
            }

            RenderResult(bitmap = bitmap, logoApplied = logoApplied, failed = false)
        } catch (_: OutOfMemoryError) {
            bitmap?.recycle()
            RenderResult(bitmap = null, logoApplied = false, failed = true)
        } catch (_: Exception) {
            bitmap?.recycle()
            RenderResult(bitmap = null, logoApplied = false, failed = true)
        }
    }

    private fun drawFinder(
        canvas: Canvas,
        paint: Paint,
        backgroundPaint: Paint,
        left: Float,
        top: Float,
        moduleSize: Float,
        style: ModuleStyle,
    ) {
        val size = moduleSize * 7
        if (style == ModuleStyle.SQUARE) {
            canvas.drawRect(left, top, left + size, top + size, paint)
            canvas.drawRect(
                left + moduleSize,
                top + moduleSize,
                left + size - moduleSize,
                top + size - moduleSize,
                backgroundPaint,
            )
            canvas.drawRect(
                left + moduleSize * 2,
                top + moduleSize * 2,
                left + size - moduleSize * 2,
                top + size - moduleSize * 2,
                paint,
            )
        } else {
            val outerRadius = moduleSize * 1.6f
            canvas.drawRoundRect(
                left,
                top,
                left + size,
                top + size,
                outerRadius,
                outerRadius,
                paint,
            )
            val inset = moduleSize
            canvas.drawRoundRect(
                left + inset,
                top + inset,
                left + size - inset,
                top + size - inset,
                outerRadius * 0.7f,
                outerRadius * 0.7f,
                backgroundPaint,
            )
            val centerInset = moduleSize * 2
            canvas.drawRoundRect(
                left + centerInset,
                top + centerInset,
                left + size - centerInset,
                top + size - centerInset,
                outerRadius * 0.45f,
                outerRadius * 0.45f,
                paint,
            )
        }
    }

    private fun drawLogo(
        canvas: Canvas,
        qrPixelWidth: Int,
        logo: Bitmap,
        style: QrStyle,
    ): Boolean {
        val logoSize = (qrPixelWidth * style.logoScale)
            .roundToInt()
            .coerceAtMost((qrPixelWidth * QrStyle.MAX_LOGO_SCALE).roundToInt())
            .coerceAtLeast(8)
        val plateSize = (logoSize * 1.18f).roundToInt()
        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f
        val plate = RectF(
            centerX - plateSize / 2f,
            centerY - plateSize / 2f,
            centerX + plateSize / 2f,
            centerY + plateSize / 2f,
        )
        val platePaint = Paint().apply {
            isAntiAlias = true
            color = style.background.toInt()
        }
        canvas.drawRoundRect(plate, plateSize * 0.18f, plateSize * 0.18f, platePaint)

        val scale = minOf(logoSize.toFloat() / logo.width, logoSize.toFloat() / logo.height)
        val drawnWidth = (logo.width * scale).roundToInt().coerceAtLeast(1)
        val drawnHeight = (logo.height * scale).roundToInt().coerceAtLeast(1)
        val destination = Rect(
            (centerX - drawnWidth / 2f).roundToInt(),
            (centerY - drawnHeight / 2f).roundToInt(),
            (centerX - drawnWidth / 2f).roundToInt() + drawnWidth,
            (centerY - drawnHeight / 2f).roundToInt() + drawnHeight,
        )
        val logoPaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        canvas.drawBitmap(logo, null, destination, logoPaint)
        return true
    }

    private data class Region(val left: Int, val top: Int, val size: Int) {
        fun contains(x: Int, y: Int): Boolean =
            x in left until (left + size) && y in top until (top + size)
    }

    private fun finderRegions(modules: Int): List<Region> = listOf(
        Region(0, 0, 7),
        Region(modules - 7, 0, 7),
        Region(0, modules - 7, 7),
    )
}
