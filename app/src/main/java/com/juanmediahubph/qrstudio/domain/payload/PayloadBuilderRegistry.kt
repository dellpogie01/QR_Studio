package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType

object PayloadBuilderRegistry {
    private val builders: Map<QrType, PayloadBuilder> = listOf(
        WifiPayloadBuilder(),
        VCardPayloadBuilder(),
        UrlPayloadBuilder(),
        TextPayloadBuilder(),
        EmailPayloadBuilder(),
        PhonePayloadBuilder(),
        SmsPayloadBuilder(),
        LocationPayloadBuilder(),
    ).associateBy { it.type }

    fun get(type: QrType): PayloadBuilder = requireNotNull(builders[type]) { "No builder for $type" }

    fun all(): List<PayloadBuilder> = builders.values.toList()
}
