package com.mohammadsalik.secureit.core.security

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.view.WindowManager
import android.os.Build

object ScreenRecordingProtectionManager {
    
    private var isRecordingDetected = false
    private var recordingDetectionCallback: ((Boolean) -> Unit)? = null
    
    fun setRecordingDetectionCallback(callback: (Boolean) -> Unit) {
        recordingDetectionCallback = callback
    }
    
    fun detectScreenRecording(context: Context): Boolean {
        val isRecording = try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            runningServices.any { service ->
                service.service.className.contains("MediaProjection") ||
                service.service.className.contains("ScreenCapture") ||
                service.service.className.contains("Recording")
            }
        } catch (e: Exception) {
            false
        }
        
        if (isRecording != isRecordingDetected) {
            isRecordingDetected = isRecording
            recordingDetectionCallback?.invoke(isRecording)
        }
        
        return isRecording
    }
    
    fun enableScreenRecordingProtection(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
    
    fun disableScreenRecordingProtection(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    
    fun hideSensitiveContent(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
    
    fun showSensitiveContent(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    
    fun isScreenRecordingProtected(): Boolean {
        return isRecordingDetected
    }
}

data class ScreenRecordingStatus(
    val isRecording: Boolean,
    val isProtected: Boolean,
    val recommendations: List<String>
)
