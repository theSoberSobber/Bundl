package com.bundl.app.domain.usecase.orders

import com.bundl.app.domain.model.Order
import com.bundl.app.domain.repository.OrderRepository
import com.bundl.app.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject

data class GetActiveOrdersParams(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 0.5
)

class GetActiveOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : ParameterizedUseCase<GetActiveOrdersParams, List<Order>> {
    
    override suspend fun invoke(parameters: GetActiveOrdersParams): Result<List<Order>> {
        return orderRepository.getActiveOrders(
            latitude = parameters.latitude,
            longitude = parameters.longitude,
            radiusKm = parameters.radiusKm
        )
    }
}
