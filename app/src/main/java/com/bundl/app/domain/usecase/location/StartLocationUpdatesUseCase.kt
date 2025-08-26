package com.bundl.app.domain.usecase.location

import com.bundl.app.domain.repository.LocationRepository
import com.bundl.app.domain.usecase.base.VoidUseCase
import javax.inject.Inject

class StartLocationUpdatesUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) : VoidUseCase {
    
    override suspend fun invoke(): Result<Unit> {
        return try {
            locationRepository.startLocationUpdates()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
