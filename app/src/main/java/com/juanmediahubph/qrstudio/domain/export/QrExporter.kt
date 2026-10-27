package com.juanmediahubph.qrstudio.domain.export

import android.net.Uri
import com.juanmediahubph.qrstudio.domain.model.PdfLayout
import com.juanmediahubph.qrstudio.domain.model.QrStyle
import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.qr.EncodedQr

interface QrExporter {
    suspend fun savePng(encoded: EncodedQr, style: QrStyle, title: String): Result<Uri>

    /** Writes a PNG to the app cache and returns a content URI that can be shared. */
    suspend fun prepareShareImage(encoded: EncodedQr, style: QrStyle, title: String): Result<Uri>

    suspend fun savePdf(
        encoded: EncodedQr,
        style: QrStyle,
        title: String,
        type: QrType,
        layout: PdfLayout,
    ): Result<Uri>
}
