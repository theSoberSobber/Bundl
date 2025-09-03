package com.pavit.bundl.domain.usecase.credits

import com.pavit.bundl.domain.payment.PaymentService
import com.pavit.bundl.domain.usecase.base.UseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to restore previous purchases from RevenueCat
 * This is useful for users who have made purchases on other devices
 * or after reinstalling the app
 */
@Singleton
class RestorePurchasesUseCase @Inject constructor(
    private val paymentService: PaymentService
) : UseCase<Unit> {
    
    override suspend fun invoke(): Result<Unit> {
        return paymentService.restorePurchases()
    }
}
