package com.mohammadsalik.secureit.presentation.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadsalik.secureit.domain.model.Document
import com.mohammadsalik.secureit.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentListUiState())
    val uiState: StateFlow<DocumentListUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
        loadCategories()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val documents = documentRepository.getAllDocuments().first()
                _uiState.update { 
                    it.copy(
                        documents = documents,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Failed to load documents",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = documentRepository.getAllCategories().first()
                _uiState.update { it.copy(categories = categories) }
            } catch (e: Exception) {
                // Handle error silently for categories
            }
        }
    }

    fun searchDocuments(query: String) {
        viewModelScope.launch {
            try {
                val documents = if (query.isBlank()) {
                    documentRepository.getAllDocuments().first()
                } else {
                    documentRepository.searchDocuments(query).first()
                }
                _uiState.update { it.copy(documents = documents) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to search documents")
                }
            }
        }
    }

    fun filterByCategory(category: String?) {
        viewModelScope.launch {
            try {
                val documents = if (category == null) {
                    documentRepository.getAllDocuments().first()
                } else {
                    documentRepository.getDocumentsByCategory(category).first()
                }
                _uiState.update { 
                    it.copy(
                        documents = documents,
                        selectedCategory = category
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to filter documents")
                }
            }
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(document)
                val remaining = _uiState.value.documents.filter { it.id != document.id }
                _uiState.update { it.copy(documents = remaining) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete document") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun refresh() { loadDocuments(); loadCategories() }
}

data class DocumentListUiState(
    val documents: List<Document> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
