package com.juanmediahubph.qrstudio.domain.payload

enum class FormError {
    REQUIRED,
    INVALID_URL,
    INVALID_EMAIL,
    INVALID_PHONE,
    INVALID_LATITUDE,
    INVALID_LONGITUDE,
    SSID_TOO_LONG,
    NAME_OR_PHONE_REQUIRED,
}
