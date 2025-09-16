package com.pavit.bundl.domain.repository

import com.pavit.bundl.domain.model.ChatMessage
import com.pavit.bundl.domain.model.ChatRoom
import com.pavit.bundl.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    
    // Connection management
    fun getConnectionState(): Flow<ConnectionState>
    suspend fun connect(userId: String)
    suspend fun disconnect()
    
    // Room management
    suspend fun joinChatRoom(orderId: String, userId: String)
    suspend fun leaveChatRoom(orderId: String, userId: String)
    fun getChatRoomByOrderId(orderId: String): Flow<ChatRoom?>
    fun getActiveChatRooms(): Flow<List<ChatRoom>>
    
    // Message management
    fun getMessagesForOrder(orderId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(orderId: String, content: String): Result<ChatMessage>
    suspend fun markMessagesAsRead(orderId: String)
    
    // Sync operations
    suspend fun syncChatRoom(orderId: String)
    suspend fun syncMessages(orderId: String)
}
