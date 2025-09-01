package com.bundl.app.domain.usecase.location

import com.bundl.app.data.utils.BackgroundLocationService
import com.bundl.app.data.utils.GeohashLocationService
import com.bundl.app.data.utils.LocationManager
import com.bundl.app.domain.usecase.base.UseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing geohash-based location tracking and FCM subscriptions
 */
@Singleton
class GeohashLocationUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationManager: LocationManager,
    private val geohashLocationService: GeohashLocationService
) {
    
    private val tag = "GeohashLocationUseCase"
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Start background location tracking with geohash subscriptions
     */
    fun startLocationTracking(): Result<Unit> {
        return try {
            // Start the background location service
            BackgroundLocationService.startLocationTracking(context)
            
            // Start observing location changes for immediate geohash subscription updates
            serviceScope.launch {
                locationManager.currentLocation.collect { locationData ->
                    if (locationData.isFromUser) {
                        updateGeohashSubscriptions(locationData)
                    }
                }
            }
            
            Log.i(tag, "Location tracking started successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start location tracking", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop background location tracking
     */
    fun stopLocationTracking(): Result<Unit> {
        return try {
            BackgroundLocationService.stopLocationTracking(context)
            Log.i(tag, "Location tracking stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop location tracking", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update geohash subscriptions for current location
     */
    suspend fun updateGeohashSubscriptions(locationData: LocationManager.LocationData): Result<Unit> {
        return geohashLocationService.updateLocationSubscriptions(locationData)
    }
    
    /**
     * Get current subscription information
     */
    fun getSubscriptionInfo(): GeohashLocationService.SubscriptionInfo {
        return geohashLocationService.getSubscriptionInfo()
    }
    
    /**
     * Get current geohashes StateFlow
     */
    fun getCurrentGeohashes() = geohashLocationService.currentGeohashes
    
    /**
     * Get subscription status StateFlow
     */
    fun getSubscriptionStatus() = geohashLocationService.subscriptionStatus
    
    /**
     * Clean up all subscriptions
     */
    suspend fun cleanup(): Result<Unit> {
        return try {
            stopLocationTracking()
            geohashLocationService.cleanup()
            Log.i(tag, "Cleanup completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error during cleanup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return locationManager.hasLocationPermission()
    }
}
