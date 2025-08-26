package com.bundl.app.domain.usecase.credits

import com.bundl.app.domain.payment.PaymentService
import com.bundl.app.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject
import javax.inject.Singleton

data class StartPaymentParams(
    val orderId: String,
    val sessionId: String
)

@Singleton
class StartPaymentUseCase @Inject constructor(
    private val paymentService: PaymentService
) : ParameterizedUseCase<StartPaymentParams, Unit> {
    
    override suspend fun invoke(parameters: StartPaymentParams): Result<Unit> {
        return paymentService.startUpiPayment(
            orderId = parameters.orderId,
            sessionId = parameters.sessionId
        )
    }
}
