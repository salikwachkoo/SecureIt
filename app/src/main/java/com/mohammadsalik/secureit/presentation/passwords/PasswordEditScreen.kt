package com.mohammadsalik.secureit.presentation.passwords

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohammadsalik.secureit.domain.model.Password

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordEditScreen(
    passwordId: Long? = null,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PasswordEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showGeneratedPassword by remember { mutableStateOf(false) }

    // Load password data if editing
    LaunchedEffect(passwordId) {
        if (passwordId != null) {
            viewModel.loadPassword(passwordId)
        }
    }

    // Update form fields when password is loaded
    LaunchedEffect(uiState.password) {
        uiState.password?.let { loadedPassword ->
            title = loadedPassword.title
            username = loadedPassword.username
            password = loadedPassword.password
            website = loadedPassword.website
            notes = loadedPassword.notes
            category = loadedPassword.category
        }
    }

    // Handle save success
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSave()
        }
    }
}