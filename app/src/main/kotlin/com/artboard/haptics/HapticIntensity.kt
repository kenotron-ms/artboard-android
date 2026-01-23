package com.artboard.haptics

import android.view.HapticFeedbackConstants

/**
 * Haptic intensity levels for different interaction types
 * 
 * Based on HAPTIC_FEEDBACK.md specification
 */
enum class HapticIntensity {
    /**
     * Subtle tick - For slider increments, minor actions, zoom snap points
     * Duration: ~5-10ms
     */
    LIGHT,
    
    /**
     * Standard feedback - For button taps, selections, gesture recognition
     * Duration: ~15-20ms
     */
    MEDIUM,
    
    /**
     * Strong feedback - For mode changes, important actions, destructive warnings
     * Duration: ~30-50ms
     */
    HEAVY,
    
    /**
     * Positive confirmation - For successful saves, exports, operations
     * Pattern: Quick success pattern
     */
    SUCCESS,
    
    /**
     * Alert/Warning - For destructive actions, errors, important alerts
     * Pattern: Attention-grabbing pattern
     */
    WARNING;
    
    /**
     * Map to Android HapticFeedbackConstants for View-based haptics
     */
    fun toHapticFeedbackConstant(): Int {
        return when (this) {
            LIGHT -> HapticFeedbackConstants.CLOCK_TICK
            MEDIUM -> HapticFeedbackConstants.KEYBOARD_TAP
            HEAVY -> HapticFeedbackConstants.LONG_PRESS
            SUCCESS -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            WARNING -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
        }
    }
    
    /**
     * Get vibration duration (ms) for VibrationEffect on older APIs
     */
    fun getDuration(): Long {
        return when (this) {
            LIGHT -> 10L
            MEDIUM -> 20L
            HEAVY -> 40L
            SUCCESS -> 25L
            WARNING -> 50L
        }
    }
    
    /**
     * Get vibration amplitude (0-255) for VibrationEffect
     */
    fun getAmplitude(): Int {
        return when (this) {
            LIGHT -> 80
            MEDIUM -> 150
            HEAVY -> 255
            SUCCESS -> 180
            WARNING -> 255
        }
    }
}

/**
 * Haptic patterns for special feedback scenarios
 */
object HapticPatterns {
    /**
     * Success pattern - Quick positive confirmation
     * Pattern: Short-pause-short (ascending)
     */
    val SUCCESS = longArrayOf(0, 15, 10, 10) to intArrayOf(0, 120, 0, 180)
    
    /**
     * Warning pattern - Single strong pulse
     * Pattern: Immediate strong vibration
     */
    val WARNING = longArrayOf(0, 50) to intArrayOf(0, 255)
    
    /**
     * Tick pattern - Minimal feedback for continuous actions
     * Pattern: Ultra-brief tick
     */
    val TICK = longArrayOf(0, 8) to intArrayOf(0, 100)
    
    /**
     * Snap pattern - Magnetic snap point feedback
     * Pattern: Quick double tick
     */
    val SNAP = longArrayOf(0, 10, 10, 10) to intArrayOf(0, 120, 0, 120)
    
    /**
     * Mode change pattern - Noticeable transition
     * Pattern: Medium-pause-medium
     */
    val MODE_CHANGE = longArrayOf(0, 20, 30, 20) to intArrayOf(0, 180, 0, 180)
}
