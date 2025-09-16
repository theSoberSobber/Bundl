package com.pavit.bundl.data.repository

import android.util.Log
import com.pavit.bundl.data.local.dao.ChatMessageDao
import com.pavit.bundl.data.local.dao.ChatRoomDao
import com.pavit.bundl.data.local.entity.ChatMessageEntity
import com.pavit.bundl.data.local.entity.ChatRoomEntity
import com.pavit.bundl.data.remote.websocket.WebSocketChatService
import com.pavit.bundl.domain.model.*
import com.pavit.bundl.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val webSocketService: WebSocketChatService,
    private val chatMessageDao: ChatMessageDao,
    private val chatRoomDao: ChatRoomDao
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Listen for incoming messages from WebSocket
        repositoryScope.launch {
            webSocketService.incomingMessages
                .filterNotNull()
                .collect { messageDto ->
                    handleIncomingMessage(messageDto)
                }
        }

        // Listen for order updates
        repositoryScope.launch {
            webSocketService.orderUpdates
                .filterNotNull()
                .collect { orderUpdate ->
                    handleOrderUpdate(orderUpdate)
                }
        }

        // Listen for participant updates
        repositoryScope.launch {
            webSocketService.participantUpdates
                .filterNotNull()
                .collect { participantUpdate ->
                    handleParticipantUpdate(participantUpdate)
                }
        }
    }

    override fun getConnectionState(): Flow<ConnectionState> {
        return webSocketService.connectionState
    }

    override suspend fun connect(userId: String) {
        webSocketService.connect(userId)
    }

    override suspend fun disconnect() {
        webSocketService.disconnect()
    }

    override suspend fun joinChatRoom(orderId: String, userId: String) {
        // Create or update local chat room
        val existingRoom = chatRoomDao.getChatRoomByOrderId(orderId)
        if (existingRoom == null) {
            val newRoom = ChatRoomEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                participants = listOf(userId),
                lastMessage = null,
                lastMessageTime = null,
                isActive = true
            )
            chatRoomDao.insertChatRoom(newRoom)
        }

        // Join via WebSocket
        webSocketService.joinRoom(orderId, userId)
        Log.d(TAG, "Joined chat room for order: $orderId")
    }

    override suspend fun leaveChatRoom(orderId: String, userId: String) {
        // Deactivate local chat room
        chatRoomDao.deactivateChatRoom(orderId)
        
        // Leave via WebSocket
        webSocketService.leaveRoom(orderId, userId)
        Log.d(TAG, "Left chat room for order: $orderId")
    }

    override fun getChatRoomByOrderId(orderId: String): Flow<ChatRoom?> {
        return chatRoomDao.observeChatRoomByOrderId(orderId)
            .map { entity -> entity?.toDomain() }
    }

    override fun getActiveChatRooms(): Flow<List<ChatRoom>> {
        return chatRoomDao.getActiveChatRooms()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getMessagesForOrder(orderId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesForOrder(orderId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun sendMessage(orderId: String, content: String): Result<ChatMessage> {
        return try {
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            
            // Create local message with SENDING status
            val localMessage = ChatMessageEntity(
                id = messageId,
                orderId = orderId,
                senderId = getCurrentUserId(), // You'll need to implement this
                senderName = getCurrentUserName(), // You'll need to implement this
                content = content,
                messageType = MessageType.TEXT,
                timestamp = timestamp,
                deliveryStatus = DeliveryStatus.SENDING,
                isSystemMessage = false
            )
            
            // Insert to local database immediately
            chatMessageDao.insertMessage(localMessage)
            
            // Send via WebSocket
            webSocketService.sendMessage(orderId, content, messageId)
            
            // Update delivery status
            chatMessageDao.updateMessageDeliveryStatus(messageId, DeliveryStatus.SENT)
            
            Result.success(localMessage.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            Result.failure(e)
        }
    }

    override suspend fun markMessagesAsRead(orderId: String) {
        chatRoomDao.updateUnreadCount(orderId, 0)
    }

    override suspend fun syncChatRoom(orderId: String) {
        // TODO: Implement sync with backend API if needed
        Log.d(TAG, "Sync chat room: $orderId")
    }

    override suspend fun syncMessages(orderId: String) {
        // TODO: Implement sync with backend API if needed
        Log.d(TAG, "Sync messages for order: $orderId")
    }

    private suspend fun handleIncomingMessage(messageDto: com.pavit.bundl.data.remote.dto.IncomingChatMessageDto) {
        Log.d(TAG, "Handling incoming message: ${messageDto.id}")
        
        val messageEntity = ChatMessageEntity(
            id = messageDto.id,
            orderId = messageDto.orderId,
            senderId = messageDto.senderId,
            senderName = messageDto.senderName,
            content = messageDto.content,
            messageType = when (messageDto.messageType) {
                "system" -> MessageType.SYSTEM
                "order_update" -> MessageType.ORDER_UPDATE
                else -> MessageType.TEXT
            },
            timestamp = messageDto.timestamp,
            deliveryStatus = DeliveryStatus.DELIVERED,
            isSystemMessage = messageDto.messageType == "system"
        )
        
        chatMessageDao.insertMessage(messageEntity)
        
        // Update chat room last message
        chatRoomDao.updateLastMessage(
            orderId = messageDto.orderId,
            message = messageDto.content,
            timestamp = messageDto.timestamp
        )
        
        // Increment unread count (you might want to check if user is currently viewing this chat)
        val currentRoom = chatRoomDao.getChatRoomByOrderId(messageDto.orderId)
        currentRoom?.let {
            chatRoomDao.updateUnreadCount(messageDto.orderId, it.unreadCount + 1)
        }
    }

    private suspend fun handleOrderUpdate(orderUpdate: com.pavit.bundl.data.remote.dto.OrderUpdateDto) {
        Log.d(TAG, "Handling order update: ${orderUpdate.orderId}")
        
        // Create system message for order update
        val systemMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            orderId = orderUpdate.orderId,
            senderId = "system",
            senderName = "System",
            content = orderUpdate.message,
            messageType = MessageType.SYSTEM,
            timestamp = orderUpdate.timestamp,
            deliveryStatus = DeliveryStatus.DELIVERED,
            isSystemMessage = true
        )
        
        chatMessageDao.insertMessage(systemMessage)
    }

    private suspend fun handleParticipantUpdate(participantUpdate: com.pavit.bundl.data.remote.dto.ParticipantUpdateDto) {
        Log.d(TAG, "Handling participant update: ${participantUpdate.orderId}")
        
        val message = if (participantUpdate.action == "joined") {
            "${participantUpdate.userName ?: "Someone"} joined the order"
        } else {
            "${participantUpdate.userName ?: "Someone"} left the order"
        }
        
        val systemMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            orderId = participantUpdate.orderId,
            senderId = "system",
            senderName = "System",
            content = message,
            messageType = MessageType.SYSTEM,
            timestamp = participantUpdate.timestamp,
            deliveryStatus = DeliveryStatus.DELIVERED,
            isSystemMessage = true
        )
        
        chatMessageDao.insertMessage(systemMessage)
    }

    // Helper methods - you'll need to implement these based on your auth system
    private fun getCurrentUserId(): String {
        // TODO: Get current user ID from your auth system
        return "current_user_id"
    }

    private fun getCurrentUserName(): String? {
        // TODO: Get current user name from your auth system
        return "Current User"
    }
}

// Extension functions to convert between entities and domain models
private fun ChatMessageEntity.toDomain(): ChatMessage {
    return ChatMessage(
        id = id,
        orderId = orderId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        messageType = messageType,
        timestamp = timestamp,
        deliveryStatus = deliveryStatus,
        isSystemMessage = isSystemMessage
    )
}

private fun ChatRoomEntity.toDomain(): ChatRoom {
    return ChatRoom(
        id = id,
        orderId = orderId,
        participants = participants,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        unreadCount = unreadCount,
        isActive = isActive
    )
}
