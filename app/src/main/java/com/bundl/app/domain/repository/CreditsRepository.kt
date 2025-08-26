package com.bundl.app.domain.repository

import com.bundl.app.domain.model.CreditPackage
import com.bundl.app.domain.payment.VerifyResponse
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