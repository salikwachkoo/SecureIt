package com.mohammadsalik.secureit.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.core.preferences.SearchHistoryStore
import com.mohammadsalik.secureit.domain.model.Document
import com.mohammadsalik.secureit.domain.model.Password
import com.mohammadsalik.secureit.domain.model.SecureNote
import com.mohammadsalik.secureit.domain.repository.DocumentRepository
import com.mohammadsalik.secureit.domain.repository.PasswordRepository
import com.mohammadsalik.secureit.domain.repository.SecureNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val documentRepository: DocumentRepository,
    private val secureNoteRepository: SecureNoteRepository,
    private val searchHistoryStore: SearchHistoryStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            searchHistoryStore.recentQueries().collect { queries ->
                _uiState.update { it.copy(history = queries) }
            }
        }
    }

    fun updateFilters(filters: SearchFilters) {
        _uiState.update { it.copy(filters = filters) }
    }

    fun clearHistory() {
        viewModelScope.launch { searchHistoryStore.clearAll() }
    }

    fun searchAll(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false, query = query) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, query = query) }
            try {
                val filters = _uiState.value.filters
                val results = mutableListOf<ScoredResult>()

                if (filters.includePasswords) {
                    val passwords = passwordRepository.searchPasswords(query).first()
                    passwords.forEach { p ->
                        val score = baseScore(query, p.title, p.username + " " + p.website, p.updatedAt.toString())
                        results += ScoredResult(SearchResult.Password(p), score)
                    }
                }
                if (filters.includeDocuments) {
                    val documents = documentRepository.searchDocuments(query).first()
                    documents.forEach { d ->
                        val score = baseScore(query, d.title, d.fileName, d.updatedAt.toString())
                        results += ScoredResult(SearchResult.Document(d), score)
                    }
                }
                if (filters.includeNotes) {
                    val notes = secureNoteRepository.searchNotes(query).first()
                    notes.forEach { n ->
                        val score = baseScore(query, n.title, n.content, n.updatedAt.toString())
                        results += ScoredResult(SearchResult.Note(n), score)
                    }
                }

                // Apply favorites boost and category filter if provided
                val filtered = results.filter { scored ->
                    val cat = _uiState.value.filters.category
                    val favoritesOnly = _uiState.value.filters.favoritesOnly
                    when (val r = scored.result) {
                        is SearchResult.Password -> (
                            (cat == null || r.password.category.equals(cat, true)) && (!favoritesOnly || r.password.isFavorite)
                        )
                        is SearchResult.Document -> (
                            (cat == null || r.document.category.equals(cat, true)) && (!favoritesOnly || r.document.isFavorite)
                        )
                        is SearchResult.Note -> (
                            (cat == null || r.note.category.equals(cat, true)) && (!favoritesOnly || r.note.isFavorite)
                        )
                    }
                }.map { scored ->
                    var s = scored.score
                    when (val r = scored.result) {
                        is SearchResult.Password -> if (r.password.isFavorite) s += 10
                        is SearchResult.Document -> if (r.document.isFavorite) s += 10
                        is SearchResult.Note -> if (r.note.isFavorite) s += 10
                    }
                    scored.copy(score = s)
                }

                val sorted = filtered.sortedByDescending { it.score }.map { it.result }

                _uiState.update { it.copy(searchResults = sorted, isLoading = false) }

                // Save query
                searchHistoryStore.addQuery(query)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to search",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun baseScore(query: String, title: String, content: String, recency: String): Int {
        var score = 0
        if (title.contains(query, true)) score += 50
        if (content.contains(query, true)) score += 20
        // Basic recency boost: if recency string contains recent year
        val year = java.time.LocalDate.now().year.toString()
        if (recency.contains(year)) score += 5
        return score
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class GlobalSearchUiState(
    val query: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val history: List<String> = emptyList(),
    val filters: SearchFilters = SearchFilters()
)

data class SearchFilters(
    val includePasswords: Boolean = true,
    val includeDocuments: Boolean = true,
    val includeNotes: Boolean = true,
    val favoritesOnly: Boolean = false,
    val category: String? = null
)

data class ScoredResult(val result: SearchResult, val score: Int)
