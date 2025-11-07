package com.pavit.bundl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pavit.bundl.data.local.entity.ChatMessageEntity
import com.pavit.bundl.data.local.entity.ChatRoomEntity
import com.pavit.bundl.domain.model.DeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    
    @Query("SELECT * FROM chat_messages WHERE orderId = :orderId ORDER BY timestamp ASC")
    fun getMessagesForOrder(orderId: String): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)
    
    @Query("UPDATE chat_messages SET deliveryStatus = :status WHERE id = :messageId")
    suspend fun updateMessageDeliveryStatus(messageId: String, status: DeliveryStatus)
    
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    @Query("DELETE FROM chat_messages WHERE orderId = :orderId")
    suspend fun deleteAllMessagesForOrder(orderId: String)
    
    @Query("SELECT COUNT(*) FROM chat_messages WHERE orderId = :orderId")
    suspend fun getMessageCountForOrder(orderId: String): Int
    
    @Query("SELECT * FROM chat_messages WHERE orderId = :orderId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForOrder(orderId: String): ChatMessageEntity?
}

@Dao
interface ChatRoomDao {
    
    @Query("SELECT * FROM chat_rooms WHERE isActive = 1 ORDER BY lastMessageTime DESC")
    fun getActiveChatRooms(): Flow<List<ChatRoomEntity>>
    
    @Query("SELECT * FROM chat_rooms WHERE orderId = :orderId")
    suspend fun getChatRoomByOrderId(orderId: String): ChatRoomEntity?
    
    @Query("SELECT * FROM chat_rooms WHERE orderId = :orderId")
    fun observeChatRoomByOrderId(orderId: String): Flow<ChatRoomEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(chatRoom: ChatRoomEntity)
    
    @Update
    suspend fun updateChatRoom(chatRoom: ChatRoomEntity)
    
    @Query("UPDATE chat_rooms SET unreadCount = :count WHERE orderId = :orderId")
    suspend fun updateUnreadCount(orderId: String, count: Int)
    
    @Query("UPDATE chat_rooms SET lastMessage = :message, lastMessageTime = :timestamp WHERE orderId = :orderId")
    suspend fun updateLastMessage(orderId: String, message: String, timestamp: Long)
    
    @Query("UPDATE chat_rooms SET isActive = 0 WHERE orderId = :orderId")
    suspend fun deactivateChatRoom(orderId: String)
    
    @Query("DELETE FROM chat_rooms WHERE orderId = :orderId")
    suspend fun deleteChatRoom(orderId: String)
    
    @Query("SELECT COUNT(*) FROM chat_rooms WHERE isActive = 1 AND unreadCount > 0")
    fun getTotalUnreadRoomsCount(): Flow<Int>
}
