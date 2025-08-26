package com.bundl.app.presentation.dashboard

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.bundl.app.data.remote.api.ApiKeyService
import com.bundl.app.data.remote.api.AuthApiService
import com.bundl.app.data.remote.api.OrderApiService
import com.bundl.app.data.remote.api.PledgeRequest
import com.bundl.app.data.remote.api.CreateOrderRequest
import com.bundl.app.domain.maps.MapProvider
import com.bundl.app.domain.model.Order
import com.bundl.app.domain.model.UserStats
import com.bundl.app.domain.repository.ApiKeyRepository
import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.domain.repository.OrderRepository
import com.bundl.app.utils.DeviceUtils
import com.bundl.app.utils.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bundl.app.presentation.navigation.Route
import javax.inject.Inject

data class HomeState(
    val activeOrders: List<Order> = emptyList(),
    val selectedOrderId: String? = null,
    val userCredits: Int = 0,
    val cashbackPoints: Int = 0,
    val creditMode: String = "moderate",
    val isLoading: Boolean = false,
    val isUpdatingCreditMode: Boolean = false,
    val secondsUntilRefresh: Int = 30,
    val errorMessage: String? = null,
    val userStats: UserStats? = null,
    val isLoadingOrders: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val deviceUtils: DeviceUtils,
    private val apiKeyService: ApiKeyService,
    private val authApiService: AuthApiService,
    private val orderApiService: OrderApiService,
    private val mapProvider: MapProvider,
    private val locationManager: LocationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    private var creditsRefreshJob: Job? = null
    private var countdownJob: Job? = null
    private val creditsRefreshInterval = 30000L // 30 seconds
    
    // Store initial location
    private var initialLocation = locationManager.currentLocation.value
    
    private var hasInitiallyFitMap = false  // Add this flag at the class level near other private vars
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    init {
        // Get FCM token and register device if user is logged in
        viewModelScope.launch {
            if (authRepository.isLoggedIn().first()) {
                Log.d(TAG, "User is logged in, proceeding with initialization")
                getFcmToken()
                fetchUserStats()
                fetchCreditsInfo()
                fetchActiveOrders()
                
                // Add a delay and refresh credits again to ensure they're loaded
                delay(2000)
                Log.d(TAG, "Refreshing credits after delay to ensure they're loaded")
                Log.d("BUNDL_CREDITS", "Performing delayed refresh to ensure credits are loaded")
                fetchCreditsInfo()
                
                startCreditsPolling()
                startCountdownTimer()
                
                // Start observing location changes
                observeLocationChanges()
            } else {
                Log.d(TAG, "User is not logged in, skipping initialization")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCreditsPolling()
        stopCountdownTimer()
        
        // Stop location updates when ViewModel is cleared
        locationManager.stopLocationUpdates()
        Log.d(TAG, "ViewModel cleared, stopping location updates")
    }
    
    private fun observeLocationChanges() {
        // Start location updates to track user movement
        locationManager.startLocationUpdates()
        
        // Observe location changes continually but only update markers, not the camera
        viewModelScope.launch {
            locationManager.currentLocation.collectLatest { locationData ->
                if (locationData.isFromUser) {
                    // Store updated location
                    initialLocation = locationData
                    
                    // Fetch orders at new location
                    Log.d(TAG, "Location updated to: ${locationData.latitude}, ${locationData.longitude}. Fetching orders at new location.")
                    fetchActiveOrders(showLoading = false)
                    
                    // Note: We don't update the camera, just the markers
                }
            }
        }
    }
    
    fun setCreditMode(mode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdatingCreditMode = true) }
            try {
                val request = mapOf("mode" to mode)
                val response = apiKeyService.setCreditMode(request)
                if (response["success"] == true) {
                    _state.update { it.copy(creditMode = mode) }
                    Log.d(TAG, "Credit mode updated to: $mode")
                    // Refresh stats after mode change
                    fetchUserStats(showLoading = false)
                } else {
                    _state.update { it.copy(errorMessage = "Failed to update credit mode") }
                    Log.e(TAG, "Failed to update credit mode: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating credit mode", e)
                _state.update { it.copy(errorMessage = "Error updating credit mode: ${e.message}") }
            } finally {
                _state.update { it.copy(isUpdatingCreditMode = false) }
            }
        }
    }

    fun refreshCredits() {
        fetchCreditsInfo()
        fetchUserStats()
        fetchActiveOrders()
    }
    
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
    
    private fun startCreditsPolling() {
        stopCreditsPolling() // Stop any existing polling
        
        creditsRefreshJob = viewModelScope.launch {
            while (true) {
                delay(creditsRefreshInterval)
                Log.d(TAG, "Auto-refreshing credits info")
                fetchCreditsInfo(showLoading = false)
                fetchUserStats(showLoading = false)
                fetchActiveOrders(showLoading = false)
                resetCountdown()
            }
        }
    }
    
    private fun stopCreditsPolling() {
        creditsRefreshJob?.cancel()
        creditsRefreshJob = null
    }
    
    private fun startCountdownTimer() {
        stopCountdownTimer() // Stop any existing timer
        
        countdownJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                _state.update { 
                    val current = it.secondsUntilRefresh
                if (current > 0) {
                        it.copy(secondsUntilRefresh = current - 1)
                } else {
                        it.copy(secondsUntilRefresh = 30) // Reset to 30 seconds
                    }
                }
            }
        }
    }
    
    private fun stopCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = null
    }
    
    private fun resetCountdown() {
        _state.update { it.copy(secondsUntilRefresh = 30) }
    }
    
    private fun getFcmToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                _fcmToken.value = token
                Log.d(TAG, "FCM Token retrieved: $token")
                registerDevice(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting FCM token", e)
                _state.update { it.copy(errorMessage = "Failed to get FCM token: ${e.message}") }
            }
        }
    }
    
    private suspend fun registerDevice(fcmToken: String) {
        try {
            val deviceId = deviceUtils.getDeviceHash()
            val deviceInfo = "Android"
            
            // This API endpoint doesn't exist yet, so we'll just log the intent
            Log.d(TAG, "Would register device with ID: $deviceId and token: $fcmToken")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing device registration", e)
            _state.update { it.copy(errorMessage = "Error preparing device registration: ${e.message}") }
        }
    }
    
    fun fetchCreditsInfo(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoading = true) }
            }
            
            try {
                // Only fetch credits using the existing endpoint - don't fetch credit mode
                val creditsResponse = apiKeyService.getCredits()
                
                // Special debugging for credits response
                Log.d(TAG, "CREDITS RESPONSE RAW: $creditsResponse")
                Log.d("BUNDL_CREDITS", "CREDITS RESPONSE RAW: $creditsResponse")
                Log.d(TAG, "CREDITS TYPE: ${creditsResponse.javaClass}")
                Log.d("BUNDL_CREDITS", "CREDITS TYPE: ${creditsResponse.javaClass}")
                Log.d(TAG, "CREDITS VALUE: ${creditsResponse.credits}")
                Log.d("BUNDL_CREDITS", "CREDITS VALUE: ${creditsResponse.credits}")
                
                // Update state in a more direct way to ensure it's actually updated
                val newCredits = creditsResponse.credits
                
                _state.update { 
                    Log.d(TAG, "UPDATING STATE: Old credits=${it.userCredits}, New credits=$newCredits")
                    Log.d("BUNDL_CREDITS", "UPDATING STATE: Old credits=${it.userCredits}, New credits=$newCredits")
                    
                    it.copy(
                        userCredits = newCredits,
                        cashbackPoints = 0 // Not available in current API
                        // Don't update creditMode since endpoint doesn't exist
                    )
                }
                
                // Verify the update happened
                Log.d(TAG, "CREDITS STATE AFTER UPDATE: ${_state.value.userCredits}")
                Log.d("BUNDL_CREDITS", "CREDITS STATE AFTER UPDATE: ${_state.value.userCredits}")
                
                // Log the current state after update
                Log.d(TAG, "State after update: userCredits=${_state.value.userCredits}")
                Log.d("BUNDL_CREDITS", "State after update: userCredits=${_state.value.userCredits}")
                
                Log.d(TAG, "Credits info retrieved: ${creditsResponse.credits} credits")
                Log.d("BUNDL_CREDITS", "API Response - Credits: ${creditsResponse.credits}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching credits info", e)
                Log.e("BUNDL_CREDITS", "API Error: ${e.message}")
                _state.update { it.copy(errorMessage = "Failed to fetch credits: ${e.message}") }
            } finally {
                if (showLoading) {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    
    private fun showToast(message: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show toast", e)
            }
        }
    }
    
    private fun fetchUserStats(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoading = true) }
            }
            
            try {
                val stats = authApiService.getUserStats()
                _state.update { it.copy(userStats = stats) }
                Log.d(TAG, "User stats retrieved: $stats")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user stats", e)
                _state.update { it.copy(errorMessage = "Failed to fetch user stats: ${e.message}") }
            } finally {
                if (showLoading) {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    
    fun fetchActiveOrders(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoadingOrders = true) }
            }
            
            try {
                // Use initial location instead of current location
                val location = initialLocation
                orderRepository.getActiveOrders(
                    latitude = location.latitude,
                    longitude = location.longitude
                ).fold(
                    onSuccess = { orders ->
                        _state.update { it.copy(activeOrders = orders) }
                        Log.d(TAG, "Active orders retrieved: ${orders.size} orders")
                        
                        // Only fit map on initial load
                        if (!hasInitiallyFitMap) {
                            fitMapToOrders(orders, location.latitude, location.longitude)
                            hasInitiallyFitMap = true
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error fetching active orders", e)
                        _state.update { it.copy(errorMessage = "Failed to fetch active orders: ${e.message}") }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching active orders", e)
                _state.update { it.copy(errorMessage = "Failed to fetch active orders: ${e.message}") }
            } finally {
                if (showLoading) {
                    _state.update { it.copy(isLoadingOrders = false) }
                }
            }
        }
    }
    
    private fun fitMapToOrders(orders: List<Order>, userLat: Double, userLon: Double) {
        // If no orders, use a much smaller zoom area
        if (orders.isEmpty()) {
            mapProvider.animateCamera(
                latitude = userLat,
                longitude = userLon,
                zoom = 16.0, // More zoomed in for 0.5km radius
                duration = 1000,
                paddingBottom = 300f
            )
            return
        }

        // Calculate the bounds that include all orders and user location
        var minLat = userLat
        var maxLat = userLat
        var minLon = userLon
        var maxLon = userLon

        orders.forEach { order ->
            minLat = minOf(minLat, order.latitude)
            maxLat = maxOf(maxLat, order.latitude)
            minLon = minOf(minLon, order.longitude)
            maxLon = maxOf(maxLon, order.longitude)
        }

        // Calculate center point
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        // Calculate appropriate zoom level based on the bounds
        val latDiff = maxLat - minLat
        val lonDiff = maxLon - minLon
        val maxDiff = maxOf(latDiff, lonDiff)
        
        // Adjusted zoom levels for 0.5km radius (approximately doubled from previous values)
        val zoom = when {
            maxDiff > 0.05 -> 13.0  // Very spread out
            maxDiff > 0.025 -> 13.7 // Moderately spread
            maxDiff > 0.01 -> 14.4 // Somewhat close
            maxDiff > 0.005 -> 15.0 // Close together
            else -> 15.5          // Very close together
        }

        // Add padding to account for the bottom sheet (50% of screen height)
        mapProvider.animateCamera(
            latitude = centerLat,
            longitude = centerLon,
            zoom = zoom,
            duration = 1000,
            paddingBottom = 300f  // Approximate padding for bottom sheet
        )
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                Log.d(TAG, "User logged out successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                _state.update { it.copy(errorMessage = "Error during logout: ${e.message}") }
            }
        }
    }
    
    fun centerMapOnUserLocation() {
        val location = initialLocation
        val currentOrders = _state.value.activeOrders
        fitMapToOrders(currentOrders, location.latitude, location.longitude)
    }
    
    // Centers the map on user location with a specific zoom level
    fun centerMapWithZoom(zoomLevel: Double) {
        // Use the stored initial location rather than getting the current one
        val location = initialLocation
        mapProvider.zoomToLocation(location.latitude, location.longitude, zoomLevel)
    }
    
    /**
     * Center the user's location in the visible area of the map (bounding box)
     * rather than center of the entire screen
     * 
     * @param visibleMapHeightDp The height of the visible map area in dp
     * @param screenHeightDp The total screen height in dp
     */
    fun centerMapOnUserLocationInVisibleArea(visibleMapHeightDp: Float, screenHeightDp: Float) {
        val location = initialLocation
        val currentOrders = _state.value.activeOrders
        fitMapToOrders(currentOrders, location.latitude, location.longitude)
    }
    
    // Animate the map camera with the specified parameters
    fun animateMapCamera(
        targetZoom: Double? = null,
        duration: Long = 300
    ) {
        // Use the stored initial location rather than getting the current one
        val location = initialLocation
        mapProvider.animateCamera(
            latitude = location.latitude,
            longitude = location.longitude,
            zoom = targetZoom,
            duration = duration
        )
    }
    
    fun selectOrderOnMap(order: Order) {
        // Store the selected order ID
        _state.update { it.copy(selectedOrderId = order.id) }
        
        mapProvider.setSelectedOrder(order.id)
        
        // Get current location
        val location = initialLocation
        
        // Calculate midpoint between user and order
        val centerLat = (location.latitude + order.latitude) / 2
        val centerLon = (location.longitude + order.longitude) / 2
        
        // Use a more zoomed in level for better visibility of the selected order
        val currentZoom = 16.0  // Increased from 14.5 to zoom in more
        
        // Animate to new position
        mapProvider.animateCamera(
            latitude = centerLat,
            longitude = centerLon,
            zoom = currentZoom,
            duration = 500,
            paddingBottom = 300f  // Keep consistent padding
        )
    }
    
    /**
     * Check if a specific order is currently selected
     * @param orderId The ID of the order to check
     * @return true if this order is selected, false otherwise
     */
    fun isOrderSelected(orderId: String): Boolean {
        return _state.value.selectedOrderId == orderId
    }
    
    /**
     * Check if any order is currently selected
     * @return true if an order is selected, false otherwise
     */
    fun hasSelectedOrder(): Boolean {
        return _state.value.selectedOrderId != null
    }
    
    /**
     * Get the ID of the currently selected order
     * @return the selected order ID or empty string if no order is selected
     */
    fun getSelectedOrderId(): String {
        return _state.value.selectedOrderId ?: ""
    }
    
    fun pledgeToOrder(orderId: String, amount: Int, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = orderApiService.pledgeToOrder(PledgeRequest(orderId = orderId, pledgeAmount = amount))
                
                if (response.isSuccessful) {
                    // Find the order we pledged to
                    val order = _state.value.activeOrders.find { it.id == orderId }
                    
                    // If order exists, pass it to the success callback to be added to MyOrdersViewModel
                    order?.let { 
                        // Navigate to MyOrdersScreen and pass the order
                        onSuccess(Route.MyOrders.route)
                    }
                } else {
                    Log.e(TAG, "Failed to pledge to order: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pledging to order", e)
            }
        }
    }
    
    fun createOrder(amountNeeded: Double, platform: String, initialPledge: Int, onSuccess: (String, Order) -> Unit) {
        viewModelScope.launch {
            try {
                val location = initialLocation
                Log.d("BUNDL_DEBUG", "Creating order with: amount=$amountNeeded, platform=$platform, pledge=$initialPledge")
                Log.d("BUNDL_DEBUG", "Using location: lat=${location.latitude}, lon=${location.longitude}")
                
                val response = orderApiService.createOrder(
                    CreateOrderRequest(
                        amountNeeded = amountNeeded,
                        platform = platform,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        initialPledge = initialPledge,
                        expirySeconds = 600
                    )
                )
                
                Log.d("BUNDL_DEBUG", "Order created successfully: ${response.id}")
                Log.d("BUNDL_DEBUG", "Full order response: $response")
                
                // Refresh active orders before navigating
                fetchActiveOrders()
                
                Log.d("BUNDL_DEBUG", "Calling onSuccess callback with route and order")
                onSuccess(Route.MyOrders.route, response)
                
            } catch (e: Exception) {
                Log.e("BUNDL_DEBUG", "Error creating order", e)
                showToast("Error creating order: ${e.message}")
                _state.update { it.copy(errorMessage = e.message ?: "Error creating order") }
            }
        }
    }
} 