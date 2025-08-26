package com.bundl.app.domain.usecase.orders

import com.bundl.app.domain.model.OrderStatus
import com.bundl.app.domain.repository.OrderRepository
import com.bundl.app.domain.usecase.base.ParameterizedUseCase
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
