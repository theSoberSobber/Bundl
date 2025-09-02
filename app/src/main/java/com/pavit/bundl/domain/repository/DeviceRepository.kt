package com.pavit.bundl.domain.repository

import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    suspend fun getDeviceId(): Result<String>
    suspend fun getFcmToken(): Result<String>
    suspend fun getDeviceInfo(): Result<DeviceInfo>
}

data class DeviceInfo(
    val deviceId: String,
    val fcmToken: String
)
