package com.bundl.app.domain.usecase.location

import com.bundl.app.domain.repository.LocationRepository
import com.bundl.app.domain.repository.LocationData
import com.bundl.app.domain.usecase.base.UseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetCurrentLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) : UseCase<LocationData> {
    
    override suspend fun invoke(): Result<LocationData> {
        return try {
            val location = locationRepository.getCurrentLocation().first()
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun asFlow(): Flow<LocationData> = locationRepository.getCurrentLocation()
}
