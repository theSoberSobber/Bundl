package com.bundl.app.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bundl.app.domain.model.Order
import com.bundl.app.presentation.theme.BundlColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideOption(
    order: Order,
    onClick: () -> Unit,
    isSelected: Boolean,
    isFeatured: Boolean
) {
    val backgroundColor = BundlColors.SurfaceLight
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row with platform and users count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform name
                Text(
                    text = order.platform.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Users circles
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserCircles(
                        count = order.totalUsers,
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${order.totalUsers} joined",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }
            }
                
            Spacer(modifier = Modifier.height(12.dp))
                
            // Progress section
            Column {
                // Amount progress text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "₹${order.amountNeeded - order.totalPledge} more needed",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "out of ₹${order.amountNeeded}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { (order.totalPledge).toFloat() / order.amountNeeded.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = BundlColors.SwitchTrack
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Pledged amount
                Text(
                    text = "₹${order.totalPledge} pledged so far",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
