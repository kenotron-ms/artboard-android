package com.artboard.ui.gestures

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/**
 * Handles pinch-to-zoom and two-finger pan gestures for canvas navigation
 * 
 * Features:
 * - Pinch zoom with scale factor calculation (10%-1000% range)
 * - Two-finger pan/drag for canvas movement
 * - Snap points at 25%, 50%, 100%, 200% with haptic feedback
 * - Smooth interpolation for natural feel
 * 
 * Based on GESTURE_SYSTEM.md specification
 */
class ZoomPanHandler(
    private val onZoom: (Float, Offset) -> Unit, // (scaleFactor, focusPoint)
    private val onPan: (Offset) -> Unit,
    private val onZoomSnapPoint: (() -> Unit)? = null // Haptic callback for snap points
) {
    private var initialSpan = 0f
    private var previousSpan = 0f
    private var currentScale = 1f
    
    private var previousPanPosition = Offset.Zero
    private var isPanning = false
    private var isZooming = false
    
    private var pointer1Id = -1
    private var pointer2Id = -1
    
    companion object {
        // Zoom limits
        const val MIN_ZOOM = 0.1f  // 10%
        const val MAX_ZOOM = 10.0f  // 1000%
        
        // Snap points for zoom (with haptic feedback)
        val SNAP_POINTS = listOf(0.25f, 0.5f, 1.0f, 2.0f)
        const val SNAP_THRESHOLD = 0.05f // 5% threshold for snapping
        
        // Movement threshold to distinguish tap from pan
        const val MOVEMENT_THRESHOLD = 10f
    }
    
    /**
     * Process touch event for zoom/pan detection
     */
    fun onTouchEvent(event: MotionEvent): GestureState {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointer1Id = event.getPointerId(0)
                return GestureState.Idle
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    // Second finger down - start zoom/pan gesture
                    pointer2Id = event.getPointerId(event.actionIndex)
                    
                    initialSpan = calculateSpan(event)
                    previousSpan = initialSpan
                    previousPanPosition = calculateMidpoint(event)
                    
                    isPanning = false
                    isZooming = false
                    
                    return GestureState.GestureStarted
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2 && pointer1Id != -1 && pointer2Id != -1) {
                    val currentSpan = calculateSpan(event)
                    val currentMidpoint = calculateMidpoint(event)
                    
                    // Determine if this is zoom or pan based on span change
                    val spanDelta = currentSpan - initialSpan
                    val panDelta = currentMidpoint - previousPanPosition
                    
                    if (!isZooming && !isPanning) {
                        // Detect which gesture is starting
                        if (kotlin.math.abs(spanDelta) > MOVEMENT_THRESHOLD) {
                            isZooming = true
                        } else if (panDelta.getDistance() > MOVEMENT_THRESHOLD) {
                            isPanning = true
                        }
                    }
                    
                    if (isZooming) {
                        // Calculate scale factor
                        val scaleFactor = currentSpan / previousSpan
                        val newScale = (currentScale * scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        
                        // Check for snap points
                        val snappedScale = snapToPoint(newScale)
                        if (snappedScale != newScale) {
                            onZoomSnapPoint?.invoke()
                        }
                        
                        currentScale = snappedScale
                        previousSpan = currentSpan
                        
                        onZoom(scaleFactor, currentMidpoint)
                        return GestureState.Zooming(currentScale)
                    } else if (isPanning) {
                        // Calculate pan offset
                        val panOffset = currentMidpoint - previousPanPosition
                        previousPanPosition = currentMidpoint
                        
                        onPan(panOffset)
                        return GestureState.Panning(panOffset)
                    }
                }
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                
                if (pointerId == pointer1Id || pointerId == pointer2Id) {
                    // One of our tracked pointers is up
                    pointer1Id = -1
                    pointer2Id = -1
                    isPanning = false
                    isZooming = false
                    
                    return GestureState.GestureEnded
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pointer1Id = -1
                pointer2Id = -1
                isPanning = false
                isZooming = false
                
                return GestureState.GestureEnded
            }
        }
        
        return GestureState.Idle
    }
    
    /**
     * Calculate distance between two pointers (span)
     */
    private fun calculateSpan(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        
        val index1 = event.findPointerIndex(pointer1Id)
        val index2 = event.findPointerIndex(pointer2Id)
        
        if (index1 == -1 || index2 == -1) return 0f
        
        val dx = event.getX(index1) - event.getX(index2)
        val dy = event.getY(index1) - event.getY(index2)
        
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Calculate midpoint between two pointers (focus point)
     */
    private fun calculateMidpoint(event: MotionEvent): Offset {
        if (event.pointerCount < 2) return Offset.Zero
        
        val index1 = event.findPointerIndex(pointer1Id)
        val index2 = event.findPointerIndex(pointer2Id)
        
        if (index1 == -1 || index2 == -1) return Offset.Zero
        
        val x = (event.getX(index1) + event.getX(index2)) / 2
        val y = (event.getY(index1) + event.getY(index2)) / 2
        
        return Offset(x, y)
    }
    
    /**
     * Snap zoom scale to predefined snap points
     */
    private fun snapToPoint(scale: Float): Float {
        for (snapPoint in SNAP_POINTS) {
            if (kotlin.math.abs(scale - snapPoint) < SNAP_THRESHOLD) {
                return snapPoint
            }
        }
        return scale
    }
    
    /**
     * Set current scale (useful for initialization)
     */
    fun setCurrentScale(scale: Float) {
        currentScale = scale.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }
    
    /**
     * Get current scale
     */
    fun getCurrentScale(): Float = currentScale
    
    /**
     * Reset handler state
     */
    fun reset() {
        initialSpan = 0f
        previousSpan = 0f
        previousPanPosition = Offset.Zero
        isPanning = false
        isZooming = false
        pointer1Id = -1
        pointer2Id = -1
    }
}

/**
 * Sealed class representing zoom/pan gesture states
 */
sealed class GestureState {
    object Idle : GestureState()
    object GestureStarted : GestureState()
    data class Zooming(val currentScale: Float) : GestureState()
    data class Panning(val offset: Offset) : GestureState()
    object GestureEnded : GestureState()
}
