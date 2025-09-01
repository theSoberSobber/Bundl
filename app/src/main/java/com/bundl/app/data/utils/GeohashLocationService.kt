package com.bundl.app.data.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * GeohashLocationService - Manages location-based geohash subscriptions for FCM
 * 
 * This service divides the world into geohash squares and subscribes users to
 * FCM topics based on their current location for receiving nearby orders.
 * 
 * Features:
 * - Automatic geohash calculation from location
 * - Multi-precision geohash coverage for optimal nearby detection
 * - FCM topic subscription management
 * - Dynamic subscription updates based on location changes
 * - Performance optimized for Android
 */
@Singleton
class GeohashLocationService @Inject constructor() {
    
    private val tag = "GeohashLocationService"
    
    // Configuration constants for 200m precision targeting
    private val GEOHASH_PRECISION_LEVELS = listOf(7) // ~153m accuracy (perfect for 200m radius)
    private val MAX_RADIUS_METERS = 200.0 // 200m maximum search radius as requested
    private val LOCATION_UPDATE_THRESHOLD_METERS = 50.0 // Update subscriptions if user moves 50m (more sensitive)
    
    // Alternative configurations (uncomment to use):
    // For ultra-precise 100m targeting:
    // private val GEOHASH_PRECISION_LEVELS = listOf(7, 8) // ~153m + ~38m accuracy
    // private val MAX_RADIUS_METERS = 100.0
    
    // For even more precise 50m targeting:
    // private val GEOHASH_PRECISION_LEVELS = listOf(8) // ~38m accuracy  
    // private val MAX_RADIUS_METERS = 50.0
    
    // State management
    private val _currentGeohashes = MutableStateFlow<Set<String>>(emptySet())
    val currentGeohashes: StateFlow<Set<String>> = _currentGeohashes
    
    private val _subscriptionStatus = MutableStateFlow(SubscriptionStatus.IDLE)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus
    
    private var lastSubscribedLocation: LocationData? = null
    private var currentSubscribedTopics: Set<String> = emptySet()
    
    /**
     * Update geohash subscriptions based on current location
     */
    suspend fun updateLocationSubscriptions(locationData: LocationManager.LocationData): Result<Unit> {
        return try {
            // Check if we need to update subscriptions
            if (!shouldUpdateSubscriptions(locationData)) {
                Log.d(tag, "Location change too small, skipping subscription update")
                return Result.success(Unit)
            }
            
            _subscriptionStatus.value = SubscriptionStatus.UPDATING
            
            // Calculate geohashes for current location
            val newGeohashes = calculateGeohashesForLocation(
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                radiusMeters = MAX_RADIUS_METERS
            )
            
            Log.d(tag, "Calculated ${newGeohashes.size} geohashes for location: ${locationData.latitude}, ${locationData.longitude}")
            
            // Update FCM subscriptions
            updateFCMSubscriptions(newGeohashes)
            
            // Update state
            _currentGeohashes.value = newGeohashes
            lastSubscribedLocation = LocationData(
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                isFromUser = locationData.isFromUser
            )
            
            _subscriptionStatus.value = SubscriptionStatus.SUBSCRIBED
            Log.i(tag, "Successfully updated location subscriptions with ${newGeohashes.size} geohashes")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error updating location subscriptions", e)
            _subscriptionStatus.value = SubscriptionStatus.ERROR
            Result.failure(e)
        }
    }
    
    /**
     * Calculate geohashes covering a circular area around the given location
     */
    private fun calculateGeohashesForLocation(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double
    ): Set<String> {
        val geohashes = mutableSetOf<String>()
        
        // For each precision level, calculate coverage
        for (precision in GEOHASH_PRECISION_LEVELS) {
            val precisionGeohashes = GeohashUtils.getCoverageGeohashes(
                centerLat = latitude,
                centerLon = longitude,
                radiusMeters = radiusMeters,
                precision = precision
            )
            geohashes.addAll(precisionGeohashes)
        }
        
        return geohashes
    }
    
