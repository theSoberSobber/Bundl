package com.bundl.app.domain.payment

import android.app.Activity
import android.util.Log
import com.bundl.app.data.remote.api.ApiKeyService
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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

@Singleton
class PaymentService @Inject constructor(
    private val apiKeyService: ApiKeyService
) {
    companion object {
        private const val TAG = "PaymentService"
        private var activityRef: WeakReference<Activity>? = null
        private var paymentCallback: CFCheckoutResponseCallback? = null
    }

    fun initialize(activity: Activity, callback: CFCheckoutResponseCallback) {
        activityRef = WeakReference(activity)
        paymentCallback = callback
        
        try {
            CFPaymentGatewayService.getInstance().setCheckoutCallback(callback)
            Log.d(TAG, "Cashfree SDK initialized")
        } catch (e: CFException) {
            Log.e(TAG, "Failed to initialize Cashfree SDK", e)
        }
    }

    suspend fun createOrder(credits: Int, currency: String = "INR"): Result<OrderResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "credits" to credits.toString(),
                    "currency" to currency
                )
                
                // Call the API to create an order
                val response = apiKeyService.createCreditOrder(request)
                Log.d(TAG, "Order created: ${response.orderId}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating order", e)
                Result.failure(e)
            }
        }
    }

    suspend fun verifyOrder(orderId: String): Result<VerifyResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf("orderId" to orderId)
                val response = apiKeyService.verifyCreditOrder(request)
                Log.d(TAG, "Order verified: $orderId, success: ${response.success}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying order", e)
                Result.failure(e)
            }
        }
    }

    suspend fun startUpiPayment(orderId: String, sessionId: String): Result<Unit> {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val activity = activityRef?.get() ?: run {
                        Log.e(TAG, "Activity reference lost")
                        continuation.resume(Result.failure(Exception("Activity reference lost")))
                        return@suspendCancellableCoroutine
                    }

                    // Create session object
                    val cfSession = CFSession.CFSessionBuilder()
                        .setEnvironment(CFSession.Environment.SANDBOX)
                        .setPaymentSessionID(sessionId)
                        .setOrderId(orderId)
                        .build()

                    // Create theme (optional customization)
                    val cfTheme = CFWebCheckoutTheme.CFWebCheckoutThemeBuilder()
                        .setNavigationBarBackgroundColor("#1C1C1C")
                        .setNavigationBarTextColor("#FFFFFF")
                        .build()

                    // Create web checkout payment object
                    val cfWebCheckoutPayment = CFWebCheckoutPayment.CFWebCheckoutPaymentBuilder()
                        .setSession(cfSession)
                        .setCFWebCheckoutUITheme(cfTheme)
                        .build()

                    // Start payment
                    CFPaymentGatewayService.getInstance().doPayment(activity, cfWebCheckoutPayment)
                    continuation.resume(Result.success(Unit))
                } catch (e: CFException) {
                    Log.e(TAG, "Failed to start UPI payment", e)
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }
} 