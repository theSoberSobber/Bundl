package com.bundl.app.domain.repository

import com.bundl.app.domain.model.Order

interface OrderRepository {
    suspend fun getActiveOrders(latitude: Double, longitude: Double, radiusKm: Double = 0.5): Result<List<Order>>
} 