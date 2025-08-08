package com.mohammadsalik.secureit.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val biometricManager = BiometricManager.from(context)

    /**
     * Checks if biometric authentication is available on the device
     */
    fun isBiometricAvailable(): Boolean {
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Gets the available biometric types on the device
     */
    fun getAvailableBiometricTypes(): List<BiometricType> {
        val types = mutableListOf<BiometricType>()

        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            // Check for specific biometric types (this is a simplified approach)
            types.add(BiometricType.FINGERPRINT)
            types.add(BiometricType.FACE)
            types.add(BiometricType.IRIS)
        }

        return types
    }

    /**
     * Initiates biometric authentication
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Please authenticate to continue",
        negativeButtonText: String = "Cancel"
    ): BiometricResult {
        return suspendCancellableCoroutine { continuation ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .build()

            val biometricPrompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(BiometricResult.Error(errorCode, errString.toString()))
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    continuation.resume(BiometricResult.Failed)
                }
            })

            biometricPrompt.authenticate(promptInfo)
        }
    }

    /**
     * Initiates biometric authentication with crypto object
     */
    suspend fun authenticateWithCrypto(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String = "Biometric Authentication",
        subtitle: String = "Please authenticate to continue",
        negativeButtonText: String = "Cancel"
    ): BiometricResult {
        return suspendCancellableCoroutine { continuation ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .build()

            val biometricPrompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(BiometricResult.Error(errorCode, errString.toString()))
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    continuation.resume(BiometricResult.Failed)
                }
            })

            biometricPrompt.authenticate(promptInfo, cryptoObject)
        }
    }

    /**
     * Creates a crypto object for biometric authentication
     */
    fun createCryptoObject(cipher: Cipher): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(cipher)
    }
}

enum class BiometricType {
    FINGERPRINT,
    FACE,
    IRIS
}

sealed class BiometricResult {
    object Success : BiometricResult()
    object Failed : BiometricResult()
    data class Error(val errorCode: Int, val errorMessage: String) : BiometricResult()
}