package com.pavit.bundl.domain.usecase.orders

import com.pavit.bundl.domain.model.Order
import com.pavit.bundl.domain.repository.OrderRepository
import com.pavit.bundl.domain.usecase.base.ParameterizedUseCase
import javax.inject.Inject

data class SaveOrderLocallyParams(
    val order: Order
)

class SaveOrderLocallyUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : ParameterizedUseCase<SaveOrderLocallyParams, Unit> {
    
    override suspend fun invoke(parameters: SaveOrderLocallyParams): Result<Unit> {
        return orderRepository.saveOrderLocally(parameters.order)
    }
}
