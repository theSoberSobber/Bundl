package com.pavit.bundl.domain.payment

import android.app.Activity
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.pavit.bundl.domain.model.CreditPackage
import javax.inject.Inject
import javax.inject.Singleton

data class RevenueCatCreditPackage(
    val id: String,
    val credits: Int,
    val price: String,
    val priceAmountMicros: Long,
    val name: String,
    val description: String,
    val productId: String,
    val revenueCatPackage: Package? = null
) {
    fun toCreditPackage(): CreditPackage {
        return CreditPackage(
            id = id,
            credits = credits,
            price = (priceAmountMicros / 1000000.0).toInt(), // Convert micros to rupees as Int
            name = name,
            description = description
        )
    }
}

@Singleton
class RevenueCatManager @Inject constructor(
    private val revenueCatService: RevenueCatService
) {
    companion object {
        private const val TAG = "RevenueCatManager"
        
        // Credit mapping for your packages
        private val CREDIT_MAPPING = mapOf(
            RevenueCatService.PRODUCT_5_CREDITS to 5,
            RevenueCatService.PRODUCT_10_CREDITS to 10,
            RevenueCatService.PRODUCT_20_CREDITS to 20
        )
        
        // Package names for your offerings
        private val PACKAGE_NAMES = mapOf(
            RevenueCatService.PRODUCT_5_CREDITS to "Basic Package",
            RevenueCatService.PRODUCT_10_CREDITS to "Standard Package",
            RevenueCatService.PRODUCT_20_CREDITS to "Premium Package"
        )
        
        // Package descriptions
        private val PACKAGE_DESCRIPTIONS = mapOf(
            RevenueCatService.PRODUCT_5_CREDITS to "5 credits for creating or pledging to orders",
            RevenueCatService.PRODUCT_10_CREDITS to "10 credits for creating or pledging to orders",
            RevenueCatService.PRODUCT_20_CREDITS to "20 credits for creating or pledging to orders"
        )
    }

    /**
     * Set up the service (called automatically by DI)
     */
    init {
        // Set up customer info listener on initialization
        revenueCatService.setupCustomerInfoListener()
    }

    /**
     * Login user to RevenueCat
     */
    suspend fun loginUser(userId: String): Result<CustomerInfo> {
        return revenueCatService.loginUser(userId)
    }

    /**
     * Logout current user
     */
    suspend fun logoutUser(): Result<CustomerInfo> {
        return revenueCatService.logoutUser()
    }

    /**
     * Get available credit packages from RevenueCat
     */
    suspend fun getCreditPackages(): Result<List<RevenueCatCreditPackage>> {
        return try {
            val offeringsResult = revenueCatService.getOfferings()
            
            if (offeringsResult.isFailure) {
                return Result.failure(offeringsResult.exceptionOrNull() ?: Exception("Failed to get offerings"))
            }
            
            val packages = offeringsResult.getOrThrow()
            val creditPackages = packages.mapNotNull { packageInfo ->
                val credits = CREDIT_MAPPING[packageInfo.product.id]
                val name = PACKAGE_NAMES[packageInfo.product.id]
                val description = PACKAGE_DESCRIPTIONS[packageInfo.product.id]
                
                if (credits != null && name != null && description != null) {
                    RevenueCatCreditPackage(
                        id = packageInfo.identifier,
                        credits = credits,
                        price = packageInfo.product.price,
                        priceAmountMicros = packageInfo.product.priceAmountMicros,
                        name = name,
                        description = description,
                        productId = packageInfo.product.id
                    )
                } else {
                    Log.w(TAG, "Unknown product ID: ${packageInfo.product.id}")
                    null
                }
            }
            
            Log.i(TAG, "Retrieved ${creditPackages.size} credit packages")
            Result.success(creditPackages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting credit packages", e)
            Result.failure(e)
        }
    }

    /**
     * Purchase credits using product ID
     */
    suspend fun purchaseCredits(
        productId: String,
        activity: Activity
    ): Result<PurchaseCreditsResult> {
        return try {
            // First get the offerings to find the package
            val offeringsResult = revenueCatService.getOfferings()
            if (offeringsResult.isFailure) {
                return Result.failure(offeringsResult.exceptionOrNull() ?: Exception("Failed to get offerings"))
            }
            
            // Find the RevenueCat package info that matches the product ID
            val packageInfos = offeringsResult.getOrThrow()
            val targetPackageInfo = packageInfos.find { it.product.id == productId }
                ?: return Result.failure(Exception("Product not found: $productId"))
                
            // Get the actual RevenueCat Package object
            val revenueCatPackage = targetPackageInfo.revenueCatPackage
                ?: return Result.failure(Exception("RevenueCat package not available for: $productId"))
                
            // Get credits for this product
            val credits = CREDIT_MAPPING[productId] 
                ?: return Result.failure(Exception("Unknown credits for product: $productId"))
                
            // Use the RevenueCat service to make the purchase
            val purchaseResult = revenueCatService.purchasePackage(revenueCatPackage, activity)
            
            if (purchaseResult.success) {
                val customerInfo = purchaseResult.customerInfo
                
                // Check if the purchase was successful by looking at customer info
                val hasCredits = customerInfo?.nonSubscriptionTransactions?.isNotEmpty() ?: false
                
                Log.i(TAG, "Purchase completed for $productId, success: $hasCredits")
                
                Result.success(
                    PurchaseCreditsResult(
                        success = hasCredits,
                        credits = credits,
                        productId = productId,
                        transactionId = System.currentTimeMillis().toString(),
                        message = if (hasCredits) "Purchase successful" else "Purchase completed"
                    )
                )
            } else {
                val errorMessage = when (purchaseResult.errorCode) {
                    com.pavit.bundl.domain.payment.RevenueCatErrorCode.PURCHASE_CANCELLED -> "Purchase was cancelled"
                    com.pavit.bundl.domain.payment.RevenueCatErrorCode.PRODUCT_ALREADY_PURCHASED -> "You have already purchased this item"
                    com.pavit.bundl.domain.payment.RevenueCatErrorCode.PRODUCT_NOT_AVAILABLE -> "This product is not available for purchase"
                    com.pavit.bundl.domain.payment.RevenueCatErrorCode.NETWORK_ERROR -> "Network error, please check your connection and try again"
                    com.pavit.bundl.domain.payment.RevenueCatErrorCode.STORE_PROBLEM -> "There's a problem with the app store. Please try again later"
                    com.pavit.bundl.domain.payment.RevenueCatErrorCode.PAYMENT_PENDING -> "Your payment is being processed. Please wait."
                    com.pavit.bundl.domain.payment.RevenueCatErrorCode.INSUFFICIENT_PERMISSIONS -> "Insufficient permissions to make purchases"
                    com.pavit.bundl.domain.payment.RevenueCatErrorCode.CONFIGURATION_ERROR -> "App configuration error. Please contact support"
                    else -> purchaseResult.error ?: "Purchase failed"
                }
                
                Log.e(TAG, "Purchase failed for $productId: $errorMessage (Code: ${purchaseResult.errorCode})")
                
                Result.success(
                    PurchaseCreditsResult(
                        success = false,
                        credits = 0,
                        productId = productId,
                        transactionId = "",
                        error = errorMessage,
                        userCancelled = purchaseResult.userCancelled
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error purchasing credits", e)
            Result.failure(e)
        }
    }

    /**
     * Get current customer info and extract relevant data
     */
    suspend fun getCustomerInfo(): Result<CustomerInfo> {
        return revenueCatService.getCustomerInfo()
    }

    /**
     * Restore purchases
     */
    suspend fun restorePurchases(): Result<CustomerInfo> {
        return revenueCatService.restorePurchases()
    }

    /**
     * Check if user has active entitlements (if you use entitlements)
     */
    suspend fun hasActiveEntitlements(): Result<Boolean> {
        return try {
            val customerInfoResult = revenueCatService.getCustomerInfo()
            if (customerInfoResult.isFailure) {
                return Result.failure(customerInfoResult.exceptionOrNull() ?: Exception("Failed to get customer info"))
            }
            
            val customerInfo = customerInfoResult.getOrThrow()
            val hasActive = customerInfo.activeSubscriptions.isNotEmpty() || 
                           customerInfo.nonSubscriptionTransactions.isNotEmpty()
            
            Result.success(hasActive)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking active entitlements", e)
            Result.failure(e)
        }
    }

    /**
     * Set listener for customer info updates
     */
    fun setCustomerInfoUpdateListener(listener: (CustomerInfo) -> Unit) {
        revenueCatService.setCustomerInfoUpdateListener(listener)
    }

    /**
     * Clear customer info update listener
     */
    fun clearCustomerInfoUpdateListener() {
        revenueCatService.clearCustomerInfoUpdateListener()
    }

    /**
     * Check if RevenueCat is configured
     */
    fun isConfigured(): Boolean = revenueCatService.isConfigured()

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = revenueCatService.getCurrentUserId()
}

data class PurchaseCreditsResult(
    val success: Boolean,
    val credits: Int = 0,
    val productId: String = "",
    val transactionId: String = "",
    val error: String? = null,
    val message: String? = null,
    val userCancelled: Boolean = false
)
