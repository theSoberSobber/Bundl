package com.bundl.app.presentation.orders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bundl.app.data.local.OrderDao
import com.bundl.app.data.local.OrderEntity
import com.bundl.app.data.remote.api.OrderApiService
import com.bundl.app.data.remote.api.PledgeRequest
import com.bundl.app.domain.model.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.GsonBuilder

data class OrderStatusResponse(
    val id: String,
    val amountNeeded: String,
    val totalPledge: String,
    val totalUsers: Int,
    val longitude: String,
    val latitude: String,
    val creatorId: String,
    val platform: String,
    val status: String,
    val pledgeMap: Map<String, Int>
)

data class MyOrdersState(
    val localActiveOrders: List<Order> = emptyList(),
    val localNonActiveOrders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val secondsUntilNextPoll: Int = 30,
    val searchRadiusKm: Double = 0.5
)

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val orderApiService: OrderApiService,
    private val orderDao: OrderDao
) : ViewModel() {
    
    private val _state = MutableStateFlow(MyOrdersState())
    val state: StateFlow<MyOrdersState> = _state.asStateFlow()
    
    private var pollingJob: Job? = null
    private var countdownJob: Job? = null
    private val POLLING_INTERVAL = 30_000L // 30 seconds
    
    // Companion object to store orders across ViewModel instances
    companion object {
        // Static storage for active orders
        private val activeOrders = mutableListOf<Order>()
        private val nonActiveOrders = mutableListOf<Order>()
    }
    
    init {
        Log.d("BUNDL_DEBUG", "Initializing MyOrdersViewModel")
        // Start observing active orders from Room DB
        viewModelScope.launch {
            // Combine both active and non-active orders flows
            combine(
                orderDao.getActiveOrders(),
                orderDao.getNonActiveOrders()
            ) { activeEntities, nonActiveEntities ->
                Log.d("BUNDL_DEBUG", "Received ${activeEntities.size} active orders and ${nonActiveEntities.size} non-active orders from DB")
                Log.d("BUNDL_DEBUG", "Active order IDs: ${activeEntities.map { it.orderId }}")
                Pair(activeEntities.map { it.toOrder() }, nonActiveEntities.map { it.toOrder() })
            }.collect { (activeOrders, nonActiveOrders) ->
                Log.d("BUNDL_DEBUG", "Updating state with ${activeOrders.size} active orders and ${nonActiveOrders.size} non-active orders")
                Log.d("BUNDL_DEBUG", "Active order IDs in state update: ${activeOrders.map { it.id }}")
                _state.update { it.copy(
                    localActiveOrders = activeOrders,
                    localNonActiveOrders = nonActiveOrders
                )}
                
                // Start polling if we have active orders
                if (activeOrders.isNotEmpty() && pollingJob?.isActive != true) {
                    startPolling()
                }
            }
        }
        
        startCountdownTimer()
    }
    
    private fun showToast(message: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                val context = com.bundl.app.BundlApplication.getInstance()
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("BUNDL_DEBUG", "Failed to show toast", e)
            }
        }
    }

    fun addOrderToPledged(order: Order) {
        viewModelScope.launch {
            try {
                Log.d("BUNDL_DEBUG", "Adding order to Room DB: ${order.id}")
                Log.d("BUNDL_DEBUG", "Order details before Room - status: ${order.status}, platform: ${order.platform}, amount: ${order.amountNeeded}")
                Log.d("BUNDL_DEBUG", "More order details - creatorId: ${order.creatorId}, totalPledge: ${order.totalPledge}, totalUsers: ${order.totalUsers}")
                Log.d("BUNDL_DEBUG", "Location details - lat: ${order.latitude}, lon: ${order.longitude}")
                showToast("Adding order ${order.id} to Room DB...")
                
                // Set status to ACTIVE when adding new order
                val orderWithStatus = order.copy(status = "ACTIVE")
                Log.d("BUNDL_DEBUG", "Order after status update: $orderWithStatus")
                
                val orderEntity = OrderEntity.fromOrder(orderWithStatus)
                Log.d("BUNDL_DEBUG", "Created OrderEntity: $orderEntity")
                
                orderDao.insertOrder(orderEntity)
                Log.d("BUNDL_DEBUG", "Successfully added order to Room DB: ${order.id}")
                showToast("Successfully added order to Room DB!")
                
                // Start polling if not already started
                if (pollingJob?.isActive != true) {
                    Log.d("BUNDL_DEBUG", "Starting polling for order updates")
                    showToast("Starting polling for order updates...")
                    startPolling()
                } else {
                    Log.d("BUNDL_DEBUG", "Polling already active")
                }

                // Log current active orders from state
                Log.d("BUNDL_DEBUG", "Current active orders in state: ${state.value.localActiveOrders.map { it.id }}")
                
            } catch (e: Exception) {
                Log.e("BUNDL_DEBUG", "Error adding order to Room DB", e)
                Log.e("BUNDL_DEBUG", "Order that failed: $order")
                showToast("Error adding order to Room DB: ${e.message}")
            }
        }
    }
    
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while(true) {
                pollActiveOrders()
                delay(POLLING_INTERVAL)
            }
        }
    }
    
    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while(true) {
                for (i in 30 downTo 1) {
                    _state.update { it.copy(secondsUntilNextPoll = i) }
                    delay(1000)
                }
            }
        }
    }
    
    private suspend fun pollActiveOrders() {
        _state.update { it.copy(isLoading = true) }
        
        try {
            state.value.localActiveOrders.forEach { order ->
                try {
                    Log.d("BUNDL_ORDERS", "Polling status for order: ${order.id}")
                    val response = orderApiService.getOrderStatus(order.id)
                    
                    // Parse values
                    val status = response.status.uppercase()
                    val totalPledgeInt = response.totalPledge.replace(".00", "").toIntOrNull() ?: 0
                    val amountNeededInt = response.amountNeeded.replace(".00", "").toIntOrNull() ?: 0
                    val totalUsers = response.totalUsers
                    
                    Log.d("BUNDL_ORDERS", "Received status: $status for order: ${order.id}")
                    Log.d("BUNDL_ORDERS", "Total pledge: $totalPledgeInt, Amount needed: $amountNeededInt")
                    
                    if (status != "ACTIVE") {
                        // Instead of deleting, update the status to keep it in history
                        orderDao.updateOrderStatus(
                            orderId = order.id,
                            status = status,
                            totalPledge = totalPledgeInt,
                            totalUsers = totalUsers
                        )
                        Log.d("BUNDL_ORDERS", "Moved order to history: ${order.id}, status: $status")
                    } else {
                        // Update order status in Room DB
                        orderDao.updateOrderStatus(
                            orderId = order.id,
                            status = status,
                            totalPledge = totalPledgeInt,
                            totalUsers = totalUsers
                        )
                        Log.d("BUNDL_ORDERS", "Updated active order: ${order.id}, pledge: $totalPledgeInt")
                    }
                } catch (e: Exception) {
                    Log.e("BUNDL_ORDERS", "Error polling order ${order.id}: ${e.message}")
                }
            }
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        countdownJob?.cancel()
    }
    
    fun updateSearchRadius(radiusKm: Double) {
        _state.update { it.copy(searchRadiusKm = radiusKm) }
        refreshOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            try {
                Log.d("BUNDL_ORDERS", "Refreshing orders with radius: ${state.value.searchRadiusKm}km")
                val response = orderApiService.getActiveOrders(
                    latitude = 0.0, // TODO: Get actual location
                    longitude = 0.0,
                    radiusKm = state.value.searchRadiusKm
                )
                
                response.forEach { order ->
                    addOrderToPledged(order)
                }
            } catch (e: Exception) {
                Log.e("BUNDL_ORDERS", "Error refreshing orders", e)
            }
        }
    }
} 