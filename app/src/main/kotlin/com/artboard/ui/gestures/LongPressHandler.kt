package com.artboard.ui.gestures

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/**
 * Handles long-press gesture for eyedropper color picking
 * 
 * Features:
 * - Detects long-press (1 second default)
 * - Provides progress feedback for visual indicator
 * - Minimal movement threshold to avoid accidental cancellation
 * - Single finger only (multi-finger cancels)
 * 
 * Based on GESTURE_SYSTEM.md specification (AC5)
 */
class LongPressHandler(
    private val longPressDuration: Long = 1000L, // 1 second default (spec says 1s, but AC5 says 1s)
    private val maxMovement: Float = 10f // 10dp tolerance
) {
    private var pressStartTime = 0L
    private var pressStartPosition = Offset.Zero
    private var isLongPressTriggered = false
    private var pointerId = -1
    
    /**
     * Process touch event for long-press detection
     */
    fun onTouch(event: MotionEvent): LongPressResult {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Single finger down - start tracking
                if (event.pointerCount == 1) {
                    pressStartTime = event.eventTime
                    pressStartPosition = Offset(event.x, event.y)
                    isLongPressTriggered = false
                    pointerId = event.getPointerId(0)
                    
                    return LongPressResult.Started(pressStartPosition)
                }
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Additional finger down - cancel long-press
                reset()
                return LongPressResult.Cancelled
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (pointerId == -1) return LongPressResult.None
                
                val currentPosition = Offset(event.x, event.y)
                val duration = event.eventTime - pressStartTime
                val movement = calculateDistance(pressStartPosition, currentPosition)
                
                // Too much movement - cancel
                if (movement > maxMovement) {
                    reset()
                    return LongPressResult.Cancelled
                }
                
                // Long-press triggered!
                if (duration >= longPressDuration && !isLongPressTriggered) {
                    isLongPressTriggered = true
                    return LongPressResult.Triggered(currentPosition)
                }
                
                // Still in progress - return progress for visual feedback
                if (duration < longPressDuration && movement <= maxMovement) {
                    val progress = duration.toFloat() / longPressDuration
                    return LongPressResult.InProgress(progress, currentPosition)
                }
                
                // Long-press already triggered, track movement for eyedropper
                if (isLongPressTriggered) {
                    return LongPressResult.Moving(currentPosition)
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isLongPressTriggered) {
                    // Long-press was triggered, now released
                    val finalPosition = Offset(event.x, event.y)
                    reset()
                    return LongPressResult.Released(finalPosition)
                } else {
                    // Released before long-press completed
                    reset()
                    return LongPressResult.Cancelled
                }
            }
            
            MotionEvent.ACTION_CANCEL -> {
                reset()
                return LongPressResult.Cancelled
            }
        }
        
        return LongPressResult.None
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(p1: Offset, p2: Offset): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Check if long-press is currently active
     */
    fun isActive(): Boolean = isLongPressTriggered
    
    /**
     * Reset the handler state
     */
    fun reset() {
        pressStartTime = 0L
        pressStartPosition = Offset.Zero
        isLongPressTriggered = false
        pointerId = -1
    }
}

/**
 * Sealed class representing long-press states
 */
sealed class LongPressResult {
    object None : LongPressResult()
    
    /** Long-press tracking started */
    data class Started(val position: Offset) : LongPressResult()
    
    /** Long-press in progress (0.0 to 1.0) */
    data class InProgress(val progress: Float, val position: Offset) : LongPressResult()
    
    /** Long-press triggered! Eyedropper should activate */
    data class Triggered(val position: Offset) : LongPressResult()
    
    /** Moving after long-press triggered (for eyedropper) */
    data class Moving(val position: Offset) : LongPressResult()
    
    /** Long-press released */
    data class Released(val position: Offset) : LongPressResult()
    
    /** Long-press cancelled (movement or additional finger) */
    object Cancelled : LongPressResult()
}
