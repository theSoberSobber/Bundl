package com.bundl.app.data.local

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.google.gson.Gson

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
    
    // New method to also update phone number map and note
    suspend fun updateOrderStatusWithPhoneMap(
        orderId: String, 
        status: String, 
        totalPledge: Int, 
        totalUsers: Int,
        phoneNumberMap: Map<String, Int>?,
        note: String?
    ) {
        val converters = Converters()
        val phoneNumberMapString = phoneNumberMap?.let { converters.fromMap(it) }
        
        // Log what we're updating
        Log.d("BUNDL_PHONE_NUMBERS", "Updating order $orderId with phoneNumberMap: $phoneNumberMap")
        Log.d("BUNDL_PHONE_NUMBERS", "Converted to string: $phoneNumberMapString")
        
        updateOrderStatusFull(
            orderId = orderId,
            status = status,
            totalPledge = totalPledge,
            totalUsers = totalUsers,
            phoneNumberMap = phoneNumberMapString,
            note = note
        )
    }
    
    @Query("UPDATE active_orders SET status = :status, totalPledge = :totalPledge, totalUsers = :totalUsers, phoneNumberMap = :phoneNumberMap, note = :note WHERE orderId = :orderId")
    suspend fun updateOrderStatusFull(
        orderId: String, 
        status: String, 
        totalPledge: Int, 
        totalUsers: Int,
        phoneNumberMap: String?,
        note: String?
    )
} 