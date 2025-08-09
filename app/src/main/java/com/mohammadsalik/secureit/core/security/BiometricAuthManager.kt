package com.mohammadsalik.secureit.core.security

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
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
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS ||
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun canAuthenticateStatus(): Int {
        val strong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return if (strong == BiometricManager.BIOMETRIC_SUCCESS) strong else biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    }

    fun isEnrollmentRequired(): Boolean {
        val strong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val weak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return strong == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED || weak == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
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
            val allowed = when {
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> BiometricManager.Authenticators.BIOMETRIC_STRONG
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> BiometricManager.Authenticators.BIOMETRIC_WEAK
                else -> 0
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(allowed)
                .build()

            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
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
            val allowed = when {
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> BiometricManager.Authenticators.BIOMETRIC_STRONG
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> BiometricManager.Authenticators.BIOMETRIC_WEAK
                else -> 0
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(allowed)
                .build()

            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
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

    fun launchBiometricEnrollment(activity: FragmentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
            }
            activity.startActivity(enrollIntent)
        } else {
            activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
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