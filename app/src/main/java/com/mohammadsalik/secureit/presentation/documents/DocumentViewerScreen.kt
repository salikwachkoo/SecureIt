package com.mohammadsalik.secureit.presentation.documents

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    documentId: Long,
    onBack: () -> Unit,
    onOpenFullScreen: (Int) -> Unit,
    viewModel: DocumentViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(documentId) { viewModel.load(documentId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.document?.title ?: "Document") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }
                uiState.error != null -> { Text(uiState.error!!, modifier = Modifier.align(Alignment.Center)) }
                uiState.previews.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(uiState.previews) { index, bmp ->
                            bmp?.let {
                                Card {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenFullScreen(index) }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> { Text("Preview not available for this file type", modifier = Modifier.align(Alignment.Center)) }
            }
        }
    }
}