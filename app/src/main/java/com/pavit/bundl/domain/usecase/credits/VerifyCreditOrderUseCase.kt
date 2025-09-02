package com.pavit.bundl.domain.usecase.credits

import com.pavit.bundl.domain.payment.VerifyResponse
import com.pavit.bundl.domain.repository.CreditsRepository
import com.pavit.bundl.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject
import javax.inject.Singleton

data class VerifyCreditOrderParams(
    val orderId: String
)

@Singleton
class VerifyCreditOrderUseCase @Inject constructor(
    private val creditsRepository: CreditsRepository
) : ParameterizedUseCase<VerifyCreditOrderParams, VerifyResponse> {
    
    override suspend fun invoke(parameters: VerifyCreditOrderParams): Result<VerifyResponse> {
        return creditsRepository.verifyCreditOrder(parameters.orderId)
    }
}
