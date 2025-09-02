package com.pavit.bundl.domain.maps

import com.pavit.bundl.data.maps.MapboxProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating MapProvider implementations
 * This allows us to easily switch between different map providers
 */
@Singleton
class MapProviderFactory @Inject constructor(
    private val mapboxProvider: MapboxProvider
) {
    /**
     * Available map provider types
     */
    enum class MapType {
        MAPBOX
    }
    
    /**
     * Get a MapProvider implementation
     * @param type The type of map provider to use
     * @return A MapProvider implementation
     */
    fun getProvider(type: MapType = MapType.MAPBOX): MapProvider {
        return when (type) {
            MapType.MAPBOX -> mapboxProvider
            // We can add more providers here in the future (Google Maps, etc.)
        }
    }
} 