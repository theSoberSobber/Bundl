package com.bundl.app.domain.usecase.orders

import com.bundl.app.domain.model.Order
import com.bundl.app.data.remote.api.OrderApiService
import com.bundl.app.data.remote.api.CreateOrderRequest
import com.bundl.app.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject

data class CreateOrderParams(
    val amountNeeded: Double,
    val platform: String,
    val latitude: Double,
    val longitude: Double,
    val initialPledge: Int,
    val expirySeconds: Int = 600
)

class CreateOrderUseCase @Inject constructor(
    private val orderApiService: OrderApiService
) : ParameterizedUseCase<CreateOrderParams, Order> {
    
    override suspend fun invoke(parameters: CreateOrderParams): Result<Order> {
        return try {
            val response = orderApiService.createOrder(
                CreateOrderRequest(
                    amountNeeded = parameters.amountNeeded,
                    platform = parameters.platform,
                    latitude = parameters.latitude,
                    longitude = parameters.longitude,
                    initialPledge = parameters.initialPledge,
                    expirySeconds = parameters.expirySeconds
                )
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
