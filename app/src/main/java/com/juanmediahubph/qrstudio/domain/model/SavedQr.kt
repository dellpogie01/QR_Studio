package com.juanmediahubph.qrstudio.domain.model

data class SavedQr(
    val id: Long = 0,
    val type: QrType,
    val title: String,
    val payloadText: String,
    val fields: Map<String, String>,
    val style: QrStyle,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
)
