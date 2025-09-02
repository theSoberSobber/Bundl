package com.pavit.bundl.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.pavit.bundl.domain.model.Order
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class Converters {
    @TypeConverter
    fun fromString(value: String?): Map<String, Int>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, Int>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMap(map: Map<String, Int>?): String? {
        if (map == null) return null
        return Gson().toJson(map)
    }
}

@Entity(tableName = "active_orders")
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val status: String,
    val creatorId: String,
    val amountNeeded: Int,
    val totalPledge: Int,
    val totalUsers: Int,
    val platform: String,
    val latitude: Double,
    val longitude: Double,
    val phoneNumberMap: String? = null,
    val note: String? = null,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
) {
    fun toOrder(): Order {
        return Order(
            id = orderId,
            status = status,
            creatorId = creatorId,
            amountNeeded = amountNeeded,
            pledgeMap = emptyMap(), // We don't store pledge map locally
            phoneNumberMap = phoneNumberMap?.let { Converters().fromString(it) },
            note = note,
            totalPledge = totalPledge,
            totalUsers = totalUsers,
            platform = platform,
            latitude = latitude,
            longitude = longitude,
            createdAt = createdAt
        )
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        
        fun fromOrder(order: Order): OrderEntity {
            val timestamp = order.createdAt ?: dateFormat.format(Date())
            return OrderEntity(
                orderId = order.id,
                status = order.status,
                creatorId = order.creatorId,
                amountNeeded = order.amountNeeded,
                totalPledge = order.totalPledge,
                totalUsers = order.totalUsers,
                platform = order.platform,
                latitude = order.latitude,
                longitude = order.longitude,
                phoneNumberMap = order.phoneNumberMap?.let { Converters().fromMap(it) },
                note = order.note,
                createdAt = timestamp
            )
        }
    }
} 