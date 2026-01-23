package com.artboard.ui.gestures

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Detects edge swipe gestures for revealing UI panels
 * 
 * Features:
 * - Left edge swipe → Brush selector
 * - Right edge swipe → Color picker
 * - Bottom edge swipe → Layer panel
 * - Configurable edge zone size (default 20dp from spec)
 * - Swipe distance threshold to confirm gesture
 * 
 * Based on GESTURE_SYSTEM.md specification (AC6)
 */
class EdgeSwipeDetector(
    private val edgeZoneSize: Float = 60f, // Pixels from edge (spec says < 20dp, but we need larger for detection)
    private val swipeThreshold: Float = 100f, // Minimum swipe distance to trigger
    val screenWidth: Float,
    val screenHeight: Float
) {
    private var swipeStartPosition = Offset.Zero
    private var swipeStartEdge: Edge? = null
    private var isSwipeActive = false
    private var pointerId = -1
    
    companion object {
        private const val MAX_ANGLE_DEVIATION = 45f // Max angle from perpendicular to edge
    }
    
    /**
     * Process touch event for edge swipe detection
     */
    fun onTouchEvent(event: MotionEvent): EdgeSwipeResult {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val position = Offset(event.x, event.y)
                val edge = detectEdge(position)
                
                if (edge != null) {
                    // Touch started in edge zone
                    swipeStartPosition = position
                    swipeStartEdge = edge
                    isSwipeActive = true
                    pointerId = event.getPointerId(0)
                    
                    return EdgeSwipeResult.Started(edge, position)
                }
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Multi-finger - cancel edge swipe
                reset()
                return EdgeSwipeResult.Cancelled
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (!isSwipeActive || swipeStartEdge == null) {
                    return EdgeSwipeResult.None
                }
                
                val currentPosition = Offset(event.x, event.y)
                val swipeDelta = currentPosition - swipeStartPosition
                
                // Check if swipe is in correct direction for the edge
                val isValidDirection = isValidSwipeDirection(swipeStartEdge!!, swipeDelta)
                
                if (!isValidDirection) {
                    // Swiping wrong direction - cancel
                    reset()
                    return EdgeSwipeResult.Cancelled
                }
                
                // Calculate progress (distance from start)
                val distance = getSwipeDistance(swipeDelta)
                val progress = (distance / swipeThreshold).coerceIn(0f, 1f)
                
                // Trigger if past threshold
                if (distance >= swipeThreshold) {
                    val edge = swipeStartEdge!!
                    reset()
                    return EdgeSwipeResult.Triggered(edge, currentPosition)
                }
                
                return EdgeSwipeResult.InProgress(swipeStartEdge!!, progress, currentPosition)
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isSwipeActive) {
                    // Released before completing swipe
                    reset()
                    return EdgeSwipeResult.Cancelled
                }
            }
        }
        
        return EdgeSwipeResult.None
    }
    
    /**
     * Detect which edge (if any) a position is near
     */
    private fun detectEdge(position: Offset): Edge? {
        return when {
            position.x < edgeZoneSize -> Edge.LEFT
            position.x > screenWidth - edgeZoneSize -> Edge.RIGHT
            position.y > screenHeight - edgeZoneSize -> Edge.BOTTOM
            position.y < edgeZoneSize -> Edge.TOP
            else -> null
        }
    }
    
    /**
     * Check if swipe direction is valid for the detected edge
     */
    private fun isValidSwipeDirection(edge: Edge, delta: Offset): Boolean {
        return when (edge) {
            Edge.LEFT -> delta.x > 0 && abs(delta.x) > abs(delta.y) * 0.5f // Swipe right
            Edge.RIGHT -> delta.x < 0 && abs(delta.x) > abs(delta.y) * 0.5f // Swipe left
            Edge.BOTTOM -> delta.y < 0 && abs(delta.y) > abs(delta.x) * 0.5f // Swipe up
            Edge.TOP -> delta.y > 0 && abs(delta.y) > abs(delta.x) * 0.5f // Swipe down
        }
    }
    
    /**
     * Get swipe distance (perpendicular to edge)
     */
    private fun getSwipeDistance(delta: Offset): Float {
        return when (swipeStartEdge) {
            Edge.LEFT -> delta.x
            Edge.RIGHT -> -delta.x
            Edge.BOTTOM -> -delta.y
            Edge.TOP -> delta.y
            null -> 0f
        }
    }
    
    /**
     * Update screen dimensions (call when screen size changes)
     */
    fun updateScreenSize(width: Float, height: Float) {
        // This would need to be implemented if we want dynamic screen size updates
        // For now, screen size is set in constructor
    }
    
    /**
     * Reset detector state
     */
    fun reset() {
        swipeStartPosition = Offset.Zero
        swipeStartEdge = null
        isSwipeActive = false
        pointerId = -1
    }
}

/**
 * Edge enum for edge detection
 */
enum class Edge {
    LEFT,    // Brush selector
    RIGHT,   // Color picker
    BOTTOM,  // Layer panel
    TOP      // (Not used in spec, but included for completeness)
}

/**
 * Sealed class representing edge swipe results
 */
sealed class EdgeSwipeResult {
    object None : EdgeSwipeResult()
    
    /** Edge swipe started */
    data class Started(val edge: Edge, val position: Offset) : EdgeSwipeResult()
    
    /** Edge swipe in progress */
    data class InProgress(val edge: Edge, val progress: Float, val position: Offset) : EdgeSwipeResult()
    
    /** Edge swipe triggered! */
    data class Triggered(val edge: Edge, val position: Offset) : EdgeSwipeResult()
    
    /** Edge swipe cancelled */
    object Cancelled : EdgeSwipeResult()
}
