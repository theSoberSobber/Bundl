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
import com.orvio.app.data.local.TokenManager
import com.orvio.app.data.remote.api.ApiKeyService
import com.orvio.app.data.remote.api.AuthApiService
import com.orvio.app.data.remote.dto.DeviceRegisterRequestDto
import com.orvio.app.domain.repository.AuthRepository
import com.orvio.app.presentation.MainActivity
import com.orvio.app.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class OrvioFirebaseMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var apiKeyService: ApiKeyService
    
    @Inject
    lateinit var authApiService: AuthApiService
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    @Inject
    lateinit var deviceUtils: DeviceUtils
    
    @Inject
    lateinit var authRepository: AuthRepository
    
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
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: ${token.take(10)}...")
        
        // Register the new token with our backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Always try to register the token - no need to check tokens
                // Get the phone number for registration
                val phoneNumber = deviceUtils.getPhoneNumber() ?: ""
                
                // Register with backend using phoneNumber and fcmToken
                authApiService.registerDevice(DeviceRegisterRequestDto(token, phoneNumber))
                
                Log.d(TAG, "Successfully registered new FCM token with backend")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token with backend", e)
                
                // If we get a 403, let the interceptor handle the logout
                if (e.message?.contains("403") == true) {
                    Log.d(TAG, "Got 403 during token registration")
                    // AuthInterceptor will handle the logout if needed
                }
            }
        }
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
        val message = remoteMessage.data["message"]

        if (message != null){
            showToast("Server message found: $message")
        }
        
        when (type) {
            "OTP" -> {
                Log.d(TAG, "Handling OTP message")
                showToast("Handling OTP message")
                handleOtpMessage(otp, phoneNumber, timestamp, tid, message)
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
    
    private fun handleOtpMessage(otp: String?, phoneNumber: String?, timestamp: String?, tid: String?, message: String?) {
        if (message == null || phoneNumber == null || tid == null) {
            val errorMsg = "Missing required fields in OTP message: otp=$otp, phone=$phoneNumber, timestamp=$timestamp, tid=$tid"
            Log.e(TAG, errorMsg)
            showToast(errorMsg)
            return
        }
        
        try {
            Log.d(TAG, "Processing OTP message with timestamp: $timestamp")
            
            // Get the SMS manager
            val smsManager = SmsManager.getDefault()
            
            // Split the message into chunks if it's too long
            val messageParts = smsManager.divideMessage(message)
            
            if (messageParts.size > 1) {
                Log.d(TAG, "Message is too long, splitting into ${messageParts.size} parts")
                
                // Create a list of PendingIntents for each part
                val sentIntents = ArrayList<PendingIntent>()
                val deliveryIntents = ArrayList<PendingIntent>()
                
                // Create a unique request code for each part
                val requestCode = System.currentTimeMillis().toInt()
                
                // Create sent and delivery intents for each part
                for (i in messageParts.indices) {
                    val sentIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode + i,
                        Intent("SMS_SENT").apply {
                            putExtra("part", i)
                            putExtra("total", messageParts.size)
                            putExtra("tid", tid)
                        },
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                    sentIntents.add(sentIntent)
                    
                    val deliveryIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode + i + messageParts.size,
                        Intent("SMS_DELIVERED").apply {
                            putExtra("part", i)
                            putExtra("total", messageParts.size)
                            putExtra("tid", tid)
                        },
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                    deliveryIntents.add(deliveryIntent)
                }
                
                // Send the multipart message
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    messageParts,
                    sentIntents,
                    deliveryIntents
                )
                Log.d(TAG, "Multipart SMS sent successfully to $phoneNumber")
                showToast("Multipart SMS sent successfully to $phoneNumber")
            } else {
                // Message is short enough to send in one part
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d(TAG, "Single part SMS sent successfully to $phoneNumber")
                showToast("SMS sent successfully to $phoneNumber")
            }
            
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