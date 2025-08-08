package com.mohammadsalik.secureit.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val secureNoteRepository: SecureNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    fun searchAll(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val results = mutableListOf<SearchResult>()

                // Search passwords
                val passwords = passwordRepository.searchPasswords(query).first()
                results.addAll(passwords.map { SearchResult.Password(it) })

                // Search documents
                val documents = documentRepository.searchDocuments(query).first()
                results.addAll(documents.map { SearchResult.Document(it) })

                // Search notes
                val notes = secureNoteRepository.searchNotes(query).first()
                results.addAll(notes.map { SearchResult.Note(it) })

                // Sort results by relevance (title matches first, then content)
                val sortedResults = results.sortedWith(compareBy<SearchResult> { result ->
                    val title = when (result) {
                        is SearchResult.Password -> result.password.title
                        is SearchResult.Document -> result.document.title
                        is SearchResult.Note -> result.note.title
                    }
                    !title.contains(query, ignoreCase = true)
                }.thenBy { result ->
                    val content = when (result) {
                        is SearchResult.Password -> result.password.username + " " + result.password.website
                        is SearchResult.Document -> result.document.fileName
                        is SearchResult.Note -> result.note.content
                    }
                    !content.contains(query, ignoreCase = true)
                })

                _uiState.update {
                    it.copy(
                        searchResults = sortedResults,
                        isLoading = false
                    )
                }
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class GlobalSearchUiState(
    val searchResults: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
