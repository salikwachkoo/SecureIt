package com.mohammadsalik.secureit.presentation.documents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.domain.model.Document
import com.mohammadsalik.secureit.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentUploadViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object { private const val TAG = "DocUploadVM" }

    private val _uiState = MutableStateFlow(DocumentUploadUiState())
    val uiState: StateFlow<DocumentUploadUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    fun persistUriPermission(uri: Uri, context: Context) {
        // Not required anymore since we copy to app-private storage
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {}
    }

    fun setSelectedFile(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "setSelectedFile uri=$uri")
                val fileName = getFileName(uri, context)
                val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
                val size = getFileSize(uri, context)
                _uiState.update { it.copy(selectedFile = SelectedFile(uri, fileName, mimeType, size)) }
            } catch (e: Exception) {
                Log.e(TAG, "setSelectedFile error", e)
                _uiState.update { it.copy(error = "Failed to read file: ${e.message}") }
            }
        }
    }

    fun cancelUpload() { currentJob?.cancel() }

    fun uploadDocument(title: String, category: String, notes: String) {
        if (title.isBlank() || uiState.value.selectedFile == null) {
            _uiState.update { it.copy(error = "Title and file are required") }
            return
        }

        currentJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isUploading = true, error = null) }
            try {
                val selected = uiState.value.selectedFile!!
                val safeName = (selected.name.ifBlank { "Document" }).replace(Regex("[^A-Za-z0-9_.-]"), "_")
                val dir = File(appContext.filesDir, "secure_docs").apply { mkdirs() }
                val dest = File(dir, System.currentTimeMillis().toString() + "_" + safeName)
                appContext.contentResolver.openInputStream(selected.uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Unable to open selected file")

                val document = Document.create(
                    title = title,
                    fileName = selected.name,
                    mimeType = selected.mimeType,
                    filePath = dest.absolutePath,
                    fileSize = selected.size,
                    category = category
                )
                documentRepository.insertDocument(document)
                _uiState.update { it.copy(isUploaded = true, isUploading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
                _uiState.update { it.copy(error = "Failed to save document: ${e.message}", isUploading = false) }
            }
        }
    }

    private fun getFileName(uri: Uri, context: Context): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) return it.getString(displayNameIndex)
                }
            }
            uri.lastPathSegment ?: "Document.pdf"
        } catch (e: Exception) { uri.lastPathSegment ?: "Document.pdf" }
    }

    private fun getFileSize(uri: Uri, context: Context): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) return it.getLong(sizeIndex)
                }
            }
            0L
        } catch (e: Exception) { 0L }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun resetUploadState() { _uiState.update { it.copy(isUploaded = false) } }
}

data class DocumentUploadUiState(
    val selectedFile: SelectedFile? = null,
    val isUploading: Boolean = false,
    val isUploaded: Boolean = false,
    val processedBytes: Long = 0L,
    val error: String? = null
)

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long
)
