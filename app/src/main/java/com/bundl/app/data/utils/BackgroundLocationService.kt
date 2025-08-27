package com.bundl.app.data.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

/**
 * BackgroundLocationService - Provides continuous location tracking and geohash subscription updates
 * 
 * This service runs in the background to:
 * - Track user location continuously
 * - Update geohash subscriptions when location changes significantly
 * - Maintain FCM topic subscriptions for nearby orders
 * - Handle location permission changes gracefully
 */
@AndroidEntryPoint
class BackgroundLocationService : Service() {
    
    private val tag = "BackgroundLocationService"
    
    companion object {
        const val NOTIFICATION_ID = 12345
        const val CHANNEL_ID = "location_service_channel"
        
        // Intent actions
        const val ACTION_START_LOCATION_TRACKING = "START_LOCATION_TRACKING"
        const val ACTION_STOP_LOCATION_TRACKING = "STOP_LOCATION_TRACKING"
        
        fun startLocationTracking(context: Context) {
            val intent = Intent(context, BackgroundLocationService::class.java).apply {
                action = ACTION_START_LOCATION_TRACKING
            }
            context.startForegroundService(intent)
        }
        
        fun stopLocationTracking(context: Context) {
            val intent = Intent(context, BackgroundLocationService::class.java).apply {
                action = ACTION_STOP_LOCATION_TRACKING
            }
            context.stopService(intent)
        }
    }
    
    @Inject
    lateinit var locationManager: LocationManager
    
    @Inject
    lateinit var geohashLocationService: GeohashLocationService
    
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var serviceJob: Job? = null
    private var isTrackingLocation = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "BackgroundLocationService created")
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATION_TRACKING -> startLocationTracking()
            ACTION_STOP_LOCATION_TRACKING -> stopLocationTracking()
        }
        return START_STICKY // Restart service if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startLocationTracking() {
        if (isTrackingLocation) {
            Log.d(tag, "Location tracking already active")
            return
        }
        
        if (!hasLocationPermission()) {
            Log.w(tag, "Cannot start location tracking - no permission")
            stopSelf()
            return
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceJob = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            startLocationUpdates()
            observeLocationChanges()
        }
        
        isTrackingLocation = true
        Log.i(tag, "Started background location tracking")
    }
    
    private fun stopLocationTracking() {
        isTrackingLocation = false
        
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
        
        serviceJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        Log.i(tag, "Stopped background location tracking")
    }
    
    private suspend fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                15_000 // 15 seconds for higher precision tracking
            )
                .setMinUpdateIntervalMillis(10_000) // 10 seconds minimum
                .setMaxUpdateDelayMillis(30_000) // 30 seconds maximum delay  
                .build()
            
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        val locationData = LocationManager.LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isFromUser = true
                        )
                        
                        // Update location manager
                        locationManager.updateLocationManually(locationData)
                        
                        Log.d(tag, "Background location update: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                fusedLocationClient?.requestLocationUpdates(
                    locationRequest,
                    locationCallback!!,
                    Looper.getMainLooper()
                )
            }
            
            Log.d(tag, "Location updates started")
        } catch (e: SecurityException) {
            Log.e(tag, "Security exception when requesting location updates", e)
        }
    }
    
    private suspend fun observeLocationChanges() {
        locationManager.currentLocation.collect { locationData ->
            if (locationData.isFromUser && isTrackingLocation) {
                // Update geohash subscriptions based on new location
                val result = geohashLocationService.updateLocationSubscriptions(locationData)
                
                if (result.isSuccess) {
                    updateNotification("") // The updateNotification function now handles the text internally
                    Log.d(tag, "Geohash subscriptions updated for location: ${locationData.latitude}, ${locationData.longitude}")
                } else {
                    Log.e(tag, "Failed to update geohash subscriptions", result.exceptionOrNull())
                    updateNotification("") // Still call updateNotification to refresh the display
                }
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location to show nearby orders"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Listening for Orders Nearby")
        .setContentText("Monitoring your area for new orders")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .setSilent(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()
    
    private fun updateNotification(text: String) {
        val geohashCount = geohashLocationService.currentGeohashes.value.size
        val notificationText = if (geohashCount > 0) {
            "Listening on $geohashCount areas"
        } else {
            "Setting up nearby monitoring..."
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Listening for Orders Nearby")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun formatLocation(location: LocationManager.LocationData): String {
        return "%.4f, %.4f".format(location.latitude, location.longitude)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        Log.d(tag, "BackgroundLocationService destroyed")
    }
}
