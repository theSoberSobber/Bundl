package com.bundl.app.di

import android.content.Context
import com.bundl.app.utils.DeviceUtils
import com.bundl.app.utils.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {
    
    @Provides
    @Singleton
    fun provideDeviceUtils(@ApplicationContext context: Context): DeviceUtils {
        return DeviceUtils(context)
    }
    
    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager {
        return LocationManager(context)
    }
} 