package com.pavit.bundl.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pavit.bundl.domain.model.DeliveryStatus
import com.pavit.bundl.domain.model.MessageType

@Entity(tableName = "chat_messages")
@TypeConverters(ChatMessageConverters::class)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val orderId: String,
    val senderId: String,
    val senderName: String?,
    val content: String,
    val messageType: MessageType,
    val timestamp: Long,
    val deliveryStatus: DeliveryStatus,
    val isSystemMessage: Boolean = false,
    val localTimestamp: Long = System.currentTimeMillis() // For local sorting
)

@Entity(tableName = "chat_rooms")
@TypeConverters(ChatRoomConverters::class)
data class ChatRoomEntity(
    @PrimaryKey
    val id: String,
    val orderId: String,
    val participants: List<String>,
    val lastMessage: String?,
    val lastMessageTime: Long?,
    val unreadCount: Int = 0,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

class ChatMessageConverters {
    @TypeConverter
    fun fromMessageType(messageType: MessageType): String {
        return messageType.name
    }
    
    @TypeConverter
    fun toMessageType(messageType: String): MessageType {
        return MessageType.valueOf(messageType)
    }
    
    @TypeConverter
    fun fromDeliveryStatus(deliveryStatus: DeliveryStatus): String {
        return deliveryStatus.name
    }
    
    @TypeConverter
    fun toDeliveryStatus(deliveryStatus: String): DeliveryStatus {
        return DeliveryStatus.valueOf(deliveryStatus)
    }
}

class ChatRoomConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromParticipantsList(participants: List<String>): String {
        return gson.toJson(participants)
    }
    
    @TypeConverter
    fun toParticipantsList(participantsJson: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(participantsJson, listType) ?: emptyList()
    }
}
