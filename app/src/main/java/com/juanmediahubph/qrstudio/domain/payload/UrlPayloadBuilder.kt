package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.validation.UrlNormalizer

class UrlPayloadBuilder : PayloadBuilder {
    override val type: QrType = QrType.URL

    override fun validate(fields: Map<String, String>): Map<String, FormError> {
        val raw = fields[URL].orEmpty()
        if (raw.isBlank()) return mapOf(URL to FormError.REQUIRED)
        if (UrlNormalizer.normalize(raw) == null) return mapOf(URL to FormError.INVALID_URL)
        return emptyMap()
    }

    override fun build(fields: Map<String, String>): BuiltPayload? {
        if (validate(fields).isNotEmpty()) return null
        val normalized = UrlNormalizer.normalize(fields[URL].orEmpty()) ?: return null
        val title = UrlNormalizer.hostOf(normalized) ?: normalized
        return BuiltPayload(payload = normalized, title = title)
    }

    companion object {
        const val URL = "url"
    }
}
