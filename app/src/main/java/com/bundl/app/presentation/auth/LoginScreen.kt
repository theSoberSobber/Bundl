package com.bundl.app.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bundl.app.R
import com.bundl.app.presentation.theme.Blue
import com.bundl.app.data.utils.DeviceUtils
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Composable
fun LoginScreen(
    onNavigateToOtp: (String, String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    
    var phoneNumber by remember { mutableStateOf("") }
    var isPhoneNumberValid by remember { mutableStateOf(false) }
    
    // Track if phone number field has been focused - we'll only show the phone hint dialog once
    var hasShownPhoneHint by remember { mutableStateOf(false) }
    
    // Create phone number hint launcher
    val phoneNumberHintLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                // Get the phone number from the intent
                val phoneNumberHint = Identity.getSignInClient(context)
                    .getPhoneNumberFromIntent(result.data)
                
                // Format the phone number to remove country code if needed
                // For India (+91), we extract the last 10 digits
                val formattedNumber = phoneNumberHint.takeLastWhile { it.isDigit() }
                    .takeLast(10)
                
                // Update the phone number field
                phoneNumber = formattedNumber
            }
        } catch (e: Exception) {
            viewModel.showError("Failed to get phone number: ${e.message}")
        }
    }
    
    // Function to request phone number hint
    fun requestPhoneNumberHint() {
        if (hasShownPhoneHint) return  // Only show once per screen session
        
        activity?.let {
            try {
                val request = GetPhoneNumberHintIntentRequest.builder().build()
                Identity.getSignInClient(it)
                    .getPhoneNumberHintIntent(request)
                    .addOnSuccessListener { result ->
                        try {
                            phoneNumberHintLauncher.launch(
                                IntentSenderRequest.Builder(result).build()
                            )
                            hasShownPhoneHint = true
                        } catch (e: Exception) {
                            viewModel.showError("Failed to launch phone number hint: ${e.message}")
                        }
                    }
                    .addOnFailureListener { e ->
                        viewModel.showError("Phone number hint failed: ${e.message}")
                        hasShownPhoneHint = true  // Mark as shown even if it failed
                    }
            } catch (e: Exception) {
                viewModel.showError("Error setting up phone number hint: ${e.message}")
                hasShownPhoneHint = true  // Mark as shown even if it failed
            }
        }
    }
    
    // Validate phone number whenever it changes
    LaunchedEffect(phoneNumber) {
        isPhoneNumberValid = phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }
    }
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Phone input with country code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isSystemInDarkTheme()) 
                            MaterialTheme.colorScheme.surface 
                        else 
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Remove ripple effect
                    ) {
                        // Trigger phone number hint when the row is clicked
                        requestPhoneNumberHint()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Use flag emoji instead of vector drawable
                    Text(
                        text = "🇮🇳",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                
                Text(
                    text = "+91",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        // Only allow digits and limit to 10 characters
                        if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                            phoneNumber = newValue
                        }
                    },
                    placeholder = { Text(stringResource(R.string.enter_phone_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    // Use interactionSource to detect focus and show phone hint dialog
                    interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                // Request phone hint when field is focused
                                requestPhoneNumberHint()
                            }
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (isPhoneNumberValid) {
                        viewModel.sendOtp("+91$phoneNumber") { transactionId ->
                            onNavigateToOtp(transactionId, "+91$phoneNumber")
                        }
                    } else {
                        viewModel.showError("Please enter a valid 10-digit phone number")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && isPhoneNumberValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (isLoading) 
                        stringResource(R.string.sending) 
                    else 
                        stringResource(R.string.continue_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun isSystemInDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background == Color.Black
} 