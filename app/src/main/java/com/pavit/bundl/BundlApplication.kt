package com.pavit.bundl

import android.app.Application
import android.util.Log
import com.pavit.bundl.constants.RevenueCatConstants
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Bundl app.
 * Uses Hilt for dependency injection.
 */
@HiltAndroidApp
class BundlApplication : Application() {
    
    companion object {
        private const val TAG = "BundlApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeRevenueCat()
    }
    
    private fun initializeRevenueCat() {
        try {
            Log.d(TAG, "Initializing RevenueCat SDK...")
            
            val apiKey = RevenueCatConstants.API_KEY
            
            // Check if API key is properly configured
            if (apiKey == "your_revenuecat_api_key_here" || apiKey.isBlank()) {
                Log.e(TAG, "RevenueCat API key not configured! Please check REVENUECAT_SETUP.md for setup instructions")
                return
            }
            
            Log.d(TAG, "Using RevenueCat API key: ${apiKey.take(10)}...")
            
            // Configure RevenueCat with basic settings
            Purchases.logLevel = com.revenuecat.purchases.LogLevel.DEBUG
            Purchases.configure(
                PurchasesConfiguration.Builder(this, apiKey)
                    .showInAppMessagesAutomatically(true) // Show Google Play messages for grace periods
                    .build()
            )
            
            Log.i(TAG, "RevenueCat SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RevenueCat SDK", e)
            // Don't crash the app, but log the error
        }
    }
} 