package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiPayloadBuilderTest {
    private val builder = WifiPayloadBuilder()

    private fun fields(
        ssid: String = "Home",
        security: String = "WPA",
        password: String = "secret",
        hidden: String = "false",
    ) = mapOf(
        WifiPayloadBuilder.SSID to ssid,
        WifiPayloadBuilder.SECURITY to security,
        WifiPayloadBuilder.PASSWORD to password,
        WifiPayloadBuilder.HIDDEN to hidden,
    )

    @Test
    fun `builds a standard WPA payload`() {
        val result = builder.build(fields())!!
        assertEquals("WIFI:T:WPA;S:Home;P:secret;H:false;;", result.payload)
        assertEquals("Home", result.title)
    }

    @Test
    fun `escapes structural characters in ssid and password`() {
        val result = builder.build(
            fields(
                ssid = "Café;Guest:1,2\"A\\B",
                password = "p;a:s,s\"word\\",
            ),
        )!!
        assertEquals(
            "WIFI:T:WPA;S:Café\\;Guest\\:1\\,2\\\"A\\\\B;P:p\\;a\\:s\\,s\\\"word\\\\;H:false;;",
            result.payload,
        )
    }

    @Test
    fun `omits the password for an open network`() {
        val result = builder.build(fields(security = "NONE", password = ""))!!
        assertEquals("WIFI:T:nopass;S:Home;H:false;;", result.payload)
    }

    @Test
    fun `includes hidden flag`() {
        val result = builder.build(fields(hidden = "true"))!!
        assertEquals("WIFI:T:WPA;S:Home;P:secret;H:true;;", result.payload)
    }

    @Test
    fun `requires an ssid`() {
        val errors = builder.validate(fields(ssid = "   "))
        assertEquals(FormError.REQUIRED, errors[WifiPayloadBuilder.SSID])
        assertNull(builder.build(fields(ssid = "")))
    }

    @Test
    fun `requires a password for secured networks but not for open ones`() {
        assertTrue(
            builder.validate(fields(password = "")).containsKey(WifiPayloadBuilder.PASSWORD),
        )
        assertTrue(builder.validate(fields(security = "NONE", password = "")).isEmpty())
    }

    @Test
    fun `rejects ssids longer than 32 utf8 bytes`() {
        val ascii32 = "a".repeat(32)
        assertTrue(builder.validate(fields(ssid = ascii32)).isEmpty())
        assertEquals(
            FormError.SSID_TOO_LONG,
            builder.validate(fields(ssid = "a".repeat(33)))[WifiPayloadBuilder.SSID],
        )
        assertEquals(
            FormError.SSID_TOO_LONG,
            builder.validate(fields(ssid = "é".repeat(17)))[WifiPayloadBuilder.SSID],
        )
    }

    @Test
    fun `default fields select wpa and visible network`() {
        val defaults = builder.defaultFields()
        assertEquals("WPA", defaults[WifiPayloadBuilder.SECURITY])
        assertEquals("false", defaults[WifiPayloadBuilder.HIDDEN])
        assertEquals(QrType.WIFI, builder.type)
        assertNotNull(
            builder.build(
                defaults + mapOf(
                    WifiPayloadBuilder.SSID to "x",
                    WifiPayloadBuilder.PASSWORD to "y",
                ),
            ),
        )
    }

    @Test
    fun `wep token is preserved`() {
        val result = builder.build(fields(security = "WEP"))!!
        assertTrue(result.payload.startsWith("WIFI:T:WEP;"))
    }
}
