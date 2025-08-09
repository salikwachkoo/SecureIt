package com.mohammadsalik.secureit.core.security

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.util.Log
import java.io.File

object DebugPreventionManager {
    
    fun isDebuggerAttached(): Boolean {
        return try {
            Debug.isDebuggerConnected()
        } catch (e: Exception) {
            false
        }
    }
    
    fun isDebugModeEnabled(context: Context): Boolean {
        return try {
            val applicationInfo = context.applicationInfo
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    fun isEmulatorRunning(): Boolean {
        return try {
            val emulatorProps = listOf(
                "ro.kernel.qemu" to "1",
                "ro.hardware" to "goldfish",
                "ro.hardware" to "ranchu",
                "ro.hardware" to "vbox86",
                "ro.product.cpu.abi" to "x86",
                "ro.product.cpu.abi2" to "x86"
            )
            
            emulatorProps.any { (key, value) ->
                getSystemProperty(key) == value
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun isTamperedWith(context: Context): Boolean {
        return try {
            // Use the newer API for Android 9+ and fallback for older versions
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName, 
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName, 
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
            }
            
            // Check signatures based on API level
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                signingInfo?.apkContentsSigners?.any { cert ->
                    cert.toString().contains("Android Debug")
                } ?: false
            } else {
                @Suppress("DEPRECATION")
                val signatures = packageInfo.signatures
                signatures?.any { signature ->
                    signature.toCharsString().contains("Android Debug")
                } ?: false
            }
        } catch (e: Exception) {
            Log.w("DebugPrevention", "Error checking tampering: ${e.message}")
            false
        }
    }
    
    fun shouldBlockExecution(context: Context): Boolean {
        return try {
            isDebuggerAttached() || 
            isDebugModeEnabled(context) || 
            isEmulatorRunning() ||
            isTamperedWith(context)
        } catch (e: Exception) {
            Log.w("DebugPrevention", "Error in shouldBlockExecution: ${e.message}")
            false
        }
    }
    
    fun getSecurityStatus(context: Context): DebugSecurityStatus {
        return try {
            val debuggerAttached = isDebuggerAttached()
            val debugModeEnabled = isDebugModeEnabled(context)
            val emulatorRunning = isEmulatorRunning()
            val tamperedWith = isTamperedWith(context)
            
            val isCompromised = debuggerAttached || debugModeEnabled || emulatorRunning || tamperedWith
            
            DebugSecurityStatus(
                isCompromised = isCompromised,
                debuggerAttached = debuggerAttached,
                debugModeEnabled = debugModeEnabled,
                emulatorRunning = emulatorRunning,
                tamperedWith = tamperedWith,
                recommendations = getRecommendations(debuggerAttached, debugModeEnabled, emulatorRunning, tamperedWith)
            )
        } catch (e: Exception) {
            Log.w("DebugPrevention", "Error getting security status: ${e.message}")
            DebugSecurityStatus(
                isCompromised = false,
                debuggerAttached = false,
                debugModeEnabled = false,
                emulatorRunning = false,
                tamperedWith = false,
                recommendations = listOf("⚠️ Security check failed - proceeding with caution")
            )
        }
    }
    
    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val result = reader.readLine()
            reader.close()
            process.waitFor()
            result
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getRecommendations(
        debuggerAttached: Boolean,
        debugModeEnabled: Boolean,
        emulatorRunning: Boolean,
        tamperedWith: Boolean
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (debuggerAttached) {
            recommendations.add("🚨 Debugger is attached - Security compromised")
            recommendations.add("🛑 Remove debugger connection immediately")
        }
        
        if (debugModeEnabled) {
            recommendations.add("⚠️ Debug mode is enabled")
            recommendations.add("🔒 Disable debug mode for production")
        }
        
        if (emulatorRunning) {
            recommendations.add("⚠️ Running on emulator")
            recommendations.add("🔒 Use physical device for security")
        }
        
        if (tamperedWith) {
            recommendations.add("🚨 App appears to be tampered with")
            recommendations.add("🛑 Install from trusted source only")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("✅ Debug security checks passed")
        }
        
        return recommendations
    }
}

data class DebugSecurityStatus(
    val isCompromised: Boolean,
    val debuggerAttached: Boolean,
    val debugModeEnabled: Boolean,
    val emulatorRunning: Boolean,
    val tamperedWith: Boolean,
    val recommendations: List<String>
)
