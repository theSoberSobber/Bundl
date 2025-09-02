package com.pavit.bundl.domain.usecase.credits

import com.pavit.bundl.domain.model.CreditPackage
import com.pavit.bundl.domain.payment.OrderResponse
import com.pavit.bundl.domain.payment.PaymentService
import com.pavit.bundl.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject
import javax.inject.Singleton

data class PurchaseCreditsParams(
    val creditPackage: CreditPackage
)

@Singleton
class PurchaseCreditsUseCase @Inject constructor(
    private val paymentService: PaymentService
) : ParameterizedUseCase<PurchaseCreditsParams, OrderResponse> {
    
    override suspend fun invoke(parameters: PurchaseCreditsParams): Result<OrderResponse> {
        return paymentService.createOrder(
            credits = parameters.creditPackage.credits,
            currency = "INR"
        )
    }
}
