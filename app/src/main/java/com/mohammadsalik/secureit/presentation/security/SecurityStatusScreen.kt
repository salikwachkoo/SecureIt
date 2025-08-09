package com.mohammadsalik.secureit.presentation.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohammadsalik.secureit.core.security.SecurityManager
import com.mohammadsalik.secureit.core.security.SecurityStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityStatusScreen(
    onBack: () -> Unit,
    context: android.content.Context
) {
    var securityStatus by remember { mutableStateOf<SecurityStatus?>(null) }
    
    LaunchedEffect(Unit) {
        securityStatus = SecurityManager.performSecurityCheck(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            securityStatus?.let { status ->
                // Overall Security Status
                item {
                    SecurityStatusCard(status = status)
                }
                
                // Root Detection Details
                item {
                    RootDetectionCard(rootDetection = status.rootDetection)
                }
                
                // Debug Status Details
                item {
                    DebugStatusCard(debugStatus = status.debugStatus)
                }
                
                // Screen Recording Status
                item {
                    ScreenRecordingCard(isRecording = status.screenRecording)
                }
                
                // Background Security Status
                item {
                    BackgroundSecurityCard(backgroundSecurity = status.backgroundSecurity)
                }
                
                // Recommendations
                item {
                    RecommendationsCard(recommendations = status.recommendations)
                }
            }
        }
    }
}

@Composable
fun SecurityStatusCard(status: SecurityStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isCompromised) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (status.isCompromised) Icons.Default.Security else Icons.Default.Verified,
                    contentDescription = null,
                    tint = if (status.isCompromised) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (status.isCompromised) "Security Compromised" else "Security Status: Secure",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (status.isCompromised) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun RootDetectionCard(rootDetection: com.mohammadsalik.secureit.core.security.RootDetectionResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Root Detection",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (rootDetection.isRooted) "⚠️ Device appears to be rooted" else "✅ No root detected",
                color = if (rootDetection.isRooted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            if (rootDetection.rootIndicators.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Risk Level: ${rootDetection.riskLevel}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DebugStatusCard(debugStatus: com.mohammadsalik.secureit.core.security.DebugSecurityStatus) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Debug Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (debugStatus.isCompromised) "⚠️ Debug environment detected" else "✅ Clean environment",
                color = if (debugStatus.isCompromised) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ScreenRecordingCard(isRecording: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Screen Recording",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRecording) "🚨 Recording detected" else "✅ No recording detected",
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BackgroundSecurityCard(backgroundSecurity: com.mohammadsalik.secureit.core.security.BackgroundSecurityStatus) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Background Security",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (backgroundSecurity.isInBackground) "🔒 App in background" else "✅ App in foreground",
                color = if (backgroundSecurity.isInBackground) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Security Recommendations",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            recommendations.forEach { recommendation ->
                Text(
                    text = "• $recommendation",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
