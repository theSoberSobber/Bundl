package com.bundl.app.di

import android.content.Context
import com.bundl.app.data.maps.MapboxProvider
import com.bundl.app.domain.maps.MapProvider
import com.bundl.app.domain.maps.MapProviderFactory
import com.bundl.app.data.utils.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapModule {
    
    @Provides
    @Singleton
    fun provideMapboxProvider(
        @ApplicationContext context: Context,
        locationManager: LocationManager
    ): MapboxProvider {
        return MapboxProvider(context, locationManager)
    }
    
    @Provides
    @Singleton
    fun provideMapProviderFactory(mapboxProvider: MapboxProvider): MapProviderFactory {
        return MapProviderFactory(mapboxProvider)
    }
    
    @Provides
    @Singleton
    fun provideDefaultMapProvider(mapProviderFactory: MapProviderFactory): MapProvider {
        return mapProviderFactory.getProvider(MapProviderFactory.MapType.MAPBOX)
    }
} 