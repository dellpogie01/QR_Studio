package com.juanmediahubph.qrstudio.domain.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.juanmediahubph.qrstudio.domain.model.EcLevel
import com.juanmediahubph.qrstudio.domain.payload.WifiPayloadBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrEncoderRoundTripTest {
    private val encoder = QrEncoder()

    @Test
    fun `payload round trips through zxing`() {
        val payload = "https://example.com/привет?q=✓&x=1"
        val encoded = (encoder.encode(payload, EcLevel.M) as EncodeResult.Success).qr
        assertTrue(encoded.version >= 1)
        assertEquals(payload, decode(encoded))
        assertEquals(payload.toByteArray(Charsets.UTF_8).size, encoded.byteCount)
    }

    @Test
    fun `wifi payload with escaping round trips`() {
        val payload = WifiPayloadBuilder().build(
            mapOf(
                WifiPayloadBuilder.SSID to "Café;Guest:1,2\"A\\B",
                WifiPayloadBuilder.SECURITY to "WPA",
                WifiPayloadBuilder.PASSWORD to "p;a:s,s\"word\\",
                WifiPayloadBuilder.HIDDEN to "true",
            ),
        )!!.payload
        val encoded = (encoder.encode(payload, EcLevel.H) as EncodeResult.Success).qr
        assertEquals(payload, decode(encoded))
        assertTrue(encoded.version > 1)
    }

    @Test
    fun `round trips at every error correction level`() {
        EcLevel.entries.forEach { level ->
            val encoded = (encoder.encode("QR Studio $level", level) as EncodeResult.Success).qr
            assertEquals(level, encoded.errorCorrection)
            assertEquals("QR Studio $level", decode(encoded))
        }
    }

    @Test
    fun `long payloads still decode`() {
        val payload = buildString {
            repeat(60) { append("line ").append(it).append(": the quick brown fox\n") }
        }
        val encoded = (encoder.encode(payload, EcLevel.L) as EncodeResult.Success).qr
        assertEquals(payload, decode(encoded))
        assertTrue(encoded.version > 20)
    }

    @Test
    fun `empty and oversized payloads are reported`() {
        assertEquals(EncodeResult.Empty, encoder.encode("", EcLevel.M))
        assertEquals(EncodeResult.TooLong, encoder.encode("a".repeat(2000), EcLevel.H))
        assertTrue(encoder.encode("a".repeat(3000), EcLevel.L) is EncodeResult.TooLong)
    }

    @Test
    fun `capacity helper agrees with the encoder limits`() {
        assertTrue(QrCapacity.isOverCapacity("é".repeat(700), EcLevel.H))
        assertTrue(QrCapacity.isOverCapacity("a".repeat(1274), EcLevel.H))
        assertTrue(!QrCapacity.isOverCapacity("a".repeat(1273), EcLevel.H))
        assertTrue(QrCapacity.isNearCapacity("a".repeat(1150), EcLevel.H))
        assertEquals(2, QrCapacity.byteCount("é"))
    }

    private fun decode(encoded: EncodedQr): String {
        val quiet = 4
        val scale = 4
        val padded = BitMatrix(
            (encoded.matrix.width + quiet * 2) * scale,
            (encoded.matrix.height + quiet * 2) * scale,
        )
        for (y in 0 until encoded.matrix.height) {
            for (x in 0 until encoded.matrix.width) {
                if (encoded.matrix.get(x, y)) {
                    padded.setRegion((x + quiet) * scale, (y + quiet) * scale, scale, scale)
                }
            }
        }
        val source = BitMatrixLuminanceSource(padded)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    private class BitMatrixLuminanceSource(
        private val bits: BitMatrix,
    ) : LuminanceSource(bits.width, bits.height) {
        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            val output = if (row != null && row.size >= width) row else ByteArray(width)
            for (x in 0 until width) output[x] = if (bits.get(x, y)) 0 else 0xFF.toByte()
            return output
        }

        override fun getMatrix(): ByteArray {
            val output = ByteArray(width * height)
            var index = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    output[index++] = if (bits.get(x, y)) 0 else 0xFF.toByte()
                }
            }
            return output
        }
    }
}
