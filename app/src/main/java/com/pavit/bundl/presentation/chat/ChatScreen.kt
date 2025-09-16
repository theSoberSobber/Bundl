package com.pavit.bundl.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pavit.bundl.domain.model.ChatMessage
import com.pavit.bundl.domain.model.ConnectionState
import com.pavit.bundl.domain.model.MessageType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    orderId: String,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Initialize chat when screen loads
    LaunchedEffect(orderId) {
        viewModel.initializeChat(orderId, "current_user_id") // TODO: Get actual user ID
        viewModel.markMessagesAsRead()
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Order Chat")
                        Text(
                            text = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> "Connected • ${uiState.chatRoom?.participants?.size ?: 0} participants"
                                ConnectionState.CONNECTING -> "Connecting..."
                                ConnectionState.DISCONNECTED -> "Disconnected"
                                ConnectionState.ERROR_NETWORK -> "Network Error"
                                ConnectionState.ERROR_AUTH -> "Authentication Error"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> Color.Green
                                ConnectionState.CONNECTING -> Color.Yellow
                                else -> Color.Red
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                messageText = uiState.messageText,
                onMessageTextChange = viewModel::updateMessageText,
                onSendMessage = viewModel::sendMessage,
                isConnected = uiState.connectionState == ConnectionState.CONNECTED,
                isLoading = uiState.isLoading
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Messages list
            if (uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet.\nStart the conversation!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        MessageItem(
                            message = message,
                            isCurrentUser = message.senderId == "current_user_id" // TODO: Get actual user ID
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else if (message.isSystemMessage) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.secondary
    }
    
    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else if (message.isSystemMessage) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSecondary
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = if (isCurrentUser) 48.dp else 0.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // System messages have different styling
                if (message.isSystemMessage) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Sender name (only for other users)
                    if (!isCurrentUser && !message.senderName.isNullOrEmpty()) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Message content
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Timestamp and delivery status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (message.deliveryStatus) {
                                com.pavit.bundl.domain.model.DeliveryStatus.SENDING -> "⏳"
                                com.pavit.bundl.domain.model.DeliveryStatus.SENT -> "✓"
                                com.pavit.bundl.domain.model.DeliveryStatus.DELIVERED -> "✓✓"
                                com.pavit.bundl.domain.model.DeliveryStatus.FAILED -> "❌"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isConnected: Boolean,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        if (isConnected) "Type a message..." else "Connecting..."
                    ) 
                },
                enabled = isConnected && !isLoading,
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
