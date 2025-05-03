package com.orvio.app.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.orvio.app.data.remote.api.ApiKeyService
import com.orvio.app.data.remote.api.AuthApiService
import com.orvio.app.domain.model.UserStats
import com.orvio.app.domain.repository.ApiKeyRepository
import com.orvio.app.domain.repository.AuthRepository
import com.orvio.app.utils.DeviceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val authRepository: AuthRepository,
    private val deviceUtils: DeviceUtils,
    private val apiKeyService: ApiKeyService,
    private val authApiService: AuthApiService
) : ViewModel() {
    
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()
    
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _credits = MutableStateFlow(0)
    val credits: StateFlow<Int> = _credits.asStateFlow()
    
    private val _creditMode = MutableStateFlow("")
    val creditMode: StateFlow<String> = _creditMode.asStateFlow()
    
    private val _isLoadingCredits = MutableStateFlow(false)
    val isLoadingCredits: StateFlow<Boolean> = _isLoadingCredits.asStateFlow()
    
    private val _secondsUntilRefresh = MutableStateFlow(30)
    val secondsUntilRefresh: StateFlow<Int> = _secondsUntilRefresh.asStateFlow()
    
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()
    
    private val _isLoadingStats = MutableStateFlow(false)
    val isLoadingStats: StateFlow<Boolean> = _isLoadingStats.asStateFlow()
    
    private var registrationAttempts = 0
    private val maxRegistrationAttempts = 3
    
    private var creditsRefreshJob: Job? = null
    private var countdownJob: Job? = null
    private val creditsRefreshInterval = 30000L // 30 seconds
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    init {
        // Get FCM token and register device if user is logged in
        viewModelScope.launch {
            if (authRepository.isLoggedIn().first()) {
                Log.d(TAG, "User is logged in, proceeding with device registration")
                getFcmToken()
                fetchUserStats()
                fetchCreditsInfo()
                startCreditsPolling()
                startCountdownTimer()
            } else {
                Log.d(TAG, "User is not logged in, skipping device registration")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCreditsPolling()
        stopCountdownTimer()
    }
    
    private fun startCreditsPolling() {
        stopCreditsPolling() // Stop any existing polling
        
        creditsRefreshJob = viewModelScope.launch {
            while (true) {
                delay(creditsRefreshInterval)
                Log.d(TAG, "Auto-refreshing credits info")
                fetchCreditsInfo(showLoading = false)
                fetchUserStats(showLoading = false)
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
                val current = _secondsUntilRefresh.value
                if (current > 0) {
                    _secondsUntilRefresh.value = current - 1
                } else {
                    _secondsUntilRefresh.value = 30 // Reset to 30 seconds
                }
            }
        }
    }
    
    private fun stopCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = null
    }
    
    private fun resetCountdown() {
        _secondsUntilRefresh.value = 30
    }
    
    private fun getFcmToken() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Requesting FCM token...")
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM token received: ${token.take(10)}...")
                _fcmToken.value = token
                registerDevice(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FCM token", e)
                _errorMessage.value = "Failed to get FCM token: ${e.message}"
            }
        }
    }
    
    private fun registerDevice(fcmToken: String) {
        registrationAttempts++
        
        viewModelScope.launch {
            try {
                val deviceHash = deviceUtils.getDeviceHash()
                Log.d(TAG, "Registering device with FCM token: ${fcmToken.take(10)}... (Attempt $registrationAttempts/$maxRegistrationAttempts)")
                
                // Check if user is still logged in before attempting registration
                if (!authRepository.isLoggedIn().first()) {
                    Log.w(TAG, "User is no longer logged in, aborting device registration")
                    _errorMessage.value = "Cannot register device: User is not logged in"
                    return@launch
                }
                
                apiKeyRepository.registerDevice(deviceHash, fcmToken).fold(
                    onSuccess = {
                        Log.d(TAG, "Device registration successful")
                        _isRegistered.value = true
                        registrationAttempts = 0
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Device registration failed", e)
                        
                        if (registrationAttempts < maxRegistrationAttempts) {
                            Log.d(TAG, "Will retry registration after delay. Attempt $registrationAttempts/$maxRegistrationAttempts")
                            // Exponential backoff
                            delay(1000L * registrationAttempts)
                            registerDevice(fcmToken)
                        } else {
                            Log.e(TAG, "Max registration attempts reached. Giving up.")
                            _errorMessage.value = "Device registration failed after multiple attempts: ${e.message}"
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register device", e)
                _errorMessage.value = "Failed to register device: ${e.message}"
            }
        }
    }
    
    private fun fetchUserStats(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _isLoadingStats.value = true
            }
            
            try {
                // Fetch user stats
                val stats = authApiService.getUserStats()
                _userStats.value = stats
                Log.d(TAG, "Fetched user stats successfully")
                
                // Update credits from stats as well
                _credits.value = stats.credits.balance
                _creditMode.value = stats.credits.mode
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch user stats", e)
                if (showLoading) {
                    _errorMessage.value = "Failed to fetch user stats: ${e.message}"
                }
            } finally {
                if (showLoading) {
                    _isLoadingStats.value = false
                }
            }
        }
    }
    
    private fun fetchCreditsInfo(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _isLoadingCredits.value = true
            }
            
            try {
                // Fetch credits
                val creditsResponse = apiKeyService.getCredits()
                _credits.value = creditsResponse.credits
                Log.d(TAG, "Fetched credits: ${creditsResponse.credits}")
                
                // Fetch credit mode
                val creditModeResponse = apiKeyService.getCreditMode()
                _creditMode.value = creditModeResponse.mode
                Log.d(TAG, "Fetched credit mode: ${creditModeResponse.mode}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch credits info", e)
                // Only show error message for manual refreshes
                if (showLoading) {
                    _errorMessage.value = "Failed to fetch credits: ${e.message}"
                }
            } finally {
                if (showLoading) {
                    _isLoadingCredits.value = false
                }
            }
        }
    }
    
    fun refreshCredits() {
        fetchCreditsInfo()
        fetchUserStats()
        resetCountdown() // Reset the countdown when manually refreshing
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
} 