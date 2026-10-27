package com.juanmediahubph.qrstudio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.domain.model.AppSettings
import com.juanmediahubph.qrstudio.domain.model.EcLevel
import com.juanmediahubph.qrstudio.domain.model.QrSize
import com.juanmediahubph.qrstudio.domain.model.ThemeMode
import com.juanmediahubph.qrstudio.domain.repository.LogoRepository
import com.juanmediahubph.qrstudio.domain.repository.QrRepository
import com.juanmediahubph.qrstudio.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val qrRepository: QrRepository,
    private val logoRepository: LogoRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    private val messagesChannel = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = messagesChannel.receiveAsFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(mode) }
    }

    fun setDefaultEcLevel(level: EcLevel) {
        viewModelScope.launch { settingsRepository.setDefaultEcLevel(level) }
    }

    fun setDefaultSize(size: QrSize) {
        viewModelScope.launch { settingsRepository.setDefaultSize(size) }
    }

    fun setDefaultForeground(color: Long) {
        viewModelScope.launch { settingsRepository.setDefaultForeground(color) }
    }

    fun setDefaultBackground(color: Long) {
        viewModelScope.launch { settingsRepository.setDefaultBackground(color) }
    }

    fun setSaveHistory(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSaveHistory(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            qrRepository.clearAll()
            logoRepository.clearAll()
            messagesChannel.send(R.string.snack_history_cleared)
        }
    }
}
