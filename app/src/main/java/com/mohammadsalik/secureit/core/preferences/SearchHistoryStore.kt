package com.mohammadsalik.secureit.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_prefs")

@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyRecentQueries = stringPreferencesKey("recent_queries_csv")

    fun recentQueries(): Flow<List<String>> = context.searchDataStore.data.map { prefs ->
        val csv = prefs[keyRecentQueries] ?: ""
        if (csv.isBlank()) emptyList() else csv.split('\n')
    }

    suspend fun addQuery(query: String, max: Int = 10) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        context.searchDataStore.edit { prefs ->
            val csv = prefs[keyRecentQueries] ?: ""
            val list = if (csv.isBlank()) mutableListOf() else csv.split('\n').toMutableList()
            // Move to front
            list.remove(normalized)
            list.add(0, normalized)
            val trimmed = if (list.size > max) list.take(max) else list
            prefs[keyRecentQueries] = trimmed.joinToString("\n")
        }
    }

    suspend fun clearAll() {
        context.searchDataStore.edit { prefs ->
            prefs.remove(keyRecentQueries)
        }
    }
}