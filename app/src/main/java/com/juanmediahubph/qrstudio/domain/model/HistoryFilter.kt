package com.juanmediahubph.qrstudio.domain.model

data class HistoryFilter(
    val favoritesOnly: Boolean = false,
    val type: QrType? = null,
) {
    companion object {
        val All = HistoryFilter()
        val Favorites = HistoryFilter(favoritesOnly = true)
        fun ofType(type: QrType) = HistoryFilter(type = type)
    }
}

enum class PdfLayout {
    A4,
    STICKER,
}