    /**
     * Calculate geohashes for a specific precision level
     */
    private fun calculateGeohashesForPrecision(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        precision: Int
    ): Set<String> {
        // Use our custom GeohashUtils for coverage calculation
        return GeohashUtils.getCoverageGeohashes(
            centerLat = latitude,
            centerLon = longitude,
            radiusMeters = radiusMeters,
            precision = precision
        )
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return GeohashUtils.distanceMeters(lat1, lon1, lat2, lon2)
    }
    
    /**
     * Update FCM topic subscriptions
     */
    private suspend fun updateFCMSubscriptions(newGeohashes: Set<String>) {
        val messaging = FirebaseMessaging.getInstance()
        
        // Unsubscribe from old topics
        val topicsToUnsubscribe = currentSubscribedTopics - newGeohashes
        for (topic in topicsToUnsubscribe) {
            try {
                messaging.unsubscribeFromTopic("geohash_$topic").await()
                Log.d(tag, "Unsubscribed from topic: geohash_$topic")
            } catch (e: Exception) {
                Log.w(tag, "Failed to unsubscribe from topic: geohash_$topic", e)
            }
        }
        
        // Subscribe to new topics
        val topicsToSubscribe = newGeohashes - currentSubscribedTopics
        for (topic in topicsToSubscribe) {
            try {
                messaging.subscribeToTopic("geohash_$topic").await()
                Log.d(tag, "Subscribed to topic: geohash_$topic")
            } catch (e: Exception) {
                Log.w(tag, "Failed to subscribe to topic: geohash_$topic", e)
                throw e
            }
        }
        
        // Update current subscriptions
        currentSubscribedTopics = newGeohashes
        Log.i(tag, "FCM subscription update complete. Total subscriptions: ${currentSubscribedTopics.size}")
    }
    
    /**
     * Check if subscriptions should be updated based on location change
     */
    private fun shouldUpdateSubscriptions(newLocation: LocationManager.LocationData): Boolean {
        val lastLocation = lastSubscribedLocation ?: return true
        
        val distance = calculateDistance(
            lat1 = lastLocation.latitude,
            lon1 = lastLocation.longitude,
            lat2 = newLocation.latitude,
            lon2 = newLocation.longitude
        )
        
        return distance >= LOCATION_UPDATE_THRESHOLD_METERS
    }
    
    /**
     * Get human-readable info about current geohash subscriptions
     */
    fun getSubscriptionInfo(): SubscriptionInfo {
        return SubscriptionInfo(
            totalTopics = currentSubscribedTopics.size,
            precisionLevels = GEOHASH_PRECISION_LEVELS,
            maxRadius = MAX_RADIUS_METERS,
            lastLocation = lastSubscribedLocation,
            status = _subscriptionStatus.value
        )
    }
    
    /**
     * Clean up all subscriptions (call when user logs out)
     */
    suspend fun cleanup() {
        try {
            val messaging = FirebaseMessaging.getInstance()
            
            for (topic in currentSubscribedTopics) {
                messaging.unsubscribeFromTopic("geohash_$topic").await()
            }
            
            currentSubscribedTopics = emptySet()
            _currentGeohashes.value = emptySet()
            _subscriptionStatus.value = SubscriptionStatus.IDLE
            lastSubscribedLocation = null
            
            Log.i(tag, "Successfully cleaned up all geohash subscriptions")
        } catch (e: Exception) {
            Log.e(tag, "Error during cleanup", e)
        }
    }
    
    /**
     * Data classes
     */
    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val isFromUser: Boolean
    )
    
    data class SubscriptionInfo(
        val totalTopics: Int,
        val precisionLevels: List<Int>,
        val maxRadius: Double,
        val lastLocation: LocationData?,
        val status: SubscriptionStatus
    )
    
    enum class SubscriptionStatus {
        IDLE,           // Not subscribed to any topics
        UPDATING,       // Currently updating subscriptions  
        SUBSCRIBED,     // Successfully subscribed
        ERROR           // Error occurred during subscription
    }
}
