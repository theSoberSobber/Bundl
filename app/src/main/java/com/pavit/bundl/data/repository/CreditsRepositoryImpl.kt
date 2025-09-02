package com.pavit.bundl.data.repository

import com.pavit.bundl.data.remote.api.CreditsService
import com.pavit.bundl.domain.model.CreditPackage
import com.pavit.bundl.domain.payment.VerifyResponse
import com.pavit.bundl.domain.repository.CreditsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditsRepositoryImpl @Inject constructor(
    private val creditsService: CreditsService
) : CreditsRepository {
    
    override suspend fun getCredits(): Result<com.pavit.bundl.domain.repository.CreditsInfo> {
        return try {
            val creditsResponse = creditsService.getCredits()
            Result.success(com.pavit.bundl.domain.repository.CreditsInfo(creditsResponse.credits))
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