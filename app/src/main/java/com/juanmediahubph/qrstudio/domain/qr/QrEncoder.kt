package com.juanmediahubph.qrstudio.domain.qr

import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.juanmediahubph.qrstudio.domain.model.EcLevel

data class EncodedQr(
    val payload: String,
    val matrix: BitMatrix,
    val version: Int,
    val errorCorrection: EcLevel,
    val byteCount: Int,
)

sealed interface EncodeResult {
    data class Success(val qr: EncodedQr) : EncodeResult
    data object Empty : EncodeResult
    data object TooLong : EncodeResult
    data object Failure : EncodeResult
}

class QrEncoder {
    fun encode(payload: String, level: EcLevel): EncodeResult {
        if (payload.isEmpty()) return EncodeResult.Empty
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to level.toZxing(),
                EncodeHintType.MARGIN to 0,
            )
            val code = Encoder.encode(payload, level.toZxing(), hints)
            val byteMatrix = code.matrix
            val matrix = BitMatrix(byteMatrix.width, byteMatrix.height)
            for (y in 0 until byteMatrix.height) {
                for (x in 0 until byteMatrix.width) {
                    if (byteMatrix.get(x, y).toInt() == 1) matrix.set(x, y)
                }
            }
            EncodeResult.Success(
                EncodedQr(
                    payload = payload,
                    matrix = matrix,
                    version = code.version.versionNumber,
                    errorCorrection = level,
                    byteCount = payload.toByteArray(Charsets.UTF_8).size,
                ),
            )
        } catch (_: WriterException) {
            EncodeResult.TooLong
        } catch (_: Exception) {
            EncodeResult.Failure
        }
    }
}

internal fun EcLevel.toZxing(): ErrorCorrectionLevel = when (this) {
    EcLevel.L -> ErrorCorrectionLevel.L
    EcLevel.M -> ErrorCorrectionLevel.M
    EcLevel.Q -> ErrorCorrectionLevel.Q
    EcLevel.H -> ErrorCorrectionLevel.H
}
