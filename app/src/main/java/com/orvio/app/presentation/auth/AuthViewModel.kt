package com.orvio.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvio.app.domain.repository.AuthRepository
import com.orvio.app.utils.DeviceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceUtils: DeviceUtils
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    val isLoggedIn = authRepository.isLoggedIn()
    
    fun getDevicePhoneNumber(): String {
        return deviceUtils.getPhoneNumber() ?: ""
    }
    
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
                        onSuccess(response.transactionId)
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
    
    fun verifyOtp(transactionId: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                authRepository.verifyOtp(transactionId, otp).fold(
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
            authRepository.clearAuthTokens()
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
} 