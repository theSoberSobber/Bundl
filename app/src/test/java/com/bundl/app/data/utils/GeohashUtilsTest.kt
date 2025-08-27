package com.bundl.app.data.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * Test cases for GeohashUtils to verify geohash calculations work correctly
 */
class GeohashUtilsTest {
    
    @Test
    fun testBasicEncoding() {
        // Test known geohash for Bangalore coordinates
        val latitude = 12.9716
        val longitude = 77.5946
        val precision = 5
        
        val geohash = GeohashUtils.encode(latitude, longitude, precision)
        
        assertNotNull(geohash)
        assertEquals(precision, geohash.length)
        assertTrue("Geohash should contain valid base32 characters", 
                   geohash.all { it in "0123456789bcdefghjkmnpqrstuvwxyz" })
    }
    
    @Test
    fun testEncodingDecoding() {
        val originalLat = 12.9716
        val originalLon = 77.5946
        val precision = 6
        
        val geohash = GeohashUtils.encode(originalLat, originalLon, precision)
        val decoded = GeohashUtils.decode(geohash)
        
        // Should be within reasonable precision (0.01 degrees ≈ 1km)
        val latDiff = Math.abs(decoded.latitude - originalLat)
        val lonDiff = Math.abs(decoded.longitude - originalLon)
        
        assertTrue("Latitude should decode within 0.01 degrees", latDiff < 0.01)
        assertTrue("Longitude should decode within 0.01 degrees", lonDiff < 0.01)
    }
    
    @Test
    fun testHighPrecisionFor200mTargeting() {
        val latitude = 12.9716
        val longitude = 77.5946
        val precision = 7 // For 200m targeting
        
        val geohash = GeohashUtils.encode(latitude, longitude, precision)
        val decoded = GeohashUtils.decode(geohash)
        
        // For precision 7, should be within ~100m accuracy
        val distance = GeohashUtils.distanceMeters(
            latitude, longitude, 
            decoded.latitude, decoded.longitude
        )
        
        assertTrue("Precision 7 should be accurate within 100m for 200m targeting", distance < 100)
        assertEquals("Should have 7 character geohash", 7, geohash.length)
    }
    
    @Test
    fun testCoverageFor200mRadius() {
        val centerLat = 12.9716
        val centerLon = 77.5946
        val radiusMeters = 200.0 // 200m as requested
        val precision = 7 // ~153m cells
        
        val geohashes = GeohashUtils.getCoverageGeohashes(
            centerLat, centerLon, radiusMeters, precision
        )
        
        assertTrue("Should have at least one geohash", geohashes.isNotEmpty())
        assertTrue("Should have reasonable number for 200m radius", geohashes.size < 20)
        
        // Verify all geohashes are within reasonable distance
        val centerGeohash = GeohashUtils.encode(centerLat, centerLon, precision)
        assertTrue("Should include center geohash", geohashes.contains(centerGeohash))
        
        // Test that all geohashes are actually within or near the radius
        geohashes.forEach { geohash ->
            val decoded = GeohashUtils.decode(geohash)
            val distance = GeohashUtils.distanceMeters(
                centerLat, centerLon,
                decoded.latitude, decoded.longitude
            )
            
            // Should be within radius + cell size tolerance (~350m max)
            assertTrue("Geohash $geohash should be within coverage area", distance < 350)
        }
    }
    
    @Test
    fun testNeighbors() {
        val geohash = "dr5ru" // Bangalore area
        val neighbors = GeohashUtils.getNeighbors(geohash)
        
        assertTrue("Should have neighbors", neighbors.isNotEmpty())
        assertTrue("Should have up to 8 neighbors", neighbors.size <= 8)
        
        // All neighbors should have same precision
        neighbors.forEach { neighbor ->
            assertEquals("Neighbor should have same precision", geohash.length, neighbor.length)
        }
    }
    
    @Test
    fun testDistanceCalculation() {
        val lat1 = 12.9716 // Bangalore
        val lon1 = 77.5946
        val lat2 = 13.0827 // Another point in Bangalore
        val lon2 = 80.2707
        
        val distance = GeohashUtils.distanceMeters(lat1, lon1, lat2, lon2)
        
        assertTrue("Distance should be positive", distance > 0)
        assertTrue("Distance should be reasonable", distance < 1000000) // Less than 1000km
    }
    
    @Test
    fun testPrecisionLevels() {
        val latitude = 12.9716
        val longitude = 77.5946
        
        for (precision in 1..8) {
            val geohash = GeohashUtils.encode(latitude, longitude, precision)
            assertEquals("Precision should match requested", precision, geohash.length)
        }
    }
    
    @Test
    fun testBoundaryConditions() {
        // Test edge cases
        val edgeCases = listOf(
            Pair(90.0, 180.0),   // North-East extreme
            Pair(-90.0, -180.0), // South-West extreme
            Pair(0.0, 0.0),      // Equator-Prime Meridian
            Pair(12.9716, 77.5946) // Regular case
        )
        
        edgeCases.forEach { (lat, lon) ->
            val geohash = GeohashUtils.encode(lat, lon, 5)
            val decoded = GeohashUtils.decode(geohash)
            
            assertNotNull("Should encode boundary condition", geohash)
            assertNotNull("Should decode boundary condition", decoded)
        }
    }
}
