package com.bundl.app.domain.usecase.maps

import com.bundl.app.domain.maps.MapProvider
import com.bundl.app.domain.usecase.base.VoidParameterizedUseCase
import javax.inject.Inject

data class AnimateCameraParams(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double? = null,
    val duration: Long = 300,
    val paddingBottom: Float = 0f
)

class AnimateCameraUseCase @Inject constructor(
    private val mapProvider: MapProvider
) : VoidParameterizedUseCase<AnimateCameraParams> {
    
    override suspend fun invoke(parameters: AnimateCameraParams): Result<Unit> {
        return try {
            mapProvider.animateCamera(
                latitude = parameters.latitude,
                longitude = parameters.longitude,
                zoom = parameters.zoom,
                duration = parameters.duration,
                paddingBottom = parameters.paddingBottom
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
