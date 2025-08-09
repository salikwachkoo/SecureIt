package com.mohammadsalik.secureit.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

enum class ThemeMode { System, Light, Dark }

@Singleton
class UserSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyThemeMode = intPreferencesKey("theme_mode")
    private val keyDynamicColor = booleanPreferencesKey("dynamic_color")
    private val keyTextScale = floatPreferencesKey("text_scale")
    private val keyReduceMotion = booleanPreferencesKey("reduce_motion")

    data class Settings(
        val themeMode: ThemeMode = ThemeMode.System,
        val dynamicColor: Boolean = true,
        val textScale: Float = 1.0f,
        val reduceMotion: Boolean = false
    )

    fun settings(): Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            themeMode = ThemeMode.values().getOrElse(prefs[keyThemeMode] ?: 0) { ThemeMode.System },
            dynamicColor = prefs[keyDynamicColor] ?: true,
            textScale = (prefs[keyTextScale] ?: 1.0f).coerceIn(0.85f, 1.5f),
            reduceMotion = prefs[keyReduceMotion] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[keyThemeMode] = mode.ordinal }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyDynamicColor] = enabled }
    }

    suspend fun setTextScale(scale: Float) {
        context.settingsDataStore.edit { it[keyTextScale] = scale.coerceIn(0.85f, 1.5f) }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.settingsDataStore.edit { it[keyReduceMotion] = enabled }
    }
}
