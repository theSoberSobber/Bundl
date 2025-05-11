package com.bundl.app.presentation.dashboard

import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.bundl.app.R
import com.bundl.app.domain.maps.MapProvider
import com.bundl.app.domain.model.Order
import com.bundl.app.presentation.navigation.Route
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.bundl.app.presentation.credits.CreditActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import com.bundl.app.presentation.orders.MyOrdersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onLogout: () -> Unit = {},
    mapProvider: MapProvider,
    navController: NavController
) {
    val state by viewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    val context = LocalContext.current
    
    // Define sheet height (50% of screen)
    val sheetHeight = screenHeight * 0.5f
    
    // Set up pointer intercept area for the map - this is the bounding box
    // We'll implement the bounding box through hit testing rather than sizing
    var mapBoundingBoxY by remember { mutableFloatStateOf(0f) }
    
    // Add state for map style (dark/light mode)
    var isDarkMap by remember { mutableStateOf(true) }
    
    // Add scroll state for the orders list
    val listScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    // Add state for showing dialogs
    var showPledgeDialog by remember { mutableStateOf(false) }
    var showCreateOrderDialog by remember { mutableStateOf(false) }
    var selectedOrder: Order? by remember { mutableStateOf(null) }
    
    // Get the MyOrdersViewModel to add pledged orders
    val myOrdersViewModel: MyOrdersViewModel = hiltViewModel()
    
    // Effect to scroll to selected order
    LaunchedEffect(state.selectedOrderId) {
        state.selectedOrderId?.let { selectedId ->
            // Find the index of the selected order
            val selectedIndex = state.activeOrders.indexOfFirst { it.id == selectedId }
            if (selectedIndex != -1) {
                // Calculate approximate scroll position (each item is about 100dp tall including spacing)
                val approximateScrollPosition = selectedIndex * 108
            coroutineScope.launch {
                    listScrollState.animateScrollTo(approximateScrollPosition)
                }
            }
        }
    }
    
    // Log screen and bounding box heights
    LaunchedEffect(Unit) {
        android.util.Log.d("BUNDL_BOUNDING_BOX", "Total screen height: $screenHeight")
    }
    
    LaunchedEffect(mapBoundingBoxY) {
        val boundingBoxHeightPx = screenHeight.value * density.density - mapBoundingBoxY
        val boundingBoxHeightDp = boundingBoxHeightPx / density.density
        android.util.Log.d("BUNDL_BOUNDING_BOX", "Map bounding box height: ${boundingBoxHeightDp}dp, Y coordinate: ${mapBoundingBoxY / density.density}dp")
        
        // When bounding box height is first calculated, center the map properly
        if (mapBoundingBoxY > 0f) {
            // Calculate the visible map height in dp
            val visibleMapHeightDp = boundingBoxHeightDp
            
            // Center in the visible area of the map with a small delay to ensure map is ready
            viewModel.centerMapOnUserLocationInVisibleArea(
                visibleMapHeightDp = visibleMapHeightDp,
                screenHeightDp = screenHeight.value
            )
        }
    }
    
    // First, add a LaunchedEffect to poll for credits
    LaunchedEffect(Unit) {
        while(true) {
            viewModel.refreshCredits()
            delay(30000) // 30 seconds refresh interval
        }
    }
    
    // Add immediate refresh when screen becomes active
    LaunchedEffect(Unit) {
        Log.d("BUNDL_CREDITS", "HomeTab became active - forcing immediate refresh")
        viewModel.fetchCreditsInfo(true) // Force an immediate refresh with loading indicator
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Full screen map with pointer events conditional on position
                        Box(
                            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Only allow map interactions in the top half (above the sheet)
                        if (offset.y < mapBoundingBoxY) {
                            android.util.Log.d("BUNDL_BOUNDING_BOX", "Touch inside map area: ${offset.y}")
                            // Pass through to map
                        } else {
                            android.util.Log.d("BUNDL_BOUNDING_BOX", "Touch inside sheet area: ${offset.y}")
                            // Intercept for bottom sheet
                        }
                    }
                }
        ) {
            // Map view rendered at full screen
            mapProvider.RenderMap(
                modifier = Modifier.fillMaxSize(),
                orders = state.activeOrders,
                onMarkerClick = { order -> 
                    viewModel.selectOrderOnMap(order)
                },
                isDarkMode = isDarkMap
            )
        }
            
        // Top row containing My Orders and sun/moon toggle
        Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // My Orders button
            Button(
                onClick = { navController.navigate(Route.MyOrdersScreen.route) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                        Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "My Orders",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("My Orders")
            }

            // Style toggle button
            FloatingActionButton(
                onClick = { isDarkMap = !isDarkMap },
                modifier = Modifier.size(40.dp),
                containerColor = Color(0xCC000000), // Semi-transparent black
                contentColor = Color.White
            ) {
                Text(
                    text = if (isDarkMap) "🌙" else "☀️",
                    fontSize = 16.sp
                )
            }
        }
        
        // Location button - positioned in bottom right corner (above the sheet)
        FloatingActionButton(
            onClick = { 
                // Calculate the visible map height in dp
                val visibleMapHeightDp = with(density) { 
                    (screenHeight.value * density.density - mapBoundingBoxY) / density.density 
                }
                
                // Center in the visible area of the map
                viewModel.centerMapOnUserLocationInVisibleArea(
                    visibleMapHeightDp = visibleMapHeightDp,
                    screenHeightDp = screenHeight.value
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = if (mapBoundingBoxY > 0f) {
                    with(density) { (-mapBoundingBoxY / density.density).dp - 16.dp }
                } else {
                    -16.dp
                })
                .padding(end = 16.dp),
            containerColor = Color(0xCC000000), // Semi-transparent black
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = "Center location"
            )
        }
        
        // Bottom sheet (fixed at 50% height)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .align(Alignment.BottomCenter)
                .onSizeChanged { size ->
                    // Get the top Y coordinate of the sheet to set bounding box
                    mapBoundingBoxY = size.height.toFloat()
                },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color(0xFF1C1C1C)  // Darker shade for Uber-like look
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag handle
                Box(
                modifier = Modifier
                .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(
                                color = Color.LightGray,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
                
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                        text = "Choose an order",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    var isBlackBackground by remember { mutableStateOf(false) }
                    
                    IconButton(
                        onClick = { showCreateOrderDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Order",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Content area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    if (state.activeOrders.isEmpty()) {
                        EmptyOrdersCard()
                    } else {
        Column(
            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(listScrollState)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Display all orders
                            state.activeOrders.forEach { order ->
                                RideOption(
                                    order = order,
                                    onClick = { viewModel.selectOrderOnMap(order) },
                                    isSelected = viewModel.isOrderSelected(order.id),
                                    isFeatured = false
                                )
            Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                
                // Fixed bottom bar
                Surface(
                    color = Color(0xFF1C1C1C),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Credits row (formerly payment method row)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { 
                                    // Launch CreditActivity
                                    val intent = Intent(context, CreditActivity::class.java)
                                    context.startActivity(intent)
                                },
                            verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                                text = "💰",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "Get More Credits",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Show credits balance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF2C2C2C))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // Force refresh of credits when this part of UI is drawn
                                LaunchedEffect(Unit) {
                                    Log.d("BUNDL_CREDITS", "Credits UI element became visible - requesting refresh")
                                    viewModel.fetchCreditsInfo(false)
                                }
                                
                                // Debug the entire state object
                                Log.d("BUNDL_CREDITS", "ENTIRE STATE: $state")
                                
                                // Use the credits value from state
                                val creditsValue = state.userCredits
                                Log.d("BUNDL_CREDITS", "Displaying credits value from API: $creditsValue")
                                
                                // Show the actual value from API
                        Text(
                                    text = "$creditsValue",
                            style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                        
                        // Two buttons side by side
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Show the pledge button
                            Button(
                                onClick = { 
                                    selectedOrder = state.activeOrders.find { it.id == state.selectedOrderId }
                                    showPledgeDialog = true 
                                },
                                modifier = Modifier.weight(1f),
                                enabled = state.selectedOrderId != null,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = Color(0xFF2C2C2C)
                                )
                            ) {
                                Text(
                                    text = "Pledge to this Order!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Refresh button
                            Button(
                                onClick = {
                                    viewModel.refreshCredits()
                                    android.util.Log.d("BUNDL_ORDERS", "Refreshing orders and credits")
                                },
                                modifier = Modifier.width(56.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2C2C2C)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                        Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh orders and credits",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Show pledge dialog when an order is selected for pledging
    if (showPledgeDialog && selectedOrder != null) {
        PledgeDialog(
            order = selectedOrder!!,
            onDismiss = { showPledgeDialog = false },
            onConfirm = { amount ->
                showPledgeDialog = false
                viewModel.pledgeToOrder(selectedOrder!!.id, amount) { route ->
                    // Add logging
                    Log.d("BUNDL_ORDERS", "Pledging to order: ${selectedOrder!!.id}, amount: $amount")
                    Log.d("BUNDL_ORDERS", "Order details: platform=${selectedOrder!!.platform}, totalUsers=${selectedOrder!!.totalUsers}")
                    
                    // Add the order to MyOrdersViewModel
                    myOrdersViewModel.addOrderToPledged(selectedOrder!!)
                    
                    // More logging
                    Log.d("BUNDL_ORDERS", "Added order to MyOrdersViewModel, navigating to $route")
                    
                    // Navigate to MyOrdersScreen
                    navController.navigate(route)
                }
            }
        )
    }
    
    // Show create order dialog
    if (showCreateOrderDialog) {
        CreateOrderDialog(
            onDismiss = { showCreateOrderDialog = false },
            onConfirm = { amountNeeded, platform, initialPledge ->
                Log.d("BUNDL_DEBUG", "CreateOrderDialog confirmed with: amount=$amountNeeded, platform=$platform, pledge=$initialPledge")
                showCreateOrderDialog = false
                viewModel.createOrder(amountNeeded, platform, initialPledge) { route, order ->
                    Log.d("BUNDL_DEBUG", "Order created, received callback with order: ${order.id}")
                    Log.d("BUNDL_DEBUG", "Order details - status: ${order.status}, platform: ${order.platform}, amount: ${order.amountNeeded}")
                    
                    // Add the order to MyOrdersViewModel
                    Log.d("BUNDL_DEBUG", "Adding order to MyOrdersViewModel")
                    myOrdersViewModel.addOrderToPledged(order)
                    Log.d("BUNDL_DEBUG", "Order added to MyOrdersViewModel")
                    
                    // Navigate to MyOrdersScreen
                    Log.d("BUNDL_DEBUG", "Navigating to route: $route")
                    navController.navigate(route)
                }
            }
        )
    }
}

@Composable
fun PaymentBar(
    onExpand: () -> Unit
) {
    Surface(
        color = Color.DarkGray.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Payment method row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { /* Show payment options */ },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkmark
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Personal text
                        Text(
                    text = "Personal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Cash text
                        Text(
                    text = "Cash",
                            style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Down arrow
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Payment options",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Expand arrow - only in collapsed state
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Expand",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onExpand() }
                )
            }
            
            // Button (only in expanded states)
            Button(
                onClick = { /* Book ride */ },
                        modifier = Modifier
                            .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "Choose Uber Go",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun UserCircles(
    count: Int,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF4285F4), // Google Blue
        Color(0xFFDB4437), // Google Red
        Color(0xFFF4B400), // Google Yellow
        Color(0xFF0F9D58), // Google Green
        Color(0xFF7B1FA2)  // Purple
    )
    
    Box(modifier = modifier) {
        // Show up to 4 circles + a count if there are more
        val circleSize = 20.dp
        val overlap = 12.dp // Amount of overlap between circles
        val maxVisibleCircles = minOf(count, 4)
        
        // Draw circles from right to left
        for (i in 0 until maxVisibleCircles) {
            Box(
            modifier = Modifier
                    .size(circleSize)
                    .offset(x = -(overlap * i))
                    .zIndex((maxVisibleCircles - i).toFloat()) // Higher z-index for left circles
                    .background(colors[i], CircleShape)
                    .border(1.dp, Color(0xFF2C2C2C), CircleShape)
            ) {
                // If this is the last circle and we have more users, show the count
                if (i == maxVisibleCircles - 1 && count > 4) {
            Text(
                        text = "+${count - 3}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideOption(
    order: Order,
    onClick: () -> Unit,
    isSelected: Boolean,
    isFeatured: Boolean
) {
    val backgroundColor = Color(0xFF2C2C2C)
    
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
                    text = order.platform.capitalize(),
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
                    progress = (order.totalPledge).toFloat() / order.amountNeeded.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF4D4D4D)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportOption(
    rideName: String,
    price: String,
    estimatedTime: String,
    distance: String,
    isFaster: Boolean,
    order: Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle image with different icons based on type
                Box(
                    modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = when(rideName) {
                            "Auto" -> Color(0xFFFBC02D) // Yellow for auto
                            "Moto" -> Color(0xFFFF7043) // Orange for motorcycle
                            else -> MaterialTheme.colorScheme.surfaceVariant // Default for cars
                        },
                        shape = CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_location), // Replace with appropriate vehicle icons
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Ride details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rideName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = price,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$estimatedTime • $distance away",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Description or faster badge
                if (isFaster) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_location),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                            Text(
                                    text = "Faster",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                            Text(
                        text = when(rideName) {
                            "Auto" -> "Pay directly to driver, cash/UPI only"
                            "Go Sedan" -> "Affordable sedans"
                            "Moto" -> "Affordable motorcycle rides"
                            else -> "Comfortable ride"
                        },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyOrdersCard() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(24.dp)
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

@Composable
private fun PledgeDialog(
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
                        color = Color(0xFF4CAF50), // Material Green
                        modifier = Modifier.fillMaxWidth()
                            )
                } else {
                            Text(
                        text = "₹$remainingAfterPledge will still be needed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE57373), // Material Red
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
                    containerColor = Color(0xFF2C2C2C)
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateOrderDialog(
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
                    containerColor = Color(0xFF2C2C2C)
                )
            ) {
                Text("Cancel")
            }
        }
    )
} 