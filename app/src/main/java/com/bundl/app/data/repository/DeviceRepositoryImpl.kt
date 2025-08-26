package com.bundl.app.data.repository

import com.bundl.app.domain.repository.DeviceRepository
import com.bundl.app.data.utils.DeviceUtils
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceUtils: DeviceUtils
) : DeviceRepository {
    
    override suspend fun getDeviceId(): Result<String> {
        return try {
            val deviceId = deviceUtils.getDeviceHash()
            Result.success(deviceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getFcmToken(): Result<String> {
        return try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            Result.success(fcmToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDeviceInfo(): Result<com.bundl.app.domain.repository.DeviceInfo> {
        return try {
            val deviceId = deviceUtils.getDeviceHash()
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            
            Result.success(com.bundl.app.domain.repository.DeviceInfo(deviceId, fcmToken))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
