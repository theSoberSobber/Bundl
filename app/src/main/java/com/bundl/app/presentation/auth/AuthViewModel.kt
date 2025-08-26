package com.bundl.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bundl.app.domain.usecase.auth.CheckLoginStatusUseCase
import com.bundl.app.domain.usecase.auth.GetFcmTokenUseCase
import com.bundl.app.domain.usecase.auth.LogoutUseCase
import com.bundl.app.domain.usecase.auth.SendOtpParams
import com.bundl.app.domain.usecase.auth.SendOtpUseCase
import com.bundl.app.domain.usecase.auth.VerifyOtpParams
import com.bundl.app.domain.usecase.auth.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val checkLoginStatusUseCase: CheckLoginStatusUseCase,
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val getFcmTokenUseCase: GetFcmTokenUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        // Initialize login status
        viewModelScope.launch {
            checkLoginStatusUseCase().fold(
                onSuccess = { loggedIn ->
                    _isLoggedIn.value = loggedIn
                },
                onFailure = { 
                    _isLoggedIn.value = false
                }
            )
        }
    }
    
    fun showError(message: String) {
        _errorMessage.value = message
    }
    
    fun sendOtp(phoneNumber: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                sendOtpUseCase(SendOtpParams(phoneNumber)).fold(
                    onSuccess = { tid ->
                        onSuccess(tid)
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
                verifyOtpUseCase(VerifyOtpParams(tid, otp)).fold(
                    onSuccess = { success ->
                        if (success) {
                            onSuccess()
                        } else {
                            _errorMessage.value = "Invalid OTP"
                        }
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
                Log.d(TAG, "Initiating logout")
                logoutUseCase().fold(
                    onSuccess = { 
                        Log.d(TAG, "Logout successful")
                        _isLoggedIn.value = false
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Logout failed", error)
                        _errorMessage.value = "Failed to sign out: ${error.message}"
                    }
                )
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