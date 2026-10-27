package com.juanmediahubph.qrstudio.domain.model

enum class QrType {
    WIFI,
    VCARD,
    URL,
    TEXT,
    EMAIL,
    PHONE,
    SMS,
    LOCATION;

    companion object {
        fun fromNameOrText(name: String?): QrType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TEXT
    }
}
