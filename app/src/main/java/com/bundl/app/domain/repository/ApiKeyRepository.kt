package com.bundl.app.domain.repository

import com.bundl.app.domain.model.ApiKey
import kotlinx.coroutines.flow.Flow

data class CreditsInfo(
    val credits: Int
)

interface ApiKeyRepository {
    suspend fun getApiKeys(): Result<List<ApiKey>>
    suspend fun createApiKey(name: String): Result<ApiKey>
    suspend fun deleteApiKey(key: String): Result<Boolean>
    suspend fun testApiKey(key: String): Result<Boolean>
    suspend fun testApiKey(key: String, phoneNumber: String): Result<Boolean>
    suspend fun getCredits(): Result<CreditsInfo>
    suspend fun setCreditMode(mode: String): Result<Unit>
} 