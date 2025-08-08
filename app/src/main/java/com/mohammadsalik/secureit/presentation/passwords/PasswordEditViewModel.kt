package com.mohammadsalik.secureit.presentation.passwords

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.domain.model.Password
import com.mohammadsalik.secureit.domain.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject

@HiltViewModel
class PasswordEditViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordEditUiState())
    val uiState: StateFlow<PasswordEditUiState> = _uiState.asStateFlow()

    private val secureRandom = SecureRandom()

    fun loadPassword(passwordId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val password = passwordRepository.getPasswordById(passwordId)
                _uiState.update { 
                    it.copy(
                        password = password,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to load password",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun savePassword(
        title: String,
        username: String,
        password: String,
        website: String,
        notes: String,
        category: String
    ) {
        if (title.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.update { 
                it.copy(error = "Title, username, and password are required")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val passwordToSave = if (uiState.value.password != null) {
                    // Update existing password
                    uiState.value.password!!.copy(
                        title = title,
                        username = username,
                        password = password,
                        website = website,
                        notes = notes,
                        category = category
                    )
                } else {
                    // Create new password
                    Password.create(
                        title = title,
                        username = username,
                        password = password,
                        website = website,
                        notes = notes,
                        category = category
                    )
                }

                passwordRepository.insertPassword(passwordToSave)
                _uiState.update { 
                    it.copy(
                        isSaved = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to save password",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun generatePassword(): String {
        val length = 16
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Password", text)
        clipboard.setPrimaryClip(clip)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }
}

data class PasswordEditUiState(
    val password: Password? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)