package com.pavit.bundl.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavit.bundl.domain.model.ChatMessage
import com.pavit.bundl.domain.model.ChatRoom
import com.pavit.bundl.domain.model.ConnectionState
import com.pavit.bundl.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val chatRoom: ChatRoom? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val messageText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentOrderId: String? = null

    fun initializeChat(orderId: String, userId: String) {
        currentOrderId = orderId
        
        viewModelScope.launch {
            // Connect to WebSocket
            chatRepository.connect(userId)
            
            // Join the chat room
            chatRepository.joinChatRoom(orderId, userId)
            
            // Collect connection state
            chatRepository.getConnectionState()
                .collect { connectionState ->
                    _uiState.update { it.copy(connectionState = connectionState) }
                }
        }
        
        // Collect messages for this order
        viewModelScope.launch {
            chatRepository.getMessagesForOrder(orderId)
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
        }
        
        // Collect chat room info
        viewModelScope.launch {
            chatRepository.getChatRoomByOrderId(orderId)
                .collect { chatRoom ->
                    _uiState.update { it.copy(chatRoom = chatRoom) }
                }
        }
    }

    fun updateMessageText(text: String) {
        _uiState.update { it.copy(messageText = text) }
    }

    fun sendMessage() {
        val orderId = currentOrderId ?: return
        val messageText = _uiState.value.messageText.trim()
        
        if (messageText.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = chatRepository.sendMessage(orderId, messageText)
            
            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            messageText = "",
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Failed to send message: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun markMessagesAsRead() {
        val orderId = currentOrderId ?: return
        
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(orderId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        
        // Leave chat room when ViewModel is cleared
        val orderId = currentOrderId ?: return
        viewModelScope.launch {
            chatRepository.leaveChatRoom(orderId, "current_user_id") // TODO: Get actual user ID
        }
    }
}
