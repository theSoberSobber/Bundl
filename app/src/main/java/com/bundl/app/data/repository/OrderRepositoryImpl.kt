package com.bundl.app.data.repository

import android.util.Log
import com.bundl.app.data.remote.api.OrderApiService
import com.bundl.app.domain.model.Order
import com.bundl.app.domain.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderApiService: OrderApiService
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
} 