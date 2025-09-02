package com.pavit.bundl.data.repository

import android.util.Log
import com.pavit.bundl.data.local.OrderDao
import com.pavit.bundl.data.local.OrderEntity
import com.pavit.bundl.data.remote.api.OrderApiService
import com.pavit.bundl.data.remote.dto.OrderStatusResponse
import com.pavit.bundl.domain.model.Order
import com.pavit.bundl.domain.model.OrderStatus
import com.pavit.bundl.domain.repository.OrderRepository
import com.pavit.bundl.domain.usecase.orders.LocalOrdersData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

fun OrderStatusResponse.toOrderStatus(): OrderStatus {
    return OrderStatus(
        id = this.id,
        amountNeeded = this.amountNeeded,
        totalPledge = this.totalPledge,
        totalUsers = this.totalUsers,
        longitude = this.longitude,
        latitude = this.latitude,
        creatorId = this.creatorId,
        platform = this.platform,
        status = this.status,
        pledgeMap = this.pledgeMap,
        phoneNumberMap = this.phoneNumerMap,
        note = this.note
    )
}

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderApiService: OrderApiService,
    private val orderDao: OrderDao
) : OrderRepository {
    
    companion object {
        private const val TAG = "OrderRepositoryImpl"
    }
    
    override suspend fun getActiveOrders(latitude: Double, longitude: Double, radiusKm: Double): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val orders = orderApiService.getActiveOrders(latitude, longitude, radiusKm)
            
            // Process orders to add additional data
            val processedOrders = orders.map { order ->
                // Calculate distance - would normally come from backend
                val distance = calculateDistance(
                    lat1 = latitude,
                    lon1 = longitude,
                    lat2 = order.latitude,
                    lon2 = order.longitude
                )
                
                // Add calculated fields
                order.copy(
                    totalAmount = order.amountNeeded, // Using amountNeeded as totalAmount for now
                    distanceKm = distance,
                    createdAt = getCurrentDate() // Using current date as we don't have real created date
                )
            }
            
            Log.d(TAG, "Fetched ${processedOrders.size} active orders")
            Result.success(processedOrders)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active orders", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculate approximate distance between two coordinates using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radiusOfEarth = 6371.0 // Earth radius in kilometers
        
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return radiusOfEarth * c
    }
    
    /**
     * Get current date in YYYY-MM-DD format
     */
    private fun getCurrentDate(): String {
        val currentDate = Date()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd")
        return dateFormat.format(currentDate)
    }
    
    override suspend fun getOrderStatus(orderId: String): Result<OrderStatus> = withContext(Dispatchers.IO) {
        try {
            val response = orderApiService.getOrderStatus(orderId)
            Result.success(response.toOrderStatus())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching order status for $orderId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun observeLocalOrders(): Result<Flow<LocalOrdersData>> {
        return try {
            val flow = combine(
                orderDao.getActiveOrders(),
                orderDao.getNonActiveOrders()
            ) { activeEntities, nonActiveEntities ->
                LocalOrdersData(
                    activeOrders = activeEntities.map { it.toOrder() },
                    nonActiveOrders = nonActiveEntities.map { it.toOrder() }
                )
            }
            Result.success(flow)
        } catch (e: Exception) {
            Log.e(TAG, "Error observing local orders", e)
            Result.failure(e)
        }
    }
    
    override suspend fun saveOrderLocally(order: Order): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val orderWithStatus = order.copy(status = "ACTIVE")
            val orderEntity = OrderEntity.fromOrder(orderWithStatus)
            orderDao.insertOrder(orderEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving order locally", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        totalPledge: String,
        totalUsers: Int,
        pledgeMap: Map<String, Int>,
        phoneNumberMap: Map<String, Int>?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Convert string totalPledge to Int
            val totalPledgeInt = totalPledge.replace(".00", "").toIntOrNull() ?: 0
            
            orderDao.updateOrderStatusWithPhoneMap(
                orderId = orderId,
                status = status,
                totalPledge = totalPledgeInt,
                totalUsers = totalUsers,
                phoneNumberMap = phoneNumberMap,
                note = null // Add note parameter if needed
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order status", e)
            Result.failure(e)
        }
    }
} 