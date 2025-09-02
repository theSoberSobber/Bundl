package com.pavit.bundl.domain.usecase.orders

import com.pavit.bundl.domain.model.Order
import com.pavit.bundl.domain.repository.OrderRepository
import com.pavit.bundl.domain.usecase.base.UseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class LocalOrdersData(
    val activeOrders: List<Order>,
    val nonActiveOrders: List<Order>
)

class ObserveLocalOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : UseCase<Flow<LocalOrdersData>> {
    
    override suspend fun invoke(): Result<Flow<LocalOrdersData>> {
        return orderRepository.observeLocalOrders()
    }
}
