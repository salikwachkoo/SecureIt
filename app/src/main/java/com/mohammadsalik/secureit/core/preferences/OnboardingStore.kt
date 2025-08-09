package com.mohammadsalik.secureit.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")

@Singleton
class OnboardingStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyCompleted = booleanPreferencesKey("onboarding_completed")

    fun isCompleted(): Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[keyCompleted] ?: false
    }

    suspend fun setCompleted(completed: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[keyCompleted] = completed
        }
    }
}
