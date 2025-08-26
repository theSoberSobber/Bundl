package com.bundl.app.data.repository

import com.bundl.app.data.remote.api.ApiKeyService
import com.bundl.app.data.remote.api.AuthApiService
import com.bundl.app.data.remote.dto.OtpSendRequestDto
import com.bundl.app.domain.model.ApiKey
import com.bundl.app.domain.repository.ApiKeyRepository
import com.bundl.app.data.utils.DeviceUtils
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class ApiKeyRepositoryImpl @Inject constructor(
    private val apiKeyService: ApiKeyService,
    private val authApiService: AuthApiService,
    private val deviceUtils: DeviceUtils
) : ApiKeyRepository {
    
    override suspend fun getApiKeys(): Result<List<ApiKey>> {
        return try {
            val apiKeys = apiKeyService.getApiKeys().map { it.toApiKey() }
            Result.success(apiKeys)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createApiKey(name: String): Result<ApiKey> {
        return try {
            val request = mapOf("name" to name)
            val apiKeyString = apiKeyService.createApiKey(request)
            Result.success(ApiKey(
                id = UUID.randomUUID().toString(), // Generate a temporary ID
                name = name,
                key = apiKeyString,
                createdAt = Date(),
                lastUsed = null
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteApiKey(key: String): Result<Boolean> {
        return try {
            val request = mapOf("apiKey" to key)
            apiKeyService.revokeApiKey(request)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun testApiKey(key: String): Result<Boolean> {
        return try {
            // Only test sending OTP
            val sendOtpRequest = OtpSendRequestDto("9770483089") // Test phone number
            apiKeyService.serviceSendOtp(sendOtpRequest)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun testApiKey(key: String, phoneNumber: String): Result<Boolean> {
        return try {
            // Test sending OTP with user-provided phone number
            val sendOtpRequest = OtpSendRequestDto(phoneNumber)
            apiKeyService.serviceSendOtp(sendOtpRequest)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCredits(): Result<com.bundl.app.domain.repository.CreditsInfo> {
        return try {
            val creditsResponse = apiKeyService.getCredits()
            Result.success(com.bundl.app.domain.repository.CreditsInfo(creditsResponse.credits))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setCreditMode(mode: String): Result<Unit> {
        return try {
            val request = mapOf("mode" to mode)
            val response = apiKeyService.setCreditMode(request)
            if (response["success"] == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update credit mode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 