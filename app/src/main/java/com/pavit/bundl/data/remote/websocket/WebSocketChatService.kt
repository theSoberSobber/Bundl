package com.pavit.bundl.data.remote.websocket

import android.util.Log
import com.google.gson.Gson
import com.pavit.bundl.data.local.TokenManager
import com.pavit.bundl.data.remote.dto.*
import com.pavit.bundl.domain.model.ConnectionState
import com.pavit.bundl.utils.network.NetworkConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketChatService @Inject constructor(
    private val tokenManager: TokenManager,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "WebSocketChatService"
        private const val RECONNECT_DELAY_MS = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _incomingMessages = MutableStateFlow<IncomingChatMessageDto?>(null)
    val incomingMessages: StateFlow<IncomingChatMessageDto?> = _incomingMessages.asStateFlow()
    
    private val _messageSentConfirmations = MutableStateFlow<String?>(null)
    val messageSentConfirmations: StateFlow<String?> = _messageSentConfirmations.asStateFlow()
    
    private val _orderUpdates = MutableStateFlow<OrderUpdateDto?>(null)
    val orderUpdates: StateFlow<OrderUpdateDto?> = _orderUpdates.asStateFlow()
    
    private val _participantUpdates = MutableStateFlow<ParticipantUpdateDto?>(null)
    val participantUpdates: StateFlow<ParticipantUpdateDto?> = _participantUpdates.asStateFlow()
    
    private val _connectionErrors = MutableStateFlow<String?>(null)
    val connectionErrors: StateFlow<String?> = _connectionErrors.asStateFlow()
    
    private val _userUsername = MutableStateFlow<String?>(null)
    val userUsername: StateFlow<String?> = _userUsername.asStateFlow()

    private var webSocket: WebSocket? = null
    private var currentUserId: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            _connectionState.value = ConnectionState.CONNECTED
            _connectionErrors.value = null // Clear any previous errors
            reconnectAttempts = 0
            reconnectJob?.cancel()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            handleIncomingMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
            attemptReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error", t)
            _connectionState.value = ConnectionState.ERROR_NETWORK
            attemptReconnect()
        }
    }

    fun connect(userId: String) {
        if (_connectionState.value == ConnectionState.CONNECTED || 
            _connectionState.value == ConnectionState.CONNECTING) {
            return
        }
        
        currentUserId = userId
        _connectionState.value = ConnectionState.CONNECTING
        
        scope.launch {
            try {
                tokenManager.getAccessToken().collect { token ->
                    if (token.isNullOrEmpty()) {
                        Log.e(TAG, "No access token available")
                        _connectionState.value = ConnectionState.ERROR_AUTH
                        return@collect
                    }

                    val request = Request.Builder()
                        .url(NetworkConfig.BASE_WS_URL)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("User-Id", userId)
                        .build()

                    webSocket = client.newWebSocket(request, webSocketListener)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect WebSocket", e)
                _connectionState.value = ConnectionState.ERROR_NETWORK
                attemptReconnect()
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun sendMessage(orderId: String, content: String, messageId: String) {
        val socket = webSocket
        if (socket == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot send message - not connected")
            return
        }

        // Send in NestJS @SubscribeMessage format
        val messageData = mapOf(
            "orderId" to orderId,
            "message" to content
        )
        
        val nestJSMessage = mapOf(
            "event" to "send_message",
            "data" to messageData
        )

        val success = socket.send(gson.toJson(nestJSMessage))
        Log.d(TAG, "Message send result: $success")
    }

    fun joinRoom(orderId: String, userId: String) {
        val socket = webSocket
        if (socket == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot join room - not connected")
            return
        }

        // Send in NestJS @SubscribeMessage format
        val joinData = mapOf("orderId" to orderId)
        val nestJSMessage = mapOf(
            "event" to "join_order",
            "data" to joinData
        )

        socket.send(gson.toJson(nestJSMessage))
        Log.d(TAG, "Joined room: $orderId")
    }

    fun leaveRoom(orderId: String, userId: String) {
        val socket = webSocket
        if (socket == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot leave room - not connected")
            return
        }

        // Send in NestJS @SubscribeMessage format
        val leaveData = mapOf("orderId" to orderId)
        val nestJSMessage = mapOf(
            "event" to "leave_order",
            "data" to leaveData
        )

        socket.send(gson.toJson(nestJSMessage))
        Log.d(TAG, "Left room: $orderId")
    }

    private fun handleIncomingMessage(text: String) {
        try {
            Log.d(TAG, "🔍 Processing WebSocket message: $text")
            
            // Backend sends direct JSON objects, not wrapped in WebSocketMessage
            val messageJson = gson.fromJson(text, Map::class.java) as Map<String, Any>
            val messageType = messageJson["type"] as? String
            
            Log.d(TAG, "📨 Message type: $messageType")
            
            when (messageType) {
                "new_message" -> {
                    // Convert backend message format to our DTO
                    val incomingMessage = IncomingChatMessageDto(
                        id = messageJson["messageId"] as? String ?: "",
                        orderId = messageJson["orderId"] as? String ?: "",
                        senderId = "other_user", // Backend doesn't send sender ID in this format
                        senderName = messageJson["username"] as? String, // Backend sends 'username'
                        content = messageJson["message"] as? String ?: "",
                        timestamp = (messageJson["timestamp"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                        messageType = "text"
                    )
                    _incomingMessages.value = incomingMessage
                }
                "message_sent" -> {
                    // Message confirmation from backend
                    val messageId = messageJson["messageId"] as? String
                    if (messageId != null) {
                        _messageSentConfirmations.value = messageId
                        Log.d(TAG, "Message sent confirmation: $messageId")
                    }
                }
                "error" -> {
                    val errorMessage = messageJson["message"] as? String ?: "Unknown error"
                    Log.e(TAG, "🚨 WebSocket error received: $errorMessage")
                    Log.e(TAG, "🚨 Full error message: $text")
                    // Emit error to be handled by UI
                    _connectionErrors.value = errorMessage
                }
                "join_success" -> {
                    Log.d(TAG, "✅ Join success received: $text")
                    // Handle successful join with username
                    val username = messageJson["username"] as? String
                    if (username != null) {
                        _userUsername.value = username
                        Log.d(TAG, "🎭 Received username: $username")
                    }
                    // Clear any connection errors since join was successful
                    _connectionErrors.value = null
                    Log.d(TAG, "🧹 Join successful - cleared connection errors")
                }
                else -> {
                    Log.w(TAG, "Unknown message type: $messageType")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse incoming message: $text", e)
        }
    }

    private fun attemptReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }
        
        val userId = currentUserId ?: return
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            reconnectAttempts++
            Log.d(TAG, "Attempting reconnect #$reconnectAttempts")
            connect(userId)
        }
    }
}
