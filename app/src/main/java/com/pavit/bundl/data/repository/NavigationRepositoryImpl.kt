package com.pavit.bundl.data.repository

import android.content.Context
import com.pavit.bundl.data.local.TokenManager
import com.pavit.bundl.data.local.UserPreferencesManager
import com.pavit.bundl.domain.repository.NavigationRepository
import com.pavit.bundl.data.permissions.PermissionHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationRepositoryImpl @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : NavigationRepository {
    
    override fun hasSeenOnboarding(): Flow<Boolean> {
        return userPreferencesManager.hasSeenOnboarding()
    }
    
    override suspend fun markOnboardingAsSeen() {
        userPreferencesManager.markOnboardingAsSeen()
    }
    
    override fun isLoggedIn(): Flow<Boolean> {
        return tokenManager.isLoggedIn()
    }
    
    override fun hasLocationPermissions(): Boolean {
        // Check basic location permissions - foreground service handles background access!
        return PermissionHandler.hasPermissions(context, PermissionHandler.locationPermissions)
    }
    
    override fun hasNotificationPermissions(): Boolean {
        return PermissionHandler.hasPermissions(context, PermissionHandler.notificationPermissions)
    }
}
