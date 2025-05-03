package com.orvio.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.orvio.app.domain.model.ApiKey
import com.orvio.app.domain.repository.ApiKeyRepository
import com.orvio.app.utils.DeviceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val deviceUtils: DeviceUtils
) : ViewModel() {
    
    private val _apiKeys = MutableStateFlow<List<ApiKey>>(emptyList())
    val apiKeys: StateFlow<List<ApiKey>> = _apiKeys.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()
    
    private val _testResult = MutableStateFlow<Boolean?>(null)
    val testResult: StateFlow<Boolean?> = _testResult.asStateFlow()
    
    init {
        loadApiKeys()
    }
    
    fun loadApiKeys() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                apiKeyRepository.getApiKeys().fold(
                    onSuccess = { keys ->
                        _apiKeys.value = keys
                    },
                    onFailure = { throwable ->
                        _errorMessage.value = throwable.message ?: "Failed to load API keys"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createApiKey(name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                apiKeyRepository.createApiKey(name).fold(
                    onSuccess = { newKey ->
                        loadApiKeys() // Reload keys to include the new one
                        _successMessage.value = "API key '${name}' created successfully"
                        onSuccess()
                    },
                    onFailure = { throwable ->
                        _errorMessage.value = throwable.message ?: "Failed to create API key"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteApiKey(key: String, keyName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                apiKeyRepository.deleteApiKey(key).fold(
                    onSuccess = { _ ->
                        // Remove the key from the local list to update UI immediately
                        _apiKeys.value = _apiKeys.value.filter { it.key != key }
                        _successMessage.value = "API key '${keyName}' deleted successfully"
                    },
                    onFailure = { throwable ->
                        _errorMessage.value = throwable.message ?: "Failed to delete API key"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun testApiKey(key: String) {
        viewModelScope.launch {
            _isTestingKey.value = true
            _errorMessage.value = null
            _testResult.value = null
            
            try {
                apiKeyRepository.testApiKey(key).fold(
                    onSuccess = { result ->
                        _testResult.value = result
                    },
                    onFailure = { throwable ->
                        _errorMessage.value = throwable.message ?: "Failed to test API key"
                        _testResult.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                _testResult.value = false
            } finally {
                _isTestingKey.value = false
            }
        }
    }
    
    fun testApiKey(key: String, phoneNumber: String) {
        viewModelScope.launch {
            _isTestingKey.value = true
            _errorMessage.value = null
            _testResult.value = null
            
            try {
                apiKeyRepository.testApiKey(key, phoneNumber).fold(
                    onSuccess = { result ->
                        _testResult.value = result
                    },
                    onFailure = { throwable ->
                        _errorMessage.value = throwable.message ?: "Failed to test API key"
                        _testResult.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                _testResult.value = false
            } finally {
                _isTestingKey.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    fun clearTestResult() {
        _testResult.value = null
    }
} 