package com.mohammadsalik.secureit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecureVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any application-level configurations here
    }
} 