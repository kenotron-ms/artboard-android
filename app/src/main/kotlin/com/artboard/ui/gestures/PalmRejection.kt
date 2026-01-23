package com.artboard.ui.gestures

import android.view.MotionEvent

/**
 * Palm rejection algorithm to ignore palm touches while using stylus
 * 
 * Features:
 * - Detects when stylus is active
 * - Rejects large, low-pressure touches (palm characteristics)
 * - Tracks rejected touches to ignore subsequent events
 * - Automatically clears when stylus is lifted
 * 
 * Algorithm based on GESTURE_SYSTEM.md specification (AC7)
 * Palm characteristics:
 * - Large touch area (size > 0.3)
 * - Lower pressure (pressure < 0.3)
 * - Appears while stylus is already down
 */
class PalmRejection(
    private val palmSizeThreshold: Float = 0.3f,
    private val palmPressureThreshold: Float = 0.3f,
    private val enabled: Boolean = true
) {
    private var stylusPointerId: Int? = null
    private val rejectedTouches = mutableSetOf<Int>()
    
    /**
     * Check if a touch event should be rejected as palm touch
     */
    fun shouldReject(event: MotionEvent): Boolean {
        if (!enabled) return false
        
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val toolType = event.getToolType(pointerIndex)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Track stylus input
                if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                    stylusPointerId = pointerId
                    return false // Never reject stylus
                }
                
                // If stylus is active, check if this is a palm
                if (stylusPointerId != null && toolType == MotionEvent.TOOL_TYPE_FINGER) {
                    if (isPalmLikely(event, pointerIndex)) {
                        rejectedTouches.add(pointerId)
                        return true
                    }
                }
                
                // Check if this pointer was previously rejected
                return pointerId in rejectedTouches
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Continue rejecting previously rejected touches
                if (pointerId in rejectedTouches) {
                    return true
                }
                
                // Check all active pointers for palm characteristics
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    if (pid in rejectedTouches) {
                        continue
                    }
                    
                    val tool = event.getToolType(i)
                    if (stylusPointerId != null && tool == MotionEvent.TOOL_TYPE_FINGER) {
                        if (isPalmLikely(event, i)) {
                            rejectedTouches.add(pid)
                            return true
                        }
                    }
                }
            }
            
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerId = event.getPointerId(pointerIndex)
                
                // If stylus is lifted, clear rejection state
                if (upPointerId == stylusPointerId) {
                    stylusPointerId = null
                    rejectedTouches.clear()
                }
                
                // Remove this pointer from rejected set
                rejectedTouches.remove(upPointerId)
                
                // Check if the up event is for a rejected touch
                return upPointerId in rejectedTouches
            }
            
            MotionEvent.ACTION_CANCEL -> {
                // Clear all rejection state on cancel
                stylusPointerId = null
                rejectedTouches.clear()
            }
        }
        
        return false
    }
    
    /**
     * Determine if a touch is likely from palm based on size and pressure
     */
    private fun isPalmLikely(event: MotionEvent, pointerIndex: Int): Boolean {
        // Must have stylus active
        if (stylusPointerId == null) {
            return false
        }
        
        val size = event.getSize(pointerIndex)
        val pressure = event.getPressure(pointerIndex)
        
        // Palm characteristics from spec:
        // - Large touch area (size > 0.3)
        // - Lower pressure (pressure < 0.3)
        // - Appears while stylus is already down
        
        return size > palmSizeThreshold && pressure < palmPressureThreshold
    }
    
    /**
     * Check if stylus is currently active
     */
    fun isStylusActive(): Boolean = stylusPointerId != null
    
    /**
     * Check if a specific pointer is rejected
     */
    fun isPointerRejected(pointerId: Int): Boolean = pointerId in rejectedTouches
    
    /**
     * Get count of currently rejected touches
     */
    fun getRejectedCount(): Int = rejectedTouches.size
    
    /**
     * Manually reset rejection state (useful for testing)
     */
    fun reset() {
        stylusPointerId = null
        rejectedTouches.clear()
    }
    
    /**
     * Force enable/disable palm rejection
     */
    companion object {
        fun createDisabled() = PalmRejection(enabled = false)
    }
}

/**
 * Palm rejection result for diagnostic purposes
 */
data class PalmRejectionInfo(
    val isRejected: Boolean,
    val reason: String,
    val size: Float,
    val pressure: Float,
    val stylusActive: Boolean
)
