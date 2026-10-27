package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.validation.PhoneNumber

class SmsPayloadBuilder : PayloadBuilder {
    override val type: QrType = QrType.SMS

    override fun validate(fields: Map<String, String>): Map<String, FormError> {
        val number = fields[NUMBER].orEmpty()
        if (number.isBlank()) return mapOf(NUMBER to FormError.REQUIRED)
        if (PhoneNumber.normalize(number) == null) return mapOf(NUMBER to FormError.INVALID_PHONE)
        return emptyMap()
    }

    override fun build(fields: Map<String, String>): BuiltPayload? {
        if (validate(fields).isNotEmpty()) return null
        val number = PhoneNumber.normalize(fields[NUMBER].orEmpty()) ?: return null
        val message = fields[MESSAGE].orEmpty()
        return BuiltPayload(payload = "SMSTO:$number:$message", title = number)
    }

    companion object {
        const val NUMBER = "number"
        const val MESSAGE = "message"
    }
}
