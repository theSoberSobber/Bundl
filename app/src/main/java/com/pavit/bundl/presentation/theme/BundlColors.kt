package com.pavit.bundl.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Bundl app color system
 * Centralized color definitions to avoid duplication across components
 */
object BundlColors {
    // Surface colors
    val SurfaceDark = Color(0xFF1C1C1C)           // Primary dark surface
    val SurfaceLight = Color(0xFF2C2C2C)          // Secondary dark surface  
    val SurfaceAccent = Color(0xFF3D3D3D)         // Accent surface (hover states)
    val SurfaceBanner = Color(0xFF242424)         // Banner area background
    
    // Text colors
    val TextPrimary = Color.White                 // Primary text on dark backgrounds
    val TextSecondary = Color(0xFFB0B0B0)         // Secondary/subtitle text
    val TextDisabled = Color.Gray                 // Disabled text
    
    // Component colors
    val ButtonDisabled = Color(0xFF2C2C2C)        // Disabled button background
    val ProgressTrack = Color(0xFF4D4D4D)         // Progress indicator track
    val DividerColor = Color(0xFF444444)          // Dividers and separators
    
    // Avatar colors for customer profile pictures  
    val GoogleBlue = Color(0xFF4285F4)
    val GoogleRed = Color(0xFFDB4437)
    val GoogleYellow = Color(0xFFF4B400)
    val GoogleGreen = Color(0xFF0F9D58)
    val Purple = Color(0xFF7B1FA2)
    
    // Vehicle type colors
    val VehicleAuto = Color(0xFFFBC02D)           // Yellow for auto
    val VehicleMoto = Color(0xFFFF7043)           // Orange for motorcycle
    
    // Status colors
    val StatusSuccess = Color(0xFF4CAF50)         // Material Green
    val StatusError = Color(0xFFE57373)           // Material Red
    
    // Switch colors
    val SwitchTrack = Color(0xFF4D4D4D)
    
    // Common radius values
    object Radius {
        val Small = 4
        val Medium = 8  
        val Large = 12
        val Button = 8
        val Card = 8
        val Sheet = 16
    }
    
    // Common spacing values
    object Spacing {
        val ExtraSmall = 4
        val Small = 8
        val Medium = 12
        val Large = 16
        val ExtraLarge = 24
    }
}
