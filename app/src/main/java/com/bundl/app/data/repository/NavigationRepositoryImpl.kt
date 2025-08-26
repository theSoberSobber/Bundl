package com.bundl.app.data.repository

import android.content.Context
import com.bundl.app.data.local.TokenManager
import com.bundl.app.data.local.UserPreferencesManager
import com.bundl.app.domain.repository.NavigationRepository
import com.bundl.app.utils.PermissionHandler
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
        // Check basic location permissions
        val hasBasicLocation = PermissionHandler.hasPermissions(context, PermissionHandler.locationPermissions)
        
        // For Android 10+, also check background location
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            hasBasicLocation && PermissionHandler.hasPermissions(context, PermissionHandler.backgroundLocationPermissions)
        } else {
            // Android 9 and below - basic location is enough
            hasBasicLocation
        }
    }
    
    override fun hasNotificationPermissions(): Boolean {
        return PermissionHandler.hasPermissions(context, PermissionHandler.notificationPermissions)
    }
}
