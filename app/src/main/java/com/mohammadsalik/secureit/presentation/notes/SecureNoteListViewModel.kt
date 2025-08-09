package com.mohammadsalik.secureit.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.domain.model.SecureNote
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
class SecureNoteListViewModel @Inject constructor(
    private val secureNoteRepository: SecureNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecureNoteListUiState())
    val uiState: StateFlow<SecureNoteListUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
        loadCategories()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val notes = secureNoteRepository.getAllNotes().first()
                _uiState.update { 
                    it.copy(
                        notes = notes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to load notes",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = secureNoteRepository.getAllCategories().first()
                _uiState.update { it.copy(categories = categories) }
            } catch (e: Exception) { }
        }
    }

    fun searchNotes(query: String) {
        viewModelScope.launch {
            try {
                val notes = if (query.isBlank()) {
                    secureNoteRepository.getAllNotes().first()
                } else {
                    secureNoteRepository.searchNotes(query).first()
                }
                _uiState.update { it.copy(notes = notes) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to search notes") }
            }
        }
    }

    fun filterByCategory(category: String?) {
        viewModelScope.launch {
            try {
                val notes = if (category == null) {
                    secureNoteRepository.getAllNotes().first()
                } else {
                    secureNoteRepository.getNotesByCategory(category).first()
                }
                _uiState.update { it.copy(notes = notes, selectedCategory = category) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to filter notes") }
            }
        }
    }

    fun deleteNote(note: SecureNote) {
        viewModelScope.launch {
            try {
                secureNoteRepository.deleteNote(note)
                val remaining = _uiState.value.notes.filter { it.id != note.id }
                _uiState.update { it.copy(notes = remaining) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete note") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun refresh() { loadNotes(); loadCategories() }
}

data class SecureNoteListUiState(
    val notes: List<SecureNote> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
