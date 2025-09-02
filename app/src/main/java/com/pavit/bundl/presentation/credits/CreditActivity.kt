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
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.utils.CFErrorResponse
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreditActivity : ComponentActivity(), CFCheckoutResponseCallback {
    
    @Inject
    lateinit var paymentService: PaymentService
    
    private val viewModel: CreditsViewModel by viewModels()
    
    companion object {
        private const val TAG = "CreditActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Cashfree with this activity as the callback handler
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
    
    override fun onPaymentVerify(orderId: String) {
        Log.d(TAG, "Payment verified for order: $orderId")
        viewModel.onPaymentCompleted(true, null)
    }
    
    override fun onPaymentFailure(cfErrorResponse: CFErrorResponse, orderId: String) {
        Log.e(TAG, "Payment failed for order: $orderId, error: ${cfErrorResponse.message}")
        viewModel.onPaymentCompleted(false, cfErrorResponse.message)
    }
} 