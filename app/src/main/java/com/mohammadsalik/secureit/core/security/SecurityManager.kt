package com.mohammadsalik.secureit.core.security

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.WindowManager

object SecurityManager {
    
    private var securityCallback: ((SecurityStatus) -> Unit)? = null
    
    fun setSecurityCallback(callback: (SecurityStatus) -> Unit) {
        securityCallback = callback
    }
    
    fun performSecurityCheck(context: Context): SecurityStatus {
        return try {
            val rootDetection = RootDetectionManager.isDeviceRooted(context)
            val debugStatus = DebugPreventionManager.getSecurityStatus(context)
            val screenRecording = ScreenRecordingProtectionManager.isScreenRecordingProtected()
            val backgroundSecurity = AppBackgroundSecurityManager.getBackgroundSecurityStatus()
            
            val isCompromised = rootDetection.isRooted || 
                               debugStatus.isCompromised || 
                               screenRecording ||
                               backgroundSecurity.isInBackground
            
            val securityStatus = SecurityStatus(
                isCompromised = isCompromised,
                rootDetection = rootDetection,
                debugStatus = debugStatus,
                screenRecording = screenRecording,
                backgroundSecurity = backgroundSecurity,
                recommendations = getAllRecommendations(context, rootDetection, debugStatus, screenRecording, backgroundSecurity)
            )
            
            securityCallback?.invoke(securityStatus)
            securityStatus
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error in performSecurityCheck: ${e.message}")
            SecurityStatus(
                isCompromised = false,
                rootDetection = RootDetectionResult(
                    isRooted = false,
                    checks = emptyList(),
                    rootIndicators = emptyList(),
                    riskLevel = RootRiskLevel.LOW
                ),
                debugStatus = DebugSecurityStatus(
                    isCompromised = false,
                    debuggerAttached = false,
                    debugModeEnabled = false,
                    emulatorRunning = false,
                    tamperedWith = false,
                    recommendations = listOf("⚠️ Security check failed")
                ),
                screenRecording = false,
                backgroundSecurity = BackgroundSecurityStatus(
                    isInBackground = false,
                    isProtected = false,
                    recommendations = listOf("⚠️ Background security check failed")
                ),
                recommendations = listOf("⚠️ Security check failed - proceeding with caution")
            )
        }
    }
    
    fun enableSecurityProtection(activity: Activity) {
        try {
            // Enable screen recording protection
            ScreenRecordingProtectionManager.enableScreenRecordingProtection(activity)
            
            // Enable background security
            AppBackgroundSecurityManager.enableBackgroundSecurity(activity)
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error enabling security protection: ${e.message}")
        }
    }
    
    fun disableSecurityProtection(activity: Activity) {
        try {
            // Disable screen recording protection
            ScreenRecordingProtectionManager.disableScreenRecordingProtection(activity)
            
            // Disable background security
            AppBackgroundSecurityManager.disableBackgroundSecurity(activity)
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error disabling security protection: ${e.message}")
        }
    }
    
    fun shouldBlockApp(context: Context): Boolean {
        return try {
            val rootDetection = RootDetectionManager.shouldBlockApp(context)
            val debugBlock = DebugPreventionManager.shouldBlockExecution(context)
            
            rootDetection || debugBlock
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error in shouldBlockApp: ${e.message}")
            false
        }
    }
    
    fun initializeSecurity(context: Context) {
        try {
            // Initialize background security
            AppBackgroundSecurityManager.initializeBackgroundSecurity(context)
            
            // Set up screen recording detection
            ScreenRecordingProtectionManager.setRecordingDetectionCallback { isRecording ->
                performSecurityCheck(context)
            }
            
            // Set up background security callback
            AppBackgroundSecurityManager.setBackgroundSecurityCallback { isBackground ->
                performSecurityCheck(context)
            }
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error initializing security: ${e.message}")
        }
    }
    
    fun getSecurityRecommendations(context: Context): List<String> {
        return try {
            val status = performSecurityCheck(context)
            status.recommendations
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error getting security recommendations: ${e.message}")
            listOf("⚠️ Security check failed - proceeding with caution")
        }
    }
    
    fun isEncryptionAvailable(): Boolean {
        return try {
            EncryptionManager.isEncryptionAvailable()
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error checking encryption availability: ${e.message}")
            false
        }
    }
    
    fun encryptString(text: String): String {
        return try {
            EncryptionManager.encryptString(text)
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error encrypting string: ${e.message}")
            text // Return original text if encryption fails
        }
    }
    
    fun decryptString(encryptedText: String): String {
        return try {
            EncryptionManager.decryptString(encryptedText)
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error decrypting string: ${e.message}")
            encryptedText // Return encrypted text if decryption fails
        }
    }
    
    fun generateSecurePassword(length: Int = 16, includeSpecial: Boolean = true): String {
        return try {
            EncryptionManager.generateSecurePassword(length, includeSpecial)
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error generating secure password: ${e.message}")
            "SecurePassword123!" // Fallback password
        }
    }
    
    private fun getAllRecommendations(
        context: Context,
        rootDetection: RootDetectionResult,
        debugStatus: DebugSecurityStatus,
        screenRecording: Boolean,
        backgroundSecurity: BackgroundSecurityStatus
    ): List<String> {
        return try {
            val recommendations = mutableListOf<String>()
            
            // Root detection recommendations
            if (rootDetection.isRooted) {
                recommendations.addAll(RootDetectionManager.getSecurityRecommendations(context))
            }
            
            // Debug status recommendations
            if (debugStatus.isCompromised) {
                recommendations.addAll(debugStatus.recommendations)
            }
            
            // Screen recording recommendations
            if (screenRecording) {
                recommendations.add("🚨 Screen recording detected")
                recommendations.add("🔒 Sensitive content is hidden")
            }
            
            // Background security recommendations
            if (backgroundSecurity.isInBackground) {
                recommendations.add("🔒 App is in background")
                recommendations.add("🛡️ Content is protected")
            }
            
            // Overall security status
            if (recommendations.isEmpty()) {
                recommendations.add("✅ All security checks passed")
                recommendations.add("🔒 Device is secure")
            } else {
                recommendations.add("⚠️ Security concerns detected")
                recommendations.add("🔐 Enable additional security measures")
            }
            
            recommendations
        } catch (e: Exception) {
            Log.w("SecurityManager", "Error getting recommendations: ${e.message}")
            listOf("⚠️ Security check failed - proceeding with caution")
        }
    }
}

data class SecurityStatus(
    val isCompromised: Boolean,
    val rootDetection: RootDetectionResult,
    val debugStatus: DebugSecurityStatus,
    val screenRecording: Boolean,
    val backgroundSecurity: BackgroundSecurityStatus,
    val recommendations: List<String>
)
