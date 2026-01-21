package com.artboard.data.model

import java.util.UUID

/**
 * Represents a complete brush stroke
 */
data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Point>,
    val brush: Brush,
    val color: Int,               // ARGB color
    val layerId: String           // Which layer this stroke belongs to
) {
    /**
     * Get bounding box of the stroke
     */
    fun getBounds(): Rect {
        if (points.isEmpty()) {
            return Rect(0f, 0f, 0f, 0f)
        }
        
        val maxRadius = brush.size / 2f
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        
        points.forEach { point ->
            val radius = brush.getEffectiveSize(point.pressure) / 2f
            minX = minOf(minX, point.x - radius)
            minY = minOf(minY, point.y - radius)
            maxX = maxOf(maxX, point.x + radius)
            maxY = maxOf(maxY, point.y + radius)
        }
        
        return Rect(minX, minY, maxX, maxY)
    }
    
    /**
     * Calculate approximate length of the stroke
     */
    fun getLength(): Float {
        var length = 0f
        for (i in 1 until points.size) {
            length += points[i - 1].distanceTo(points[i])
        }
        return length
    }
}

/**
 * Simple rectangle for bounds calculation
 */
data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    
    fun isEmpty(): Boolean = width <= 0 || height <= 0
    
    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }
    
    fun intersects(other: Rect): Boolean {
        return left < other.right && right > other.left &&
               top < other.bottom && bottom > other.top
    }
    
    fun expand(amount: Float): Rect {
        return Rect(
            left - amount,
            top - amount,
            right + amount,
            bottom + amount
        )
    }
}
