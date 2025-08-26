package com.bundl.app.domain.repository

import kotlinx.coroutines.flow.Flow

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val isFromUser: Boolean
)

interface LocationRepository {
    fun getCurrentLocation(): Flow<LocationData>
    suspend fun startLocationUpdates(): Result<Unit>
    suspend fun stopLocationUpdates(): Result<Unit>
    suspend fun hasLocationPermission(): Boolean
}
