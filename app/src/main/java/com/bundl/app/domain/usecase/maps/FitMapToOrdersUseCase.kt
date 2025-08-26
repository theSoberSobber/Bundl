package com.bundl.app.domain.usecase.maps

import com.bundl.app.domain.model.Order
import com.bundl.app.domain.maps.MapProvider
import com.bundl.app.domain.usecase.base.VoidParameterizedUseCase
import javax.inject.Inject

data class FitMapToOrdersParams(
    val orders: List<Order>,
    val userLatitude: Double,
    val userLongitude: Double
)

class FitMapToOrdersUseCase @Inject constructor(
    private val mapProvider: MapProvider
) : VoidParameterizedUseCase<FitMapToOrdersParams> {
    
    override suspend fun invoke(parameters: FitMapToOrdersParams): Result<Unit> {
        return try {
            fitMapToOrders(parameters.orders, parameters.userLatitude, parameters.userLongitude)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun fitMapToOrders(orders: List<Order>, userLat: Double, userLon: Double) {
        // If no orders, use a much smaller zoom area
        if (orders.isEmpty()) {
            mapProvider.animateCamera(
                latitude = userLat,
                longitude = userLon,
                zoom = 16.0, // More zoomed in for 0.5km radius
                duration = 1000,
                paddingBottom = 300f
            )
            return
        }

        // Calculate the bounds that include all orders and user location
        var minLat = userLat
        var maxLat = userLat
        var minLon = userLon
        var maxLon = userLon

        orders.forEach { order ->
            minLat = minOf(minLat, order.latitude)
            maxLat = maxOf(maxLat, order.latitude)
            minLon = minOf(minLon, order.longitude)
            maxLon = maxOf(maxLon, order.longitude)
        }

        // Calculate center point
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        // Calculate appropriate zoom level based on the bounds
        val latDiff = maxLat - minLat
        val lonDiff = maxLon - minLon
        val maxDiff = maxOf(latDiff, lonDiff)
        
        // Adjusted zoom levels for 0.5km radius (approximately doubled from previous values)
        val zoom = when {
            maxDiff > 0.05 -> 13.0  // Very spread out
            maxDiff > 0.025 -> 13.7 // Moderately spread
            maxDiff > 0.01 -> 14.4 // Somewhat close
            maxDiff > 0.005 -> 15.0 // Close together
            else -> 15.5          // Very close together
        }

        // Add padding to account for the bottom sheet (50% of screen height)
        mapProvider.animateCamera(
            latitude = centerLat,
            longitude = centerLon,
            zoom = zoom,
            duration = 1000,
            paddingBottom = 300f  // Approximate padding for bottom sheet
        )
    }
}
