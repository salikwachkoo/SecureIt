package com.mohammadsalik.secureit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohammadsalik.secureit.presentation.auth.AuthViewModel
import com.mohammadsalik.secureit.ui.theme.SecureItTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecureItTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecureVaultApp()
                }
            }
        }
    }
}

@Composable
fun SecureVaultApp() {
    val viewModel: AuthViewModel = hiltViewModel()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }

    // Determine initial screen based on authentication state
    LaunchedEffect(authState) {
        currentScreen = when {
            !authState.isPinSet -> Screen.Welcome
            !authState.isAuthenticated -> Screen.PinEntry
            else -> Screen.MainVault
        }
    }

    when (currentScreen) {
        Screen.Welcome -> WelcomeScreen(
            onContinue = { currentScreen = Screen.PinSetup }
        )
        Screen.PinSetup -> PinSetupScreen(
            onPinSetupComplete = { currentScreen = Screen.BiometricSetup }
        )
        Screen.BiometricSetup -> BiometricSetupScreen(
            onBiometricSetupComplete = { currentScreen = Screen.MainVault }
        )
        Screen.MainVault -> MainVaultScreen(
            onLogout = {
                viewModel.logout()
                currentScreen = Screen.PinEntry
            }
        )
        Screen.PinEntry -> PinEntryScreen(
            onPinCorrect = { currentScreen = Screen.MainVault },
            onForgotPin = { currentScreen = Screen.Welcome }
        )
    }
}

sealed class Screen {
    object Welcome : Screen()
    object PinSetup : Screen()
    object BiometricSetup : Screen()
    object MainVault : Screen()
    object PinEntry : Screen()
}

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SecureVault",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Personal Security Vault",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Store your passwords, documents, and sensitive information securely with military-grade encryption.",
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Get Started", fontSize = 18.sp)
        }
    }
}

@Composable
fun PinSetupScreen(onPinSetupComplete: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Set up your SecureVault",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create a 4-digit PIN to access your vault. For enhanced security, enable biometric or facial recognition.",
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Input Fields
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    pin = it
                    showError = false
                }
            },
            label = { Text("Enter 4-digit PIN") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    confirmPin = it
                    showError = false
                }
            },
            label = { Text("Confirm PIN") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            singleLine = true,
            isError = showError
        )

        if (showError) {
            Text(
                text = "PINs do not match",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (pin == confirmPin && pin.length == 4) {
                    onPinSetupComplete()
                } else {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = pin.length == 4 && confirmPin.length == 4
        ) {
            Text("Create Vault", fontSize = 18.sp)
        }
    }
}

@Composable
fun BiometricSetupScreen(onBiometricSetupComplete: () -> Unit) {
    var biometricEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enhanced Security",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enable biometric authentication for quick and secure access to your vault.",
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enable Biometric/Facial Recognition",
                fontSize = 16.sp
            )

            Switch(
                checked = biometricEnabled,
                onCheckedChange = { biometricEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBiometricSetupComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onBiometricSetupComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now", fontSize = 16.sp)
        }
    }
}

@Composable
fun PinEntryScreen(
    onPinCorrect: () -> Unit,
    onForgotPin: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter your PIN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (index < pin.length) "●" else "",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (showError) {
            Text(
                text = "Incorrect PIN",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Number Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) { row ->
                Row {
                    repeat(3) { col ->
                        val number = row * 3 + col + 1
                        NumberButton(
                            number = number.toString(),
                            onClick = {
                                if (pin.length < 4) {
                                    pin += number.toString()
                                    showError = false
                                }
                            }
                        )
                    }
                }
            }

            Row {
                NumberButton(
                    number = "0",
                    onClick = {
                        if (pin.length < 4) {
                            pin += "0"
                            showError = false
                        }
                    }
                )

                NumberButton(
                    number = "⌫",
                    onClick = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            showError = false
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(
            onClick = onForgotPin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Forgot PIN?", fontSize = 16.sp)
        }

        // Auto-submit when PIN is complete
        LaunchedEffect(pin) {
            if (pin.length == 4) {
                // Simulate PIN validation
                if (pin == "1234") {
                    onPinCorrect()
                } else {
                    showError = true
                    pin = ""
                }
            }
        }
    }
}

@Composable
fun NumberButton(
    number: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .padding(4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = number,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MainVaultScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with logout button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SecureVault",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your Personal Security Vault",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to your secure vault! Here you can store and manage your passwords, documents, and other sensitive information.",
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Placeholder for vault categories
        VaultCategoryCard(
            title = "Passwords",
            description = "Manage your login credentials",
            icon = "🔑"
        )

        Spacer(modifier = Modifier.height(16.dp))

        VaultCategoryCard(
            title = "Documents",
            description = "Store encrypted files and documents",
            icon = "📄"
        )

        Spacer(modifier = Modifier.height(16.dp))

        VaultCategoryCard(
            title = "Notes",
            description = "Secure text notes and information",
            icon = "📝"
        )
    }
}

@Composable
fun VaultCategoryCard(
    title: String,
    description: String,
    icon: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}