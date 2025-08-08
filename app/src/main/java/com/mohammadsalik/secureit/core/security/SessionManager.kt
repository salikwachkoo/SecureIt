package com.mohammadsalik.secureit.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val DEFAULT_SESSION_TIMEOUT = 5 * 60 * 1000L // 5 minutes
    }

    private val isAuthenticatedKey = booleanPreferencesKey("is_authenticated")
    private val lastActivityKey = longPreferencesKey("last_activity")
    private val sessionTimeoutKey = longPreferencesKey("session_timeout")
    private val autoLockEnabledKey = booleanPreferencesKey("auto_lock_enabled")

    /**
     * Starts a new session
     */
    suspend fun startSession() {
        context.sessionDataStore.edit { preferences ->
            preferences[isAuthenticatedKey] = true
            preferences[lastActivityKey] = System.currentTimeMillis()
        }
    }

    /**
     * Ends the current session
     */
    suspend fun endSession() {
        context.sessionDataStore.edit { preferences ->
            preferences[isAuthenticatedKey] = false
            preferences[lastActivityKey] = 0L
        }
    }

    /**
     * Updates the last activity timestamp
     */
    suspend fun updateLastActivity() {
        context.sessionDataStore.edit { preferences ->
            preferences[lastActivityKey] = System.currentTimeMillis()
        }
    }

    /**
     * Checks if the user is currently authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        return context.sessionDataStore.data.first()[isAuthenticatedKey] ?: false
    }

    /**
     * Checks if the session has expired
     */
    suspend fun isSessionExpired(): Boolean {
        val isAuthenticated = context.sessionDataStore.data.first()[isAuthenticatedKey] ?: false
        if (!isAuthenticated) return true

        val lastActivity = context.sessionDataStore.data.first()[lastActivityKey] ?: 0L
        val sessionTimeout = context.sessionDataStore.data.first()[sessionTimeoutKey] ?: DEFAULT_SESSION_TIMEOUT

        val timeSinceLastActivity = System.currentTimeMillis() - lastActivity
        return timeSinceLastActivity > sessionTimeout
    }

    /**
     * Gets the remaining session time in milliseconds
     */
    suspend fun getRemainingSessionTime(): Long {
        val lastActivity = context.sessionDataStore.data.first()[lastActivityKey] ?: 0L
        val sessionTimeout = context.sessionDataStore.data.first()[sessionTimeoutKey] ?: DEFAULT_SESSION_TIMEOUT

        val timeSinceLastActivity = System.currentTimeMillis() - lastActivity
        val remainingTime = sessionTimeout - timeSinceLastActivity

        return if (remainingTime > 0) remainingTime else 0L
    }

    /**
     * Sets the session timeout duration
     */
    suspend fun setSessionTimeout(timeoutMs: Long) {
        context.sessionDataStore.edit { preferences ->
            preferences[sessionTimeoutKey] = timeoutMs
        }
    }

    /**
     * Gets the current session timeout duration
     */
    suspend fun getSessionTimeout(): Long {
        return context.sessionDataStore.data.first()[sessionTimeoutKey] ?: DEFAULT_SESSION_TIMEOUT
    }

    /**
     * Enables or disables auto-lock
     */
    suspend fun setAutoLockEnabled(enabled: Boolean) {
        context.sessionDataStore.edit { preferences ->
            preferences[autoLockEnabledKey] = enabled
        }
    }

    /**
     * Checks if auto-lock is enabled
     */
    suspend fun isAutoLockEnabled(): Boolean {
        return context.sessionDataStore.data.first()[autoLockEnabledKey] ?: true
    }

    /**
     * Gets the current session state as a Flow
     */
    fun getSessionState(): Flow<SessionState> {
        return context.sessionDataStore.data.map { preferences ->
            val isAuthenticated = preferences[isAuthenticatedKey] ?: false
            val lastActivity = preferences[lastActivityKey] ?: 0L
            val sessionTimeout = preferences[sessionTimeoutKey] ?: DEFAULT_SESSION_TIMEOUT
            val autoLockEnabled = preferences[autoLockEnabledKey] ?: true

            val timeSinceLastActivity = System.currentTimeMillis() - lastActivity
            val remainingTime = sessionTimeout - timeSinceLastActivity
            val isExpired = remainingTime <= 0

            SessionState(
                isAuthenticated = isAuthenticated,
                remainingTime = if (remainingTime > 0) remainingTime else 0L,
                isExpired = isExpired,
                autoLockEnabled = autoLockEnabled
            )
        }
    }

    suspend fun resetSession() {
        context.sessionDataStore.edit { preferences ->
            preferences[isAuthenticatedKey] = false
            preferences[lastActivityKey] = 0L
            preferences[sessionTimeoutKey] = DEFAULT_SESSION_TIMEOUT
            preferences[autoLockEnabledKey] = true
        }
    }
}

data class SessionState(
    val isAuthenticated: Boolean,
    val remainingTime: Long,
    val isExpired: Boolean,
    val autoLockEnabled: Boolean
)