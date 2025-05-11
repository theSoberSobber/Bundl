package com.bundl.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BundlApplication : Application() {
    
    companion object {
        private const val TAG = "BundlApplication"
        private lateinit var instance: BundlApplication
        
        fun getInstance(): BundlApplication {
            return instance
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Set instance
        instance = this
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "Firebase initialized")
    }
} 