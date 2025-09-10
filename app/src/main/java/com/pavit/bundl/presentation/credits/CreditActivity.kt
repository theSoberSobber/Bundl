package com.pavit.bundl.presentation.credits

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pavit.bundl.domain.payment.PaymentService
import com.pavit.bundl.presentation.theme.BundlTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreditActivity : ComponentActivity(), PaymentService.PaymentCallback {
    
    @Inject
    lateinit var paymentService: PaymentService
    
    private val viewModel: CreditsViewModel by viewModels()
    
    companion object {
        private const val TAG = "CreditActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize PaymentService with RevenueCat callback
        paymentService.initialize(this, this)
        
        setContent {
            CreditScreen()
        }
    }
    
    @Composable
    private fun CreditScreen() {
        BundlTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                GetMoreCreditsScreen(navController = navController, viewModel = viewModel)
            }
        }
    }
    
    override fun onPaymentSuccess(orderId: String, credits: Int) {
        Log.d(TAG, "Payment successful for order: $orderId, credits: $credits")
        viewModel.onPaymentCompleted(true, null)
    }
    
    override fun onPaymentFailure(error: String) {
        Log.e(TAG, "Payment failed: $error")
        viewModel.onPaymentCompleted(false, error)
    }
    
    override fun onPaymentCancelled() {
        Log.d(TAG, "Payment cancelled by user")
        viewModel.onPaymentCompleted(false, "Payment cancelled")
    }
} 