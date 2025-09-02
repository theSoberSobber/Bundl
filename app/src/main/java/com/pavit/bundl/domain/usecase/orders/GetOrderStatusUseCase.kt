package com.pavit.bundl.domain.usecase.orders

import com.pavit.bundl.domain.model.OrderStatus
import com.pavit.bundl.domain.repository.OrderRepository
import com.pavit.bundl.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject

data class GetOrderStatusParams(
    val orderId: String
)

class GetOrderStatusUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : ParameterizedUseCase<GetOrderStatusParams, OrderStatus> {
    
    override suspend fun invoke(parameters: GetOrderStatusParams): Result<OrderStatus> {
        return orderRepository.getOrderStatus(parameters.orderId)
    }
}
