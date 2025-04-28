package com.orvio.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.orvio.app.R
import com.orvio.app.data.remote.api.ApiKeyService
import com.orvio.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class OrvioFirebaseMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var apiKeyService: ApiKeyService
    
    companion object {
        private const val TAG = "OrvioFCMService"
        private const val CHANNEL_ID = "orvio_notifications"
        private const val NOTIFICATION_ID = 1
    }
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received: ${remoteMessage.data}")
        showToast("FCM Message Received: ${remoteMessage.data}")
        
        // Check if message contains a notification payload
        if (remoteMessage.notification != null) {
            // Handle foreground message
            showNotification(remoteMessage.notification!!)
            return
        }
        
        // Handle data message
        val type = remoteMessage.data["type"]
        val otp = remoteMessage.data["otp"]
        val phoneNumber = remoteMessage.data["phoneNumber"]
        val timestamp = remoteMessage.data["timestamp"]
        val tid = remoteMessage.data["tid"]
        
        when (type) {
            "OTP" -> {
                Log.d(TAG, "Handling OTP message")
                showToast("Handling OTP message")
                handleOtpMessage(otp, phoneNumber, timestamp, tid)
            }
            "PING" -> {
                Log.d(TAG, "Handling PING message")
                showToast("Handling PING message")
                handlePingMessage(timestamp)
            }
            else -> {
                Log.d(TAG, "Unknown message type: $type")
                showToast("Unknown message type: $type")
            }
        }
    }
    
    private fun handleOtpMessage(otp: String?, phoneNumber: String?, timestamp: String?, tid: String?) {
        if (otp == null || phoneNumber == null || timestamp == null || tid == null) {
            val errorMsg = "Missing required fields in OTP message: otp=$otp, phone=$phoneNumber, timestamp=$timestamp, tid=$tid"
            Log.e(TAG, errorMsg)
            showToast(errorMsg)
            return
        }
        
        try {
            Log.d(TAG, "Processing OTP message with timestamp: $timestamp")
            // Parse ISO 8601 timestamp
            try {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(timestamp)
                
                if (date == null) {
                    throw IllegalArgumentException("Failed to parse timestamp: $timestamp")
                }
                
                val formattedTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
                
                // Send SMS
                val smsManager = SmsManager.getDefault()
                val message = "$otp. It was requested at $formattedTimestamp"
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d(TAG, "SMS sent successfully to $phoneNumber")
                showToast("SMS sent successfully to $phoneNumber")
                
                // Send acknowledgment
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        apiKeyService.serviceAck(mapOf("tid" to tid))
                        Log.d(TAG, "Acknowledgment sent for TID: $tid")
                        showToast("Acknowledgment sent for TID: $tid")
                    } catch (e: Exception) {
                        val ackError = "Failed to send acknowledgment: ${e.message}"
                        Log.e(TAG, ackError, e)
                        showToast(ackError)
                    }
                }
            } catch (e: Exception) {
                val parseError = "Invalid timestamp format. Raw timestamp: '$timestamp', Error: ${e.message}"
                Log.e(TAG, parseError, e)
                showToast(parseError)
            }
        } catch (e: Exception) {
            val errorMsg = "Error handling OTP message: ${e::class.simpleName}: ${e.message}"
            Log.e(TAG, errorMsg, e)
            showToast(errorMsg)
        }
    }
    
    private fun handlePingMessage(timestamp: String?) {
        // Handle ping message if needed
        Log.d(TAG, "Received ping at: $timestamp")
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
            val name = "Orvio Notifications"
            val descriptionText = "Notifications for Orvio app"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showToast(message: String) {
        mainHandler.post {
            // Use LONG duration for error messages
            val duration = if (message.contains("error", ignoreCase = true) || 
                             message.contains("failed", ignoreCase = true) || 
                             message.contains("invalid", ignoreCase = true)) {
                Toast.LENGTH_LONG
            } else {
                Toast.LENGTH_SHORT
            }
            Toast.makeText(this, message, duration).show()
        }
    }
} 