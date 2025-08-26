package com.bundl.app.domain.usecase.credits

import com.bundl.app.domain.model.CreditPackage
import com.bundl.app.domain.payment.OrderResponse
import com.bundl.app.domain.payment.PaymentService
import com.bundl.app.domain.usecase.base.ParameterizedUseCase
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
