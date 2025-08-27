package com.bundl.app.presentation.orders

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bundl.app.domain.model.Order
import androidx.compose.ui.platform.LocalContext
import com.bundl.app.presentation.dashboard.components.RideOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: MyOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Add debug logging
    LaunchedEffect(state.localActiveOrders, state.localNonActiveOrders) {
        Log.d("BUNDL_ORDERS", "MyOrdersScreen - Active orders: ${state.localActiveOrders.size}")
        Log.d("BUNDL_ORDERS", "MyOrdersScreen - Non-active orders: ${state.localNonActiveOrders.size}")
        
        // Log details of each active order
        state.localActiveOrders.forEachIndexed { index, order ->
            Log.d("BUNDL_ORDERS", "Active order $index: id=${order.id}, platform=${order.platform}, totalUsers=${order.totalUsers}")
        }
    }
    
    // Force a refresh of orders when screen becomes visible
    LaunchedEffect(Unit) {
        Log.d("BUNDL_ORDERS", "MyOrdersScreen became active, refreshing orders")
        viewModel.refreshOrders()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add refresh button
                    IconButton(
                        onClick = { 
                            Log.d("BUNDL_ORDERS", "Manual refresh requested")
                            viewModel.refreshOrders()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh, 
                            contentDescription = "Refresh Orders"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Active Orders Section
            Text(
                text = "Active Orders",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            if (state.localActiveOrders.isEmpty()) {
                EmptyOrdersCard()
            } else {
                // Polling countdown
                Text(
                    text = "Refreshing in ${state.secondsUntilNextPoll} seconds",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Active orders list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.localActiveOrders) { order ->
                        RideOption(
                            order = order,
                            onClick = {},
                            isSelected = false,
                            isFeatured = false
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Order History Section
            Text(
                text = "Order History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (state.localNonActiveOrders.isEmpty()) {
                Text(
                    text = "No order history",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.localNonActiveOrders) { order ->
                        HistoryOrderCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryOrderCard(order: Order) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = order.platform.replaceFirstChar { it.uppercase() } + " (${order.totalPledge}/${order.amountNeeded})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                StatusChip(status = order.status)
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show pledge progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${order.totalPledge}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " out of ₹${order.amountNeeded}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show total users
                Text(
                    text = "${order.totalUsers} users joined",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Show phone numbers if available
                order.phoneNumberMap?.let { phoneMap ->
                    Log.d("BUNDL_PHONE_NUMBERS", "Order ${order.id} has phone numbers: $phoneMap")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Participants:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    phoneMap.forEach { (phone, amount) ->
                        Log.d("BUNDL_PHONE_NUMBERS", "Displaying phone: $phone with amount: $amount")
                        Text(
                            text = "$phone - ₹$amount",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } ?: run {
                    Log.d("BUNDL_PHONE_NUMBERS", "Order ${order.id} has NO phone numbers map")
                }
                
                // Show note if available
                order.note?.let { note ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OrderCard(
    order: Order,
    onOrderClick: () -> Unit,
    isSelected: Boolean
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOrderClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform name
                Text(
                    text = order.platform.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Users joined
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${order.totalUsers} users joined",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = order.totalPledge.toFloat() / order.amountNeeded.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Amount display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${order.totalPledge} pledged",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "₹${order.amountNeeded} needed",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun EmptyOrdersCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = Color(0xFF2C2C2C),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = Color.LightGray,
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "No Active Orders Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
} 