package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.validation.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlPayloadBuilderTest {
    private val builder = UrlPayloadBuilder()

    private fun fields(url: String) = mapOf(UrlPayloadBuilder.URL to url)

    @Test
    fun `adds https when the scheme is missing`() {
        assertEquals("https://example.com", builder.build(fields("example.com"))!!.payload)
        assertEquals("https://example.com/path?q=1", builder.build(fields("example.com/path?q=1"))!!.payload)
    }

    @Test
    fun `keeps an existing http or https scheme`() {
        assertEquals("http://example.com", builder.build(fields("http://example.com"))!!.payload)
        assertEquals("HTTPS://Example.com/A", builder.build(fields("HTTPS://Example.com/A"))!!.payload)
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals("https://example.com", builder.build(fields("  example.com  "))!!.payload)
    }

    @Test
    fun `rejects blank values`() {
        assertEquals(FormError.REQUIRED, builder.validate(fields(" "))[UrlPayloadBuilder.URL])
        assertNull(builder.build(fields("")))
    }

    @Test
    fun `rejects non web schemes and malformed hosts`() {
        listOf(
            "ftp://example.com",
            "javascript:alert(1)",
            "not a url",
            "https://",
            "https://exa mple.com",
            "https://example.com:99999",
            "hello",
            "https://-bad.example.com",
            "https://example..com",
        ).forEach { raw ->
            assertEquals("Expected rejection of '$raw'", FormError.INVALID_URL, builder.validate(fields(raw))[UrlPayloadBuilder.URL])
        }
    }

    @Test
    fun `accepts ipv4 and localhost hosts`() {
        assertEquals("https://192.168.1.4", builder.build(fields("192.168.1.4"))!!.payload)
        assertEquals("https://localhost:8080", builder.build(fields("localhost:8080"))!!.payload)
    }

    @Test
    fun `title uses the host without www`() {
        assertEquals("example.com", builder.build(fields("www.example.com"))!!.title)
    }

    @Test
    fun `normalizer handles uppercase schemes and hosts`() {
        assertEquals("https://Sub.Example.COM/x", UrlNormalizer.normalize("Sub.Example.COM/x"))
        assertEquals("example.com", UrlNormalizer.hostOf("https://www.example.com/a"))
        assertNull(UrlNormalizer.normalize("   "))
    }
}
