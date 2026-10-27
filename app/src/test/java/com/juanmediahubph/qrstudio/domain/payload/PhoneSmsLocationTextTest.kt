package com.juanmediahubph.qrstudio.domain.payload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneSmsLocationTextTest {
    @Test
    fun `phone normalizes formatting`() {
        val builder = PhonePayloadBuilder()
        val result = builder.build(mapOf(PhonePayloadBuilder.NUMBER to " +1 (415) 555-0123 "))!!
        assertEquals("tel:+14155550123", result.payload)
        assertEquals("+14155550123", result.title)
        assertNull(builder.build(emptyMap()))
        assertEquals(
            FormError.INVALID_PHONE,
            builder.validate(mapOf(PhonePayloadBuilder.NUMBER to "call me"))[PhonePayloadBuilder.NUMBER],
        )
        assertEquals(
            FormError.INVALID_PHONE,
            builder.validate(mapOf(PhonePayloadBuilder.NUMBER to "12"))[PhonePayloadBuilder.NUMBER],
        )
        assertEquals(
            FormError.INVALID_PHONE,
            builder.validate(mapOf(PhonePayloadBuilder.NUMBER to "1+4155550123"))[PhonePayloadBuilder.NUMBER],
        )
    }

    @Test
    fun `sms includes the message verbatim`() {
        val builder = SmsPayloadBuilder()
        val result = builder.build(
            mapOf(SmsPayloadBuilder.NUMBER to "+14155550123", SmsPayloadBuilder.MESSAGE to "Meet at 5: email me"),
        )!!
        assertEquals("SMSTO:+14155550123:Meet at 5: email me", result.payload)
        assertEquals("+14155550123", result.title)
        assertEquals(
            "SMSTO:+14155550123:",
            builder.build(mapOf(SmsPayloadBuilder.NUMBER to "+14155550123"))!!.payload,
        )
        assertEquals(FormError.REQUIRED, builder.validate(emptyMap())[SmsPayloadBuilder.NUMBER])
    }

    @Test
    fun `location validates ranges and builds geo payload`() {
        val builder = LocationPayloadBuilder()
        assertEquals(
            "geo:37.7749,-122.4194",
            builder.build(
                mapOf(
                    LocationPayloadBuilder.LATITUDE to " 37.7749 ",
                    LocationPayloadBuilder.LONGITUDE to "-122.4194",
                ),
            )!!.payload,
        )
        assertEquals(
            FormError.INVALID_LATITUDE,
            builder.validate(
                mapOf(
                    LocationPayloadBuilder.LATITUDE to "91",
                    LocationPayloadBuilder.LONGITUDE to "0",
                ),
            )[LocationPayloadBuilder.LATITUDE],
        )
        assertEquals(
            FormError.INVALID_LONGITUDE,
            builder.validate(
                mapOf(
                    LocationPayloadBuilder.LATITUDE to "0",
                    LocationPayloadBuilder.LONGITUDE to "-180.1",
                ),
            )[LocationPayloadBuilder.LONGITUDE],
        )
        assertEquals(
            FormError.REQUIRED,
            builder.validate(mapOf(LocationPayloadBuilder.LATITUDE to "0"))[LocationPayloadBuilder.LONGITUDE],
        )
        assertNull(builder.build(mapOf(LocationPayloadBuilder.LATITUDE to "abc", LocationPayloadBuilder.LONGITUDE to "0")))
        assertEquals(
            "geo:0,-180",
            builder.build(mapOf(LocationPayloadBuilder.LATITUDE to "0", LocationPayloadBuilder.LONGITUDE to "-180"))!!.payload,
        )
    }

    @Test
    fun `text keeps raw payload and builds a compact title`() {
        val builder = TextPayloadBuilder()
        val text = "  Hello\n\nworld  "
        val result = builder.build(mapOf(TextPayloadBuilder.TEXT to text))!!
        assertEquals(text, result.payload)
        assertEquals("Hello world", result.title)
        val long = "a".repeat(80)
        val longTitle = TextPayloadBuilder.titleFor(long)
        assertEquals(49, longTitle.length)
        assertTrue(longTitle.endsWith("…"))
        assertEquals(FormError.REQUIRED, builder.validate(mapOf(TextPayloadBuilder.TEXT to " \n "))[TextPayloadBuilder.TEXT])
        assertNull(builder.build(emptyMap()))
    }
}
