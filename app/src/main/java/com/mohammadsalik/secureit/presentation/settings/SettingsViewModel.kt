package com.mohammadsalik.secureit.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.core.preferences.ThemeMode
import com.mohammadsalik.secureit.core.preferences.UserSettingsStore
import com.mohammadsalik.secureit.core.security.PinAuthManager
import com.mohammadsalik.secureit.core.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: UserSettingsStore,
    private val pinAuthManager: PinAuthManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.settings().collect { s ->
                _uiState.value = SettingsUiState(
                    themeMode = s.themeMode,
                    dynamicColor = s.dynamicColor,
                    textScale = s.textScale,
                    reduceMotion = s.reduceMotion,
                    fingerprintEnabled = try { pinAuthManager.isBiometricEnabled() } catch (_: Exception) { false }
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { store.setThemeMode(mode) } }
    fun setDynamicColor(enabled: Boolean) { viewModelScope.launch { store.setDynamicColor(enabled) } }
    fun setTextScale(scale: Float) { viewModelScope.launch { store.setTextScale(scale) } }
    fun setReduceMotion(enabled: Boolean) { viewModelScope.launch { store.setReduceMotion(enabled) } }

    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                pinAuthManager.setBiometricEnabled(enabled)
                _uiState.value = _uiState.value.copy(fingerprintEnabled = enabled)
            } catch (_: Exception) { /* ignore */ }
        }
    }
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val textScale: Float = 1.0f,
    val reduceMotion: Boolean = false,
    val fingerprintEnabled: Boolean = false
)
