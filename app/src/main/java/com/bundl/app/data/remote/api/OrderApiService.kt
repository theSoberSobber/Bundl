package com.bundl.app.data.remote.api

import com.bundl.app.domain.model.Order
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import com.bundl.app.presentation.orders.OrderStatusResponse

data class PledgeRequest(
    val orderId: String,
    val pledgeAmount: Int
)

data class CreateOrderRequest(
    val amountNeeded: Double,
    val platform: String,
    val latitude: Double,
    val longitude: Double,
    val initialPledge: Int,
    val expirySeconds: Int
)

interface OrderApiService {
    @POST("orders/createOrder")
    suspend fun createOrder(@Body request: CreateOrderRequest): Order

    @GET("/orders/activeOrders")
    suspend fun getActiveOrders(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double = 0.5
    ): List<Order>

    @POST("orders/pledgeToOrder")
    suspend fun pledgeToOrder(
        @Body request: PledgeRequest
    ): Response<Unit>

    @GET("orders/orderStatus/{orderId}")
    suspend fun getOrderStatus(@Path("orderId") orderId: String): OrderStatusResponse
} 