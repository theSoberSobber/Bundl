package com.pavit.bundl.domain.model

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("orderId")
    val orderId: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("senderName")
    val senderName: String?,
    @SerializedName("content")
    val content: String,
    @SerializedName("messageType")
    val messageType: MessageType = MessageType.TEXT,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("deliveryStatus")
    val deliveryStatus: DeliveryStatus = DeliveryStatus.SENT,
    @SerializedName("isSystemMessage")
    val isSystemMessage: Boolean = false
)

data class ChatRoom(
    @SerializedName("id")
    val id: String,
    @SerializedName("orderId")
    val orderId: String,
    @SerializedName("participants")
    val participants: List<String> = emptyList(),
    @SerializedName("lastMessage")
    val lastMessage: String? = null,
    @SerializedName("lastMessageTime")
    val lastMessageTime: Long? = null,
    @SerializedName("unreadCount")
    val unreadCount: Int = 0,
    @SerializedName("isActive")
    val isActive: Boolean = true
)

enum class MessageType {
    @SerializedName("text")
    TEXT,
    @SerializedName("system")
    SYSTEM,
    @SerializedName("order_update")
    ORDER_UPDATE
}

enum class DeliveryStatus {
    @SerializedName("sending")
    SENDING,
    @SerializedName("sent")
    SENT,
    @SerializedName("delivered")
    DELIVERED,
    @SerializedName("failed")
    FAILED
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR_NETWORK,
    ERROR_AUTH
}
