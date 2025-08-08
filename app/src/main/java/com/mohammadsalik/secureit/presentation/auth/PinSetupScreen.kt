package com.mohammadsalik.secureit.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PinSetupScreen(
    onPinSetupComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val pinSetupState by viewModel.pinSetupState.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showConfirmPin by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showConfirmPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pinSetupState.isPinSet) {
        if (pinSetupState.isPinSet) {
            onPinSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (showConfirmPin) "Confirm Your PIN" else "Create Your PIN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (showConfirmPin)
                "Enter your PIN again to confirm"
            else
                "Create a 4-digit PIN to secure your vault",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Input Field
        OutlinedTextField(
            value = if (showConfirmPin) confirmPin else pin,
            onValueChange = { newValue ->
                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                    if (showConfirmPin) {
                        confirmPin = newValue
                    } else {
                        pin = newValue
                    }
                    errorMessage = null
                }
            },
            label = { Text(if (showConfirmPin) "Confirm PIN" else "Enter PIN") },
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPin) "Hide PIN" else "Show PIN"
                    )
                }
            },
            isError = errorMessage != null
        )

        // Error Message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PIN Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val currentPin = if (showConfirmPin) confirmPin else pin
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentPin.length) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "○",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        if (showConfirmPin) {
            // Confirm PIN buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showConfirmPin = false
                        confirmPin = ""
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        when {
                            confirmPin.length != 4 -> {
                                errorMessage = "Please enter a 4-digit PIN"
                            }
                            confirmPin != pin -> {
                                errorMessage = "PINs don't match. Please try again."
                            }
                            else -> {
                                // PINs match, proceed with setup
                                viewModel.setupPin(pin)
                            }
                        }
                    },
                    enabled = confirmPin.length == 4 && !pinSetupState.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (pinSetupState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm")
                    }
                }
            }
        } else {
            // Initial PIN setup button
            Button(
                onClick = {
                    when {
                        pin.length != 4 -> {
                            errorMessage = "Please enter a 4-digit PIN"
                        }
                        else -> {
                            showConfirmPin = true
                            errorMessage = null
                        }
                    }
                },
                enabled = pin.length == 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skip biometric setup option
        if (!showConfirmPin) {
            TextButton(
                onClick = {
                    // Skip biometric setup and go directly to main vault
                    viewModel.setupPin(pin)
                }
            ) {
                Text("Skip biometric setup for now")
            }
        }
    }
}