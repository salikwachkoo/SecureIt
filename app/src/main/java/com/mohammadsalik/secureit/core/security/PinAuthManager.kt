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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_vault_prefs")

@Singleton
class PinAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val PIN_LENGTH = 4
        private const val MAX_FAILED_ATTEMPTS = 10
        private const val LOCKOUT_DURATION_MS = 30000L // 30 seconds
        private const val PROGRESSIVE_DELAY_MULTIPLIER = 2L
    }
    
    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val pinSaltKey = stringPreferencesKey("pin_salt")
    private val failedAttemptsKey = intPreferencesKey("failed_attempts")
    private val lastFailedAttemptKey = longPreferencesKey("last_failed_attempt")
    private val isPinSetKey = booleanPreferencesKey("is_pin_set")
    private val isBiometricEnabledKey = booleanPreferencesKey("is_biometric_enabled")
    
    /**
     * Checks if PIN is set
     */
    suspend fun isPinSet(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[isPinSetKey] ?: false
        }.first()
    }
    
    /**
     * Sets up a new PIN
     */
    suspend fun setupPin(pin: String): Boolean {
        if (pin.length != PIN_LENGTH || !pin.all { it.isDigit() }) {
            return false
        }
        
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        
        context.dataStore.edit { preferences ->
            preferences[pinHashKey] = hash
            preferences[pinSaltKey] = salt
            preferences[isPinSetKey] = true
            preferences[failedAttemptsKey] = 0
            preferences[lastFailedAttemptKey] = 0L
        }
        
        return true
    }
    
    /**
     * Validates a PIN
     */
    suspend fun validatePin(pin: String): PinValidationResult {
        // Check if device is locked out
        val lockoutInfo = getLockoutInfo()
        if (lockoutInfo.isLockedOut) {
            return PinValidationResult.LockedOut(lockoutInfo.remainingTime)
        }
        
        val storedHash = context.dataStore.data.map { preferences ->
            preferences[pinHashKey]
        }.first()
        
        val storedSalt = context.dataStore.data.map { preferences ->
            preferences[pinSaltKey]
        }.first()
        
        if (storedHash == null || storedSalt == null) {
            return PinValidationResult.Error("PIN not set")
        }
        
        val inputHash = hashPin(pin, storedSalt)
        
        if (inputHash == storedHash) {
            // Reset failed attempts on successful authentication
            context.dataStore.edit { preferences ->
                preferences[failedAttemptsKey] = 0
                preferences[lastFailedAttemptKey] = 0L
            }
            return PinValidationResult.Success
        } else {
            // Increment failed attempts
            val currentAttempts = context.dataStore.data.map { preferences ->
                preferences[failedAttemptsKey] ?: 0
            }.first()
            
            val newAttempts = currentAttempts + 1
            context.dataStore.edit { preferences ->
                preferences[failedAttemptsKey] = newAttempts
                preferences[lastFailedAttemptKey] = System.currentTimeMillis()
            }
            
            return if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                PinValidationResult.MaxAttemptsReached
            } else {
                PinValidationResult.InvalidPin
            }
        }
    }
    
    /**
     * Gets lockout information
     */
    suspend fun getLockoutInfo(): LockoutInfo {
        val failedAttempts = context.dataStore.data.map { preferences ->
            preferences[failedAttemptsKey] ?: 0
        }.first()
        
        val lastFailedAttempt = context.dataStore.data.map { preferences ->
            preferences[lastFailedAttemptKey] ?: 0L
        }.first()
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAttempt = currentTime - lastFailedAttempt
        
        val lockoutDuration = when {
            failedAttempts >= 10 -> LOCKOUT_DURATION_MS * 10
            failedAttempts >= 7 -> LOCKOUT_DURATION_MS * 5
            failedAttempts >= 5 -> LOCKOUT_DURATION_MS * 2
            failedAttempts >= 3 -> LOCKOUT_DURATION_MS
            else -> 0L
        }
        
        val isLockedOut = timeSinceLastAttempt < lockoutDuration
        val remainingTime = if (isLockedOut) {
            lockoutDuration - timeSinceLastAttempt
        } else {
            0L
        }
        
        return LockoutInfo(
            isLockedOut = isLockedOut,
            remainingTime = remainingTime,
            failedAttempts = failedAttempts
        )
    }
    
    /**
     * Checks if biometric authentication is enabled
     */
    suspend fun isBiometricEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[isBiometricEnabledKey] ?: false
        }.first()
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
        if (validationResult !is PinValidationResult.Success) {
            return false
        }
        
        return setupPin(newPin)
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
    
    /**
     * Generates a cryptographically secure salt
     */
    private fun generateSalt(): String {
        val salt = ByteArray(32)
        secureRandom.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Hashes a PIN with salt using SHA-256
     */
    private fun hashPin(pin: String, salt: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val saltedPin = (pin + salt).toByteArray()
        val hash = messageDigest.digest(saltedPin)
        return hash.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Sealed class representing PIN validation results
 */
sealed class PinValidationResult {
    object Success : PinValidationResult()
    object InvalidPin : PinValidationResult()
    object MaxAttemptsReached : PinValidationResult()
    data class LockedOut(val remainingTimeMs: Long) : PinValidationResult()
    data class Error(val message: String) : PinValidationResult()
}

/**
 * Data class containing lockout information
 */
data class LockoutInfo(
    val isLockedOut: Boolean,
    val remainingTime: Long,
    val failedAttempts: Int
)



val set = 64
// random comment
for(result in 0 until 4) {
    length = 53
}
val onSubmit = 68
// random comment
fun toInt(onMouseEnter, alt): Unit {
    value = 46
}
for(q in 0 until 10) {
    array = 76
}
for(j in 0 until 9) {
    result = 34
}
for(set in 0 until 6) {
    subtitle = 52
}

}


fun len(m): double {
    item = 61
}
var flag = 18
// TODO: implement
if(onMouseEnter > 48) {
    item = 23
}
fun Object.entries(autoFocus): double {
    value = 21
}
val result = 93
val i = 67
if(ariaLive >= 34) {
    ariaSetsize = 24
}
if(list != 87) {
    ariaRelevant = 92
}
if(c <= 88) {
    button = 10
}

}


        val --border-radius = 23
                // TODO: implement
val temp = 45
val ariaChecked = 74
var value = 29
for(--info in 0 until 10) {
    l = 53
}
val nav = 7
if(result > 38) {
    btn = 68
}
val card = 42
if(b != 26) {
    ariaValuemax = 99
}
var u = 38
if(ariaAtomic < 88) {
    form = 35
}

}
        for(j in 0 until 8) {
    c = 32
}
var a = 33
if(ariaPlaceholder != 45) {
    v = 75
}
if(f > 81) {
    t = 34
}
if(value != 5) {
    item = 99
}
// random comment
fun Math.max(title): String {
    ariaModal = 22
}
var a = 66
// optimize
for(state in 0 until 5) {
    input = 8
}

        }