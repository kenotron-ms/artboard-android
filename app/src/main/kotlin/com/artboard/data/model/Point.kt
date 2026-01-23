package com.artboard.data.model

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Represents a single point in a stroke with full stylus input data
 * including position, pressure, tilt, and timing
 */
data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float,      // 0.0-1.0
    val tiltX: Float = 0f,    // -90 to +90 degrees (left/right tilt)
    val tiltY: Float = 0f,    // -90 to +90 degrees (up/down tilt)
    val orientation: Float = 0f, // 0-360 degrees (stylus rotation)
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Calculate distance to another point
     */
    fun distanceTo(other: Point): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Calculate velocity based on distance and time delta
     */
    fun velocityTo(other: Point): Float {
        val distance = distanceTo(other)
        val timeDelta = (other.timestamp - timestamp).toFloat()
        return if (timeDelta > 0) distance / timeDelta else 0f
    }
    
    /**
     * Calculate total tilt magnitude from perpendicular
     * Returns angle in degrees (0 = perpendicular, 90 = flat)
     */
    fun tiltMagnitude(): Float {
        return sqrt(tiltX * tiltX + tiltY * tiltY)
    }
    
    /**
     * Calculate tilt direction as angle
     * Returns angle in degrees (0-360)
     */
    fun tiltDirection(): Float {
        val angleRad = atan2(tiltY, tiltX)
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
        return (angleDeg + 360f) % 360f
    }
    
    /**
     * Check if stylus has tilt data
     */
    fun hasTilt(): Boolean {
        return tiltX != 0f || tiltY != 0f
    }
}
