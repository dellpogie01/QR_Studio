package com.juanmediahubph.qrstudio.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.juanmediahubph.qrstudio.domain.model.AppSettings
import com.juanmediahubph.qrstudio.domain.model.EcLevel
import com.juanmediahubph.qrstudio.domain.model.QrSize
import com.juanmediahubph.qrstudio.domain.model.ThemeMode
import com.juanmediahubph.qrstudio.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            val defaults = AppSettings()
            AppSettings(
                theme = ThemeMode.fromStorage(preferences[Keys.theme]),
                defaultEcLevel = preferences[Keys.ecLevel]
                    ?.let { stored -> EcLevel.entries.firstOrNull { it.name == stored } }
                    ?: defaults.defaultEcLevel,
                defaultForeground = (preferences[Keys.foreground] ?: defaults.defaultForeground) or 0xFF000000L,
                defaultBackground = (preferences[Keys.background] ?: defaults.defaultBackground) or 0xFF000000L,
                defaultSize = preferences[Keys.size]
                    ?.let { stored -> QrSize.entries.firstOrNull { it.name == stored } }
                    ?: defaults.defaultSize,
                saveHistory = preferences[Keys.saveHistory] ?: defaults.saveHistory,
            )
        }
        .distinctUntilChanged()

    override suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { it[Keys.theme] = theme.name }
    }

    override suspend fun setDefaultEcLevel(level: EcLevel) {
        dataStore.edit { it[Keys.ecLevel] = level.name }
    }

    override suspend fun setDefaultForeground(argb: Long) {
        dataStore.edit { it[Keys.foreground] = argb or 0xFF000000L }
    }

    override suspend fun setDefaultBackground(argb: Long) {
        dataStore.edit { it[Keys.background] = argb or 0xFF000000L }
    }

    override suspend fun setDefaultSize(size: QrSize) {
        dataStore.edit { it[Keys.size] = size.name }
    }

    override suspend fun setSaveHistory(enabled: Boolean) {
        dataStore.edit { it[Keys.saveHistory] = enabled }
    }

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val ecLevel = stringPreferencesKey("default_ec_level")
        val foreground = longPreferencesKey("default_foreground")
        val background = longPreferencesKey("default_background")
        val size = stringPreferencesKey("default_size")
        val saveHistory = booleanPreferencesKey("save_history")
    }
}
