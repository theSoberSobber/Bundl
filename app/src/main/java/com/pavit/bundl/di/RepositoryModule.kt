package com.pavit.bundl.di

import com.pavit.bundl.data.repository.AuthRepositoryImpl
import com.pavit.bundl.data.repository.CreditsRepositoryImpl
import com.pavit.bundl.data.repository.DeviceRepositoryImpl
import com.pavit.bundl.data.repository.LocationRepositoryImpl
import com.pavit.bundl.data.repository.OrderRepositoryImpl
import com.pavit.bundl.domain.repository.AuthRepository
import com.pavit.bundl.domain.repository.CreditsRepository
import com.pavit.bundl.domain.repository.DeviceRepository
import com.pavit.bundl.domain.repository.LocationRepository
import com.pavit.bundl.domain.repository.OrderRepository
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