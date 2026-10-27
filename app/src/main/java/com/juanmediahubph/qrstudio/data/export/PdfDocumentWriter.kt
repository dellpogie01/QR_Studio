package com.juanmediahubph.qrstudio.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.withTranslation
import com.juanmediahubph.qrstudio.core.theme.BrandColors
import com.juanmediahubph.qrstudio.domain.model.PdfLayout
import java.io.OutputStream

internal object PdfDocumentWriter {

    private data class PageSpec(
        val width: Int,
        val height: Int,
        val qrSize: Int,
        val qrTop: Float,
        val titleTop: Float,
        val typeTop: Float,
        val titleSize: Float,
        val typeSize: Float,
        val footerTop: Float,
    )

    private fun spec(layout: PdfLayout): PageSpec = when (layout) {
        PdfLayout.A4 -> PageSpec(
            width = 595,
            height = 842,
            qrSize = 400,
            qrTop = 120f,
            titleTop = 570f,
            typeTop = 602f,
            titleSize = 22f,
            typeSize = 13f,
            footerTop = 800f,
        )

        PdfLayout.STICKER -> PageSpec(
            width = 288,
            height = 360,
            qrSize = 196,
            qrTop = 40f,
            titleTop = 264f,
            typeTop = 286f,
            titleSize = 14f,
            typeSize = 9f,
            footerTop = 338f,
        )
    }

    fun write(
        output: OutputStream,
        qrBitmap: Bitmap,
        title: String,
        typeLabel: String,
        appName: String,
        layout: PdfLayout,
    ) {
        val spec = spec(layout)
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(spec.width, spec.height, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(BrandColors.Paper.toInt())

            val qrRect = RectF(
                (spec.width - spec.qrSize) / 2f,
                spec.qrTop,
                (spec.width + spec.qrSize) / 2f,
                spec.qrTop + spec.qrSize,
            )
            canvas.drawBitmap(
                qrBitmap,
                null,
                qrRect,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BrandColors.Ink.toInt()
                textSize = spec.titleSize
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titleMaxWidth = (spec.width - 56f).toInt()
            val titleLayout = StaticLayout.Builder
                .obtain(title.ifBlank { typeLabel }, 0, title.ifBlank { typeLabel }.length, titlePaint, titleMaxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(false)
                .build()
            canvas.withTranslation((spec.width - titleMaxWidth) / 2f, spec.titleTop) {
                titleLayout.draw(this)
            }

            val typePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BrandColors.Accent.toInt()
                textSize = spec.typeSize
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(typeLabel.uppercase(), spec.width / 2f, spec.typeTop, typePaint)

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BrandColors.Muted.toInt()
                textSize = if (layout == PdfLayout.A4) 11f else 8f
                textAlign = Paint.Align.CENTER
            }
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BrandColors.Accent.toInt()
                strokeWidth = if (layout == PdfLayout.A4) 2f else 1.5f
                strokeCap = Paint.Cap.ROUND
            }
            val ruleY = spec.footerTop - (if (layout == PdfLayout.A4) 20f else 13f)
            canvas.drawLine(
                spec.width / 2f - (if (layout == PdfLayout.A4) 18f else 12f),
                ruleY,
                spec.width / 2f + (if (layout == PdfLayout.A4) 18f else 12f),
                ruleY,
                rulePaint,
            )
            canvas.drawText(appName, spec.width / 2f, spec.footerTop, footerPaint)

            document.finishPage(page)
            write(document, output)
        } finally {
            document.close()
        }
    }

    private fun write(document: PdfDocument, output: OutputStream) {
        document.writeTo(output)
    }
}
