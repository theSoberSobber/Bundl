package com.bundl.app.domain.usecase.orders

import com.bundl.app.data.remote.api.OrderApiService
import com.bundl.app.data.remote.api.PledgeRequest
import com.bundl.app.domain.usecase.base.ParameterizedUseCase
import retrofit2.Response
import javax.inject.Inject

data class PledgeToOrderParams(
    val orderId: String,
    val amount: Int
)

class PledgeToOrderUseCase @Inject constructor(
    private val orderApiService: OrderApiService
) : ParameterizedUseCase<PledgeToOrderParams, Response<Unit>> {
    
    override suspend fun invoke(parameters: PledgeToOrderParams): Result<Response<Unit>> {
        return try {
            val response = orderApiService.pledgeToOrder(
                PledgeRequest(
                    orderId = parameters.orderId,
                    pledgeAmount = parameters.amount
                )
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
