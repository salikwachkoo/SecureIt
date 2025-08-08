package com.mohammadsalik.secureit.presentation.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class VaultDashboardViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val documentRepository: DocumentRepository,
    private val secureNoteRepository: SecureNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultDashboardUiState())
    val uiState: StateFlow<VaultDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardStats()
    }

    private fun loadDashboardStats() {
        viewModelScope.launch {
            try {
                val passwordCount = passwordRepository.getPasswordCount()
                val documentCount = documentRepository.getDocumentCount()
                val noteCount = secureNoteRepository.getNoteCount()

                _uiState.update {
                    it.copy(
                        passwordCount = passwordCount,
                        documentCount = documentCount,
                        noteCount = noteCount,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load dashboard stats",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refresh() {
        loadDashboardStats()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class VaultDashboardUiState(
    val passwordCount: Int = 0,
    val documentCount: Int = 0,
    val noteCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)
