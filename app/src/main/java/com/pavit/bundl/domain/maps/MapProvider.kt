package com.pavit.bundl.domain.maps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pavit.bundl.domain.model.Order

/**
 * Interface for map providers (Mapbox, Google Maps, etc.)
 * This allows us to easily swap out map providers if needed
 */
interface MapProvider {
    /**
     * Renders a map in a Composable
     * 
     * @param modifier Modifier for styling
     * @param orders List of orders to display on the map
     * @param onMarkerClick Callback for when a marker is clicked
     * @param isDarkMode Whether to use dark mode styling for the map
     * @param shouldRenderMap Whether to render the map or show loading state
     * @param userLatitude User's current latitude (for initial positioning)
     * @param userLongitude User's current longitude (for initial positioning)
     */
    @Composable
    fun RenderMap(
        modifier: Modifier,
        orders: List<Order>,
        onMarkerClick: (Order) -> Unit,
        isDarkMode: Boolean,
        shouldRenderMap: Boolean = true,
        userLatitude: Double = 12.9716,
        userLongitude: Double = 77.5946
    )
    
    /**
     * Zoom to a specific location
     * 
     * @param latitude Latitude to zoom to
     * @param longitude Longitude to zoom to
     * @param zoomLevel Level of zoom (provider-specific)
     */
    fun zoomToLocation(latitude: Double, longitude: Double, zoomLevel: Double)
    
    /**
     * Centers the map on user's current location if available
     * 
     * @return true if successful, false otherwise
     */
    fun centerOnUserLocation(): Boolean
    
    /**
     * Disables auto-following of user location
     * Call this when the user manually moves the map
     */
    fun disableAutoFollow()
    
    /**
     * Enables auto-following of user location
     * Call this when the user explicitly requests to follow their location
     */
    fun enableAutoFollow()
    
    /**
     * Set the selected order to highlight on the map
     * 
     * @param orderId The ID of the order to highlight, or null to clear selection
     */
    fun setSelectedOrder(orderId: String?)
    
    /**
     * Animate camera to a new position with smooth transitions
     * 
     * @param latitude Target latitude (or null to keep current)
     * @param longitude Target longitude (or null to keep current)  
     * @param zoom Target zoom level (or null to keep current)
     * @param bearing Target bearing/rotation (or null to keep current)
     * @param tilt Target tilt/pitch (or null to keep current)
     * @param duration Animation duration in milliseconds
     * @param paddingBottom Bottom padding in dp to offset the center point vertically
     * @return true if animation started successfully
     */
    fun animateCamera(
        latitude: Double? = null,
        longitude: Double? = null,
        zoom: Double? = null,
        bearing: Double? = null,
        tilt: Double? = null,
        duration: Long = 300,
        paddingBottom: Float = 0f
    ): Boolean
} 