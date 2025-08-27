package com.bundl.app.data.utils

import kotlin.math.*

/**
 * Custom lightweight Geohash implementation for location-based subscriptions
 * 
 * This implementation provides the core geohash functionality needed for
 * dividing the world into grid squares with unique identifiers.
 * 
 * Geohash precision and approximate cell size:
 * - Precision 4: ~20 km x 20 km
 * - Precision 5: ~2.4 km x 4.8 km  
 * - Precision 6: ~0.61 km x 1.22 km
 * - Precision 7: ~0.153 km x 0.153 km (153 meters - ideal for 200m targeting)
 * - Precision 8: ~0.038 km x 0.019 km (38m x 19m - very precise)
 * - Precision 9: ~0.0048 km x 0.0048 km (4.8 meters - ultra-precise)
 */
object GeohashUtils {
    
    private const val BASE32_ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz"
    private val BASE32_MAP = BASE32_ALPHABET.withIndex().associate { (index, char) -> char to index }
    
    /**
     * Encode latitude and longitude to a geohash string
     */
    fun encode(latitude: Double, longitude: Double, precision: Int = 5): String {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        require(precision > 0) { "Precision must be positive" }
        
        val latRange = DoubleArray(2) { if (it == 0) -90.0 else 90.0 }
        val lonRange = DoubleArray(2) { if (it == 0) -180.0 else 180.0 }
        
        var isEven = true
        var bit = 0
        var base32Index = 0
        val geohash = StringBuilder()
        
        while (geohash.length < precision) {
            if (isEven) {
                // Process longitude
                val mid = (lonRange[0] + lonRange[1]) / 2
                if (longitude > mid) {
                    base32Index = (base32Index shl 1) or 1
                    lonRange[0] = mid
                } else {
                    base32Index = base32Index shl 1
                    lonRange[1] = mid
                }
            } else {
                // Process latitude
                val mid = (latRange[0] + latRange[1]) / 2
                if (latitude > mid) {
                    base32Index = (base32Index shl 1) or 1
                    latRange[0] = mid
                } else {
                    base32Index = base32Index shl 1
                    latRange[1] = mid
                }
            }
            
            isEven = !isEven
            
            if (++bit == 5) {
                geohash.append(BASE32_ALPHABET[base32Index])
                bit = 0
                base32Index = 0
            }
        }
        
        return geohash.toString()
    }
    
    /**
     * Decode a geohash string to latitude and longitude
     */
    fun decode(geohash: String): LatLng {
        require(geohash.isNotEmpty()) { "Geohash cannot be empty" }
        require(geohash.all { it in BASE32_ALPHABET }) { "Invalid geohash characters" }
        
        val latRange = doubleArrayOf(-90.0, 90.0)
        val lonRange = doubleArrayOf(-180.0, 180.0)
        
        var isEven = true
        
        for (char in geohash) {
            val base32Index = BASE32_MAP[char] ?: continue
            
            for (i in 4 downTo 0) {
                val bit = (base32Index shr i) and 1
                
                if (isEven) {
                    // Process longitude
                    val mid = (lonRange[0] + lonRange[1]) / 2
                    if (bit == 1) {
                        lonRange[0] = mid
                    } else {
                        lonRange[1] = mid
                    }
                } else {
                    // Process latitude
                    val mid = (latRange[0] + latRange[1]) / 2
                    if (bit == 1) {
                        latRange[0] = mid
                    } else {
                        latRange[1] = mid
                    }
                }
                
                isEven = !isEven
            }
        }
        
        return LatLng(
            latitude = (latRange[0] + latRange[1]) / 2,
            longitude = (lonRange[0] + lonRange[1]) / 2
        )
    }
    
