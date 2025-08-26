package com.bundl.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for navigation-related data access
 * This abstracts away the data layer from navigation logic
 */
interface NavigationRepository {
    
    /**
     * Check if user has seen onboarding
     */
    fun hasSeenOnboarding(): Flow<Boolean>
    
    /**
     * Mark onboarding as completed
     */
    suspend fun markOnboardingAsSeen()
    
    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Flow<Boolean>
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(): Boolean
    
    /**
     * Check if notification permissions are granted
     */
    fun hasNotificationPermissions(): Boolean
}
