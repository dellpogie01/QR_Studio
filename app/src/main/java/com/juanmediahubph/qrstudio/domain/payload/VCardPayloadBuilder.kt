package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.validation.EmailAddress
import com.juanmediahubph.qrstudio.domain.validation.PhoneNumber
import com.juanmediahubph.qrstudio.domain.validation.UrlNormalizer

class VCardPayloadBuilder : PayloadBuilder {
    override val type: QrType = QrType.VCARD

    override fun validate(fields: Map<String, String>): Map<String, FormError> {
        val errors = mutableMapOf<String, FormError>()
        val name = fields[NAME].orEmpty().trim()
        val phone = fields[PHONE].orEmpty().trim()
        if (name.isEmpty() && phone.isEmpty()) {
            errors[NAME] = FormError.NAME_OR_PHONE_REQUIRED
        }
        if (phone.isNotEmpty() && PhoneNumber.normalize(phone) == null) {
            errors[PHONE] = FormError.INVALID_PHONE
        }
        val email = fields[EMAIL].orEmpty().trim()
        if (email.isNotEmpty() && !EmailAddress.isValid(email)) {
            errors[EMAIL] = FormError.INVALID_EMAIL
        }
        val url = fields[URL].orEmpty().trim()
        if (url.isNotEmpty() && UrlNormalizer.normalize(url) == null) {
            errors[URL] = FormError.INVALID_URL
        }
        return errors
    }

    override fun build(fields: Map<String, String>): BuiltPayload? {
        if (validate(fields).isNotEmpty()) return null
        val name = fields[NAME].orEmpty().trim()
        val phone = fields[PHONE].orEmpty().trim()
        val normalizedPhone = PhoneNumber.normalize(phone)
        val displayName = name.ifBlank { normalizedPhone ?: phone }
        val organization = fields[ORGANIZATION].orEmpty().trim()
        val jobTitle = fields[JOB_TITLE].orEmpty().trim()
        val email = fields[EMAIL].orEmpty().trim()
        val website = fields[URL].orEmpty().trim()
        val address = fields[ADDRESS].orEmpty().trim()
        val note = fields[NOTE].orEmpty().trim()

        val builder = StringBuilder()
        builder.append("BEGIN:VCARD\r\n")
        builder.append("VERSION:3.0\r\n")
        builder.append("N:").append(escape(name.ifBlank { displayName })).append(";;;;\r\n")
        builder.append("FN:").append(escape(displayName)).append("\r\n")
        if (organization.isNotEmpty()) builder.append("ORG:").append(escape(organization)).append("\r\n")
        if (jobTitle.isNotEmpty()) builder.append("TITLE:").append(escape(jobTitle)).append("\r\n")
        if (phone.isNotEmpty()) {
            builder.append("TEL:").append(escape(normalizedPhone ?: phone)).append("\r\n")
        }
        if (email.isNotEmpty()) builder.append("EMAIL:").append(escape(email)).append("\r\n")
        if (website.isNotEmpty()) {
            val normalized = UrlNormalizer.normalize(website) ?: website
            builder.append("URL:").append(escape(normalized)).append("\r\n")
        }
        if (address.isNotEmpty()) builder.append("ADR:;;").append(escape(address)).append(";;;;\r\n")
        if (note.isNotEmpty()) builder.append("NOTE:").append(escape(note)).append("\r\n")
        builder.append("END:VCARD\r\n")

        return BuiltPayload(payload = builder.toString(), title = displayName)
    }

    companion object {
        const val NAME = "name"
        const val ORGANIZATION = "organization"
        const val JOB_TITLE = "jobTitle"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val URL = "url"
        const val ADDRESS = "address"
        const val NOTE = "note"

        fun escape(value: String): String {
            val normalized = value.replace("\r\n", "\n").replace('\r', '\n')
            return buildString(normalized.length) {
                normalized.forEach { character ->
                    when (character) {
                        '\\' -> append("\\\\")
                        ';' -> append("\\;")
                        ',' -> append("\\,")
                        '\n' -> append("\\n")
                        else -> append(character)
                    }
                }
            }
        }
    }
}
