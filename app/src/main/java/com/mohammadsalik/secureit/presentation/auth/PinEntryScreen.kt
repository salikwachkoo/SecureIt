package com.mohammadsalik.secureit.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.dp
import com.mohammadsalik.secureit.core.security.BiometricAuthManager
import com.mohammadsalik.secureit.core.security.BiometricResult

@Composable
fun PinEntryScreen(
    onPinCorrect: () -> Unit,
    onForgotPin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val bioState by viewModel.biometricSetupState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    val scope = rememberCoroutineScope()

    // Auto biometric on entry if enabled and available. If not enrolled, deep link to enroll.
    LaunchedEffect(authState.isBiometricEnabled) {
        if (activity != null && authState.isBiometricEnabled) {
            val mgr = BiometricAuthManager(ctx)
            when (mgr.canAuthenticateStatus()) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    when (mgr.authenticate(activity)) {
                        is BiometricResult.Success -> { viewModel.onBiometricSuccess(); onPinCorrect() }
                        else -> { /* stay on PIN screen */ }
                    }
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    // Prompt user to enroll biometrics in system settings
                    mgr.launchBiometricEnrollment(activity)
                }
                else -> { /* not available or hardware unavailable */ }
            }
        }
    }

    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(authState.pinError) {
        if (authState.pinError != null) {
            // Clear PIN on error
            pin = ""
            showError = true
            // Clear error after a delay
            delay(3000)
            viewModel.clearPinError()
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
            text = "Enter Your PIN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your 4-digit PIN to access your vault",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN Input Field
        OutlinedTextField(
            value = pin,
            onValueChange = { newValue ->
                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                    pin = newValue
                    if (newValue.length == 4) {
                        // Auto-submit when 4 digits are entered
                        viewModel.validatePin(newValue)
                    }
                }
            },
            label = { Text("Enter PIN") },
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        imageVector = if (showPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPin) "Hide PIN" else "Show PIN"
                    )
                }
            },
            isError = authState.pinError != null,
            enabled = !authState.isValidatingPin
        )

        // Error Message
        if (showError || authState.pinError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = authState.pinError ?: "Incorrect PIN",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Submit Button
        Button(
            onClick = { viewModel.validatePin(pin) },
            enabled = pin.length == 4 && !authState.isValidatingPin,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (authState.isValidatingPin) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Unlock")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fingerprint shortcut
        if (activity != null && authState.isBiometricEnabled) {
            OutlinedButton(
                onClick = {
                    val mgr = BiometricAuthManager(ctx)
                    when (mgr.canAuthenticateStatus()) {
                        BiometricManager.BIOMETRIC_SUCCESS -> {
                            scope.launch {
                                when (mgr.authenticate(activity)) {
                                    is BiometricResult.Success -> { viewModel.onBiometricSuccess(); onPinCorrect() }
                                    else -> { /* ignore */ }
                                }
                            }
                        }
                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> mgr.launchBiometricEnrollment(activity)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Use fingerprint")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Manual Biometric Button
        if (activity != null && authState.isBiometricEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            val scope = rememberCoroutineScope()
            OutlinedButton(onClick = {
                val mgr = BiometricAuthManager(ctx)
                when (mgr.canAuthenticateStatus()) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        scope.launch {
                            when (mgr.authenticate(activity)) {
                                is BiometricResult.Success -> { viewModel.onBiometricSuccess(); onPinCorrect() }
                                else -> { /* ignore */ }
                            }
                        }
                    }
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> mgr.launchBiometricEnrollment(activity)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Use biometric") }
        }

        // Forgot PIN Button
        TextButton(
            onClick = { showForgotPinDialog = true }
        ) {
            Text("Forgot PIN?")
        }
    }

    // Forgot PIN Dialog
    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            title = { Text("Reset PIN") },
            text = {
                Text(
                    "This will reset your PIN and biometric settings. " +
                            "You'll need to set up your PIN again. Continue?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetPin()
                        showForgotPinDialog = false
                        onForgotPin()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPinDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}