package com.bundl.app.data.repository

import com.bundl.app.data.remote.api.CreditsService
import com.bundl.app.domain.model.CreditPackage
import com.bundl.app.domain.payment.VerifyResponse
import com.bundl.app.domain.repository.CreditsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditsRepositoryImpl @Inject constructor(
    private val creditsService: CreditsService
) : CreditsRepository {
    
    override suspend fun getCredits(): Result<com.bundl.app.domain.repository.CreditsInfo> {
        return try {
            val creditsResponse = creditsService.getCredits()
            Result.success(com.bundl.app.domain.repository.CreditsInfo(creditsResponse.credits))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setCreditMode(mode: String): Result<Unit> {
        return try {
            val request = mapOf("mode" to mode)
            val response = creditsService.setCreditMode(request)
            if (response["success"] == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update credit mode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCreditPackages(): Result<List<CreditPackage>> {
        return try {
            val packagesResponse = creditsService.getCreditPackages()
            val creditPackages = packagesResponse.packages.map { it.toCreditPackage() }
            Result.success(creditPackages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun verifyCreditOrder(orderId: String): Result<VerifyResponse> {
        return try {
            val request = mapOf("orderId" to orderId)
            val verifyResponse = creditsService.verifyCreditOrder(request)
            Result.success(verifyResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 