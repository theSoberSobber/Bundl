package com.bundl.app.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bundl.app.presentation.theme.BundlColors

@Composable
fun CreateOrderDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Int) -> Unit
) {
    var amountNeeded by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var initialPledge by remember { mutableStateOf("") }
    val expirySeconds = 600 // Fixed value
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New Order",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = amountNeeded,
                    onValueChange = { amountNeeded = it },
                    label = { Text("Amount Needed for Offer/Free Delivery (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("Platform (e.g., Zomato, Swiggy)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = initialPledge,
                    onValueChange = { initialPledge = it },
                    label = { Text("What You're Paying (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Disabled expiry seconds field
                OutlinedTextField(
                    value = expirySeconds.toString(),
                    onValueChange = { },
                    label = { Text("Expiry Seconds") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountNeeded.toDoubleOrNull() ?: 0.0
                    val pledge = initialPledge.toIntOrNull() ?: 0
                    if (amount > 0 && platform.isNotBlank() && pledge > 0) {
                        onConfirm(amount, platform, pledge)
                    }
                },
                enabled = amountNeeded.toDoubleOrNull() != null && 
                         platform.isNotBlank() && 
                         initialPledge.toIntOrNull() != null
            ) {
                Text("Create Order")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BundlColors.SurfaceLight
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
