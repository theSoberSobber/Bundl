package com.pavit.bundl.data.repository

import com.pavit.bundl.data.remote.api.CreditsService
import com.pavit.bundl.domain.model.CreditPackage
import com.pavit.bundl.domain.payment.RevenueCatManager
import com.pavit.bundl.domain.payment.VerifyResponse
import com.pavit.bundl.domain.repository.CreditsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditsRepositoryImpl @Inject constructor(
    private val creditsService: CreditsService,
    private val revenueCatManager: RevenueCatManager
) : CreditsRepository {
    
    /**
     * Get user credits - Still uses backend API for user's current credit balance
     */
    override suspend fun getCredits(): Result<com.pavit.bundl.domain.repository.CreditsInfo> {
        return try {
            val creditsResponse = creditsService.getCredits()
            Result.success(com.pavit.bundl.domain.repository.CreditsInfo(creditsResponse.credits))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Set credit mode - Still uses backend API for user preferences
     */
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
    
    /**
     * Get credit packages - Now uses RevenueCat instead of backend API
     */
    override suspend fun getCreditPackages(): Result<List<CreditPackage>> {
        return try {
            val packagesResult = revenueCatManager.getCreditPackages()
            if (packagesResult.isFailure) {
                return Result.failure(
                    packagesResult.exceptionOrNull() ?: Exception("Failed to get credit packages from RevenueCat")
                )
            }
            
            val revenueCatPackages = packagesResult.getOrThrow()
            val creditPackages = revenueCatPackages.map { it.toCreditPackage() }
            Result.success(creditPackages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Verify credit order - Now uses RevenueCat customer info instead of backend verification
     * This method maintains the same interface but now checks RevenueCat purchase history
     */
    override suspend fun verifyCreditOrder(orderId: String): Result<VerifyResponse> {
        return try {
            val customerInfoResult = revenueCatManager.getCustomerInfo()
            if (customerInfoResult.isFailure) {
                return Result.failure(
                    customerInfoResult.exceptionOrNull() ?: Exception("Failed to get customer info")
                )
            }
            
            val customerInfo = customerInfoResult.getOrThrow()
            
            // Check if the product was purchased in non-subscription transactions
            val hasPurchase = customerInfo.nonSubscriptionTransactions.any { transaction ->
                transaction.productIdentifier == orderId
            }
            
            val verifyResponse = VerifyResponse(
                success = hasPurchase,
                orderStatus = if (hasPurchase) "success" else "failed",
                userId = revenueCatManager.getCurrentUserId() ?: "",
                amount = 0 // RevenueCat doesn't provide amount in customer info
            )
            
            Result.success(verifyResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 