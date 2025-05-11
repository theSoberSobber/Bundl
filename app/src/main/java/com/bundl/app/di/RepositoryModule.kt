package com.bundl.app.di

import com.bundl.app.data.repository.ApiKeyRepositoryImpl
import com.bundl.app.data.repository.AuthRepositoryImpl
import com.bundl.app.data.repository.OrderRepositoryImpl
import com.bundl.app.domain.repository.ApiKeyRepository
import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindApiKeyRepository(
        apiKeyRepositoryImpl: ApiKeyRepositoryImpl
    ): ApiKeyRepository
    
    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository
} 