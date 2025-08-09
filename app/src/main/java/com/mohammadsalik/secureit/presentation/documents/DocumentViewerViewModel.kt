package com.mohammadsalik.secureit.presentation.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.core.audit.AuditLogger
import com.mohammadsalik.secureit.domain.model.Document
import com.mohammadsalik.secureit.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val auditLogger: AuditLogger,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentViewerUiState())
    val uiState: StateFlow<DocumentViewerUiState> = _uiState.asStateFlow()

    fun load(documentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null, reLinkRequired = false) }
            try {
                val doc = documentRepository.getDocumentById(documentId)
                if (doc == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Document not found") }
                    return@launch
                }

                val previews: List<Bitmap?>
                val path = doc.filePath
                if (path.startsWith("content:")) {
                    val uri = Uri.parse(path)
                    previews = renderFromContentUri(doc, uri)
                } else {
                    val file = File(path)
                    if (!file.exists()) {
                        _uiState.update { it.copy(isLoading = false, error = "File missing") }
                        return@launch
                    }
                    previews = renderFromFile(doc, file)
                }

                _uiState.update { it.copy(isLoading = false, document = doc, previews = previews) }
                auditLogger.log("document_viewed", mapOf("documentId" to documentId.toString(), "mime" to doc.mimeType))
                documentRepository.updateLastAccessed(documentId, java.time.LocalDateTime.now())
            } catch (se: SecurityException) {
                _uiState.update { it.copy(isLoading = false, error = "Permission denied. Please re-link file.", reLinkRequired = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load document") }
            }
        }
    }

    fun relink(documentId: Long, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = documentRepository.getDocumentById(documentId) ?: return@launch
                val safeName = (doc.fileName.ifBlank { "Document" }).replace(Regex("[^A-Za-z0-9_.-]"), "_")
                val dir = File(appContext.filesDir, "secure_docs").apply { mkdirs() }
                val dest = File(dir, System.currentTimeMillis().toString() + "_" + safeName)
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Unable to open selected file")
                documentRepository.updateDocument(doc.copy(filePath = dest.absolutePath))
                load(documentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to re-link file") }
            }
        }
    }

    private fun renderFromContentUri(doc: Document, uri: Uri): List<Bitmap?> {
        return when {
            doc.isPdf() -> renderPdfFromUri(uri)
            doc.isImage() -> listOf(decodeImageFromUri(uri))
            else -> emptyList()
        }
    }

    private fun renderFromFile(doc: Document, file: File): List<Bitmap?> {
        return when {
            doc.isPdf() -> renderPdf(file)
            doc.isImage() -> listOf(BitmapFactory.decodeFile(file.absolutePath))
            else -> emptyList()
        }
    }

    private fun renderPdf(file: File): List<Bitmap> {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val pages = mutableListOf<Bitmap>()
        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pages.add(bitmap)
            }
        } finally {
            renderer.close(); fd.close()
        }
        return pages
    }

    private fun renderPdfFromUri(uri: Uri): List<Bitmap> {
        val temp = File.createTempFile("preview_uri_", ".pdf", appContext.cacheDir)
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: throw SecurityException("Cannot open content Uri")
        return try { renderPdf(temp) } finally { try { temp.delete() } catch (_: Exception) {} }
    }

    private fun decodeImageFromUri(uri: Uri): Bitmap? {
        return appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
}

data class DocumentViewerUiState(
    val isLoading: Boolean = false,
    val document: Document? = null,
    val previews: List<Bitmap?> = emptyList(),
    val error: String? = null,
    val reLinkRequired: Boolean = false
)