package com.juanmediahubph.qrstudio.domain.payload

import com.juanmediahubph.qrstudio.domain.model.QrType

class LocationPayloadBuilder : PayloadBuilder {
    override val type: QrType = QrType.LOCATION

    override fun validate(fields: Map<String, String>): Map<String, FormError> {
        val errors = mutableMapOf<String, FormError>()
        val latitude = fields[LATITUDE].orEmpty().trim()
        val longitude = fields[LONGITUDE].orEmpty().trim()
        if (latitude.isEmpty()) {
            errors[LATITUDE] = FormError.REQUIRED
        } else {
            val value = latitude.toDoubleOrNull()
            if (value == null || value.isNaN() || value < -90.0 || value > 90.0) {
                errors[LATITUDE] = FormError.INVALID_LATITUDE
            }
        }
        if (longitude.isEmpty()) {
            errors[LONGITUDE] = FormError.REQUIRED
        } else {
            val value = longitude.toDoubleOrNull()
            if (value == null || value.isNaN() || value < -180.0 || value > 180.0) {
                errors[LONGITUDE] = FormError.INVALID_LONGITUDE
            }
        }
        return errors
    }

    override fun build(fields: Map<String, String>): BuiltPayload? {
        if (validate(fields).isNotEmpty()) return null
        val latitude = fields[LATITUDE].orEmpty().trim()
        val longitude = fields[LONGITUDE].orEmpty().trim()
        return BuiltPayload(payload = "geo:$latitude,$longitude", title = "$latitude, $longitude")
    }

    companion object {
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
    }
}
