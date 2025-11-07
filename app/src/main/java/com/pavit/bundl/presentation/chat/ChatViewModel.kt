package com.pavit.bundl.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavit.bundl.domain.model.ChatMessage
import com.pavit.bundl.domain.model.ChatRoom
import com.pavit.bundl.domain.model.ConnectionState
import com.pavit.bundl.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val chatRoom: ChatRoom? = null,
    val messageText: String = "",
    val isLoading: Boolean = false,
    val currentUserUsername: String? = null,
    val shouldExit: Boolean = false,
    val errorMessage: String? = null
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
            _uiState.update { it.copy(isLoading = true) }
            
            // First, ensure we're connected to the WebSocket
            Log.d("ChatViewModel", "🔗 Connecting to WebSocket...")
            chatRepository.connect(userId)
            
            // Wait a bit for connection to establish
            kotlinx.coroutines.delay(2000)
            
            val connectionState = chatRepository.getConnectionState().first()
            
            if (connectionState != ConnectionState.CONNECTED) {
                Log.d("ChatViewModel", "❌ WebSocket connection failed: $connectionState")
                _uiState.update { it.copy(shouldExit = true, isLoading = false) }
                return@launch
            }
            
            Log.d("ChatViewModel", "✅ WebSocket connected, joining room")
            
            chatRepository.joinChatRoom(orderId, userId)
            
            chatRepository.getUserUsername()
                .filterNotNull()
                .first()
            
            Log.d("ChatViewModel", "✅ Join successful, proceeding to chat")
            _uiState.update { it.copy(isLoading = false) }
        }
        
        viewModelScope.launch {
            chatRepository.getMessagesForOrder(orderId)
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
        }
        
        viewModelScope.launch {
            chatRepository.getUserUsername()
                .collect { username ->
                    _uiState.update { it.copy(currentUserUsername = username) }
                }
        }
        
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
        
        // Clear any previous error messages
        _uiState.update { it.copy(errorMessage = null) }
        
        viewModelScope.launch {
            // Check connection state before sending
            val connectionState = chatRepository.getConnectionState().first()
            
            if (connectionState != ConnectionState.CONNECTED) {
                _uiState.update { 
                    it.copy(errorMessage = "Not connected to chat. Please check your connection and try again.") 
                }
                return@launch
            }
            
            val result = chatRepository.sendMessage(orderId, messageText)
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(messageText = "") }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(errorMessage = "Failed to send message: ${error.message}") 
                    }
                    Log.e("ChatViewModel", "Failed to send message: ${error.message}")
                }
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun markMessagesAsRead() {
        val orderId = currentOrderId ?: return
        
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(orderId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        
        val orderId = currentOrderId ?: return
        viewModelScope.launch {
            chatRepository.leaveChatRoom(orderId, "current_user_id")
        }
    }
}