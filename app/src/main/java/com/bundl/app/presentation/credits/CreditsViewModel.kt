package com.bundl.app.presentation.credits

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bundl.app.BundlApplication
import com.bundl.app.data.remote.api.ApiKeyService
import com.bundl.app.domain.model.CreditPackage
import com.bundl.app.domain.payment.PaymentService
import com.bundl.app.domain.repository.ApiKeyRepository
import com.cashfree.pg.core.api.utils.CFErrorResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CreditScreenState(
    val currentCredits: Int = 0,
    val packages: List<CreditPackage> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentOrderId: String? = null,
    val isVerifying: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class CreditsViewModel @Inject constructor(
    private val apiKeyService: ApiKeyService,
    private val apiKeyRepository: ApiKeyRepository,
    private val paymentService: PaymentService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(CreditScreenState())
    val state: StateFlow<CreditScreenState> = _state.asStateFlow()
    
    companion object {
        private const val TAG = "CreditsViewModel"
        private const val MAX_VERIFY_RETRIES = 5
        private const val VERIFY_INTERVAL = 3000L // 3 seconds between polls
        private const val VERIFY_TIMEOUT = 30000L // 30 seconds timeout
    }
    
    init {
        fetchUserCredits()
        fetchCreditPackages()
    }
    
    // For testing/development - load mock data if API isn't ready yet
    fun loadMockPackages() {
        val mockPackages = listOf(
            CreditPackage(
                id = "basic",
                credits = 5,
                price = 5,
                name = "Basic Package",
                description = "5 credits for creating or pledging to orders"
            ),
            CreditPackage(
                id = "standard",
                credits = 10,
                price = 8,
                name = "Standard Package",
                description = "10 credits for creating or pledging to orders"
            ),
            CreditPackage(
                id = "premium",
                credits = 20,
                price = 12,
                name = "Premium Package",
                description = "20 credits for creating or pledging to orders"
            )
        )
        
        _state.update { it.copy(packages = mockPackages) }
    }
    
    private fun fetchUserCredits() {
        viewModelScope.launch {
            try {
                val creditsResponse = apiKeyService.getCredits()
                _state.update { it.copy(currentCredits = creditsResponse.credits) }
                Log.d(TAG, "User credits retrieved: ${creditsResponse.credits}")
                Log.d("BUNDL_CREDITS", "Credits screen - User credits retrieved: ${creditsResponse.credits}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user credits", e)
                Log.e("BUNDL_CREDITS", "Credits screen - Error fetching user credits: ${e.message}")
            }
        }
    }
    
    private fun fetchCreditPackages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // In a real implementation, this would call the API
                // For now, we'll simulate the API call with a delay
                delay(1000) // Simulate network delay
                
                // This would be the actual API call in production:
                // val packages = apiKeyService.getCreditPackages()
                
                // For now we'll use mock data while the API endpoint is being developed
                loadMockPackages()
                
                Log.d(TAG, "Fetched credit packages: ${state.value.packages.size} packages")
                Log.d("BUNDL_CREDITS", "Available packages: ${state.value.packages.map { it.name }}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching credit packages", e)
                Log.e("BUNDL_CREDITS", "Error fetching packages: ${e.message}")
                _state.update { it.copy(errorMessage = "Failed to load credit packages: ${e.message}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun buyPackage(creditPackage: CreditPackage) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            
            try {
                // Create an order for the purchase
                val orderResult = paymentService.createOrder(
                    credits = creditPackage.credits,
                    currency = "INR"
                )
                
                orderResult.fold(
                    onSuccess = { orderResponse ->
                        _state.update { 
                            it.copy(
                                currentOrderId = orderResponse.orderId
                            )
                        }
                        
                        Log.d(TAG, "Created order: ${orderResponse.orderId}")
                        Log.d("BUNDL_CREDITS", "Order created for ${creditPackage.credits} credits: ${orderResponse.orderId}")
                        
                        // Start UPI payment
                        val paymentResult = paymentService.startUpiPayment(
                            orderId = orderResponse.orderId,
                            sessionId = orderResponse.sessionId
                        )
                        
                        paymentResult.fold(
                            onSuccess = {
                                Log.d(TAG, "Payment initiated for order: ${orderResponse.orderId}")
                                Log.d("BUNDL_CREDITS", "Payment initiated for order: ${orderResponse.orderId}")
                            },
                            onFailure = { e ->
                                _state.update { 
                                    it.copy(
                                        errorMessage = "Failed to initiate payment: ${e.message}",
                                        isProcessing = false
                                    )
                                }
                                Log.e(TAG, "Error initiating payment", e)
                                Log.e("BUNDL_CREDITS", "Payment initiation failed: ${e.message}")
                            }
                        )
                    },
                    onFailure = { e ->
                        _state.update { 
                            it.copy(
                                errorMessage = "Failed to create order: ${e.message}",
                                isProcessing = false
                            )
                        }
                        Log.e(TAG, "Error creating order", e)
                        Log.e("BUNDL_CREDITS", "Order creation failed: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        errorMessage = "Failed to process payment: ${e.message}",
                        isProcessing = false
                    )
                }
                Log.e(TAG, "Error processing payment", e)
                Log.e("BUNDL_CREDITS", "Payment processing failed: ${e.message}")
            }
        }
    }
    
    fun onPaymentCompleted(success: Boolean, errorMessage: String?) {
        if (success) {
            Log.d(TAG, "Payment completed successfully, starting verification for orderId: ${state.value.currentOrderId}")
            Log.d("BUNDL_CREDITS", "Payment completed successfully, starting verification")
            
            // Start verifying the payment with polling
            startVerifyingPayment()
        } else {
            _state.update { 
                it.copy(
                    isProcessing = false,
                    isVerifying = false,
                    errorMessage = errorMessage ?: "Payment failed",
                    currentOrderId = null
                )
            }
            Log.e(TAG, "Payment failed: $errorMessage")
            Log.e("BUNDL_CREDITS", "Payment failed: $errorMessage")
        }
    }
    
    private fun startVerifyingPayment() {
        viewModelScope.launch {
            val orderId = state.value.currentOrderId ?: return@launch
            
            Log.d(TAG, "Starting payment verification for order: $orderId")
            Log.d("BUNDL_CREDITS", "Starting payment verification for order: $orderId")
            
            _state.update { it.copy(isVerifying = true) }
            
            val startTime = System.currentTimeMillis()
            var success = false
            
            // Keep polling until timeout or success
            while (System.currentTimeMillis() - startTime < VERIFY_TIMEOUT && !success) {
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                Log.d(TAG, "Verifying payment for order: $orderId, elapsed: ${elapsedSeconds}s")
                Log.d("BUNDL_CREDITS", "Verifying payment (${elapsedSeconds}s elapsed)")
                
                try {
                    // Create request body with orderId
                    val request = mapOf("orderId" to orderId)
                    
                    // Call the /credits/verify endpoint
                    val verifyResult = apiKeyService.verifyCreditOrder(request)
                    Log.d(TAG, "Verify result: success=${verifyResult.success}, status=${verifyResult.orderStatus}, amount=${verifyResult.amount}")
                    Log.d("BUNDL_CREDITS", "Verify result: success=${verifyResult.success}, status=${verifyResult.orderStatus}, amount=${verifyResult.amount}")
                    
                    // Show Toast with raw response
                    showToast("Poll #${elapsedSeconds/3 + 1}: success=${verifyResult.success}, status=${verifyResult.orderStatus}, amount=${verifyResult.amount}")
                    
                    if (verifyResult.success) {
                        // Payment verification successful
                        _state.update { 
                            it.copy(
                                isVerifying = false,
                                isProcessing = false,
                                successMessage = if (verifyResult.amount > 0) 
                                    "Payment successful! ${verifyResult.amount} credits added to your account."
                                else
                                    "Payment successful! Credits will be added to your account soon.",
                                currentOrderId = null
                            )
                        }
                        
                        // Show final success Toast
                        showToast("VERIFICATION SUCCESS: Credits added!")
                        
                        // Refresh credits
                        fetchUserCredits()
                        
                        Log.d(TAG, "Payment verified successfully for order: $orderId")
                        Log.d("BUNDL_CREDITS", "Payment verified successfully!")
                        success = true
                        return@launch
                    } else {
                        // Not successful yet, check order status
                        val statusMessage = when(verifyResult.orderStatus) {
                            "ACTIVE" -> "Payment processing..."
                            "FAILED" -> {
                                _state.update { 
                                    it.copy(
                                        isVerifying = false,
                                        isProcessing = false,
                                        errorMessage = "Payment failed. Please try again.",
                                        currentOrderId = null
                                    )
                                }
                                
                                // Show failure Toast
                                showToast("VERIFICATION FAILED: Order status = FAILED")
                                
                                Log.w(TAG, "Payment verification failed for order: $orderId")
                                Log.w("BUNDL_CREDITS", "Payment verification failed")
                                return@launch
                            }
                            else -> "Checking payment status..."
                        }
                        
                        // Update state with status message
                        _state.update { 
                            it.copy(
                                statusMessage = statusMessage
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception verifying payment for order: $orderId", e)
                    Log.e("BUNDL_CREDITS", "Exception verifying payment: ${e.message}")
                    
                    // Show error Toast
                    showToast("POLL ERROR: ${e.message}")
                    
                    // Continue polling despite error
                }
                
                // Wait before next poll
                delay(VERIFY_INTERVAL)
            }
            
            // If we get here, we've timed out
            if (!success) {
                _state.update { 
                    it.copy(
                        isVerifying = false,
                        isProcessing = false,
                        errorMessage = "Payment verification timed out. If payment was successful, credits will be added to your account soon.",
                        currentOrderId = null
                    )
                }
                
                // Show timeout Toast
                showToast("VERIFICATION TIMEOUT: Polling ended after 30 seconds")
                
                Log.w(TAG, "Payment verification timeout for order: $orderId")
                Log.w("BUNDL_CREDITS", "Payment verification timeout after 30 seconds")
            }
        }
    }
    
    private fun showToast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show toast", e)
            }
        }
    }
    
    fun clearMessages() {
        _state.update { 
            it.copy(
                errorMessage = null,
                successMessage = null,
                statusMessage = null
            )
        }
    }
    
    fun resetPaymentState() {
        _state.update {
            it.copy(
                isProcessing = false,
                isVerifying = false,
                currentOrderId = null,
                errorMessage = null,
                successMessage = null,
                statusMessage = null
            )
        }
    }
    
    fun refreshData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            fetchUserCredits()
            fetchCreditPackages()
            _state.update { it.copy(isLoading = false) }
        }
    }
} 