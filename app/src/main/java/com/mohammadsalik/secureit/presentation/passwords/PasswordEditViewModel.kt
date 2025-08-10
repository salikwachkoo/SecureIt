package com.mohammadsalik.secureit.presentation.passwords

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.core.security.PasswordStrength
import com.mohammadsalik.secureit.core.security.StrengthResult
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
        website: String,
        username: String,
        password: String
    ) {
        if (website.isBlank() || password.isBlank()) {
            _uiState.update { 
                it.copy(error = "Website and password are required")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val passwordToSave = if (uiState.value.password != null) {
                    // Update existing password
                    uiState.value.password!!.copy(
                        title = website, // Use website as title for backward compatibility
                        username = username,
                        password = password,
                        website = website,
                        notes = "", // Keep empty for simplified version
                        category = "General" // Default category
                    )
                } else {
                    // Create new password
                    Password.create(
                        title = website, // Use website as title for backward compatibility
                        username = username,
                        password = password,
                        website = website,
                        notes = "", // Keep empty for simplified version
                        category = "General" // Default category
                    )
                }

                val newId = passwordRepository.insertPassword(passwordToSave)
                val savedPassword = passwordToSave.copy(id = newId)
                _uiState.update { 
                    it.copy(
                        password = savedPassword,
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

    fun generatePassword(
        length: Int = uiState.value.generatorLength,
        includeUppercase: Boolean = uiState.value.includeUppercase,
        includeLowercase: Boolean = uiState.value.includeLowercase,
        includeDigits: Boolean = uiState.value.includeDigits,
        includeSpecial: Boolean = uiState.value.includeSpecial
    ): String {
        val pools = mutableListOf<String>()
        if (includeUppercase) pools += "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (includeLowercase) pools += "abcdefghijklmnopqrstuvwxyz"
        if (includeDigits) pools += "0123456789"
        if (includeSpecial) pools += "!@#\$%^&*()_+-=[]{}|;:,.<>?"

        if (pools.isEmpty()) return ""

        val allChars = pools.joinToString("")

        val required = pools.map { it[secureRandom.nextInt(it.length)] }
        val remaining = (1..(length - required.size)).map { allChars[secureRandom.nextInt(allChars.length)] }
        val raw = (required + remaining).shuffled(secureRandom).joinToString("")

        return raw
    }

    fun evaluateStrength(password: String): StrengthResult {
        val result = PasswordStrength.evaluate(password)
        _uiState.update { it.copy(strength = result) }
        return result
    }

    fun setGeneratorLength(length: Int) {
        _uiState.update { it.copy(generatorLength = length.coerceIn(8, 64)) }
    }

    fun setGeneratorOption(kind: GeneratorOption, enabled: Boolean) {
        _uiState.update { state ->
            when (kind) {
                GeneratorOption.Uppercase -> state.copy(includeUppercase = enabled)
                GeneratorOption.Lowercase -> state.copy(includeLowercase = enabled)
                GeneratorOption.Digits -> state.copy(includeDigits = enabled)
                GeneratorOption.Special -> state.copy(includeSpecial = enabled)
            }
        }
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

    fun clearSuccessState() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun resetPasswordState() {
        _uiState.update { it.copy(password = null) }
    }
}

enum class GeneratorOption { Uppercase, Lowercase, Digits, Special }

data class PasswordEditUiState(
    val password: Password? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    // Generator options
    val generatorLength: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSpecial: Boolean = true,
    // Strength
    val strength: StrengthResult? = null
)