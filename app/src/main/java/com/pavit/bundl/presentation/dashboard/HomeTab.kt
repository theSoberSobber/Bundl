package com.pavit.bundl.presentation.dashboard

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
import com.pavit.bundl.R
import com.pavit.bundl.domain.maps.MapProvider
import com.pavit.bundl.domain.model.Order
import com.pavit.bundl.presentation.navigation.Route
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.pavit.bundl.presentation.credits.CreditActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import com.pavit.bundl.presentation.orders.MyOrdersViewModel
import androidx.compose.material3.Switch
import com.pavit.bundl.presentation.location.LocationTrackingViewModel
import com.pavit.bundl.presentation.theme.BundlColors
import com.pavit.bundl.presentation.dashboard.components.*

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
    
    // Get the LocationTrackingViewModel for nearby orders toggle
    val locationTrackingViewModel: LocationTrackingViewModel = hiltViewModel()
    val locationState by locationTrackingViewModel.uiState.collectAsState()
    
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
                isDarkMode = isDarkMap,
                shouldRenderMap = state.hasRealLocation, // Only render when we have real location
                userLatitude = state.userLatitude,
                userLongitude = state.userLongitude
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
                onClick = { navController.navigate(Route.MyOrders.route) },
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
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSurface
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
            color = BundlColors.SurfaceBanner  // Banner area background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Banner row between outer and inner surfaces - Notify nearby orders toggle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Notify nearby orders",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BundlColors.TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (locationState.isTrackingEnabled) {
                                    "Listening on ${locationState.currentGeohashes.size} areas"
                                } else {
                                    "~200m radius around your location"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = BundlColors.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        
                        Switch(
                            checked = locationState.isTrackingEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    locationTrackingViewModel.startLocationTracking()
                                } else {
                                    locationTrackingViewModel.stopLocationTracking()
                                }
                            }
                        )
                    }
                }
                
                // Inner Surface (back to original dark color)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = BundlColors.SurfaceDark  // Main content area
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
                        color = BundlColors.TextPrimary
                    )
                    
                    var isBlackBackground by remember { mutableStateOf(false) }
                    
                    IconButton(
                        onClick = { showCreateOrderDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(BundlColors.Radius.Medium.dp)
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
                    color = BundlColors.SurfaceDark,
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
                                color = BundlColors.TextPrimary
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Show credits balance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(BundlColors.Radius.Large.dp))
                                    .background(BundlColors.SurfaceLight)
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
                    color = BundlColors.TextPrimary
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
                                shape = RoundedCornerShape(BundlColors.Radius.Button.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = BundlColors.ButtonDisabled
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
                                shape = RoundedCornerShape(BundlColors.Radius.Button.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BundlColors.SurfaceLight
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                        Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh orders and credits",
                                    tint = BundlColors.TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
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
