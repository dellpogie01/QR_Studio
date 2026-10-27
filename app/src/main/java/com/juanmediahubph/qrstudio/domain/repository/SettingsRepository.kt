package com.juanmediahubph.qrstudio.domain.repository

import com.juanmediahubph.qrstudio.domain.model.AppSettings
import com.juanmediahubph.qrstudio.domain.model.EcLevel
import com.juanmediahubph.qrstudio.domain.model.QrSize
import com.juanmediahubph.qrstudio.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setTheme(theme: ThemeMode)

    suspend fun setDefaultEcLevel(level: EcLevel)

    suspend fun setDefaultForeground(argb: Long)

    suspend fun setDefaultBackground(argb: Long)

    suspend fun setDefaultSize(size: QrSize)

    suspend fun setSaveHistory(enabled: Boolean)
}
