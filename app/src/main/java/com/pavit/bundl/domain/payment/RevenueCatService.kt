package com.pavit.bundl.domain.payment

import android.content.Context
import android.util.Log
import com.pavit.bundl.constants.RevenueCatConstants
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class RevenueCatErrorCode {
    // Network/API errors
    NETWORK_ERROR,
    API_ENDPOINT_BLOCKED,
    INVALID_API_KEY,
    BACKEND_ERROR,
    
    // Purchase errors
    PURCHASE_CANCELLED,
    PURCHASE_PENDING,
    PURCHASE_NOT_ALLOWED,
    PURCHASE_INVALID,
    PRODUCT_NOT_AVAILABLE,
    PRODUCT_ALREADY_PURCHASED,
    
    // User/Auth errors
    INVALID_APP_USER_ID,
    OPERATION_ALREADY_IN_PROGRESS,
    
    // Store errors
    STORE_PROBLEM,
    PAYMENT_PENDING,
    INSUFFICIENT_PERMISSIONS,
    
    // Configuration errors
    CONFIGURATION_ERROR,
    
    // Unknown/Other
    UNKNOWN_ERROR
}

data class RevenueCatPurchaseResult(
    val success: Boolean,
    val customerInfo: CustomerInfo? = null,
    val transaction: StoreTransaction? = null,
    val error: String? = null,
    val errorCode: RevenueCatErrorCode? = null,
    val userCancelled: Boolean = false
)

data class RevenueCatPackageInfo(
    val identifier: String,
    val packageType: String,
    val product: RevenueCatProductInfo,
    val revenueCatPackage: Package? = null
)

data class RevenueCatProductInfo(
    val id: String,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val title: String,
    val description: String
)

/**
 * Service class to handle RevenueCat SDK integration
 */
