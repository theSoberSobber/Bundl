package com.pavit.bundl.data.remote.dto

import com.google.gson.annotations.SerializedName

// Base WebSocket message structure
data class WebSocketMessage(
    @SerializedName("type")
    val type: String,
    @SerializedName("payload")
    val payload: String
)

// Incoming message types
enum class IncomingMessageType {
    NEW_MESSAGE,
    ORDER_UPDATE,
    PARTICIPANT_JOINED,
    PARTICIPANT_LEFT
}

// Outgoing message types  
enum class OutgoingMessageType {
    NEW_MESSAGE,
    JOIN_ROOM,
    LEAVE_ROOM
}

// Incoming message DTOs
data class IncomingChatMessageDto(
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
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("messageType")
    val messageType: String = "text"
)

data class OrderUpdateDto(
    @SerializedName("orderId")
    val orderId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("timestamp")
    val timestamp: Long
)

data class ParticipantUpdateDto(
    @SerializedName("orderId")
    val orderId: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("userName")
    val userName: String?,
    @SerializedName("action")
    val action: String, // "joined" or "left"
    @SerializedName("timestamp")
    val timestamp: Long
)

// Outgoing message DTOs
data class OutgoingChatMessageDto(
    @SerializedName("orderId")
    val orderId: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("messageId")
    val messageId: String,
    @SerializedName("timestamp")
    val timestamp: Long
)

data class JoinRoomDto(
    @SerializedName("orderId")
    val orderId: String,
    @SerializedName("userId")
    val userId: String
)

data class LeaveRoomDto(
    @SerializedName("orderId")
    val orderId: String,
    @SerializedName("userId")
    val userId: String
)
