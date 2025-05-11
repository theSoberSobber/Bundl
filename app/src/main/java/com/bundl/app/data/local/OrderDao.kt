package com.bundl.app.data.local

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM active_orders WHERE UPPER(status) = 'ACTIVE'")
    fun getActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM active_orders WHERE UPPER(status) != 'ACTIVE'")
    fun getNonActiveOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("DELETE FROM active_orders WHERE orderId = :orderId")
    suspend fun deleteOrder(orderId: String)

    @Query("UPDATE active_orders SET status = :status, totalPledge = :totalPledge, totalUsers = :totalUsers WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, totalPledge: Int, totalUsers: Int)
} 