    /**
     * Get all geohashes covering a circular area around a center point
     */
    fun getCoverageGeohashes(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double,
        precision: Int
    ): Set<String> {
        val geohashes = mutableSetOf<String>()
        
        // Calculate the approximate degree equivalent of the radius
        val earthRadiusMeters = 6371000.0
        val latDegreesPerMeter = 1.0 / (earthRadiusMeters * PI / 180.0)
        val lonDegreesPerMeter = 1.0 / (earthRadiusMeters * PI / 180.0 * cos(centerLat * PI / 180.0))
        
        val latRadius = radiusMeters * latDegreesPerMeter
        val lonRadius = radiusMeters * lonDegreesPerMeter
        
        // Calculate approximate grid step size for the given precision
        val gridSize = getApproximateGridSize(precision)
        val latStep = gridSize.latDegrees
        val lonStep = gridSize.lonDegrees
        
        // Generate grid of points covering the area
        val minLat = centerLat - latRadius
        val maxLat = centerLat + latRadius
        val minLon = centerLon - lonRadius
        val maxLon = centerLon + lonRadius
        
        var lat = minLat
        while (lat <= maxLat) {
            var lon = minLon
            while (lon <= maxLon) {
                // Check if this point is within the radius
                if (distanceMeters(centerLat, centerLon, lat, lon) <= radiusMeters) {
                    val geohash = encode(lat, lon, precision)
                    geohashes.add(geohash)
                }
                lon += lonStep
            }
            lat += latStep
        }
        
        // Always include the center point
        geohashes.add(encode(centerLat, centerLon, precision))
        
        return geohashes
    }
    
    /**
     * Get neighboring geohashes (8 directions)
     */
    fun getNeighbors(geohash: String): List<String> {
        val center = decode(geohash)
        val precision = geohash.length
        val gridSize = getApproximateGridSize(precision)
        
        val neighbors = mutableListOf<String>()
        
        // 8 directions: N, NE, E, SE, S, SW, W, NW
        val offsets = listOf(
            Pair(gridSize.latDegrees, 0.0),     // N
            Pair(gridSize.latDegrees, gridSize.lonDegrees),   // NE
            Pair(0.0, gridSize.lonDegrees),     // E
            Pair(-gridSize.latDegrees, gridSize.lonDegrees),  // SE
            Pair(-gridSize.latDegrees, 0.0),    // S
            Pair(-gridSize.latDegrees, -gridSize.lonDegrees), // SW
            Pair(0.0, -gridSize.lonDegrees),    // W
            Pair(gridSize.latDegrees, -gridSize.lonDegrees)   // NW
        )
        
        for ((latOffset, lonOffset) in offsets) {
            val neighborLat = center.latitude + latOffset
            val neighborLon = center.longitude + lonOffset
            
            // Ensure coordinates are within valid bounds
            if (neighborLat in -90.0..90.0 && neighborLon in -180.0..180.0) {
                neighbors.add(encode(neighborLat, neighborLon, precision))
            }
        }
        
        return neighbors
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Get approximate grid size for a given precision
     */
    private fun getApproximateGridSize(precision: Int): GridSize {
        // More accurate cell dimensions in degrees for different precisions
        // Based on geohash bit allocation: longitude gets more bits on even positions
        return when (precision) {
            1 -> GridSize(45.0, 45.0)                    // ~5000km
            2 -> GridSize(11.25, 5.625)                  // ~1250km x 625km  
            3 -> GridSize(1.40625, 1.40625)              // ~156km
            4 -> GridSize(0.3515625, 0.17578125)         // ~39km x 19.5km
            5 -> GridSize(0.0439453125, 0.0439453125)    // ~4.9km (2.4km radius)
            6 -> GridSize(0.010986328125, 0.0054931640625) // ~1.2km x 0.6km
            7 -> GridSize(0.001373291015625, 0.001373291015625) // ~153m
            8 -> GridSize(0.000343322753906, 0.000171661376953)  // ~38m x 19m
            9 -> GridSize(0.000042915344238, 0.000042915344238)  // ~4.8m
            else -> GridSize(0.00001, 0.00001) // Fallback for higher precisions
        }
    }
    
    /**
     * Data classes
     */
    data class LatLng(
        val latitude: Double,
        val longitude: Double
    )
    
    private data class GridSize(
        val latDegrees: Double,
        val lonDegrees: Double
    )
}
