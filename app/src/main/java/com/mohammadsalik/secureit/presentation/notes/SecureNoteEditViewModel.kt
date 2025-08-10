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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecureNoteEditViewModel @Inject constructor(
    private val secureNoteRepository: SecureNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecureNoteEditUiState())
    val uiState: StateFlow<SecureNoteEditUiState> = _uiState.asStateFlow()

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val note = secureNoteRepository.getNoteById(noteId)
                _uiState.update { 
                    it.copy(
                        note = note,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to load note",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun saveNote(title: String, content: String, category: String, tags: String) {
        if (title.isBlank() || content.isBlank()) {
            _uiState.update { 
                it.copy(error = "Title and content are required")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                
                val noteToSave = if (uiState.value.note != null) {
                    // Update existing note
                    uiState.value.note!!.copy(
                        title = title,
                        content = content,
                        category = category,
                        tags = tagsList
                    )
                } else {
                    // Create new note
                    SecureNote.create(
                        title = title,
                        content = content,
                        category = category,
                        tags = tagsList
                    )
                }

                val newId = secureNoteRepository.insertNote(noteToSave)
                val savedNote = noteToSave.copy(id = newId)
                _uiState.update { 
                    it.copy(
                        note = savedNote,
                        isSaved = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to save note",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun resetNoteState() {
        _uiState.update { it.copy(note = null) }
    }
}

data class SecureNoteEditUiState(
    val note: SecureNote? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)
