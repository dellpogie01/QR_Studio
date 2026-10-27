package com.juanmediahubph.qrstudio.data.export

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.juanmediahubph.qrstudio.core.qr.QrPainter
import com.juanmediahubph.qrstudio.core.ui.labelRes
import com.juanmediahubph.qrstudio.di.IoDispatcher
import com.juanmediahubph.qrstudio.domain.export.QrExporter
import com.juanmediahubph.qrstudio.domain.model.PdfLayout
import com.juanmediahubph.qrstudio.domain.model.QrSize
import com.juanmediahubph.qrstudio.domain.model.QrStyle
import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.qr.EncodedQr
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class QrExporterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QrExporter {

    override suspend fun savePng(
        encoded: EncodedQr,
        style: QrStyle,
        title: String,
    ): Result<Uri> = withContext(ioDispatcher) {
        runCatching {
            val bitmap = renderOrThrow(encoded, style, exportSize(style))
            try {
                MediaStoreWriter.savePng(context, bitmap)
            } finally {
                bitmap.recycle()
            }
        }.recoverCatching { error ->
            throw ExportException("savePng failed", error)
        }
    }

    override suspend fun prepareShareImage(
        encoded: EncodedQr,
        style: QrStyle,
        title: String,
    ): Result<Uri> = withContext(ioDispatcher) {
        runCatching {
            val bitmap = renderOrThrow(encoded, style, exportSize(style))
            val directory = File(context.cacheDir, SHARE_DIRECTORY).apply { mkdirs() }
            pruneShareCache(directory)
            val file = File(directory, "QR_Studio_${MediaStoreWriter.timestamp()}.png")
            try {
                FileOutputStream(file).use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        "Could not encode PNG"
                    }
                }
            } finally {
                bitmap.recycle()
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }.recoverCatching { error ->
            throw ExportException("prepareShareImage failed", error)
        }
    }

    override suspend fun savePdf(
        encoded: EncodedQr,
        style: QrStyle,
        title: String,
        type: QrType,
        layout: PdfLayout,
    ): Result<Uri> = withContext(ioDispatcher) {
        runCatching {
            val bitmap = renderOrThrow(encoded, style, PDF_QR_SIZE)
            try {
                val typeLabel = context.getString(type.labelRes())
                val appName = context.getString(com.juanmediahubph.qrstudio.R.string.app_name)
                MediaStoreWriter.savePdf(context) { output ->
                    PdfDocumentWriter.write(
                        output = output,
                        qrBitmap = bitmap,
                        title = title,
                        typeLabel = typeLabel,
                        appName = appName,
                        layout = layout,
                    )
                }
            } finally {
                bitmap.recycle()
            }
        }.recoverCatching { error ->
            throw ExportException("savePdf failed", error)
        }
    }

    private fun exportSize(style: QrStyle): Int =
        style.size.px.coerceAtLeast(QrSize.minimumExportSize)

    private fun renderOrThrow(encoded: EncodedQr, style: QrStyle, targetSizePx: Int): Bitmap {
        val result = QrPainter.render(encoded, style, targetSizePx = targetSizePx)
        return result.bitmap ?: throw ExportException(
            if (result.failed) "Rendering failed (out of memory?)" else "Rendering returned no bitmap",
        )
    }

    private fun pruneShareCache(directory: File) {
        val cutoff = System.currentTimeMillis() - SHARE_CACHE_MAX_AGE_MS
        val files = directory.listFiles { file -> file.isFile }?.sortedByDescending { it.lastModified() }
            ?: return
        files.forEachIndexed { index, file ->
            if (index >= SHARE_CACHE_MAX_FILES || file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    private class ExportException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    private companion object {
        const val SHARE_DIRECTORY = "exports"
        const val PDF_QR_SIZE = 1448
        const val SHARE_CACHE_MAX_FILES = 10
        const val SHARE_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L
    }
}
