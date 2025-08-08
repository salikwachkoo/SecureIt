package com.mohammadsalik.secureit.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.core.security.BiometricAuthManager
import com.mohammadsalik.secureit.core.security.PinAuthManager
import com.mohammadsalik.secureit.core.security.PinValidationResult
import com.mohammadsalik.secureit.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TestAuthViewModel(
    private val pinAuthManager: PinAuthManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _pinSetupState = MutableStateFlow(PinSetupState())
    val pinSetupState: StateFlow<PinSetupState> = _pinSetupState.asStateFlow()

    private val _biometricSetupState = MutableStateFlow(BiometricSetupState())
    val biometricSetupState: StateFlow<BiometricSetupState> = _biometricSetupState.asStateFlow()

    // Test-specific method to initialize state without calling suspend functions
    fun initializeForTest() {
        viewModelScope.launch {
            val isPinSet = pinAuthManager.isPinSet()
            val isBiometricEnabled = pinAuthManager.isBiometricEnabled()
            val isAuthenticated = sessionManager.isAuthenticated()

            _authState.value = _authState.value.copy(
                isPinSet = isPinSet,
                isBiometricEnabled = isBiometricEnabled,
                isAuthenticated = isAuthenticated
            )
        }
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            _pinSetupState.value = _pinSetupState.value.copy(isLoading = true)

            val success = pinAuthManager.setupPin(pin)

            _pinSetupState.value = _pinSetupState.value.copy(
                isLoading = false,
                isPinSet = success
            )

            if (success) {
                _authState.value = _authState.value.copy(isPinSet = true)
            }
        }
    }

    fun validatePin(pin: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isValidatingPin = true)

            val result = pinAuthManager.validatePin(pin)

            when (result) {
                is PinValidationResult.Success -> {
                    sessionManager.startSession()
                    _authState.value = _authState.value.copy(
                        isAuthenticated = true,
                        isValidatingPin = false,
                        pinError = null
                    )
                }
                is PinValidationResult.Invalid -> {
                    _authState.value = _authState.value.copy(
                        isValidatingPin = false,
                        pinError = "Invalid PIN"
                    )
                }
                is PinValidationResult.LockedOut -> {
                    _authState.value = _authState.value.copy(
                        isValidatingPin = false,
                        pinError = "Too many failed attempts. Try again in ${result.remainingTime / 1000} seconds"
                    )
                }
            }
        }
    }

    fun setupBiometric(enabled: Boolean) {
        viewModelScope.launch {
            _biometricSetupState.value = _biometricSetupState.value.copy(isLoading = true)

            pinAuthManager.setBiometricEnabled(enabled)

            _biometricSetupState.value = _biometricSetupState.value.copy(
                isLoading = false,
                isBiometricEnabled = enabled
            )

            _authState.value = _authState.value.copy(isBiometricEnabled = enabled)
        }
    }

    fun checkBiometricAvailability() {
        viewModelScope.launch {
            val isAvailable = biometricAuthManager.isBiometricAvailable()
            val availableTypes = biometricAuthManager.getAvailableBiometricTypes()

            _biometricSetupState.value = _biometricSetupState.value.copy(
                isBiometricAvailable = isAvailable,
                availableBiometricTypes = availableTypes
            )
        }
    }

    fun authenticateWithBiometric() {
        viewModelScope.launch {
            _biometricSetupState.value = _biometricSetupState.value.copy(isTestingBiometric = true)

            // This would be called from an Activity context
            // For now, we'll simulate success
            _biometricSetupState.value = _biometricSetupState.value.copy(
                isTestingBiometric = false,
                biometricTestSuccess = true
            )
        }
    }

    fun clearPinError() {
        _authState.value = _authState.value.copy(pinError = null)
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.endSession()
            _authState.value = _authState.value.copy(isAuthenticated = false)
        }
    }

    fun resetPin() {
        viewModelScope.launch {
            pinAuthManager.resetPin()
            _authState.value = _authState.value.copy(
                isPinSet = false,
                isBiometricEnabled = false
            )
        }
    }
}
