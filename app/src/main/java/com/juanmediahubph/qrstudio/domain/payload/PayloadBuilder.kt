package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType

data class BuiltPayload(
    val payload: String,
    val title: String,
)

interface PayloadBuilder {
    val type: QrType

    fun defaultFields(): Map<String, String> = emptyMap()

    /** Field key to error. An empty map means the form is valid. */
    fun validate(fields: Map<String, String>): Map<String, FormError>

    /** Returns null when the form is invalid. */
    fun build(fields: Map<String, String>): BuiltPayload?
}
