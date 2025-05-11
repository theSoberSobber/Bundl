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
        private const val TAG = "BundlFCMService"
        private const val CHANNEL_ID = "bundl_notifications"
        private const val NOTIFICATION_ID = 1
    }
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: ${token.take(10)}...")
        
        // Update the FCM token on the server
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Send the new token to the backend
                val request = mapOf("fcmToken" to token)
                val response = authApiService.updateFcmToken(request)
                
                Log.d(TAG, "FCM token update response: $response")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token: ${e.message}")
                
                // If we get a 403, let the interceptor handle the logout
                if (e.message?.contains("403") == true) {
                    Log.d(TAG, "Got 403 during token update")
                    // AuthInterceptor will handle the logout if needed
                }
            }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Only handle notification messages
        if (remoteMessage.notification != null) {
            Log.d(TAG, "Notification Message received: ${remoteMessage.notification}")
            showNotification(remoteMessage.notification!!)
        }
    }
    
    private fun showNotification(notification: RemoteMessage.Notification) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bundl Notifications"
            val descriptionText = "Notifications for Bundl app"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 