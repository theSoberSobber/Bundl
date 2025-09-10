package com.pavit.bundl.constants

import com.pavit.bundl.BuildConfig

object RevenueCatConstants {
    // RevenueCat API key loaded from local.properties via BuildConfig
    const val API_KEY = BuildConfig.REVENUECAT_API_KEY
    
    // Product IDs - these should match your RevenueCat configuration
    const val PRODUCT_5_CREDITS = "bundle_5_credits"
    const val PRODUCT_10_CREDITS = "bundle_10_credits"  
    const val PRODUCT_20_CREDITS = "bundle_20_credits"
}
