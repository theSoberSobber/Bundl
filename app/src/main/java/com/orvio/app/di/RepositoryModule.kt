package com.orvio.app.di

import com.orvio.app.data.repository.ApiKeyRepositoryImpl
import com.orvio.app.data.repository.AuthRepositoryImpl
import com.orvio.app.domain.repository.ApiKeyRepository
import com.orvio.app.domain.repository.AuthRepository
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
} 