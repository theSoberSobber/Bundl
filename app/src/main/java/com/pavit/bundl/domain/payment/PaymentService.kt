package com.pavit.bundl.domain.payment

import android.app.Activity
import android.util.Log
import com.revenuecat.purchases.Package
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class OrderResponse(
    val orderId: String,
    val sessionId: String,
    val orderStatus: String,
    val amount: Int,
    val credits: Int
)

data class VerifyResponse(
    val success: Boolean,
    val orderStatus: String,
    val userId: String,
    val amount: Int
)

/**
 * PaymentService - Updated to use RevenueCat instead of Cashfree
 * Maintains the same interface for backward compatibility
 */
@Singleton
class PaymentService @Inject constructor(
    private val revenueCatManager: RevenueCatManager
) {
    companion object {
        private const val TAG = "PaymentService"
    }
    
    private var currentActivity: Activity? = null
    private var paymentCallback: PaymentCallback? = null

    interface PaymentCallback {
        fun onPaymentSuccess(orderId: String, credits: Int)
        fun onPaymentFailure(error: String)
        fun onPaymentCancelled()
    }

    fun initialize(activity: Activity, callback: PaymentCallback) {
        currentActivity = activity
        paymentCallback = callback
        Log.d(TAG, "PaymentService initialized with RevenueCat")
    }

    /**
     * Create order - Now returns RevenueCat product information
     * This maintains API compatibility but now works with RevenueCat
     */
    suspend fun createOrder(credits: Int, @Suppress("UNUSED_PARAMETER") currency: String = "INR"): Result<OrderResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Get available credit packages from RevenueCat
                val packagesResult = revenueCatManager.getCreditPackages()
                if (packagesResult.isFailure) {
                    return@withContext Result.failure(
                        packagesResult.exceptionOrNull() ?: Exception("Failed to get credit packages")
                    )
                }
                
                // Find the package that matches the requested credits
                val packages = packagesResult.getOrThrow()
                val targetPackage = packages.find { it.credits == credits }
                    ?: return@withContext Result.failure(
                        Exception("No package found for $credits credits")
                    )
                
                // Return order response that mimics the old API
                val orderResponse = OrderResponse(
                    orderId = targetPackage.productId,
                    sessionId = targetPackage.id, // Use package ID as session ID
                    orderStatus = "created",
                    amount = (targetPackage.priceAmountMicros / 1000000).toInt(),
                    credits = credits
                )
                
                Log.d(TAG, "Order created for $credits credits: ${targetPackage.productId}")
                Result.success(orderResponse)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating order", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Verify order - Now handles RevenueCat purchase verification
     */
    suspend fun verifyOrder(orderId: String): Result<VerifyResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Get customer info to check for purchases
                val customerInfoResult = revenueCatManager.getCustomerInfo()
                if (customerInfoResult.isFailure) {
                    return@withContext Result.failure(
                        customerInfoResult.exceptionOrNull() ?: Exception("Failed to get customer info")
                    )
                }
                
                val customerInfo = customerInfoResult.getOrThrow()
                
                // Check if the product was purchased
                val hasPurchase = customerInfo.nonSubscriptionTransactions.any { transaction ->
                    transaction.productIdentifier == orderId
                }
                
                val verifyResponse = VerifyResponse(
                    success = hasPurchase,
                    orderStatus = if (hasPurchase) "success" else "failed",
                    userId = revenueCatManager.getCurrentUserId() ?: "",
                    amount = 0 // We don't have amount info in RevenueCat customer info
                )
                
                Log.d(TAG, "Order verified: $orderId, success: $hasPurchase")
                Result.success(verifyResponse)
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying order", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Start payment - Now uses RevenueCat purchase flow
     * Maintains same interface but uses RevenueCat instead of UPI
     */
    suspend fun startUpiPayment(orderId: String, @Suppress("UNUSED_PARAMETER") sessionId: String): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                val activity = currentActivity ?: return@withContext Result.failure(
                    Exception("Activity reference lost")
                )
                
                // Get available packages to find the one to purchase
                val packagesResult = revenueCatManager.getCreditPackages()
                if (packagesResult.isFailure) {
                    return@withContext Result.failure(
                        packagesResult.exceptionOrNull() ?: Exception("Failed to get packages")
                    )
                }
                
                val packages = packagesResult.getOrThrow()
                val targetPackage = packages.find { it.productId == orderId }
                    ?: return@withContext Result.failure(Exception("Package not found: $orderId"))
                
                Log.d(TAG, "Starting purchase for package: ${targetPackage.name} (${targetPackage.credits} credits)")
                
                // Start RevenueCat purchase
                val purchaseResult = revenueCatManager.purchaseCredits(orderId, activity)
                
                if (purchaseResult.isSuccess) {
                    val result = purchaseResult.getOrThrow()
                    if (result.success && !result.userCancelled) {
                        paymentCallback?.onPaymentSuccess(orderId, result.credits)
                        Log.d(TAG, "Payment successful: $orderId")
                    } else if (result.userCancelled) {
                        paymentCallback?.onPaymentCancelled()
                        Log.d(TAG, "Payment cancelled by user: $orderId")
                    } else {
                        paymentCallback?.onPaymentFailure(result.error ?: "Unknown error")
                        Log.e(TAG, "Payment failed: ${result.error}")
                    }
                } else {
                    val error = purchaseResult.exceptionOrNull()?.message ?: "Payment failed"
                    paymentCallback?.onPaymentFailure(error)
                    Log.e(TAG, "Payment failed: $error")
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start payment", e)
                paymentCallback?.onPaymentFailure(e.message ?: "Payment failed")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Set RevenueCat user - helper method for authentication
     */
    suspend fun setUser(userId: String): Result<Unit> {
        return try {
            val result = revenueCatManager.loginUser(userId)
            if (result.isSuccess) {
                Log.d(TAG, "User logged in to RevenueCat: $userId")
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to set user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restore purchases - new method for RevenueCat
     */
    suspend fun restorePurchases(): Result<Unit> {
        return try {
            val result = revenueCatManager.restorePurchases()
            if (result.isSuccess) {
                Log.d(TAG, "Purchases restored successfully")
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to restore purchases"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring purchases", e)
            Result.failure(e)
        }
    }
} 