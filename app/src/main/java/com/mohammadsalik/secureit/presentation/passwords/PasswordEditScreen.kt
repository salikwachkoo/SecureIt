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
import com.mohammadsalik.secureit.core.security.StrengthResult
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

    // Re-evaluate strength when password changes
    LaunchedEffect(password) {
        viewModel.evaluateStrength(password)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (passwordId == null) "Add Password" else "Edit Password") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.copyToClipboard(password) }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                    IconButton(onClick = { viewModel.savePassword(title, username, password, website, notes, category) }) {
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password + strength + generator
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide" else "Show"
                            )
                        }
                        IconButton(onClick = {
                            password = viewModel.generatePassword()
                        }) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Generate")
                        }
                    }
                }
            )

            // Strength meter
            uiState.strength?.let { strength ->
                StrengthBar(result = strength)
            }

            // Generator options
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Password Generator", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Length: ${uiState.generatorLength}")
                        Spacer(Modifier.width(8.dp))
                        Slider(value = uiState.generatorLength.toFloat(), valueRange = 8f..64f, onValueChange = {
                            viewModel.setGeneratorLength(it.toInt())
                        })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(selected = uiState.includeUppercase, onClick = {
                            viewModel.setGeneratorOption(GeneratorOption.Uppercase, !uiState.includeUppercase)
                        }, label = { Text("A-Z") })
                        FilterChip(selected = uiState.includeLowercase, onClick = {
                            viewModel.setGeneratorOption(GeneratorOption.Lowercase, !uiState.includeLowercase)
                        }, label = { Text("a-z") })
                        FilterChip(selected = uiState.includeDigits, onClick = {
                            viewModel.setGeneratorOption(GeneratorOption.Digits, !uiState.includeDigits)
                        }, label = { Text("0-9") })
                        FilterChip(selected = uiState.includeSpecial, onClick = {
                            viewModel.setGeneratorOption(GeneratorOption.Special, !uiState.includeSpecial)
                        }, label = { Text("!@#") })
                    }
                }
            }

            // Website
            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            if (uiState.error != null) {
                AssistChip(onClick = { viewModel.clearError() }, label = { Text(uiState.error!!) }, leadingIcon = {
                    Icon(Icons.Default.Error, contentDescription = null)
                })
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun StrengthBar(result: StrengthResult) {
    val color = when (result.score) {
        in 0..19 -> MaterialTheme.colorScheme.error
        in 20..39 -> MaterialTheme.colorScheme.tertiary
        in 40..59 -> MaterialTheme.colorScheme.secondary
        in 60..79 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(progress = { result.score / 100f }, color = color, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Strength: ${result.label}", fontSize = 12.sp)
            if (result.suggestions.isNotEmpty()) {
                Text("• " + result.suggestions.take(2).joinToString("; "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}