package com.bundl.app.di

import com.bundl.app.data.repository.AuthRepositoryImpl
import com.bundl.app.data.repository.CreditsRepositoryImpl
import com.bundl.app.data.repository.DeviceRepositoryImpl
import com.bundl.app.data.repository.LocationRepositoryImpl
import com.bundl.app.data.repository.OrderRepositoryImpl
import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.domain.repository.CreditsRepository
import com.bundl.app.domain.repository.DeviceRepository
import com.bundl.app.domain.repository.LocationRepository
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
    abstract fun bindCreditsRepository(
        creditsRepositoryImpl: CreditsRepositoryImpl
    ): CreditsRepository
    
    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository
    
    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        deviceRepositoryImpl: DeviceRepositoryImpl
    ): DeviceRepository
    
    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository
} 