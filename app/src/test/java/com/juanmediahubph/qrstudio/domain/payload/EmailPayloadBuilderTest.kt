package com.juanmediahubph.qrstudio.domain.payload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailPayloadBuilderTest {
    private val builder = EmailPayloadBuilder()

    @Test
    fun `builds mailto with encoded subject and body`() {
        val result = builder.build(
            mapOf(
                EmailPayloadBuilder.TO to "hello@example.com",
                EmailPayloadBuilder.SUBJECT to "Hello world & more",
                EmailPayloadBuilder.BODY to "Line one\nLine two — café",
            ),
        )!!
        assertEquals(
            "mailto:hello@example.com" +
                "?subject=Hello%20world%20%26%20more" +
                "&body=Line%20one%0ALine%20two%20%E2%80%94%20caf%C3%A9",
            result.payload,
        )
        assertEquals("hello@example.com", result.title)
    }

    @Test
    fun `omits empty query parameters`() {
        assertEquals(
            "mailto:a@b.co",
            builder.build(mapOf(EmailPayloadBuilder.TO to "a@b.co", EmailPayloadBuilder.SUBJECT to ""))!!.payload,
        )
        assertEquals(
            "mailto:a@b.co?body=Hi",
            builder.build(mapOf(EmailPayloadBuilder.TO to " a@b.co ", EmailPayloadBuilder.BODY to "Hi"))!!.payload,
        )
    }

    @Test
    fun `requires a valid address`() {
        assertEquals(FormError.REQUIRED, builder.validate(emptyMap())[EmailPayloadBuilder.TO])
        assertEquals(
            FormError.INVALID_EMAIL,
            builder.validate(mapOf(EmailPayloadBuilder.TO to "not-an-email"))[EmailPayloadBuilder.TO],
        )
        assertNull(builder.build(mapOf(EmailPayloadBuilder.TO to "bad")))
    }

    @Test
    fun `supports plus addressing and subdomains`() {
        val result = builder.build(mapOf(EmailPayloadBuilder.TO to "user+tag@mail.example.co.uk"))!!
        assertEquals("mailto:user+tag@mail.example.co.uk", result.payload)
    }
}
