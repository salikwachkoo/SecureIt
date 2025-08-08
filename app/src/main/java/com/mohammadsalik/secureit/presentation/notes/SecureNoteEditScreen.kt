package com.mohammadsalik.secureit.presentation.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureNoteEditScreen(
    noteId: Long? = null,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: SecureNoteEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    // Load note data if editing
    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.loadNote(noteId)
        }
    }

    // Update form fields when note is loaded
    LaunchedEffect(uiState.note) {
        uiState.note?.let { note ->
            title = note.title
            content = note.content
            category = note.category
            tags = note.tags.joinToString(", ")
        }
    }

    // Handle save success
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSave()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "Add Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveNote(title, content, category, tags) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Enter note title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                placeholder = { Text("e.g., Personal, Work, Ideas") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags") },
                placeholder = { Text("Enter tags separated by commas") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Content
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                placeholder = { Text("Write your secure note here...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                maxLines = 15
            )

            // Character count
            Text(
                text = "${content.length} characters",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Error message
            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
