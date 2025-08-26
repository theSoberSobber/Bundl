package com.bundl.app.domain.usecase.orders

import com.bundl.app.domain.model.Order
import com.bundl.app.domain.repository.OrderRepository
import com.bundl.app.domain.usecase.base.UseCase
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
