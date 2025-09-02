package com.pavit.bundl.presentation.orders

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavit.bundl.domain.model.Order
import com.pavit.bundl.domain.usecase.orders.ObserveLocalOrdersUseCase
import com.pavit.bundl.domain.usecase.orders.SaveOrderLocallyUseCase
import com.pavit.bundl.domain.usecase.orders.SaveOrderLocallyParams
import com.pavit.bundl.domain.usecase.orders.GetOrderStatusUseCase
import com.pavit.bundl.domain.usecase.orders.GetOrderStatusParams
import com.pavit.bundl.domain.usecase.orders.UpdateOrderStatusUseCase
import com.pavit.bundl.domain.usecase.orders.UpdateOrderStatusParams
import com.pavit.bundl.domain.usecase.orders.GetActiveOrdersUseCase
import com.pavit.bundl.domain.usecase.orders.GetActiveOrdersParams
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.GsonBuilder

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
    private val observeLocalOrdersUseCase: ObserveLocalOrdersUseCase,
    private val saveOrderLocallyUseCase: SaveOrderLocallyUseCase,
    private val getOrderStatusUseCase: GetOrderStatusUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    private val getActiveOrdersUseCase: GetActiveOrdersUseCase,
    @ApplicationContext private val context: Context
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
        // Start observing orders using use case
        viewModelScope.launch {
            observeLocalOrdersUseCase().fold(
                onSuccess = { flow ->
                    flow.collect { localOrdersData ->
                        Log.d("BUNDL_DEBUG", "Received ${localOrdersData.activeOrders.size} active orders and ${localOrdersData.nonActiveOrders.size} non-active orders")
                        Log.d("BUNDL_DEBUG", "Active order IDs: ${localOrdersData.activeOrders.map { it.id }}")
                        
                        _state.update { 
                            it.copy(
                                localActiveOrders = localOrdersData.activeOrders,
                                localNonActiveOrders = localOrdersData.nonActiveOrders
                            )
                        }
                        
                        // Start polling if we have active orders
                        if (localOrdersData.activeOrders.isNotEmpty() && pollingJob?.isActive != true) {
                            startPolling()
                        }
                    }
                },
                onFailure = { error ->
                    Log.e("BUNDL_DEBUG", "Error observing local orders", error)
                    _state.update { it.copy(error = error.message) }
                }
            )
        }
        
        startCountdownTimer()
    }
    
    private fun showToast(message: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("BUNDL_DEBUG", "Failed to show toast", e)
            }
        }
    }

    fun addOrderToPledged(order: Order) {
        viewModelScope.launch {
            try {
                Log.d("BUNDL_DEBUG", "Adding order to local storage: ${order.id}")
                Log.d("BUNDL_DEBUG", "Order details - status: ${order.status}, platform: ${order.platform}, amount: ${order.amountNeeded}")
                Log.d("BUNDL_DEBUG", "More order details - creatorId: ${order.creatorId}, totalPledge: ${order.totalPledge}, totalUsers: ${order.totalUsers}")
                Log.d("BUNDL_DEBUG", "Location details - lat: ${order.latitude}, lon: ${order.longitude}")
                
                // Set status to ACTIVE when adding new order
                val orderWithStatus = order.copy(status = "ACTIVE")
                Log.d("BUNDL_DEBUG", "Order after status update: $orderWithStatus")
                
                saveOrderLocallyUseCase(SaveOrderLocallyParams(orderWithStatus)).fold(
                    onSuccess = {
                        Log.d("BUNDL_DEBUG", "Successfully added order to local storage: ${order.id}")
                        
                        // Start polling if not already started
                        if (pollingJob?.isActive != true) {
                            Log.d("BUNDL_DEBUG", "Starting polling for order updates")
                            startPolling()
                        }
                    },
                    onFailure = { error ->
                        Log.e("BUNDL_DEBUG", "Error adding order to local storage: ${error.message}")
                        showToast("Error saving order: ${error.message}")
                    }
                )
                
                // Log current active orders from state
                Log.d("BUNDL_DEBUG", "Current active orders in state: ${state.value.localActiveOrders.map { it.id }}")
                
            } catch (e: Exception) {
                Log.e("BUNDL_DEBUG", "Error adding order", e)
                Log.e("BUNDL_DEBUG", "Order that failed: $order")
                showToast("Error adding order: ${e.message}")
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
                    
                    getOrderStatusUseCase(GetOrderStatusParams(order.id)).fold(
                        onSuccess = { orderStatus ->
                            // Log the response
                            val gson = GsonBuilder().setPrettyPrinting().create()
                            Log.d("BUNDL_PHONE_NUMBERS", "Order status for ${order.id}: ${gson.toJson(orderStatus)}")
                            
                            // Parse values
                            val status = orderStatus.status.uppercase()
                            val totalPledgeInt = orderStatus.totalPledge.replace(".00", "").toIntOrNull() ?: 0
                            val amountNeededInt = orderStatus.amountNeeded.replace(".00", "").toIntOrNull() ?: 0
                            val totalUsers = orderStatus.totalUsers
                            
                            Log.d("BUNDL_ORDERS", "Received status: $status for order: ${order.id}")
                            Log.d("BUNDL_ORDERS", "Total pledge: $totalPledgeInt, Amount needed: $amountNeededInt")
                            Log.d("BUNDL_PHONE_NUMBERS", "Response has phoneNumerMap: ${orderStatus.phoneNumberMap != null}, contents: ${orderStatus.phoneNumberMap}")
                            Log.d("BUNDL_PHONE_NUMBERS", "Response has note: ${orderStatus.note != null}, contents: ${orderStatus.note}")

                            // Update order status using use case
                            updateOrderStatusUseCase(
                                UpdateOrderStatusParams(
                                    orderId = order.id,
                                    status = status,
                                    totalPledge = orderStatus.totalPledge,
                                    totalUsers = totalUsers,
                                    pledgeMap = orderStatus.pledgeMap,
                                    phoneNumberMap = orderStatus.phoneNumberMap
                                )
                            ).fold(
                                onSuccess = {
                                    if (status != "ACTIVE") {
                                        Log.d("BUNDL_ORDERS", "Moved order to history: ${order.id}, status: $status")
                                    } else {
                                        Log.d("BUNDL_ORDERS", "Updated active order: ${order.id}, pledge: $totalPledgeInt")
                                    }
                                },
                                onFailure = { error ->
                                    Log.e("BUNDL_ORDERS", "Error updating order status: ${error.message}")
                                }
                            )
                        },
                        onFailure = { error ->
                            Log.e("BUNDL_ORDERS", "Error getting order status for ${order.id}: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e("BUNDL_ORDERS", "Error polling order ${order.id}: ${e.message}")
                    e.printStackTrace()
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
                
                getActiveOrdersUseCase(
                    GetActiveOrdersParams(
                        latitude = 0.0, // TODO: Get actual location
                        longitude = 0.0,
                        radiusKm = state.value.searchRadiusKm
                    )
                ).fold(
                    onSuccess = { orders ->
                        orders.forEach { order ->
                            addOrderToPledged(order)
                        }
                    },
                    onFailure = { error ->
                        Log.e("BUNDL_ORDERS", "Error refreshing orders: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("BUNDL_ORDERS", "Error refreshing orders", e)
            }
        }
    }

    fun loadOrderHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Load orders from local storage using use case
                observeLocalOrdersUseCase().fold(
                    onSuccess = { flow ->
                        flow.collect { localOrdersData ->
                            Log.d("BUNDL_ORDERS", "Loaded ${localOrdersData.activeOrders.size} local active orders")
                            
                            // Log each order's phoneNumberMap
                            localOrdersData.activeOrders.forEach { order ->
                                Log.d("BUNDL_PHONE_NUMBERS", "Local order ${order.id} phone map: ${order.phoneNumberMap}")
                            }
                            
                            _state.update { 
                                it.copy(
                                    localActiveOrders = localOrdersData.activeOrders,
                                    localNonActiveOrders = localOrdersData.nonActiveOrders,
                                    isLoading = false
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e("BUNDL_ORDERS", "Error loading order history: ${error.message}")
                        _state.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
                
            } catch (e: Exception) {
                Log.e("BUNDL_ORDERS", "Error loading order history", e)
                _state.update { it.copy(
                    error = "Error loading orders: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }
} 