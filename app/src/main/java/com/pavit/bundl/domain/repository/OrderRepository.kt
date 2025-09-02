package com.pavit.bundl.domain.repository

import com.pavit.bundl.domain.model.Order
import com.pavit.bundl.domain.model.OrderStatus
import com.pavit.bundl.domain.usecase.orders.LocalOrdersData
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun getActiveOrders(latitude: Double, longitude: Double, radiusKm: Double = 0.5): Result<List<Order>>
    suspend fun getOrderStatus(orderId: String): Result<OrderStatus>
    suspend fun observeLocalOrders(): Result<Flow<LocalOrdersData>>
    suspend fun saveOrderLocally(order: Order): Result<Unit>
    suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        totalPledge: String,
        totalUsers: Int,
        pledgeMap: Map<String, Int>,
        phoneNumberMap: Map<String, Int>? = null
    ): Result<Unit>
} 