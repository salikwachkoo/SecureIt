package com.mohammadsalik.secureit.presentation.documents

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.domain.model.Document
import com.mohammadsalik.secureit.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentUploadViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUploadUiState())
    val uiState: StateFlow<DocumentUploadUiState> = _uiState.asStateFlow()

    fun setSelectedFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = getFileName(uri, context)
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val size = getFileSize(uri, context)

                _uiState.update {
                    it.copy(
                        selectedFile = SelectedFile(
                            uri = uri,
                            name = fileName,
                            mimeType = mimeType,
                            size = size
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to read file: ${e.message}")
                }
            }
        }
    }

    fun uploadDocument(title: String, category: String, notes: String) {
        if (title.isBlank() || uiState.value.selectedFile == null) {
            _uiState.update {
                it.copy(error = "Title and file are required")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            try {
                val selectedFile = uiState.value.selectedFile!!
                
                // Create document from selected file
                val document = Document.create(
                    title = title,
                    fileName = selectedFile.name,
                    mimeType = selectedFile.mimeType,
                    filePath = selectedFile.uri.toString(),
                    fileSize = selectedFile.size,
                    category = category
                )

                // Save document to repository
                documentRepository.insertDocument(document)
                
                _uiState.update {
                    it.copy(
                        isUploaded = true,
                        isUploading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to upload document: ${e.message}",
                        isUploading = false
                    )
                }
            }
        }
    }

    private fun getFileName(uri: Uri, context: Context): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
            uri.lastPathSegment ?: "Unknown file"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Unknown file"
        }
    }

    private fun getFileSize(uri: Uri, context: Context): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        return it.getLong(sizeIndex)
                    }
                }
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetUploadState() {
        _uiState.update { it.copy(isUploaded = false) }
    }
}

data class DocumentUploadUiState(
    val selectedFile: SelectedFile? = null,
    val isUploading: Boolean = false,
    val isUploaded: Boolean = false,
    val error: String? = null
)

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long
)
