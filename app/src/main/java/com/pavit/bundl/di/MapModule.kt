package com.pavit.bundl.di

import android.content.Context
import com.pavit.bundl.data.maps.MapboxProvider
import com.pavit.bundl.domain.maps.MapProvider
import com.pavit.bundl.domain.maps.MapProviderFactory
import com.pavit.bundl.data.utils.LocationManager
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