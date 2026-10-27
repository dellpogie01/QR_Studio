package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType

class TextPayloadBuilder : PayloadBuilder {
    override val type: QrType = QrType.TEXT

    override fun validate(fields: Map<String, String>): Map<String, FormError> {
        val text = fields[TEXT].orEmpty()
        return if (text.isBlank()) mapOf(TEXT to FormError.REQUIRED) else emptyMap()
    }

    override fun build(fields: Map<String, String>): BuiltPayload? {
        if (validate(fields).isNotEmpty()) return null
        val text = fields[TEXT].orEmpty()
        return BuiltPayload(payload = text, title = titleFor(text))
    }

    companion object {
        const val TEXT = "text"
        private const val TITLE_LENGTH = 48

        fun titleFor(text: String): String {
            val collapsed = text.trim().replace(Regex("\\s+"), " ")
            return if (collapsed.length <= TITLE_LENGTH) collapsed else collapsed.take(TITLE_LENGTH).trimEnd() + "…"
        }
    }
}
