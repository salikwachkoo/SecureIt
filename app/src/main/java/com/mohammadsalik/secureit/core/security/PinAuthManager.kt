package com.mohammadsalik.secureit.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pin_auth")

@Singleton
class PinAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30 * 60 * 1000L // 30 minutes
        private const val SALT_LENGTH = 32
    }

    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val pinSaltKey = stringPreferencesKey("pin_salt")
    private val isPinSetKey = booleanPreferencesKey("is_pin_set")
    private val failedAttemptsKey = intPreferencesKey("failed_attempts")
    private val lastFailedAttemptKey = longPreferencesKey("last_failed_attempt")
    private val isBiometricEnabledKey = booleanPreferencesKey("is_biometric_enabled")

    /**
     * Checks if a PIN is set
     */
    suspend fun isPinSet(): Boolean {
        return context.dataStore.data.first()[isPinSetKey] ?: false
    }

    /**
     * Sets up a new PIN
     */
    suspend fun setupPin(pin: String): Boolean {
        return try {
            val salt = generateSalt()
            val hashedPin = hashPin(pin, salt)

            context.dataStore.edit { preferences ->
                preferences[pinHashKey] = hashedPin
                preferences[pinSaltKey] = salt
                preferences[isPinSetKey] = true
                preferences[failedAttemptsKey] = 0
                preferences[lastFailedAttemptKey] = 0L
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates a PIN
     */
    suspend fun validatePin(pin: String): PinValidationResult {
        val lockoutInfo = getLockoutInfo()

        if (lockoutInfo.isLockedOut) {
            return PinValidationResult.LockedOut(lockoutInfo.remainingTime)
        }

        val storedHash = context.dataStore.data.first()[pinHashKey]
        val storedSalt = context.dataStore.data.first()[pinSaltKey]

        if (storedHash == null || storedSalt == null) {
            return PinValidationResult.Invalid
        }

        val inputHash = hashPin(pin, storedSalt)

        if (inputHash == storedHash) {
            // Reset failed attempts on successful validation
            context.dataStore.edit { preferences ->
                preferences[failedAttemptsKey] = 0
                preferences[lastFailedAttemptKey] = 0L
            }
            return PinValidationResult.Success
        } else {
            // Increment failed attempts
            val currentAttempts = context.dataStore.data.first()[failedAttemptsKey] ?: 0
            val newAttempts = currentAttempts + 1

            context.dataStore.edit { preferences ->
                preferences[failedAttemptsKey] = newAttempts
                preferences[lastFailedAttemptKey] = System.currentTimeMillis()
            }

            return if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                PinValidationResult.LockedOut(LOCKOUT_DURATION_MS)
            } else {
                PinValidationResult.Invalid
            }
        }
    }

    /**
     * Gets lockout information
     */
    suspend fun getLockoutInfo(): LockoutInfo {
        val failedAttempts = context.dataStore.data.first()[failedAttemptsKey] ?: 0
        val lastFailedAttempt = context.dataStore.data.first()[lastFailedAttemptKey] ?: 0L

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            val timeSinceLastAttempt = System.currentTimeMillis() - lastFailedAttempt
            val remainingTime = LOCKOUT_DURATION_MS - timeSinceLastAttempt

            return if (remainingTime > 0) {
                LockoutInfo(true, remainingTime, failedAttempts)
            } else {
                // Lockout period has expired
                context.dataStore.edit { preferences ->
                    preferences[failedAttemptsKey] = 0
                    preferences[lastFailedAttemptKey] = 0L
                }
                LockoutInfo(false, 0L, 0)
            }
        }

        return LockoutInfo(false, 0L, failedAttempts)
    }

    /**
     * Checks if biometric authentication is enabled
     */
    suspend fun isBiometricEnabled(): Boolean {
        return context.dataStore.data.first()[isBiometricEnabledKey] ?: false
    }

    /**
     * Enables or disables biometric authentication
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isBiometricEnabledKey] = enabled
        }
    }

    /**
     * Changes the PIN
     */
    suspend fun changePin(oldPin: String, newPin: String): Boolean {
        val validationResult = validatePin(oldPin)

        return when (validationResult) {
            is PinValidationResult.Success -> {
                setupPin(newPin)
            }
            else -> false
        }
    }

    /**
     * Resets the PIN (for emergency situations)
     */
    suspend fun resetPin() {
        context.dataStore.edit { preferences ->
            preferences.remove(pinHashKey)
            preferences.remove(pinSaltKey)
            preferences[isPinSetKey] = false
            preferences[failedAttemptsKey] = 0
            preferences[lastFailedAttemptKey] = 0L
            preferences[isBiometricEnabledKey] = false
        }
    }

    private fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val saltedPin = pin + salt
        val hashedBytes = messageDigest.digest(saltedPin.toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
}

sealed class PinValidationResult {
    object Success : PinValidationResult()
    object Invalid : PinValidationResult()
    data class LockedOut(val remainingTime: Long) : PinValidationResult()
}

data class LockoutInfo(
    val isLockedOut: Boolean,
    val remainingTime: Long,
    val failedAttempts: Int
)