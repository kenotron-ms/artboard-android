package com.artboard.ui.gestures

import android.view.MotionEvent

/**
 * Detects multi-finger tap gestures (2, 3, and 4 finger taps)
 * Used for undo (2), redo (3), and UI toggle (4)
 * 
 * Based on GESTURE_SYSTEM.md specification
 */
class MultiFingerDetector {
    private val fingerDownTimes = mutableMapOf<Int, Long>()
    private var firstFingerDownTime = 0L
    private var maxFingerCount = 0
    private val tapStartPositions = mutableMapOf<Int, Pair<Float, Float>>()
    
    companion object {
        private const val TAP_TIMEOUT_MS = 200L // Max duration for a tap
        private const val TAP_SLOP = 20f // Max movement in pixels
        private const val FINGER_WINDOW_MS = 100L // Window to add more fingers
    }
    
    /**
     * Process a motion event and detect multi-finger taps
     */
    fun detectTap(event: MotionEvent): TapGesture {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // First finger down - reset state
                fingerDownTimes.clear()
                tapStartPositions.clear()
                firstFingerDownTime = event.eventTime
                maxFingerCount = 1
                
                val pointerId = event.getPointerId(0)
                fingerDownTimes[pointerId] = event.eventTime
                tapStartPositions[pointerId] = Pair(event.x, event.y)
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Additional finger down
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val currentTime = event.eventTime
                
                // Check if within finger addition window
                if (currentTime - firstFingerDownTime > FINGER_WINDOW_MS) {
                    // Too slow, not a tap gesture
                    return TapGesture.None
                }
                
                fingerDownTimes[pointerId] = currentTime
                tapStartPositions[pointerId] = Pair(
                    event.getX(pointerIndex),
                    event.getY(pointerIndex)
                )
                
                maxFingerCount = maxOf(maxFingerCount, event.pointerCount)
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Check if any finger moved too much
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val startPos = tapStartPositions[pointerId] ?: continue
                    
                    val dx = event.getX(i) - startPos.first
                    val dy = event.getY(i) - startPos.second
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    
                    if (distance > TAP_SLOP) {
                        // Moved too much, not a tap
                        return TapGesture.None
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                fingerDownTimes.remove(pointerId)
                
                // Check if all fingers are up
                if (fingerDownTimes.isEmpty()) {
                    return detectTapCompletion(event)
                }
            }
            
            MotionEvent.ACTION_CANCEL -> {
                // Gesture cancelled
                fingerDownTimes.clear()
                tapStartPositions.clear()
                return TapGesture.None
            }
        }
        
        return TapGesture.None
    }
    
    /**
     * Detect if a tap gesture was completed when all fingers are released
     */
    private fun detectTapCompletion(event: MotionEvent): TapGesture {
        val duration = event.eventTime - firstFingerDownTime
        
        // Must be quick (< 200ms) to be a tap
        if (duration > TAP_TIMEOUT_MS) {
            return TapGesture.None
        }
        
        // Check what type of tap based on max finger count
        return when (maxFingerCount) {
            2 -> TapGesture.TwoFinger
            3 -> TapGesture.ThreeFinger
            4 -> TapGesture.FourFinger(maxFingerCount)
            5 -> TapGesture.FourFinger(maxFingerCount) // 5+ treated as 4
            else -> TapGesture.None
        }
    }
    
    /**
     * Reset the detector state
     */
    fun reset() {
        fingerDownTimes.clear()
        tapStartPositions.clear()
        maxFingerCount = 0
        firstFingerDownTime = 0L
    }
}

/**
 * Sealed class representing detected tap gestures
 */
sealed class TapGesture {
    object None : TapGesture()
    object TwoFinger : TapGesture()
    object ThreeFinger : TapGesture()
    data class FourFinger(val fingerCount: Int) : TapGesture()
}
