package com.bundl.app.domain.usecase.maps

import com.bundl.app.domain.model.Order
import com.bundl.app.domain.maps.MapProvider
import com.bundl.app.domain.usecase.base.VoidParameterizedUseCase
import javax.inject.Inject

data class SelectOrderOnMapParams(
    val order: Order,
    val userLatitude: Double,
    val userLongitude: Double
)

class SelectOrderOnMapUseCase @Inject constructor(
    private val mapProvider: MapProvider
) : VoidParameterizedUseCase<SelectOrderOnMapParams> {
    
    override suspend fun invoke(parameters: SelectOrderOnMapParams): Result<Unit> {
        return try {
            val order = parameters.order
            
            mapProvider.setSelectedOrder(order.id)
            
            // Calculate midpoint between user and order
            val centerLat = (parameters.userLatitude + order.latitude) / 2
            val centerLon = (parameters.userLongitude + order.longitude) / 2
            
            // Use a more zoomed in level for better visibility of the selected order
            val currentZoom = 16.0  // Increased from 14.5 to zoom in more
            
            // Animate to new position
            mapProvider.animateCamera(
                latitude = centerLat,
                longitude = centerLon,
                zoom = currentZoom,
                duration = 500,
                paddingBottom = 300f  // Keep consistent padding
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
