package com.bundl.app.data.repository

import com.bundl.app.data.remote.api.ApiKeyService
import com.bundl.app.domain.repository.ApiKeyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyRepositoryImpl @Inject constructor(
    private val apiKeyService: ApiKeyService
) : ApiKeyRepository {
    
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