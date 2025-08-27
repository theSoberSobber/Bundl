package com.bundl.app.data.utils

/**
 * Centralized notification constants for the Bundl app
 * Contains all notification text, icons, and configuration to eliminate duplication
 */
object NotificationConstants {
    // Channel information
    const val LOCATION_CHANNEL_ID = "location_updates"
    const val LOCATION_CHANNEL_NAME = "Location Updates"
    const val LOCATION_CHANNEL_DESCRIPTION = "Tracks your location to show nearby orders"
    
    // Notification content
    const val NOTIFICATION_TITLE = "Listening for Orders Nearby"
    const val NOTIFICATION_TEXT_DEFAULT = "Monitoring your area for new orders"
    const val NOTIFICATION_TEXT_SETUP = "Setting up nearby monitoring..."
    
    // Notification ID
    const val LOCATION_NOTIFICATION_ID = 12345
    
    // Notification icon
    const val NOTIFICATION_ICON = android.R.drawable.ic_menu_mylocation
    
    /**
     * Formats the notification text based on the number of geohash areas being monitored
     */
    fun getNotificationText(geohashCount: Int): String {
        return if (geohashCount > 0) {
            "Listening on $geohashCount areas"
        } else {
            NOTIFICATION_TEXT_SETUP
        }
    }
}
