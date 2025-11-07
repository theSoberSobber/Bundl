package com.pavit.bundl.di

import com.pavit.bundl.data.local.dao.ChatMessageDao
import com.pavit.bundl.data.local.dao.ChatRoomDao
import com.pavit.bundl.data.repository.ChatRepositoryImpl
import com.pavit.bundl.data.remote.websocket.WebSocketChatService
import com.pavit.bundl.domain.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatRepository(
        webSocketService: WebSocketChatService,
        chatMessageDao: ChatMessageDao,
        chatRoomDao: ChatRoomDao
    ): ChatRepository {
        return ChatRepositoryImpl(
            webSocketService = webSocketService,
            chatMessageDao = chatMessageDao,
            chatRoomDao = chatRoomDao
        )
    }
}
