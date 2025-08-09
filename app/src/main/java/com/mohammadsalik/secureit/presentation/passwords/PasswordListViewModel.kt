package com.mohammadsalik.secureit.presentation.passwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.domain.model.Password
import com.mohammadsalik.secureit.domain.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordListViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordListUiState())
    val uiState: StateFlow<PasswordListUiState> = _uiState.asStateFlow()

    init {
        loadPasswords()
        loadCategories()
    }

    private fun loadPasswords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val passwords = passwordRepository.getAllPasswords().first()
                _uiState.update { 
                    it.copy(
                        passwords = passwords,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to load passwords",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = passwordRepository.getAllCategories().first()
                _uiState.update { it.copy(categories = categories) }
            } catch (e: Exception) {
                // Handle error silently for categories
            }
        }
    }

    fun searchPasswords(query: String) {
        viewModelScope.launch {
            try {
                val passwords = if (query.isBlank()) {
                    passwordRepository.getAllPasswords().first()
                } else {
                    passwordRepository.searchPasswords(query).first()
                }
                _uiState.update { it.copy(passwords = passwords) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to search passwords")
                }
            }
        }
    }

    fun filterByCategory(category: String?) {
        viewModelScope.launch {
            try {
                val passwords = if (category == null) {
                    passwordRepository.getAllPasswords().first()
                } else {
                    passwordRepository.getPasswordsByCategory(category).first()
                }
                _uiState.update { 
                    it.copy(
                        passwords = passwords,
                        selectedCategory = category
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to filter passwords")
                }
            }
        }
    }

    fun deletePassword(password: Password) {
        viewModelScope.launch {
            try {
                passwordRepository.deletePassword(password)
                val remaining = _uiState.value.passwords.filter { it.id != password.id }
                _uiState.update { it.copy(passwords = remaining) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete password") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun refresh() { loadPasswords(); loadCategories() }
}

data class PasswordListUiState(
    val passwords: List<Password> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
