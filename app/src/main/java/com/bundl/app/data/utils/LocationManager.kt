package com.bundl.app.data.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "LocationManager"
    
    // Default location (Bangalore)
    private val defaultLatitude = 12.9716
    private val defaultLongitude = 77.5946
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val _currentLocation = MutableStateFlow(
        LocationData(defaultLatitude, defaultLongitude, false)
    )
    val currentLocation: StateFlow<LocationData> = _currentLocation
    
    private var locationCallback: LocationCallback? = null
    private var locationUpdatesActive = false
    
    init {
        // Try to get last known location when initialized
        tryGetLastLocation()
    }
    
    /**
     * Check if we have location permissions
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || 
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Try to get the last known location
     */
    fun tryGetLastLocation() {
        if (!hasLocationPermission()) {
            Log.d(tag, "Cannot get location - no permission")
            return
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    updateLocation(it)
                    Log.d(tag, "Last location: ${it.latitude}, ${it.longitude}")
                } ?: run {
                    Log.d(tag, "Last location is null, will start location updates")
                    startLocationUpdates()
                }
            }.addOnFailureListener { e ->
                Log.e(tag, "Error getting last location", e)
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Security exception when getting last location", e)
        }
    }
    
    /**
     * Start receiving location updates
     */
    fun startLocationUpdates() {
        if (locationUpdatesActive || !hasLocationPermission()) {
            return
        }
        
        try {
            val locationRequest = LocationRequest.Builder(10000) // 10 seconds
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(5000) // 5 seconds
                .build()
            
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        updateLocation(location)
                    }
                }
            }
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            
            locationUpdatesActive = true
            Log.d(tag, "Started location updates")
        } catch (e: SecurityException) {
            Log.e(tag, "Security exception when requesting location updates", e)
        }
    }
    
    /**
     * Stop receiving location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationUpdatesActive = false
            Log.d(tag, "Stopped location updates")
        }
    }
    
    /**
     * Update the current location
     */
    private fun updateLocation(location: Location) {
        _currentLocation.value = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            isFromUser = true
        )
    }
    
    /**
     * Manually update location (used by background service)
     */
    fun updateLocationManually(locationData: LocationData) {
        _currentLocation.value = locationData
    }
    
    /**
     * Cleanup when no longer needed
     */
    fun cleanup() {
        stopLocationUpdates()
    }
    
    /**
     * Data class to hold location information
     */
    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val isFromUser: Boolean
    )
} 