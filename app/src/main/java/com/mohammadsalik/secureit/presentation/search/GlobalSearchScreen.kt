package com.mohammadsalik.secureit.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohammadsalik.secureit.domain.model.Document
import com.mohammadsalik.secureit.domain.model.Password
import com.mohammadsalik.secureit.domain.model.SecureNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    onPasswordClick: (Password) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onNoteClick: (SecureNote) -> Unit,
    onAddPassword: () -> Unit,
    onAddDocument: () -> Unit,
    onAddNote: () -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.searchAll(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                actions = {
                    IconButton(onClick = { /* TODO: Filter options */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search passwords, documents, and notes...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            // Search results
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                searchQuery.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Search Your Vault",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Search across all your passwords, documents, and notes",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                uiState.searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try different keywords or check your spelling",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.searchResults) { result ->
                            SearchResultItem(
                                result = result,
                                onPasswordClick = onPasswordClick,
                                onDocumentClick = onDocumentClick,
                                onNoteClick = onNoteClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultItem(
    result: SearchResult,
    onPasswordClick: (Password) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onNoteClick: (SecureNote) -> Unit
) {
    Card(
        onClick = {
            when (result) {
                is SearchResult.Password -> onPasswordClick(result.password)
                is SearchResult.Document -> onDocumentClick(result.document)
                is SearchResult.Note -> onNoteClick(result.note)
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on result type
            Icon(
                imageVector = when (result) {
                    is SearchResult.Password -> Icons.Default.Lock
                    is SearchResult.Document -> Icons.Default.Description
                    is SearchResult.Note -> Icons.Default.Note
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Result details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (result) {
                        is SearchResult.Password -> result.password.title
                        is SearchResult.Document -> result.document.title
                        is SearchResult.Note -> result.note.title
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (result) {
                        is SearchResult.Password -> "Password • ${result.password.username}"
                        is SearchResult.Document -> "Document • ${result.document.fileName}"
                        is SearchResult.Note -> "Note • ${result.note.content.take(50)}..."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (result) {
                        is SearchResult.Password -> result.password.category.ifBlank { "General" }
                        is SearchResult.Document -> result.document.category.ifBlank { "General" }
                        is SearchResult.Note -> result.note.category.ifBlank { "General" }
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

sealed class SearchResult {
    data class Password(val password: com.mohammadsalik.secureit.domain.model.Password) : SearchResult()
    data class Document(val document: com.mohammadsalik.secureit.domain.model.Document) : SearchResult()
    data class Note(val note: com.mohammadsalik.secureit.domain.model.SecureNote) : SearchResult()
}
