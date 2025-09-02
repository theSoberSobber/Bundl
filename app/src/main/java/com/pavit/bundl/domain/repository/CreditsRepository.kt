package com.pavit.bundl.domain.repository

import com.pavit.bundl.domain.model.CreditPackage
import com.pavit.bundl.domain.payment.VerifyResponse
import kotlinx.coroutines.flow.Flow

data class CreditsInfo(
    val credits: Int
)

interface CreditsRepository {
    suspend fun getCredits(): Result<CreditsInfo>
    suspend fun setCreditMode(mode: String): Result<Unit>
    suspend fun getCreditPackages(): Result<List<CreditPackage>>
    suspend fun verifyCreditOrder(orderId: String): Result<VerifyResponse>
} 