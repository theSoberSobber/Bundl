package com.pavit.bundl.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavit.bundl.domain.model.Order
import com.pavit.bundl.presentation.theme.BundlColors

@Composable
fun PledgeDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pledgeAmount by remember { mutableStateOf(50) }
    
    // Calculate remaining amount after potential pledge
    val remainingAfterPledge = order.amountNeeded - order.totalPledge - pledgeAmount
    val wouldCompleteOrder = remainingAfterPledge <= 0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pledge to Order",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "How much would you like to pledge?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                    
                // Amount input
                OutlinedTextField(
                    value = pledgeAmount.toString(),
                    onValueChange = { 
                        val amount = it.toIntOrNull() ?: 0
                        if (amount <= order.amountNeeded) {
                            pledgeAmount = amount
                        }
                    },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show completion status
                if (wouldCompleteOrder) {
                    Text(
                        text = "🎉 This pledge will complete the order!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BundlColors.StatusSuccess, // Material Green
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "₹$remainingAfterPledge will still be needed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BundlColors.StatusError, // Material Red
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pledgeAmount) },
                enabled = pledgeAmount > 0
            ) {
                Text("Confirm Pledge")
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
