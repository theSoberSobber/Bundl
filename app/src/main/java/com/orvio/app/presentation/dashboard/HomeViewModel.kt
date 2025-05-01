package com.orvio.app.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.orvio.app.domain.repository.ApiKeyRepository
import com.orvio.app.domain.repository.AuthRepository
import com.orvio.app.utils.DeviceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val deviceUtils: DeviceUtils
) : ViewModel() {
    
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()
    
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var registrationAttempts = 0
    private val maxRegistrationAttempts = 3
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    init {
        // Get FCM token and register device if user is logged in
        viewModelScope.launch {
            if (authRepository.isLoggedIn().first()) {
                Log.d(TAG, "User is logged in, proceeding with device registration")
                getFcmToken()
            } else {
                Log.d(TAG, "User is not logged in, skipping device registration")
            }
        }
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
    
    fun clearError() {
        _errorMessage.value = null
    }
} 