package com.bundl.app.domain.usecase.location

import com.bundl.app.domain.repository.LocationRepository
import com.bundl.app.domain.usecase.base.VoidUseCase
import javax.inject.Inject

class StopLocationUpdatesUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) : VoidUseCase {
    
    override suspend fun invoke(): Result<Unit> {
        return try {
            locationRepository.stopLocationUpdates()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
