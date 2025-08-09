package com.mohammadsalik.secureit.presentation.documents

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val TAG_UPLOAD = "DocUploadUI"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentUploadScreen(
    onUploadComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: DocumentUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var hasReset by remember { mutableStateOf(false) }

    // Reset any previous upload state when screen is shown again
    LaunchedEffect(Unit) {
        Log.d(TAG_UPLOAD, "Entering Upload Screen: pre-reset isUploaded=${uiState.isUploaded} error=${uiState.error}")
        viewModel.resetUploadState()
        viewModel.clearError()
        selectedUri = null
        title = ""
        category = ""
        notes = ""
        hasReset = true
        Log.d(TAG_UPLOAD, "State reset complete")
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedUri = uri
        Log.d(TAG_UPLOAD, "Picker result uri=$uri")
        uri?.let {
            viewModel.persistUriPermission(it, context)
            viewModel.setSelectedFile(it, context)
        }
    }

    // Only navigate away if we've finished initial reset and received a fresh upload success
    LaunchedEffect(uiState.isUploaded, hasReset) {
        Log.d(TAG_UPLOAD, "Observe isUploaded=${uiState.isUploaded} hasReset=$hasReset")
        if (hasReset && uiState.isUploaded) {
            Log.d(TAG_UPLOAD, "Trigger onUploadComplete()")
            onUploadComplete()
        }
    }

    if (uiState.isUploading) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = { Log.d(TAG_UPLOAD, "Cancel pressed"); viewModel.cancelUpload() }) { Text("Cancel") } },
            title = { Text("Saving Document") },
            text = {
                Column(horizontalAlignment = Alignment.Start) {
                    val total = (uiState.selectedFile?.size ?: 0L).coerceAtLeast(1L)
                    val pct = ((uiState.processedBytes * 100f) / total).coerceIn(0f, 100f)
                    LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${uiState.processedBytes / 1024} / ${total / 1024} KB (${pct.toInt()}%)")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Document") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(
                        onClick = { Log.d(TAG_UPLOAD, "Upload clicked title='$title' uri=$selectedUri"); viewModel.uploadDocument(title, category, notes) },
                        enabled = selectedUri != null && title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "Upload")
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Select PDF", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Choose a PDF to upload to your secure vault", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { Log.d(TAG_UPLOAD, "OpenDocument launcher invoked"); documentPicker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose PDF")
                    }
                }
            }

            if (uiState.selectedFile != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = uiState.selectedFile!!.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(text = formatFileSize(uiState.selectedFile!!.size), fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Document Title") }, placeholder = { Text("Enter document title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, placeholder = { Text("e.g., Work, Personal, Financial") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, placeholder = { Text("Additional notes (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)

            if (uiState.error != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
