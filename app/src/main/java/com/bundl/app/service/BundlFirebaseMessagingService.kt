package com.bundl.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.bundl.app.R
import com.bundl.app.data.local.TokenManager
import com.bundl.app.data.remote.api.AuthApiService
import com.bundl.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BundlFirebaseMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var authApiService: AuthApiService
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    companion object {
        private const val TAG = "BUNDL_FCM"
        private const val CHANNEL_ID = "bundl_notifications"
        private const val NOTIFICATION_ID = 1
    }
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FCM Service created")
        createNotificationChannel()
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: ${token.take(10)}...")
        
        // Update the FCM token on the server
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Send the new token to the backend
                val request = mapOf("fcmToken" to token)
                val response = authApiService.updateFcmToken(request)
                
                Log.d(TAG, "FCM token update successful: $response")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token: ${e.message}")
                
                // If we get a 403, let the interceptor handle the logout
                if (e.message?.contains("403") == true) {
                    Log.d(TAG, "Got 403 during token update, will be handled by interceptor")
                }
            }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Log all data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }
        
        // Check if this is a geohash-based order notification
        val isGeohashNotification = remoteMessage.from?.startsWith("/topics/geohash_") == true
        if (isGeohashNotification) {
            handleGeohashOrderNotification(remoteMessage)
        } else {
            // Handle regular notification
            remoteMessage.notification?.let { notification ->
                Log.d(TAG, "Message Notification Title: ${notification.title}")
                Log.d(TAG, "Message Notification Body: ${notification.body}")
                showNotification(notification)
            }
        }
    }
    
    private fun handleGeohashOrderNotification(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Handling geohash-based order notification")
        
        val data = remoteMessage.data
        val geohash = data["geohash"]
        val orderType = data["orderType"]
        val estimatedEarnings = data["estimatedEarnings"]
        val distance = data["distance"]
        val title = data["title"] ?: "New Order Nearby"
        val body = data["body"] ?: "A new delivery order is available in your area"
        
        Log.d(TAG, "Geohash order details - Geohash: $geohash, Type: $orderType, Earnings: $estimatedEarnings, Distance: $distance")
        
        // Create enhanced notification for nearby orders
        showGeohashOrderNotification(
            title = title,
            body = body,
            orderType = orderType,
            estimatedEarnings = estimatedEarnings,
            distance = distance
        )
    }
    
    private fun showNotification(notification: RemoteMessage.Notification) {
        Log.d(TAG, "Showing notification - Title: ${notification.title}, Body: ${notification.body}")
        
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        Log.d(TAG, "Notification displayed successfully")
    }
    
    private fun showGeohashOrderNotification(
        title: String,
        body: String,
        orderType: String?,
        estimatedEarnings: String?,
        distance: String?
    ) {
        Log.d(TAG, "Showing geohash order notification - Title: $title, Body: $body")
        
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Add extra data to navigate to orders or map
            putExtra("notification_type", "nearby_order")
            putExtra("order_type", orderType)
            putExtra("estimated_earnings", estimatedEarnings)
            putExtra("distance", distance)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create enhanced notification with action buttons
        val enhancedBody = buildString {
            append(body)
            if (estimatedEarnings != null) {
                append("\n💰 Earnings: ₹$estimatedEarnings")
            }
            if (distance != null) {
                append("\n📍 Distance: ${distance}km")
            }
            if (orderType != null) {
                append("\n📦 Type: $orderType")
            }
        }
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(enhancedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(enhancedBody))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(
                R.drawable.ic_location, 
                "View", 
                pendingIntent
            )
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use different notification ID for order notifications
        notificationManager.notify(NOTIFICATION_ID + 1, notificationBuilder.build())
        Log.d(TAG, "Geohash order notification displayed successfully")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bundl Notifications"
            val descriptionText = "Notifications for Bundl app"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
} 