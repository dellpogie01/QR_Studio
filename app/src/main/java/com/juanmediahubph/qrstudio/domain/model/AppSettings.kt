package com.juanmediahubph.qrstudio.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultEcLevel: EcLevel = EcLevel.M,
    val defaultForeground: Long = 0xFF000000L,
    val defaultBackground: Long = 0xFFFFFFFFL,
    val defaultSize: QrSize = QrSize.PX512,
    val saveHistory: Boolean = true,
) {
    fun toStyle(): QrStyle = QrStyle(
        foreground = defaultForeground,
        background = defaultBackground,
        size = defaultSize,
        errorCorrection = defaultEcLevel,
    )
}
