package com.bundl.app.data.repository

import com.bundl.app.domain.repository.LocationRepository
import com.bundl.app.domain.repository.LocationData
import com.bundl.app.data.utils.LocationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationManager: LocationManager
) : LocationRepository {
    
    override fun getCurrentLocation(): Flow<LocationData> {
        return locationManager.currentLocation.map { locationData ->
            LocationData(
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                isFromUser = locationData.isFromUser
            )
        }
    }
    
    override suspend fun startLocationUpdates(): Result<Unit> {
        return try {
            locationManager.startLocationUpdates()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopLocationUpdates(): Result<Unit> {
        return try {
            locationManager.stopLocationUpdates()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun hasLocationPermission(): Boolean {
        return locationManager.hasLocationPermission()
    }
}
