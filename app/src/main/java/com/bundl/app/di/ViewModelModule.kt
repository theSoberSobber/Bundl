package com.bundl.app.di

import com.bundl.app.data.local.OrderDao
import com.bundl.app.data.remote.api.ApiKeyService
import com.bundl.app.data.remote.api.AuthApiService
import com.bundl.app.data.remote.api.OrderApiService
import com.bundl.app.domain.maps.MapProvider
import com.bundl.app.domain.repository.ApiKeyRepository
import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.domain.repository.OrderRepository
import com.bundl.app.presentation.auth.AuthViewModel
import com.bundl.app.presentation.dashboard.HomeViewModel
import com.bundl.app.presentation.orders.MyOrdersViewModel
import com.bundl.app.utils.DeviceUtils
import com.bundl.app.utils.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    
    @Provides
    @ViewModelScoped
    fun provideAuthViewModel(
        authRepository: AuthRepository,
        deviceUtils: DeviceUtils
    ): AuthViewModel {
        return AuthViewModel(authRepository, deviceUtils)
    }
    
    @Provides
    @ViewModelScoped
    fun provideHomeViewModel(
        apiKeyRepository: ApiKeyRepository,
        authRepository: AuthRepository,
        orderRepository: OrderRepository,
        deviceUtils: DeviceUtils,
        apiKeyService: ApiKeyService,
        authApiService: AuthApiService,
        orderApiService: OrderApiService,
        mapProvider: MapProvider,
        locationManager: LocationManager
    ): HomeViewModel {
        return HomeViewModel(
            apiKeyRepository,
            authRepository,
            orderRepository,
            deviceUtils,
            apiKeyService,
            authApiService,
            orderApiService,
            mapProvider,
            locationManager
        )
    }

    @Provides
    @ViewModelScoped
    fun provideMyOrdersViewModel(
        orderApiService: OrderApiService,
        orderDao: OrderDao
    ): MyOrdersViewModel {
        return MyOrdersViewModel(orderApiService, orderDao)
    }
} 