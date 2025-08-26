package com.bundl.app.domain.usecase.orders

import com.bundl.app.domain.model.Order
import com.bundl.app.domain.repository.OrderRepository
import com.bundl.app.domain.usecase.base.ParameterizedUseCase
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
