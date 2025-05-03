package com.orvio.app.data.repository

import com.orvio.app.data.remote.api.ApiKeyService
import com.orvio.app.data.remote.api.AuthApiService
import com.orvio.app.data.remote.dto.DeviceRegisterRequestDto
import com.orvio.app.data.remote.dto.OtpSendRequestDto
import com.orvio.app.data.remote.dto.OtpVerifyRequestDto
import com.orvio.app.domain.model.ApiKey
import com.orvio.app.domain.repository.ApiKeyRepository
import com.orvio.app.utils.DeviceUtils
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
    
    override suspend fun registerDevice(deviceHash: String, fcmToken: String): Result<Boolean> {
        return try {
            val phoneNumber = deviceUtils.getPhoneNumber() ?: throw Exception("Phone number not available")
            authApiService.registerDevice(DeviceRegisterRequestDto(fcmToken, phoneNumber))
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 