class RevenueCatService(
    private val context: Context
) {
    companion object {
        private const val TAG = "RevenueCatService"
        
        // Product IDs - these should match your RevenueCat configuration
        const val PRODUCT_5_CREDITS = RevenueCatConstants.PRODUCT_5_CREDITS
        const val PRODUCT_10_CREDITS = RevenueCatConstants.PRODUCT_10_CREDITS  
        const val PRODUCT_20_CREDITS = RevenueCatConstants.PRODUCT_20_CREDITS
    }
    
    private var customerInfoUpdateListener: ((CustomerInfo) -> Unit)? = null
    
    private val revenueCatListener = object : UpdatedCustomerInfoListener {
        override fun onReceived(customerInfo: CustomerInfo) {
            Log.d(TAG, "Customer info updated: ${customerInfo.activeSubscriptions}")
            customerInfoUpdateListener?.invoke(customerInfo)
        }
    }

    /**
     * Maps RevenueCat PurchasesError to our custom error codes for better error handling
     */
    private fun mapPurchasesErrorToCode(error: PurchasesError): RevenueCatErrorCode {
        return when (error.code.name) {
            "NETWORK_ERROR" -> RevenueCatErrorCode.NETWORK_ERROR
            "API_ENDPOINT_BLOCKED_ERROR" -> RevenueCatErrorCode.API_ENDPOINT_BLOCKED
            "INVALID_API_KEY_ERROR" -> RevenueCatErrorCode.INVALID_API_KEY
            "BACKEND_ERROR" -> RevenueCatErrorCode.BACKEND_ERROR
            "PURCHASE_CANCELLED_ERROR" -> RevenueCatErrorCode.PURCHASE_CANCELLED
            "PURCHASE_PENDING_ERROR" -> RevenueCatErrorCode.PURCHASE_PENDING
            "PURCHASE_NOT_ALLOWED_ERROR" -> RevenueCatErrorCode.PURCHASE_NOT_ALLOWED
            "PURCHASE_INVALID_ERROR" -> RevenueCatErrorCode.PURCHASE_INVALID
            "PRODUCT_NOT_AVAILABLE_FOR_PURCHASE_ERROR" -> RevenueCatErrorCode.PRODUCT_NOT_AVAILABLE
            "PRODUCT_ALREADY_PURCHASED_ERROR" -> RevenueCatErrorCode.PRODUCT_ALREADY_PURCHASED
            "INVALID_APP_USER_ID_ERROR" -> RevenueCatErrorCode.INVALID_APP_USER_ID
            "OPERATION_ALREADY_IN_PROGRESS_ERROR" -> RevenueCatErrorCode.OPERATION_ALREADY_IN_PROGRESS
            "STORE_PROBLEM_ERROR" -> RevenueCatErrorCode.STORE_PROBLEM
            "PAYMENT_PENDING_ERROR" -> RevenueCatErrorCode.PAYMENT_PENDING
            "INSUFFICIENT_PERMISSIONS_ERROR" -> RevenueCatErrorCode.INSUFFICIENT_PERMISSIONS
            "CONFIGURATION_ERROR" -> RevenueCatErrorCode.CONFIGURATION_ERROR
            else -> RevenueCatErrorCode.UNKNOWN_ERROR
        }
    }

    /**
     * Set up the customer info update listener
     * Call this after Application-level initialization
     */
    fun setupCustomerInfoListener() {
        try {
            Purchases.sharedInstance.updatedCustomerInfoListener = revenueCatListener
            Log.d(TAG, "Customer info listener set up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up customer info listener", e)
        }
    }

    /**
     * Check if RevenueCat is properly initialized
     */
    fun isRevenueCatConfigured(): Boolean {
        return try {
            // Try to access the shared instance to check if it's configured
            Purchases.sharedInstance
            true
        } catch (e: Exception) {
            Log.w(TAG, "RevenueCat not configured: ${e.message}")
            false
        }
    }

    /**
     * Set user ID for RevenueCat
     */
    suspend fun loginUser(userId: String): Result<CustomerInfo> = suspendCancellableCoroutine { continuation ->
        if (!isRevenueCatConfigured()) {
            continuation.resume(Result.failure(IllegalStateException("RevenueCat not configured")))
            return@suspendCancellableCoroutine
        }

        try {
            // Use logIn for user authentication
            Purchases.sharedInstance.logIn(
                newAppUserID = userId,
                callback = object : LogInCallback {
                    override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                        Log.i(TAG, "User logged in successfully: $userId")
                        continuation.resume(Result.success(customerInfo))
                    }

                    override fun onError(error: PurchasesError) {
                        val errorCode = mapPurchasesErrorToCode(error)
                        Log.e(TAG, "Failed to login user: ${error.message}, code: $errorCode")
                        continuation.resume(Result.failure(Exception("${error.message} (Code: $errorCode)")))
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to login user: ${e.message}")
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Logout current user
     */
    suspend fun logoutUser(): Result<CustomerInfo> = suspendCancellableCoroutine { continuation ->
        if (!isRevenueCatConfigured()) {
            continuation.resume(Result.failure(IllegalStateException("RevenueCat not configured")))
            return@suspendCancellableCoroutine
        }

        try {
            // Use logOut to logout user
            Purchases.sharedInstance.logOut(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    Log.i(TAG, "User logged out successfully")
                    continuation.resume(Result.success(customerInfo))
                }

                override fun onError(error: PurchasesError) {
                    val errorCode = mapPurchasesErrorToCode(error)
                    Log.e(TAG, "Failed to logout user: ${error.message}, code: $errorCode")
                    continuation.resume(Result.failure(Exception("${error.message} (Code: $errorCode)")))
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to logout user: ${e.message}")
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Get available offerings and packages
     */
    suspend fun getOfferings(): Result<List<RevenueCatPackageInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isRevenueCatConfigured()) {
                    return@withContext Result.failure(IllegalStateException("RevenueCat not configured"))
                }

                val offerings = Purchases.sharedInstance.awaitOfferings()
                val packages = mutableListOf<RevenueCatPackageInfo>()
                
                val mainOffering = offerings.current
                if (mainOffering != null) {
                    for (pkg in mainOffering.availablePackages) {
                        val packageInfo = RevenueCatPackageInfo(
                            identifier = pkg.identifier,
                            packageType = pkg.packageType.toString(),
                            product = RevenueCatProductInfo(
                                id = pkg.product.id,
                                price = pkg.product.price.formatted,
                                priceAmountMicros = pkg.product.price.amountMicros,
                                priceCurrencyCode = pkg.product.price.currencyCode,
                                title = pkg.product.title,
                                description = pkg.product.description ?: ""
                            ),
                            revenueCatPackage = pkg // Store the Package object
                        )
                        packages.add(packageInfo)
                    }
                }
                
                Log.i(TAG, "Retrieved ${packages.size} packages from offerings")
                Result.success(packages)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get offerings: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Purchase a package
     */
    suspend fun purchasePackage(
        packageToPurchase: Package,
        activity: android.app.Activity
    ): RevenueCatPurchaseResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!isRevenueCatConfigured()) {
                    return@withContext RevenueCatPurchaseResult(
                        success = false,
                        error = "RevenueCat not configured",
                        errorCode = RevenueCatErrorCode.CONFIGURATION_ERROR
                    )
                }

                val purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build()
                val purchaseResult = Purchases.sharedInstance.awaitPurchase(purchaseParams)
                
                val result = RevenueCatPurchaseResult(
                    success = true,
                    customerInfo = purchaseResult.customerInfo,
                    transaction = purchaseResult.storeTransaction
                )
                Log.i(TAG, "Purchase successful: ${purchaseResult.storeTransaction?.productIds?.firstOrNull()}")
                result
            } catch (e: PurchasesTransactionException) {
                val errorCode = if (e.underlyingErrorMessage?.contains("ITEM_ALREADY_OWNED") == true) {
                    RevenueCatErrorCode.PRODUCT_ALREADY_PURCHASED
                } else if (e.userCancelled) {
                    RevenueCatErrorCode.PURCHASE_CANCELLED
                } else {
                    // Try to extract error from the underlying error
                    when {
                        e.underlyingErrorMessage?.contains("BILLING_UNAVAILABLE") == true -> RevenueCatErrorCode.STORE_PROBLEM
                        e.underlyingErrorMessage?.contains("ITEM_UNAVAILABLE") == true -> RevenueCatErrorCode.PRODUCT_NOT_AVAILABLE
                        e.underlyingErrorMessage?.contains("DEVELOPER_ERROR") == true -> RevenueCatErrorCode.CONFIGURATION_ERROR
                        e.underlyingErrorMessage?.contains("SERVICE_UNAVAILABLE") == true -> RevenueCatErrorCode.NETWORK_ERROR
                        else -> RevenueCatErrorCode.PURCHASE_INVALID
                    }
                }
                
                val result = RevenueCatPurchaseResult(
                    success = false,
                    error = e.message,
                    errorCode = errorCode,
                    userCancelled = e.userCancelled
                )
                Log.e(TAG, "Purchase failed: ${e.message}, cancelled: ${e.userCancelled}, code: $errorCode")
                result
            } catch (e: Exception) {
                val result = RevenueCatPurchaseResult(
                    success = false,
                    error = e.message ?: "Unknown error",
                    errorCode = RevenueCatErrorCode.UNKNOWN_ERROR,
                    userCancelled = false
                )
                Log.e(TAG, "Purchase failed: ${e.message}")
                result
            }
        }
    }

    /**
     * Get current customer info
     */
    suspend fun getCustomerInfo(): Result<CustomerInfo> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isRevenueCatConfigured()) {
                    return@withContext Result.failure(IllegalStateException("RevenueCat not configured"))
                }

                val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
                Result.success(customerInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get customer info: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Restore purchases
     */
    suspend fun restorePurchases(): Result<CustomerInfo> = suspendCancellableCoroutine { continuation ->
        if (!isRevenueCatConfigured()) {
            continuation.resume(Result.failure(IllegalStateException("RevenueCat not configured")))
            return@suspendCancellableCoroutine
        }

        try {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    Log.i(TAG, "Purchases restored successfully")
                    continuation.resume(Result.success(customerInfo))
                }

                override fun onError(error: PurchasesError) {
                    val errorCode = mapPurchasesErrorToCode(error)
                    Log.e(TAG, "Failed to restore purchases: ${error.message}, code: $errorCode")
                    continuation.resume(Result.failure(Exception("${error.message} (Code: $errorCode)")))
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore purchases: ${e.message}")
            continuation.resume(Result.failure(e))
        }
    }

    fun setCustomerInfoUpdateListener(listener: (CustomerInfo) -> Unit) {
        customerInfoUpdateListener = listener
    }

    fun clearCustomerInfoUpdateListener() {
        customerInfoUpdateListener = null
    }

    fun isConfigured(): Boolean = isRevenueCatConfigured()

    fun getCurrentUserId(): String? = if (isRevenueCatConfigured()) {
        try {
            Purchases.sharedInstance.appUserID
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get current user ID: ${e.message}")
            null
        }
    } else {
        null
    }
}
