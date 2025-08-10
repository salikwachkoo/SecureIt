package com.mohammadsalik.secureit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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
import com.mohammadsalik.secureit.presentation.auth.AuthViewModel
import com.mohammadsalik.secureit.presentation.auth.PinEntryScreen
import com.mohammadsalik.secureit.presentation.vault.VaultDashboardScreen
import com.mohammadsalik.secureit.presentation.passwords.PasswordListScreen
import com.mohammadsalik.secureit.presentation.passwords.PasswordEditScreen
import com.mohammadsalik.secureit.presentation.documents.DocumentListScreen
import com.mohammadsalik.secureit.presentation.documents.DocumentUploadScreen
import com.mohammadsalik.secureit.presentation.notes.SecureNoteListScreen
import com.mohammadsalik.secureit.presentation.notes.SecureNoteEditScreen
import com.mohammadsalik.secureit.presentation.search.GlobalSearchScreen
import com.mohammadsalik.secureit.core.security.SecurityManager
import com.mohammadsalik.secureit.ui.theme.SecureItTheme
import com.mohammadsalik.secureit.presentation.settings.SettingsScreen
import com.mohammadsalik.secureit.presentation.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.mohammadsalik.secureit.presentation.documents.DocumentViewerScreen
import com.mohammadsalik.secureit.presentation.documents.DocumentFullScreenPager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot() }
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            try {
                SecurityManager.initializeSecurity(this@MainActivity)
                SecurityManager.performSecurityCheck(this@MainActivity)
            } catch (_: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        try { SecurityManager.enableSecurityProtection(this) } catch (_: Exception) {}
    }
    override fun onPause() { super.onPause(); try { SecurityManager.disableSecurityProtection(this) } catch (_: Exception) {} }
}

@Composable
fun AppRoot() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.uiState.collectAsState()

    SecureItTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor,
        textScale = settings.textScale
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SecureVaultApp()
        }
    }
}

@Composable
fun SecureVaultApp() {
    val viewModel: AuthViewModel = hiltViewModel()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }
    var navigationStack by remember { mutableStateOf(listOf<Screen>()) }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        currentScreen = when {
            // If no PIN yet, show onboarding welcome
            !authState.isPinSet -> Screen.Welcome
            // If PIN set but onboarding not completed, force biometric setup step
            authState.isPinSet && !authState.isOnboardingCompleted -> Screen.BiometricSetup
            // After onboarding, require PIN every time unless authenticated session active
            !authState.isAuthenticated -> Screen.PinEntry
            else -> Screen.MainVault
        }
    }

    fun navigateTo(screen: Screen) { navigationStack = navigationStack + currentScreen; currentScreen = screen }
    fun navigateBack() { if (navigationStack.isNotEmpty()) { currentScreen = navigationStack.last(); navigationStack = navigationStack.dropLast(1) } }

    BackHandler {
        if (currentScreen == Screen.MainVault) {
            showExitDialog = true
        } else {
            navigateBack()
        }
    }

    if (showExitDialog) {
        val ctx = LocalContext.current
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            confirmButton = { TextButton(onClick = { showExitDialog = false; (ctx as? ComponentActivity)?.finish() }) { Text("Exit") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancel") } },
            title = { Text("Exit App") },
            text = { Text("Are you sure you want to exit?") }
        )
    }

    when (val screen = currentScreen) {
        Screen.Welcome -> WelcomeScreen(
            onContinue = { currentScreen = Screen.PinSetup }
        )
        Screen.PinSetup -> PinSetupScreen(
            onPinSetupComplete = { currentScreen = Screen.BiometricSetup }
        )
        Screen.BiometricSetup -> BiometricSetupScreen(
            onBiometricSetupComplete = {
                currentScreen = Screen.PinEntry
            }
        )
        Screen.MainVault -> VaultDashboardScreen(
            onNavigateToPasswords = { navigateTo(Screen.PasswordList) },
            onNavigateToDocuments = { navigateTo(Screen.DocumentList) },
            onNavigateToNotes = { navigateTo(Screen.NoteList) },
            onLogout = {
                viewModel.logout()
                currentScreen = Screen.PinEntry
                navigationStack = emptyList()
            },
            onNavigateToSettings = { navigateTo(Screen.Settings) },
            onNavigateToSearch = { navigateTo(Screen.GlobalSearch) }
        )
        Screen.PinEntry -> PinEntryScreen(
            onPinCorrect = { currentScreen = Screen.MainVault },
            onForgotPin = { currentScreen = Screen.Welcome }
        )
        Screen.PasswordList -> PasswordListScreen(
            onPasswordClick = { password -> navigateTo(Screen.PasswordEdit(password.id)) },
            onAddPassword = { navigateTo(Screen.PasswordEdit()) },
            onBack = { navigateBack() }
        )
        is Screen.PasswordEdit -> PasswordEditScreen(
            passwordId = screen.passwordId,
            onSave = { navigateBack() },
            onCancel = { navigateBack() }
        )
        Screen.DocumentList -> DocumentListScreen(
            onDocumentClick = { docId -> navigateTo(Screen.DocumentViewer(docId)) },
            onAddDocument = { navigateTo(Screen.DocumentUpload) },
            onBack = { navigateBack() }
        )
        Screen.DocumentUpload -> DocumentUploadScreen(
            onUploadComplete = { navigateBack() },
            onCancel = { navigateBack() }
        )
        is Screen.DocumentViewer -> DocumentViewerScreen(
            documentId = screen.id,
            onBack = { navigateBack() },
            onOpenFullScreen = { page -> navigateTo(Screen.DocumentFullScreen(screen.id, page)) }
        )
        is Screen.DocumentFullScreen -> DocumentFullScreenPager(
            documentId = screen.documentId,
            startPage = screen.page,
            onBack = { navigateBack() }
        )
        Screen.NoteList -> SecureNoteListScreen(
            onNoteClick = { note -> navigateTo(Screen.NoteEdit(note.id)) },
            onAddNote = { navigateTo(Screen.NoteEdit()) },
            onBack = { navigateBack() }
        )
        is Screen.NoteEdit -> SecureNoteEditScreen(
            noteId = screen.noteId,
            onSave = { navigateBack() },
            onCancel = { navigateBack() }
        )
        Screen.GlobalSearch -> GlobalSearchScreen(
            onPasswordClick = { password -> navigateTo(Screen.PasswordEdit(password.id)) },
            onDocumentClick = { /* TODO */ },
            onNoteClick = { note -> navigateTo(Screen.NoteEdit(note.id)) },
            onAddPassword = { navigateTo(Screen.PasswordEdit()) },
            onAddDocument = { navigateTo(Screen.DocumentUpload) },
            onAddNote = { navigateTo(Screen.NoteEdit()) },
            onBack = { navigateBack() }
        )
        Screen.Settings -> SettingsScreen(onBack = { navigateBack() })
    }
}

