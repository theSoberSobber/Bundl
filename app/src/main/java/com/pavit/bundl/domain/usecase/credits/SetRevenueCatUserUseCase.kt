package com.pavit.bundl.domain.usecase.credits

import com.pavit.bundl.domain.payment.PaymentService
import com.pavit.bundl.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject
import javax.inject.Singleton

data class SetRevenueCatUserParams(
    val userId: String
)

/**
 * Use case to set the RevenueCat user for purchase attribution
 * This should be called after user authentication
 */
@Singleton
class SetRevenueCatUserUseCase @Inject constructor(
    private val paymentService: PaymentService
) : ParameterizedUseCase<SetRevenueCatUserParams, Unit> {
    
    override suspend fun invoke(parameters: SetRevenueCatUserParams): Result<Unit> {
        return paymentService.setUser(parameters.userId)
    }
}
