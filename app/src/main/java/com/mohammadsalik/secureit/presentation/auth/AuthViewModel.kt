package com.mohammadsalik.secureit.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.core.preferences.OnboardingStore
import com.mohammadsalik.secureit.core.security.BiometricAuthManager
import com.mohammadsalik.secureit.core.security.PinAuthManager
import com.mohammadsalik.secureit.core.security.PinValidationResult
import com.mohammadsalik.secureit.core.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val pinAuthManager: PinAuthManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val sessionManager: SessionManager,
    private val onboardingStore: OnboardingStore
) : ViewModel() {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _pinSetupState = MutableStateFlow(PinSetupState())
    val pinSetupState: StateFlow<PinSetupState> = _pinSetupState.asStateFlow()

    private val _biometricSetupState = MutableStateFlow(BiometricSetupState())
    val biometricSetupState: StateFlow<BiometricSetupState> = _biometricSetupState.asStateFlow()

    init { checkInitialAuthState() }

    private fun checkInitialAuthState() {
        viewModelScope.launch {
            onboardingStore.isCompleted().collectLatest { completed ->
                val isPinSet = pinAuthManager.isPinSet()
                val isBiometricEnabled = pinAuthManager.isBiometricEnabled()
                // Always require PIN on cold start: treat as not authenticated
                val isAuthenticated = false
                _authState.value = _authState.value.copy(
                    isPinSet = isPinSet,
                    isBiometricEnabled = isBiometricEnabled,
                    isAuthenticated = isAuthenticated,
                    isOnboardingCompleted = completed
                )
            }
        }
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            _pinSetupState.value = _pinSetupState.value.copy(isLoading = true)
            val success = pinAuthManager.setupPin(pin)
            _pinSetupState.value = _pinSetupState.value.copy(isLoading = false, isPinSet = success)
            if (success) {
                _authState.value = _authState.value.copy(isPinSet = true)
                // Do NOT mark onboarding complete here; proceed to biometric setup screen
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
                    _authState.value = _authState.value.copy(isValidatingPin = false, pinError = "Invalid PIN")
                }
                is PinValidationResult.LockedOut -> {
                    _authState.value = _authState.value.copy(isValidatingPin = false, pinError = "Too many failed attempts. Try again in ${result.remainingTime / 1000} seconds")
                }
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            sessionManager.startSession()
            _authState.value = _authState.value.copy(isAuthenticated = true, pinError = null)
        }
    }

    fun setupBiometric(enabled: Boolean) {
        viewModelScope.launch {
            _biometricSetupState.value = _biometricSetupState.value.copy(isLoading = true)
            pinAuthManager.setBiometricEnabled(enabled)
            _biometricSetupState.value = _biometricSetupState.value.copy(isLoading = false, isBiometricEnabled = enabled)
            _authState.value = _authState.value.copy(isBiometricEnabled = enabled)
            // Mark onboarding complete after biometric decision (enabled or skipped)
            onboardingStore.setCompleted(true)
            _authState.value = _authState.value.copy(isOnboardingCompleted = true)
        }
    }

    fun checkBiometricAvailability() {
        viewModelScope.launch {
            val isAvailable = biometricAuthManager.isBiometricAvailable()
            val availableTypes = biometricAuthManager.getAvailableBiometricTypes()
            _biometricSetupState.value = _biometricSetupState.value.copy(isBiometricAvailable = isAvailable, availableBiometricTypes = availableTypes)
        }
    }

    fun authenticateWithBiometric() {
        viewModelScope.launch {
            _biometricSetupState.value = _biometricSetupState.value.copy(isTestingBiometric = true)
            _biometricSetupState.value = _biometricSetupState.value.copy(isTestingBiometric = false, biometricTestSuccess = true)
        }
    }

    fun clearPinError() { _authState.value = _authState.value.copy(pinError = null) }

    fun logout() { viewModelScope.launch { sessionManager.endSession(); _authState.value = _authState.value.copy(isAuthenticated = false) } }
    fun forceLock() { viewModelScope.launch { sessionManager.endSession(); _authState.value = _authState.value.copy(isAuthenticated = false) } }

    fun resetPin() {
        viewModelScope.launch {
            pinAuthManager.resetPin()
            onboardingStore.setCompleted(false)
            _authState.value = _authState.value.copy(isPinSet = false, isBiometricEnabled = false, isOnboardingCompleted = false)
        }
    }
}

data class AuthState(
    val isPinSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isValidatingPin: Boolean = false,
    val pinError: String? = null,
    val isOnboardingCompleted: Boolean = false
)

data class PinSetupState(
    val isLoading: Boolean = false,
    val isPinSet: Boolean = false,
    val pin: String = "",
    val confirmPin: String = "",
    val showConfirmPin: Boolean = false
)

data class BiometricSetupState(
    val isLoading: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isTestingBiometric: Boolean = false,
    val biometricTestSuccess: Boolean = false,
    val availableBiometricTypes: List<com.mohammadsalik.secureit.core.security.BiometricType> = emptyList()
)
