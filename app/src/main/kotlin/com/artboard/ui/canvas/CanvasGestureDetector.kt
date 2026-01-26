package com.artboard.ui.canvas

import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Detects two-finger gestures for canvas navigation: pinch-zoom, pan, rotate.
 * Supports simultaneous gestures like Procreate - all three can happen at once.
 */
class CanvasGestureDetector(
    private val listener: GestureListener
) {
    
    interface GestureListener {
        /** Called when zoom gesture is detected. scaleFactor is relative (1.0 = no change) */
        fun onZoom(scaleFactor: Float, focusX: Float, focusY: Float)
        
        /** Called when pan gesture is detected */
        fun onPan(dx: Float, dy: Float)
        
        /** Called when rotate gesture is detected. degrees is delta */
        fun onRotate(degrees: Float, focusX: Float, focusY: Float)
        
        /** Called when two-finger tap detected (for undo) */
        fun onTwoFingerTap()
        
        /** Called when gesture ends - good time to snap rotation */
        fun onGestureEnd()
        
        /** Called when gesture begins - can cancel current drawing */
        fun onGestureBegin()
    }
    
    // Gesture state
    private var isGestureActive = false
    private var gestureStartTime = 0L
    
    // Initial two-finger positions
    private var initialDistance = 0f
    private var initialAngle = 0f
    private var initialMidpoint = PointF()
    
    // Previous frame positions (for deltas)
    private var prevDistance = 0f
    private var prevAngle = 0f
    private var prevMidpoint = PointF()
    
    // Movement tracking for tap detection
    private var totalMovement = 0f
    
    // Thresholds
    companion object {
        private const val TAP_TIMEOUT_MS = 200L
        private const val TAP_MAX_MOVEMENT = 30f
        private const val MIN_ZOOM_DELTA = 0.01f
        private const val MIN_ROTATION_DELTA = 0.5f
        private const val MIN_PAN_DELTA = 1f
    }
    
    /**
     * Process touch events. Returns true if the event was consumed by a gesture.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    startGesture(event)
                    return true
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isGestureActive && event.pointerCount >= 2) {
                    processGesture(event)
                    return true
                }
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                if (isGestureActive) {
                    endGesture()
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isGestureActive) {
                    endGesture()
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun startGesture(event: MotionEvent) {
        isGestureActive = true
        gestureStartTime = System.currentTimeMillis()
        totalMovement = 0f
        
        val p0 = PointF(event.getX(0), event.getY(0))
        val p1 = PointF(event.getX(1), event.getY(1))
        
        initialDistance = distance(p0, p1)
        initialAngle = angle(p0, p1)
        initialMidpoint = midpoint(p0, p1)
        
        prevDistance = initialDistance
        prevAngle = initialAngle
        prevMidpoint.set(initialMidpoint.x, initialMidpoint.y)
        
        listener.onGestureBegin()
    }
    
    private fun processGesture(event: MotionEvent) {
        if (event.pointerCount < 2) return
        
        val p0 = PointF(event.getX(0), event.getY(0))
        val p1 = PointF(event.getX(1), event.getY(1))
        
        val currentDistance = distance(p0, p1)
        val currentAngle = angle(p0, p1)
        val currentMidpoint = midpoint(p0, p1)
        
        // Track total movement for tap detection
        totalMovement += distance(currentMidpoint, prevMidpoint)
        
        // Calculate deltas from previous frame (for smooth, incremental updates)
        val scaleFactor = if (prevDistance > 0) currentDistance / prevDistance else 1f
        var angleDelta = currentAngle - prevAngle
        val panDx = currentMidpoint.x - prevMidpoint.x
        val panDy = currentMidpoint.y - prevMidpoint.y
        
        // Handle angle wraparound (-180 to 180)
        if (angleDelta > 180) angleDelta -= 360
        if (angleDelta < -180) angleDelta += 360
        
        // Apply all gestures simultaneously (like Procreate)
        // This allows natural pinch-zoom-rotate in one fluid motion
        
        val focusX = currentMidpoint.x
        val focusY = currentMidpoint.y
        
        // Zoom
        if (abs(scaleFactor - 1f) > MIN_ZOOM_DELTA) {
            listener.onZoom(scaleFactor, focusX, focusY)
        }
        
        // Rotate
        if (abs(angleDelta) > MIN_ROTATION_DELTA) {
            listener.onRotate(angleDelta, focusX, focusY)
        }
        
        // Pan
        if (abs(panDx) > MIN_PAN_DELTA || abs(panDy) > MIN_PAN_DELTA) {
            listener.onPan(panDx, panDy)
        }
        
        // Update previous values for next frame
        prevDistance = currentDistance
        prevAngle = currentAngle
        prevMidpoint.set(currentMidpoint.x, currentMidpoint.y)
    }
    
    private fun endGesture() {
        val duration = System.currentTimeMillis() - gestureStartTime
        
        // Check for two-finger tap (quick touch with minimal movement)
        if (duration < TAP_TIMEOUT_MS && totalMovement < TAP_MAX_MOVEMENT) {
            listener.onTwoFingerTap()
        }
        
        listener.onGestureEnd()
        isGestureActive = false
    }
    
    /**
     * Check if a gesture is currently active
     */
    fun isGestureInProgress(): Boolean = isGestureActive
    
    // Utility functions
    
    private fun distance(p0: PointF, p1: PointF): Float {
        val dx = p1.x - p0.x
        val dy = p1.y - p0.y
        return sqrt(dx * dx + dy * dy)
    }
    
    private fun angle(p0: PointF, p1: PointF): Float {
        return Math.toDegrees(
            atan2((p1.y - p0.y).toDouble(), (p1.x - p0.x).toDouble())
        ).toFloat()
    }
    
    private fun midpoint(p0: PointF, p1: PointF): PointF {
        return PointF(
            (p0.x + p1.x) / 2f,
            (p0.y + p1.y) / 2f
        )
    }
}
