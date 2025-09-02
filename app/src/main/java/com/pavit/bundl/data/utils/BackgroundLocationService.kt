package com.pavit.bundl.data.utils

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
        
        Log.d(tag, "Starting location tracking")
        isTrackingLocation = true
        
        startForeground(NotificationConstants.LOCATION_NOTIFICATION_ID, createNotification())
        
        if (!hasLocationPermission()) {
            Log.e(tag, "Location permissions not granted")
            stopSelf()
            return
        }
        
        setupLocationUpdates()
        startGeohashMonitoring()
    }
    
    private fun stopLocationTracking() {
        Log.d(tag, "Stopping location tracking")
        isTrackingLocation = false
        
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        
        serviceJob?.cancel()
        stopForeground(true)
        stopSelf()
    }
    
    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30_000L // 30 seconds
        )
            .setMinUpdateIntervalMillis(10_000L) // 10 seconds minimum
            .setWaitForAccurateLocation(false)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation ?: return
                
                Log.d(tag, "Location update: ${location.latitude}, ${location.longitude}")
                
                // Update location in repository with LocationData
                val locationData = LocationManager.LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isFromUser = true
                )
                locationManager.updateLocationManually(locationData)
                
                // Update geohash subscriptions
                CoroutineScope(Dispatchers.IO).launch {
                    geohashLocationService.updateLocationSubscriptions(locationData)
                }
            }
        }
        
        if (hasLocationPermission()) {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        }
    }
    
    private fun startGeohashMonitoring() {
        serviceJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                geohashLocationService.currentGeohashes.collect { geohashes ->
                    Log.d(tag, "Geohash subscription updated: ${geohashes.size} areas")
                    updateNotification()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error monitoring geohashes", e)
                if (isTrackingLocation) {
                    updateNotification()
                }
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConstants.LOCATION_CHANNEL_ID,
                NotificationConstants.LOCATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = NotificationConstants.LOCATION_CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, NotificationConstants.LOCATION_CHANNEL_ID)
        .setContentTitle(NotificationConstants.NOTIFICATION_TITLE)
        .setContentText(NotificationConstants.NOTIFICATION_TEXT_DEFAULT)
        .setSmallIcon(NotificationConstants.NOTIFICATION_ICON)
        .setOngoing(true)
        .setSilent(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()
    
    private fun updateNotification() {
        val geohashCount = geohashLocationService.currentGeohashes.value.size
        val notificationText = NotificationConstants.getNotificationText(geohashCount)
        
        val notification = NotificationCompat.Builder(this, NotificationConstants.LOCATION_CHANNEL_ID)
            .setContentTitle(NotificationConstants.NOTIFICATION_TITLE)
            .setContentText(notificationText)
            .setSmallIcon(NotificationConstants.NOTIFICATION_ICON)
            .setOngoing(true)
            .setSilent(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationConstants.LOCATION_NOTIFICATION_ID, notification)
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "BackgroundLocationService destroyed")
        stopLocationTracking()
    }
}
