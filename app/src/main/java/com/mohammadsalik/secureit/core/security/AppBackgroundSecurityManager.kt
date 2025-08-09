package com.mohammadsalik.secureit.core.security

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner

object AppBackgroundSecurityManager {
    
    private var isAppInBackground = false
    private var backgroundSecurityCallback: ((Boolean) -> Unit)? = null
    
    fun setBackgroundSecurityCallback(callback: (Boolean) -> Unit) {
        backgroundSecurityCallback = callback
    }
    
    fun initializeBackgroundSecurity(context: Context) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onAppBackgrounded() {
                isAppInBackground = true
                backgroundSecurityCallback?.invoke(true)
            }
            
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onAppForegrounded() {
                isAppInBackground = false
                backgroundSecurityCallback?.invoke(false)
            }
        })
    }
    
    fun hideContentWhenBackgrounded(activity: Activity) {
        if (isAppInBackground) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }
    
    fun showContentWhenForegrounded(activity: Activity) {
        if (!isAppInBackground) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    
    fun isAppInBackground(): Boolean {
        return isAppInBackground
    }
    
    fun enableBackgroundSecurity(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
    
    fun disableBackgroundSecurity(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    
    fun getBackgroundSecurityStatus(): BackgroundSecurityStatus {
        return BackgroundSecurityStatus(
            isInBackground = isAppInBackground,
            isProtected = isAppInBackground,
            recommendations = getRecommendations(isAppInBackground)
        )
    }
    
    private fun getRecommendations(isInBackground: Boolean): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (isInBackground) {
            recommendations.add("🔒 App is in background")
            recommendations.add("🛡️ Sensitive content is hidden")
            recommendations.add("🔐 Return to app to view content")
        } else {
            recommendations.add("✅ App is in foreground")
            recommendations.add("🔓 Full access to content")
        }
        
        return recommendations
    }
}

data class BackgroundSecurityStatus(
    val isInBackground: Boolean,
    val isProtected: Boolean,
    val recommendations: List<String>
)
