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
    
    private val _orderUpdates = MutableStateFlow<OrderUpdateDto?>(null)
    val orderUpdates: StateFlow<OrderUpdateDto?> = _orderUpdates.asStateFlow()
    
    private val _participantUpdates = MutableStateFlow<ParticipantUpdateDto?>(null)
    val participantUpdates: StateFlow<ParticipantUpdateDto?> = _participantUpdates.asStateFlow()

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

        val messageDto = OutgoingChatMessageDto(
            orderId = orderId,
            content = content,
            messageId = messageId,
            timestamp = System.currentTimeMillis()
        )

        val webSocketMessage = WebSocketMessage(
            type = OutgoingMessageType.NEW_MESSAGE.name,
            payload = gson.toJson(messageDto)
        )

        val success = socket.send(gson.toJson(webSocketMessage))
        Log.d(TAG, "Message send result: $success")
    }

    fun joinRoom(orderId: String, userId: String) {
        val socket = webSocket
        if (socket == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot join room - not connected")
            return
        }

        val joinDto = JoinRoomDto(orderId = orderId, userId = userId)
        val webSocketMessage = WebSocketMessage(
            type = OutgoingMessageType.JOIN_ROOM.name,
            payload = gson.toJson(joinDto)
        )

        socket.send(gson.toJson(webSocketMessage))
        Log.d(TAG, "Joined room: $orderId")
    }

    fun leaveRoom(orderId: String, userId: String) {
        val socket = webSocket
        if (socket == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot leave room - not connected")
            return
        }

        val leaveDto = LeaveRoomDto(orderId = orderId, userId = userId)
        val webSocketMessage = WebSocketMessage(
            type = OutgoingMessageType.LEAVE_ROOM.name,
            payload = gson.toJson(leaveDto)
        )

        socket.send(gson.toJson(webSocketMessage))
        Log.d(TAG, "Left room: $orderId")
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val webSocketMessage = gson.fromJson(text, WebSocketMessage::class.java)
            
            when (webSocketMessage.type) {
                IncomingMessageType.NEW_MESSAGE.name -> {
                    val messageDto = gson.fromJson(webSocketMessage.payload, IncomingChatMessageDto::class.java)
                    _incomingMessages.value = messageDto
                }
                IncomingMessageType.ORDER_UPDATE.name -> {
                    val orderUpdate = gson.fromJson(webSocketMessage.payload, OrderUpdateDto::class.java)
                    _orderUpdates.value = orderUpdate
                }
                IncomingMessageType.PARTICIPANT_JOINED.name,
                IncomingMessageType.PARTICIPANT_LEFT.name -> {
                    val participantUpdate = gson.fromJson(webSocketMessage.payload, ParticipantUpdateDto::class.java)
                    _participantUpdates.value = participantUpdate
                }
                else -> {
                    Log.w(TAG, "Unknown message type: ${webSocketMessage.type}")
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
