package com.pavit.bundl.domain.usecase.orders

import com.pavit.bundl.domain.model.OrderStatus
import com.pavit.bundl.domain.repository.OrderRepository
import com.pavit.bundl.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject

data class UpdateOrderStatusParams(
    val orderId: String,
    val status: String,
    val totalPledge: String,
    val totalUsers: Int,
    val pledgeMap: Map<String, Int>,
    val phoneNumberMap: Map<String, Int>? = null
)

class UpdateOrderStatusUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : ParameterizedUseCase<UpdateOrderStatusParams, Unit> {
    
    override suspend fun invoke(parameters: UpdateOrderStatusParams): Result<Unit> {
        return orderRepository.updateOrderStatus(
            orderId = parameters.orderId,
            status = parameters.status,
            totalPledge = parameters.totalPledge,
            totalUsers = parameters.totalUsers,
            pledgeMap = parameters.pledgeMap,
            phoneNumberMap = parameters.phoneNumberMap
        )
    }
}
