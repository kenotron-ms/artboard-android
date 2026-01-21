package com.artboard.data.model

/**
 * Represents a single point in a stroke with pressure information
 */
data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float,      // 0.0-1.0
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Calculate distance to another point
     */
    fun distanceTo(other: Point): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Calculate velocity based on distance and time delta
     */
    fun velocityTo(other: Point): Float {
        val distance = distanceTo(other)
        val timeDelta = (other.timestamp - timestamp).toFloat()
        return if (timeDelta > 0) distance / timeDelta else 0f
    }
}
