package com.mohammadsalik.secureit.presentation.auth

import com.mohammadsalik.secureit.core.security.BiometricAuthManager
import com.mohammadsalik.secureit.core.security.BiometricType
import com.mohammadsalik.secureit.core.security.PinAuthManager
import com.mohammadsalik.secureit.core.security.PinValidationResult
import com.mohammadsalik.secureit.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: TestAuthViewModel
    private lateinit var pinAuthManager: PinAuthManager
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var sessionManager: SessionManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        pinAuthManager = mock()
        biometricAuthManager = mock()
        sessionManager = mock()
        
        viewModel = TestAuthViewModel(pinAuthManager, biometricAuthManager, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test PIN setup success`() = runTest {
        // Given
        val pin = "1234"
        whenever(pinAuthManager.setupPin(pin)).thenReturn(true)

        // When
        viewModel.setupPin(pin)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val pinSetupState = viewModel.pinSetupState.first()
        assertTrue("PIN should be set successfully", pinSetupState.isPinSet)
        assertFalse("Should not be loading", pinSetupState.isLoading)

        val authState = viewModel.authState.first()
        assertTrue("Auth state should reflect PIN is set", authState.isPinSet)
    }

    @Test
    fun `test PIN setup failure`() = runTest {
        // Given
        val pin = "1234"
        whenever(pinAuthManager.setupPin(pin)).thenReturn(false)

        // When
        viewModel.setupPin(pin)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val pinSetupState = viewModel.pinSetupState.first()
        assertFalse("PIN should not be set", pinSetupState.isPinSet)
        assertFalse("Should not be loading", pinSetupState.isLoading)
    }

    @Test
    fun `test PIN validation success`() = runTest {
        // Given
        val pin = "1234"
        whenever(pinAuthManager.validatePin(pin)).thenReturn(PinValidationResult.Success)
        whenever(sessionManager.startSession()).thenReturn(Unit)

        // When
        viewModel.validatePin(pin)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val authState = viewModel.authState.first()
        assertTrue("User should be authenticated", authState.isAuthenticated)
        assertFalse("Should not be validating", authState.isValidatingPin)
        assertNull("Should not have PIN error", authState.pinError)
        
        verify(sessionManager).startSession()
    }

    @Test
    fun `test PIN validation invalid`() = runTest {
        // Given
        val pin = "1234"
        whenever(pinAuthManager.validatePin(pin)).thenReturn(PinValidationResult.Invalid)

        // When
        viewModel.validatePin(pin)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val authState = viewModel.authState.first()
        assertFalse("User should not be authenticated", authState.isAuthenticated)
        assertFalse("Should not be validating", authState.isValidatingPin)
        assertEquals("Should have invalid PIN error", "Invalid PIN", authState.pinError)
    }

    @Test
    fun `test PIN validation locked out`() = runTest {
        // Given
        val pin = "1234"
        val remainingTime = 5000L // 5 seconds
        whenever(pinAuthManager.validatePin(pin)).thenReturn(PinValidationResult.LockedOut(remainingTime))

        // When
        viewModel.validatePin(pin)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val authState = viewModel.authState.first()
        assertFalse("User should not be authenticated", authState.isAuthenticated)
        assertFalse("Should not be validating", authState.isValidatingPin)
        assertTrue("Should have lockout error", authState.pinError?.contains("Too many failed attempts") == true)
        assertTrue("Should show remaining time", authState.pinError?.contains("5") == true)
    }

    @Test
    fun `test biometric setup enabled`() = runTest {
        // Given
        val enabled = true
        whenever(pinAuthManager.setBiometricEnabled(enabled)).thenReturn(Unit)

        // When
        viewModel.setupBiometric(enabled)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val biometricState = viewModel.biometricSetupState.first()
        assertTrue("Biometric should be enabled", biometricState.isBiometricEnabled)
        assertFalse("Should not be loading", biometricState.isLoading)

        val authState = viewModel.authState.first()
        assertTrue("Auth state should reflect biometric enabled", authState.isBiometricEnabled)
    }

    @Test
    fun `test biometric setup disabled`() = runTest {
        // Given
        val enabled = false
        whenever(pinAuthManager.setBiometricEnabled(enabled)).thenReturn(Unit)

        // When
        viewModel.setupBiometric(enabled)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val biometricState = viewModel.biometricSetupState.first()
        assertFalse("Biometric should be disabled", biometricState.isBiometricEnabled)
        assertFalse("Should not be loading", biometricState.isLoading)

        val authState = viewModel.authState.first()
        assertFalse("Auth state should reflect biometric disabled", authState.isBiometricEnabled)
    }

    @Test
    fun `test biometric availability check`() = runTest {
        // Given
        val isAvailable = true
        val availableTypes = listOf(BiometricType.FINGERPRINT, BiometricType.FACE)
        whenever(biometricAuthManager.isBiometricAvailable()).thenReturn(isAvailable)
        whenever(biometricAuthManager.getAvailableBiometricTypes()).thenReturn(availableTypes)

        // When
        viewModel.checkBiometricAvailability()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val biometricState = viewModel.biometricSetupState.first()
        assertTrue("Biometric should be available", biometricState.isBiometricAvailable)
        assertEquals("Should have correct available types", availableTypes, biometricState.availableBiometricTypes)
    }

    @Test
    fun `test biometric authentication simulation`() = runTest {
        // When
        viewModel.authenticateWithBiometric()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val biometricState = viewModel.biometricSetupState.first()
        assertFalse("Should not be testing", biometricState.isTestingBiometric)
        assertTrue("Should have successful test", biometricState.biometricTestSuccess)
    }

    @Test
    fun `test clear PIN error`() = runTest {
        // Given - Set up an error state first
        val pin = "1234"
        whenever(pinAuthManager.validatePin(pin)).thenReturn(PinValidationResult.Invalid)
        viewModel.validatePin(pin)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        var authState = viewModel.authState.first()
        assertNotNull("Should have PIN error", authState.pinError)

        // When
        viewModel.clearPinError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        authState = viewModel.authState.first()
        assertNull("PIN error should be cleared", authState.pinError)
    }

    @Test
    fun `test logout`() = runTest {
        // Given
        whenever(sessionManager.endSession()).thenReturn(Unit)

        // When
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val authState = viewModel.authState.first()
        assertFalse("User should not be authenticated after logout", authState.isAuthenticated)
        verify(sessionManager).endSession()
    }

    @Test
    fun `test reset PIN`() = runTest {
        // Given
        whenever(pinAuthManager.resetPin()).thenReturn(Unit)

        // When
        viewModel.resetPin()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val authState = viewModel.authState.first()
        assertFalse("PIN should not be set after reset", authState.isPinSet)
        assertFalse("Biometric should not be enabled after reset", authState.isBiometricEnabled)
        verify(pinAuthManager).resetPin()
    }

    @Test
    fun `test PIN validation loading state`() = runTest {
        // Given
        val pin = "1234"
        whenever(pinAuthManager.validatePin(pin)).thenReturn(PinValidationResult.Success)
        whenever(sessionManager.startSession()).thenReturn(Unit)

        // When
        viewModel.validatePin(pin)
        
        // Advance to completion
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - Check final state
        val finalAuthState = viewModel.authState.first()
        assertFalse("Should not be validating after completion", finalAuthState.isValidatingPin)
        assertTrue("User should be authenticated", finalAuthState.isAuthenticated)
    }

    @Test
    fun `test PIN setup loading state`() = runTest {
        // Given
        val pin = "1234"
        whenever(pinAuthManager.setupPin(pin)).thenReturn(true)

        // When
        viewModel.setupPin(pin)
        
        // Advance to completion
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - Check final state
        val finalPinSetupState = viewModel.pinSetupState.first()
        assertFalse("Should not be loading after completion", finalPinSetupState.isLoading)
        assertTrue("PIN should be set", finalPinSetupState.isPinSet)
    }
}
