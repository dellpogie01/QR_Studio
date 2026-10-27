package com.juanmediahubph.qrstudio.domain.payload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardPayloadBuilderTest {
    private val builder = VCardPayloadBuilder()

    private fun fields(vararg pairs: Pair<String, String>): Map<String, String> =
        mapOf(*pairs)

    @Test
    fun `builds a minimal vcard with CRLF endings`() {
        val result = builder.build(fields(VCardPayloadBuilder.NAME to "Ada Lovelace"))!!
        assertEquals(
            "BEGIN:VCARD\r\n" +
                "VERSION:3.0\r\n" +
                "N:Ada Lovelace;;;;\r\n" +
                "FN:Ada Lovelace\r\n" +
                "END:VCARD\r\n",
            result.payload,
        )
        assertEquals("Ada Lovelace", result.title)
    }

    @Test
    fun `includes every optional field in order and skips empty ones`() {
        val result = builder.build(
            fields(
                VCardPayloadBuilder.NAME to "Doe, John",
                VCardPayloadBuilder.ORGANIZATION to "Acme; Inc",
                VCardPayloadBuilder.JOB_TITLE to "Lead",
                VCardPayloadBuilder.PHONE to "+1 (415) 555-0123",
                VCardPayloadBuilder.EMAIL to "john@acme.com",
                VCardPayloadBuilder.URL to "acme.com",
                VCardPayloadBuilder.ADDRESS to "1 Main St\nSpringfield",
                VCardPayloadBuilder.NOTE to "Hi, there",
            ),
        )!!
        assertEquals(
            "BEGIN:VCARD\r\n" +
                "VERSION:3.0\r\n" +
                "N:Doe\\, John;;;;\r\n" +
                "FN:Doe\\, John\r\n" +
                "ORG:Acme\\; Inc\r\n" +
                "TITLE:Lead\r\n" +
                "TEL:+14155550123\r\n" +
                "EMAIL:john@acme.com\r\n" +
                "URL:https://acme.com\r\n" +
                "ADR:;;1 Main St\\nSpringfield;;;;\r\n" +
                "NOTE:Hi\\, there\r\n" +
                "END:VCARD\r\n",
            result.payload,
        )
        assertTrue(result.payload.endsWith("\r\n"))
    }

    @Test
    fun `escapes backslashes and newlines`() {
        val result = builder.build(
            fields(
                VCardPayloadBuilder.NAME to "A\\B\nC;D,E",
                VCardPayloadBuilder.NOTE to "line1\r\nline2\rline3",
            ),
        )!!
        assertTrue(result.payload.contains("N:A\\\\B\\nC\\;D\\,E;;;;\r\n"))
        assertTrue(result.payload.contains("NOTE:line1\\nline2\\nline3\r\n"))
    }

    @Test
    fun `phone alone is enough and becomes the display name`() {
        val result = builder.build(fields(VCardPayloadBuilder.PHONE to "+44 20 7946 0958"))!!
        assertTrue(result.payload.contains("FN:+442079460958\r\n"))
        assertTrue(result.payload.contains("N:+442079460958;;;;\r\n"))
        assertEquals("+442079460958", result.title)
    }

    @Test
    fun `requires a name or a phone`() {
        assertEquals(
            FormError.NAME_OR_PHONE_REQUIRED,
            builder.validate(emptyMap())[VCardPayloadBuilder.NAME],
        )
        assertNull(builder.build(fields(VCardPayloadBuilder.NOTE to "orphan")))
    }

    @Test
    fun `validates email phone and website when present`() {
        val errors = builder.validate(
            fields(
                VCardPayloadBuilder.NAME to "A",
                VCardPayloadBuilder.EMAIL to "nope",
                VCardPayloadBuilder.PHONE to "12",
                VCardPayloadBuilder.URL to "ftp://x.example",
            ),
        )
        assertEquals(FormError.INVALID_EMAIL, errors[VCardPayloadBuilder.EMAIL])
        assertEquals(FormError.INVALID_PHONE, errors[VCardPayloadBuilder.PHONE])
        assertEquals(FormError.INVALID_URL, errors[VCardPayloadBuilder.URL])
        assertFalse(errors.containsKey(VCardPayloadBuilder.NAME))
    }

    @Test
    fun `blank optional fields are skipped`() {
        val result = builder.build(
            fields(
                VCardPayloadBuilder.NAME to "A",
                VCardPayloadBuilder.EMAIL to "  ",
                VCardPayloadBuilder.NOTE to "",
            ),
        )!!
        assertFalse(result.payload.contains("EMAIL:"))
        assertFalse(result.payload.contains("NOTE:"))
    }
}
