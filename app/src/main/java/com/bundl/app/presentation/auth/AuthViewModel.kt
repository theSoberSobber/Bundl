package com.bundl.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bundl.app.domain.repository.AuthRepository
import com.bundl.app.utils.DeviceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceUtils: DeviceUtils
) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    val isLoggedIn = authRepository.isLoggedIn()
    
    fun showError(message: String) {
        _errorMessage.value = message
    }
    
    fun sendOtp(phoneNumber: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                authRepository.sendOtp(phoneNumber).fold(
                    onSuccess = { response ->
                        onSuccess(response.tid)
                    },
                    onFailure = { throwable ->
                        _errorMessage.value = throwable.message ?: "Failed to send OTP"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun verifyOtp(tid: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Get FCM token for verification
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get FCM token", e)
                    _errorMessage.value = "Failed to get device token: ${e.message}"
                    _isLoading.value = false
                    return@launch
                }
                
                authRepository.verifyOtp(tid, otp, fcmToken).fold(
                    onSuccess = { _ ->
                        onSuccess()
                    },
                    onFailure = { throwable ->
                        _errorMessage.value = throwable.message ?: "Invalid OTP"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                // Call the repository's logout method which handles
                // FCM token deletion and token clearing
                Log.d(TAG, "Initiating logout")
                authRepository.logout()
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                _errorMessage.value = "Failed to sign out: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
} 