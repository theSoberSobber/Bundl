package com.pavit.bundl.domain.usecase.maps

import com.pavit.bundl.domain.maps.MapProvider
import com.pavit.bundl.domain.usecase.base.VoidParameterizedUseCase
import javax.inject.Inject

data class ZoomToLocationParams(
    val latitude: Double,
    val longitude: Double,
    val zoomLevel: Double
)

class ZoomToLocationUseCase @Inject constructor(
    private val mapProvider: MapProvider
) : VoidParameterizedUseCase<ZoomToLocationParams> {
    
    override suspend fun invoke(parameters: ZoomToLocationParams): Result<Unit> {
        return try {
            mapProvider.zoomToLocation(
                parameters.latitude,
                parameters.longitude,
                parameters.zoomLevel
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