sealed class Screen {
    object Welcome : Screen()
    object PinSetup : Screen()
    object BiometricSetup : Screen()
    object MainVault : Screen()
    object PinEntry : Screen()
    object PasswordList : Screen()
    data class PasswordEdit(val passwordId: Long? = null) : Screen()
    object DocumentList : Screen()
    object DocumentUpload : Screen()
    data class DocumentViewer(val id: Long) : Screen()
    data class DocumentFullScreen(val documentId: Long, val page: Int) : Screen()
    object NoteList : Screen()
    data class NoteEdit(val noteId: Long? = null) : Screen()
    object GlobalSearch : Screen()
    object Settings : Screen()
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val pinState by authViewModel.pinSetupState.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    // Navigate forward when PIN saved
    LaunchedEffect(pinState.isPinSet) {
        if (pinState.isPinSet) onPinSetupComplete()
    }

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

        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    pin = it; showError = false
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
                    confirmPin = it; showError = false
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

        if (pinState.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (pin == confirmPin && pin.length == 4) {
                    authViewModel.setupPin(pin)
                } else {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = pin.length == 4 && confirmPin.length == 4 && !pinState.isLoading
        ) {
            Text("Save PIN", fontSize = 18.sp)
        }
    }
}

@Composable
fun BiometricSetupScreen(onBiometricSetupComplete: () -> Unit) {
    val authViewModel: AuthViewModel = hiltViewModel()
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
            Text(text = "Enable Biometric/Facial Recognition", fontSize = 16.sp)
            Switch(checked = biometricEnabled, onCheckedChange = { biometricEnabled = it })
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                authViewModel.setupBiometric(biometricEnabled)
                onBiometricSetupComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                authViewModel.setupBiometric(false)
                onBiometricSetupComplete()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now", fontSize = 16.sp)
        }
    }
}

// Removed local PinEntryScreen and NumberButton; using presentation.auth.PinEntryScreen which validates via AuthViewModel
