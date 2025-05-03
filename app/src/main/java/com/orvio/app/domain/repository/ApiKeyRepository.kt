package com.orvio.app.domain.repository

import com.orvio.app.domain.model.ApiKey
import kotlinx.coroutines.flow.Flow

interface ApiKeyRepository {
    suspend fun getApiKeys(): Result<List<ApiKey>>
    suspend fun createApiKey(name: String): Result<ApiKey>
    suspend fun deleteApiKey(key: String): Result<Boolean>
    suspend fun testApiKey(key: String): Result<Boolean>
    suspend fun testApiKey(key: String, phoneNumber: String): Result<Boolean>
    suspend fun registerDevice(deviceHash: String, fcmToken: String): Result<Boolean>
} 