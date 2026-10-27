package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType

class WifiPayloadBuilder : PayloadBuilder {
    override val type: QrType = QrType.WIFI

    override fun defaultFields(): Map<String, String> = mapOf(
        SECURITY to Security.WPA.name,
        HIDDEN to "false",
    )

    override fun validate(fields: Map<String, String>): Map<String, FormError> {
        val errors = mutableMapOf<String, FormError>()
        val ssid = fields[SSID].orEmpty()
        if (ssid.isBlank()) {
            errors[SSID] = FormError.REQUIRED
        } else if (ssid.toByteArray(Charsets.UTF_8).size > MAX_SSID_BYTES) {
            errors[SSID] = FormError.SSID_TOO_LONG
        }
        if (Security.from(fields[SECURITY]) != Security.NONE && fields[PASSWORD].isNullOrEmpty()) {
            errors[PASSWORD] = FormError.REQUIRED
        }
        return errors
    }

    override fun build(fields: Map<String, String>): BuiltPayload? {
        if (validate(fields).isNotEmpty()) return null
        val ssid = fields[SSID].orEmpty().trim()
        val security = Security.from(fields[SECURITY])
        val hidden = fields[HIDDEN].toBoolean()
        val payload = buildString {
            append("WIFI:T:").append(security.token).append(';')
            append("S:").append(escapeWifiValue(ssid)).append(';')
            if (security != Security.NONE) {
                append("P:").append(escapeWifiValue(fields[PASSWORD].orEmpty())).append(';')
            }
            append("H:").append(hidden).append(";;")
        }
        return BuiltPayload(payload = payload, title = ssid)
    }

    companion object {
        const val SSID = "ssid"
        const val SECURITY = "security"
        const val PASSWORD = "password"
        const val HIDDEN = "hidden"

        private const val MAX_SSID_BYTES = 32
        private val specialCharacters = setOf('\\', ';', ',', ':', '"')

        /** Escapes the five characters that are structural inside a WIFI payload. */
        fun escapeWifiValue(value: String): String = buildString(value.length) {
            value.forEach { character ->
                if (character in specialCharacters) append('\\')
                append(character)
            }
        }

        fun securityFrom(fields: Map<String, String>): Security =
            Security.from(fields[SECURITY])
    }

    enum class Security(val token: String) {
        WPA("WPA"),
        WEP("WEP"),
        NONE("nopass"),
        ;

        companion object {
            fun from(value: String?): Security =
                entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WPA
        }
    }
}
