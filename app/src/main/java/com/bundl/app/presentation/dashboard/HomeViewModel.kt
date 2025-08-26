package com.bundl.app.presentation.dashboard

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bundl.app.domain.model.Order
import com.bundl.app.domain.model.UserStats
import com.bundl.app.domain.usecase.auth.CheckLoginStatusUseCase
import com.bundl.app.domain.usecase.auth.GetUserStatsUseCase
import com.bundl.app.domain.usecase.auth.LogoutUseCase
import com.bundl.app.domain.usecase.credits.GetCreditsUseCase
import com.bundl.app.domain.usecase.credits.SetCreditModeParams
import com.bundl.app.domain.usecase.credits.SetCreditModeUseCase
import com.bundl.app.domain.usecase.device.GetDeviceInfoUseCase
import com.bundl.app.domain.usecase.location.GetCurrentLocationUseCase
import com.bundl.app.domain.usecase.location.StartLocationUpdatesUseCase
import com.bundl.app.domain.usecase.location.StopLocationUpdatesUseCase
import com.bundl.app.domain.usecase.maps.AnimateCameraParams
import com.bundl.app.domain.usecase.maps.AnimateCameraUseCase
import com.bundl.app.domain.usecase.maps.FitMapToOrdersParams
import com.bundl.app.domain.usecase.maps.FitMapToOrdersUseCase
import com.bundl.app.domain.usecase.maps.SelectOrderOnMapParams
import com.bundl.app.domain.usecase.maps.SelectOrderOnMapUseCase
import com.bundl.app.domain.usecase.maps.ZoomToLocationParams
import com.bundl.app.domain.usecase.maps.ZoomToLocationUseCase
import com.bundl.app.domain.usecase.orders.CreateOrderParams
import com.bundl.app.domain.usecase.orders.CreateOrderUseCase
import com.bundl.app.domain.usecase.orders.GetActiveOrdersParams
import com.bundl.app.domain.usecase.orders.GetActiveOrdersUseCase
import com.bundl.app.domain.usecase.orders.PledgeToOrderParams
import com.bundl.app.domain.usecase.orders.PledgeToOrderUseCase
import com.bundl.app.domain.repository.LocationData
import com.bundl.app.presentation.navigation.Route
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
    private val checkLoginStatusUseCase: CheckLoginStatusUseCase,
    private val getUserStatsUseCase: GetUserStatsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCreditsUseCase: GetCreditsUseCase,
    private val setCreditModeUseCase: SetCreditModeUseCase,
    private val getActiveOrdersUseCase: GetActiveOrdersUseCase,
    private val pledgeToOrderUseCase: PledgeToOrderUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val startLocationUpdatesUseCase: StartLocationUpdatesUseCase,
    private val stopLocationUpdatesUseCase: StopLocationUpdatesUseCase,
    private val getDeviceInfoUseCase: GetDeviceInfoUseCase,
    private val fitMapToOrdersUseCase: FitMapToOrdersUseCase,
    private val selectOrderOnMapUseCase: SelectOrderOnMapUseCase,
    private val animateCameraUseCase: AnimateCameraUseCase,
    private val zoomToLocationUseCase: ZoomToLocationUseCase,
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
    private var initialLocation = LocationData(12.9716, 77.5946, false)
    
    private var hasInitiallyFitMap = false  // Add this flag at the class level near other private vars
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    init {
        // Get FCM token and register device if user is logged in
        viewModelScope.launch {
            checkLoginStatusUseCase().fold(
                onSuccess = { isLoggedIn ->
                    if (isLoggedIn) {
                        Log.d(TAG, "User is logged in, proceeding with initialization")
                        initializeApp()
                    } else {
                        Log.d(TAG, "User is not logged in, skipping initialization")
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Error checking login status", e)
                    _state.update { it.copy(errorMessage = "Error checking login status: ${e.message}") }
                }
            )
        }
    }
    
    private suspend fun initializeApp() {
        try {
            // Get device info (includes FCM token)
            getDeviceInfoUseCase().fold(
                onSuccess = { deviceInfo ->
                    _fcmToken.value = deviceInfo.fcmToken
                    Log.d(TAG, "Device info retrieved: ${deviceInfo.deviceId}, FCM: ${deviceInfo.fcmToken}")
                    // TODO: Register device with backend when API is available
                },
                onFailure = { e ->
                    Log.e(TAG, "Error getting device info", e)
                    _state.update { it.copy(errorMessage = "Failed to get device info: ${e.message}") }
                }
            )
            
            // Fetch initial data
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
        } catch (e: Exception) {
            Log.e(TAG, "Error during app initialization", e)
            _state.update { it.copy(errorMessage = "Error during initialization: ${e.message}") }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCreditsPolling()
        stopCountdownTimer()
        
        // Stop location updates when ViewModel is cleared
        viewModelScope.launch {
            stopLocationUpdatesUseCase().fold(
                onSuccess = {
                    Log.d(TAG, "Location updates stopped successfully")
                },
                onFailure = { e ->
                    Log.e(TAG, "Error stopping location updates", e)
                }
            )
        }
        Log.d(TAG, "ViewModel cleared")
    }
    
    private fun observeLocationChanges() {
        viewModelScope.launch {
            // Start location updates
            startLocationUpdatesUseCase().fold(
                onSuccess = {
                    Log.d(TAG, "Location updates started successfully")
                },
                onFailure = { e ->
                    Log.e(TAG, "Error starting location updates", e)
                }
            )
            
            // Observe location changes continually but only update markers, not the camera
            getCurrentLocationUseCase.asFlow().collectLatest { locationData ->
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
            
            setCreditModeUseCase(SetCreditModeParams(mode)).fold(
                onSuccess = {
                    _state.update { it.copy(creditMode = mode) }
                    Log.d(TAG, "Credit mode updated to: $mode")
                    // Refresh stats after mode change
                    fetchUserStats(showLoading = false)
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to update credit mode", e)
                    _state.update { it.copy(errorMessage = "Failed to update credit mode: ${e.message}") }
                }
            )
            
            _state.update { it.copy(isUpdatingCreditMode = false) }
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
    
    fun fetchCreditsInfo(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoading = true) }
            }
            
            getCreditsUseCase().fold(
                onSuccess = { creditsResponse ->
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
                },
                onFailure = { e ->
                    Log.e(TAG, "Error fetching credits info", e)
                    Log.e("BUNDL_CREDITS", "API Error: ${e.message}")
                    _state.update { it.copy(errorMessage = "Failed to fetch credits: ${e.message}") }
                }
            )
            
            if (showLoading) {
                _state.update { it.copy(isLoading = false) }
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
            
            getUserStatsUseCase().fold(
                onSuccess = { stats ->
                    _state.update { it.copy(userStats = stats) }
                    Log.d(TAG, "User stats retrieved: $stats")
                },
                onFailure = { e ->
                    Log.e(TAG, "Error fetching user stats", e)
                    _state.update { it.copy(errorMessage = "Failed to fetch user stats: ${e.message}") }
                }
            )
            
            if (showLoading) {
                _state.update { it.copy(isLoading = false) }
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
                
                getActiveOrdersUseCase(
                    GetActiveOrdersParams(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
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
        viewModelScope.launch {
            fitMapToOrdersUseCase(
                FitMapToOrdersParams(
                    orders = orders,
                    userLatitude = userLat,
                    userLongitude = userLon
                )
            ).fold(
                onSuccess = {
                    Log.d(TAG, "Map fitted to orders successfully")
                },
                onFailure = { e ->
                    Log.e(TAG, "Error fitting map to orders", e)
                }
            )
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            logoutUseCase().fold(
                onSuccess = {
                    Log.d(TAG, "User logged out successfully")
                },
                onFailure = { e ->
                    Log.e(TAG, "Error during logout", e)
                    _state.update { it.copy(errorMessage = "Error during logout: ${e.message}") }
                }
            )
        }
    }
    
    fun centerMapOnUserLocation() {
        val location = initialLocation
        val currentOrders = _state.value.activeOrders
        fitMapToOrders(currentOrders, location.latitude, location.longitude)
    }
    
    // Centers the map on user location with a specific zoom level
    fun centerMapWithZoom(zoomLevel: Double) {
        viewModelScope.launch {
            // Use the stored initial location rather than getting the current one
            val location = initialLocation
            zoomToLocationUseCase(
                ZoomToLocationParams(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    zoomLevel = zoomLevel
                )
            ).fold(
                onSuccess = {
                    Log.d(TAG, "Map centered with zoom successfully")
                },
                onFailure = { e ->
                    Log.e(TAG, "Error centering map with zoom", e)
                }
            )
        }
    }
    
    /**
     * Center the user's location in the visible area of the map (bounding box)
     * rather than center of the entire screen
     * 
     * @param visibleMapHeightDp The height of the visible map area in dp
     * @param screenHeightDp The total screen height in dp
     */
    @Suppress("UNUSED_PARAMETER")
    fun centerMapOnUserLocationInVisibleArea(visibleMapHeightDp: Float, screenHeightDp: Float) {
        // For now, just center the map normally - could be enhanced later to account for visible area
        val location = initialLocation
        val currentOrders = _state.value.activeOrders
        fitMapToOrders(currentOrders, location.latitude, location.longitude)
    }
    
    // Animate the map camera with the specified parameters
    fun animateMapCamera(
        targetZoom: Double? = null,
        duration: Long = 300
    ) {
        viewModelScope.launch {
            // Use the stored initial location rather than getting the current one
            val location = initialLocation
            animateCameraUseCase(
                AnimateCameraParams(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    zoom = targetZoom,
                    duration = duration
                )
            ).fold(
                onSuccess = {
                    Log.d(TAG, "Map camera animated successfully")
                },
                onFailure = { e ->
                    Log.e(TAG, "Error animating map camera", e)
                }
            )
        }
    }
    
    fun selectOrderOnMap(order: Order) {
        // Store the selected order ID
        _state.update { it.copy(selectedOrderId = order.id) }
        
        viewModelScope.launch {
            // Get current location
            val location = initialLocation
            
            selectOrderOnMapUseCase(
                SelectOrderOnMapParams(
                    order = order,
                    userLatitude = location.latitude,
                    userLongitude = location.longitude
                )
            ).fold(
                onSuccess = {
                    Log.d(TAG, "Order selected on map successfully")
                },
                onFailure = { e ->
                    Log.e(TAG, "Error selecting order on map", e)
                }
            )
        }
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
            pledgeToOrderUseCase(
                PledgeToOrderParams(orderId = orderId, amount = amount)
            ).fold(
                onSuccess = { response ->
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
                },
                onFailure = { e ->
                    Log.e(TAG, "Error pledging to order", e)
                }
            )
        }
    }
    
    fun createOrder(amountNeeded: Double, platform: String, initialPledge: Int, onSuccess: (String, Order) -> Unit) {
        viewModelScope.launch {
            try {
                val location = initialLocation
                Log.d("BUNDL_DEBUG", "Creating order with: amount=$amountNeeded, platform=$platform, pledge=$initialPledge")
                Log.d("BUNDL_DEBUG", "Using location: lat=${location.latitude}, lon=${location.longitude}")
                
                createOrderUseCase(
                    CreateOrderParams(
                        amountNeeded = amountNeeded,
                        platform = platform,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        initialPledge = initialPledge,
                        expirySeconds = 600
                    )
                ).fold(
                    onSuccess = { order ->
                        Log.d("BUNDL_DEBUG", "Order created successfully: ${order.id}")
                        Log.d("BUNDL_DEBUG", "Full order response: $order")
                        
                        // Refresh active orders before navigating
                        fetchActiveOrders()
                        
                        Log.d("BUNDL_DEBUG", "Calling onSuccess callback with route and order")
                        onSuccess(Route.MyOrders.route, order)
                    },
                    onFailure = { e ->
                        Log.e("BUNDL_DEBUG", "Error creating order", e)
                        showToast("Error creating order: ${e.message}")
                        _state.update { it.copy(errorMessage = e.message ?: "Error creating order") }
                    }
                )
                
            } catch (e: Exception) {
                Log.e("BUNDL_DEBUG", "Error creating order", e)
                showToast("Error creating order: ${e.message}")
                _state.update { it.copy(errorMessage = e.message ?: "Error creating order") }
            }
        }
    }
} 