package com.pavit.bundl.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavit.bundl.data.utils.GeohashLocationService
import com.pavit.bundl.data.utils.LocationManager
import com.pavit.bundl.domain.usecase.location.GeohashLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing location tracking and geohash subscriptions
 */
@HiltViewModel
class LocationTrackingViewModel @Inject constructor(
    private val geohashLocationUseCase: GeohashLocationUseCase,
    private val locationManager: LocationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LocationTrackingUiState())
    val uiState: StateFlow<LocationTrackingUiState> = _uiState.asStateFlow()
    
    init {
        // Observe location changes
        viewModelScope.launch {
            locationManager.currentLocation.collect { locationData ->
                updateUiState { copy(currentLocation = locationData) }
            }
        }
        
        // Observe subscription status
        viewModelScope.launch {
            geohashLocationUseCase.getSubscriptionStatus().collect { status ->
                updateUiState { copy(subscriptionStatus = status) }
            }
        }
        
        // Observe current geohashes
        viewModelScope.launch {
            geohashLocationUseCase.getCurrentGeohashes().collect { geohashes ->
                updateUiState { copy(currentGeohashes = geohashes) }
            }
        }
    }
    
    /**
     * Start location tracking
     */
    fun startLocationTracking() {
        updateUiState { copy(isLoading = true, error = null) }
        
        if (!geohashLocationUseCase.hasLocationPermission()) {
            updateUiState { 
                copy(
                    isLoading = false, 
                    error = "Location permission is required",
                    isLocationPermissionRequired = true
                )
            }
            return
        }
        
        val result = geohashLocationUseCase.startLocationTracking()
        if (result.isSuccess) {
            updateUiState { 
                copy(
                    isLoading = false, 
                    isTrackingEnabled = true,
                    error = null
                ) 
            }
        } else {
            updateUiState { 
                copy(
                    isLoading = false, 
                    error = "Failed to start location tracking: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    /**
     * Stop location tracking
     */
    fun stopLocationTracking() {
        updateUiState { copy(isLoading = true) }
        
        val result = geohashLocationUseCase.stopLocationTracking()
        if (result.isSuccess) {
            updateUiState { 
                copy(
                    isLoading = false,
                    isTrackingEnabled = false,
                    error = null
                ) 
            }
        } else {
            updateUiState { 
                copy(
                    isLoading = false,
                    error = "Failed to stop location tracking: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    /**
     * Refresh subscription info
     */
    fun refreshSubscriptionInfo() {
        val info = geohashLocationUseCase.getSubscriptionInfo()
        updateUiState { copy(subscriptionInfo = info) }
    }
    
    /**
     * Handle permission result
     */
    fun onLocationPermissionResult(granted: Boolean) {
        updateUiState { copy(isLocationPermissionRequired = !granted) }
        
        if (granted) {
            startLocationTracking()
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        updateUiState { copy(error = null) }
    }
    
    /**
     * Clean up when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            geohashLocationUseCase.cleanup()
        }
    }
    
    private fun updateUiState(update: LocationTrackingUiState.() -> LocationTrackingUiState) {
        _uiState.value = _uiState.value.update()
    }
}

/**
 * UI State for location tracking
 */
data class LocationTrackingUiState(
    val isLoading: Boolean = false,
    val isTrackingEnabled: Boolean = false,
    val isLocationPermissionRequired: Boolean = false,
    val currentLocation: LocationManager.LocationData? = null,
    val subscriptionStatus: GeohashLocationService.SubscriptionStatus = GeohashLocationService.SubscriptionStatus.IDLE,
    val currentGeohashes: Set<String> = emptySet(),
    val subscriptionInfo: GeohashLocationService.SubscriptionInfo? = null,
    val error: String? = null
) {
    val hasLocation: Boolean = currentLocation != null
    val hasActiveSubscriptions: Boolean = currentGeohashes.isNotEmpty()
    val isSubscriptionActive: Boolean = subscriptionStatus == GeohashLocationService.SubscriptionStatus.SUBSCRIBED
}
