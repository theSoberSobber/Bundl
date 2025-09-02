package com.pavit.bundl.data.maps

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pavit.bundl.domain.maps.MapProvider
import com.pavit.bundl.domain.model.Order
import com.pavit.bundl.data.utils.LocationManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class MapboxProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationManager: LocationManager
) : MapProvider {
    
    // Default location (Bangalore)
    private val defaultLatitude = 12.9716
    private val defaultLongitude = 77.5946
    
    // Search radius in km - matching the value used in OrderRepository
    private val searchRadiusKm = 0.5
    
    // Calculate appropriate zoom level to show the search radius
    // Formula: zoom level = ln(earth circumference / (radius * 256)) / ln(2)
    // We simplify this to a rough approximation for our needs
    private val defaultZoomLevel = calculateZoomLevel(searchRadiusKm.toDouble())
    
    // Store the viewport state to allow for camera control outside of composable
    private var currentViewportState: MapViewportState? = null
    
    // Selected order tracking
    private var selectedOrderId: String? = null
    
    // Auto-follow user location
    private var autoFollowUser = false
    
    // Auto-follow timer to automatically disable after a delay
    private var autoFollowTimer: Job? = null
    
    // Auto-follow duration in milliseconds (15 seconds)
    private val autoFollowDuration = 15000L
    
    // Log tag
    private val TAG = "MapboxProvider"
    
    // Colors
    private val userLocationColor = Color(0xFF2196F3) // Blue
    private val orderMarkerColor = Color(0xFFE53935) // Red
    private val selectedOrderColor = Color(0xFF4CAF50) // Green
    
    /**
     * Calculate appropriate zoom level to view a radius in km
     * This is an approximation formula to convert radius to zoom level
     */
    private fun calculateZoomLevel(radiusKm: Double): Double {
        // Start with a base zoom that shows approximately 0.5km radius on most devices
        val baseZoom = 15.5
        
        // Adjust zoom based on radius (zoom out as radius increases)
        // ln(radius) provides a logarithmic scale that works well for this purpose
        val zoomAdjustment = ln(radiusKm / 0.5) / ln(2.0)
        
        // Subtract the adjustment from base zoom (larger radius = lower zoom level)
        return baseZoom - zoomAdjustment
    }
    
    @Composable
    override fun RenderMap(
        modifier: Modifier,
        orders: List<Order>,
        onMarkerClick: (Order) -> Unit,
        isDarkMode: Boolean
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        
        // Get current location from location manager
        val locationData by locationManager.currentLocation.collectAsState()
        
        // Create and remember the viewport state
        val viewportState = rememberMapViewportState {
            // Only set the initial camera position once, when the map is first created
            setCameraOptions {
                zoom(defaultZoomLevel)
                center(Point.fromLngLat(locationData.longitude, locationData.latitude))
                pitch(0.0)
                bearing(0.0)
            }
        }
        
        // Store the viewport state for external control
        currentViewportState = viewportState
        
        // Render the Mapbox map
        MapboxMap(
            modifier = modifier.fillMaxSize(),
            style = { MapStyle(style = if (isDarkMode) Style.DARK else Style.OUTDOORS) },
            mapViewportState = viewportState
        ) {
            // Add user location marker (blue)
            CircleAnnotation(
                point = Point.fromLngLat(locationData.longitude, locationData.latitude)
            ) {
                circleRadius = 12.0
                circleColor = userLocationColor
                circleStrokeWidth = 2.0
                circleStrokeColor = Color.White
                
                interactionsState.onClicked {
                    Log.d(TAG, "Current location clicked at: ${locationData.latitude}, ${locationData.longitude}")
                    true
                }
            }
            
            // Add markers for all orders
            orders.forEach { order ->
                val isSelected = order.id == selectedOrderId
                CircleAnnotation(
                    point = Point.fromLngLat(order.longitude, order.latitude)
                ) {
                    // Style properties for the circle
                    circleRadius = if (isSelected) 10.0 else 8.0
                    circleColor = if (isSelected) selectedOrderColor else orderMarkerColor
                    circleStrokeWidth = 2.0
                    circleStrokeColor = Color.White
                    
                    // Add click handler
                    interactionsState.onClicked {
                        Log.d(TAG, "Order clicked: ${order.id}")
                        onMarkerClick(order)
                        true
                    }
                }
            }
        }
        
        // Log when location changes
        LaunchedEffect(locationData) {
            Log.d(TAG, "User location updated: ${locationData.latitude}, ${locationData.longitude} - Only marker position updated, not camera")
        }
    }
    
    /**
     * Set the selected order to highlight on the map
     */
    override fun setSelectedOrder(orderId: String?) {
        selectedOrderId = orderId
    }
    
    override fun zoomToLocation(latitude: Double, longitude: Double, zoomLevel: Double) {
        currentViewportState?.let { viewportState ->
            viewportState.setCameraOptions {
                center(Point.fromLngLat(longitude, latitude))
                zoom(zoomLevel)
            }
        }
    }
    
    override fun centerOnUserLocation(): Boolean {
        val currentLocation = locationManager.currentLocation.value
        
        if (currentLocation.isFromUser) {
            // Enable auto-follow
            enableAutoFollow()
            
            zoomToLocation(
                currentLocation.latitude,
                currentLocation.longitude,
                defaultZoomLevel // Use our calculated zoom level
            )
            return true
        }
        
        // Try to get a fresh location
        locationManager.tryGetLastLocation()
        return false
    }
    
    /**
     * Animate camera to a new position with smooth transitions
     */
    override fun animateCamera(
        latitude: Double?,
        longitude: Double?,
        zoom: Double?,
        bearing: Double?,
        tilt: Double?,
        duration: Long,
        paddingBottom: Float
    ): Boolean {
        val viewportState = currentViewportState ?: return false
        
        try {
            // Create a target camera options builder
            val cameraOptionsBuilder = CameraOptions.Builder()
            
            // Only set values that are provided
            if (latitude != null && longitude != null) {
                cameraOptionsBuilder.center(Point.fromLngLat(longitude, latitude))
                
                // Apply bottom padding if specified
                if (paddingBottom > 0) {
                    // Create padding values (in screen pixels)
                    val screenDensity = context.resources.displayMetrics.density
                    val paddingBottomPx = (paddingBottom * screenDensity).toDouble()
                    
                    // Create EdgeInsets for padding (top, left, bottom, right)
                    val edgeInsets = com.mapbox.maps.EdgeInsets(0.0, 0.0, paddingBottomPx, 0.0)
                    
                    // Set padding on the camera
                    cameraOptionsBuilder.padding(edgeInsets)
                    
                    Log.d(TAG, "Applying camera padding: bottom=${paddingBottomPx}px")
                }
            }
            
            if (zoom != null) {
                cameraOptionsBuilder.zoom(zoom)
            }
            
            if (bearing != null) {
                cameraOptionsBuilder.bearing(bearing)
            }
            
            if (tilt != null) {
                cameraOptionsBuilder.pitch(tilt)
            }
            
            // Use the basic animation without custom parameters
            viewportState.easeTo(cameraOptionsBuilder.build())
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error animating camera", e)
            return false
        }
    }
    
    /**
     * Disables auto-following of user location
     */
    override fun disableAutoFollow() {
        if (autoFollowUser) {
            autoFollowUser = false
            Log.d(TAG, "Disabling auto-follow mode")
            
            // Cancel the auto-follow timer
            autoFollowTimer?.cancel()
            autoFollowTimer = null
        }
    }
    
    /**
     * Enables auto-following of user location
     */
    override fun enableAutoFollow() {
        if (!autoFollowUser) {
            autoFollowUser = true
            Log.d(TAG, "Enabling auto-follow mode")
            
            // Cancel any existing timer
            autoFollowTimer?.cancel()
            
            // Start a new timer to automatically disable auto-follow after a delay
            autoFollowTimer = MainScope().launch {
                try {
                    delay(autoFollowDuration)
                    disableAutoFollow()
                    Log.d(TAG, "Auto-follow disabled automatically after timeout")
                } catch (e: Exception) {
                    // Timer was canceled
                }
            }
        }
    }
} 