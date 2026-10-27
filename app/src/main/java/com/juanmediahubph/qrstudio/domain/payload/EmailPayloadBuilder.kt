package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.validation.EmailAddress
import java.net.URLEncoder

class EmailPayloadBuilder : PayloadBuilder {
    override val type: QrType = QrType.EMAIL

    override fun validate(fields: Map<String, String>): Map<String, FormError> {
        val address = fields[TO].orEmpty()
        if (address.isBlank()) return mapOf(TO to FormError.REQUIRED)
        if (!EmailAddress.isValid(address)) return mapOf(TO to FormError.INVALID_EMAIL)
        return emptyMap()
    }

    override fun build(fields: Map<String, String>): BuiltPayload? {
        if (validate(fields).isNotEmpty()) return null
        val address = EmailAddress.normalize(fields[TO].orEmpty())
        val subject = fields[SUBJECT].orEmpty()
        val body = fields[BODY].orEmpty()
        val query = buildList {
            if (subject.isNotEmpty()) add("subject=${encode(subject)}")
            if (body.isNotEmpty()) add("body=${encode(body)}")
        }
        val payload = buildString {
            append("mailto:").append(address)
            if (query.isNotEmpty()) append('?').append(query.joinToString("&"))
        }
        return BuiltPayload(payload = payload, title = address)
    }

    companion object {
        const val TO = "to"
        const val SUBJECT = "subject"
        const val BODY = "body"

        fun encode(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
