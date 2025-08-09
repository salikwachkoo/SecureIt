package com.mohammadsalik.secureit.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import androidx.biometric.BiometricManager

@Composable
fun BiometricSetupScreen(
    onBiometricSetupComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val biometricSetupState by viewModel.biometricSetupState.collectAsStateWithLifecycle()
    var showSkipDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkBiometricAvailability()
    }

    LaunchedEffect(biometricSetupState.biometricTestSuccess) {
        if (biometricSetupState.biometricTestSuccess) {
            onBiometricSetupComplete()
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
            imageVector = Icons.Default.Lock, //Fingerprint
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Biometric Setup",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status message based on biometric availability
        val statusMessage = when {
            biometricSetupState.isLoading -> "Checking biometric availability..."
            !biometricSetupState.isBiometricAvailable ->
                "Biometric authentication is not available on this device."
            biometricSetupState.isBiometricEnabled ->
                "Biometric authentication is enabled. You can use fingerprint or face recognition to unlock your vault."
            else ->
                "Enable biometric authentication for quick and secure access to your vault."
        }

        Text(
            text = statusMessage,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Biometric availability info
        if (biometricSetupState.availableBiometricTypes.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Available Biometric Types:",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    biometricSetupState.availableBiometricTypes.forEach { type ->
                        Text(
                            text = "• ${type.name.lowercase().capitalize(Locale.ROOT)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        if (biometricSetupState.isBiometricAvailable) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val activity = ctx as? androidx.fragment.app.FragmentActivity

            // Single switch that triggers enrollment or enables/disables fingerprint only
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Enable fingerprint unlock")
                    var toggling by remember { mutableStateOf(false) }
                    Switch(
                        checked = biometricSetupState.isBiometricEnabled,
                        onCheckedChange = { checked ->
                            if (activity != null) {
                                // If turning on, ensure enrollment; otherwise disable directly
                                val mgr = com.mohammadsalik.secureit.core.security.BiometricAuthManager(ctx)
                                when (mgr.canAuthenticateStatus()) {
                                    BiometricManager.BIOMETRIC_SUCCESS -> {
                                        viewModel.setupBiometric(checked)
                                    }
                                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                        // open enrollment UI
                                        mgr.launchBiometricEnrollment(activity)
                                        // Keep switch off until user returns and re-checks
                                    }
                                    else -> {
                                        // hardware unavailable or not supported, keep switch off
                                        viewModel.setupBiometric(false)
                                    }
                                }
                            }
                        },
                        enabled = !biometricSetupState.isLoading
                    )
                }

                OutlinedButton(
                    onClick = { onBiometricSetupComplete() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }

                TextButton(
                    onClick = { showSkipDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Skip for now")
                }
            }
        } else {
            // Biometric not available - show continue option
            Button(
                onClick = { onBiometricSetupComplete() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to Vault")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Additional info
        Text(
            text = "You can change biometric settings later in the app settings.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }

    // Skip dialog
    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip Biometric Setup") },
            text = {
                Text(
                    "You can enable biometric authentication later in the app settings. " +
                            "Your vault will still be protected by your PIN."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSkipDialog = false
                        onBiometricSetupComplete()
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSkipDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}