package com.artboard.ui.gestures

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Manages haptic feedback for gesture interactions
 * 
 * Features:
 * - Different intensity levels (light, medium, heavy)
 * - Pattern-based feedback for special gestures
 * - Respects user preferences
 * - Android version compatibility
 * 
 * Based on GESTURE_SYSTEM.md specification (AC4, AC9)
 */
class HapticFeedbackManager(
    private val context: Context,
    private val preferences: GesturePreferences
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Perform haptic feedback with specified intensity
     */
    fun perform(intensity: HapticIntensity) {
        if (!preferences.hapticFeedbackEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use predefined effects
            val effect = when (intensity) {
                HapticIntensity.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticIntensity.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticIntensity.HEAVY -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                HapticIntensity.DOUBLE_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            }
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8+ - Use VibrationEffect
            val duration = when (intensity) {
                HapticIntensity.LIGHT -> 10L
                HapticIntensity.MEDIUM -> 20L
                HapticIntensity.HEAVY -> 40L
                HapticIntensity.DOUBLE_CLICK -> 30L
            }
            val amplitude = when (intensity) {
                HapticIntensity.LIGHT -> 100
                HapticIntensity.MEDIUM -> 150
                HapticIntensity.HEAVY -> 255
                HapticIntensity.DOUBLE_CLICK -> 150
            }
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            // Android 7 and below - Use deprecated vibrate
            val duration = when (intensity) {
                HapticIntensity.LIGHT -> 10L
                HapticIntensity.MEDIUM -> 20L
                HapticIntensity.HEAVY -> 40L
                HapticIntensity.DOUBLE_CLICK -> 30L
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
    
    /**
     * Perform haptic feedback for gesture completion
     */
    fun performGestureComplete(gestureType: GestureType) {
        if (!preferences.hapticFeedbackEnabled) return
        
        val intensity = when (gestureType) {
            GestureType.TWO_FINGER_UNDO -> HapticIntensity.MEDIUM
            GestureType.THREE_FINGER_REDO -> HapticIntensity.MEDIUM
            GestureType.FOUR_FINGER_UI_TOGGLE -> HapticIntensity.HEAVY
            GestureType.PINCH_ZOOM -> HapticIntensity.LIGHT
            GestureType.TWO_FINGER_PAN -> HapticIntensity.LIGHT
            GestureType.LONG_PRESS_EYEDROPPER -> HapticIntensity.MEDIUM
            GestureType.EDGE_SWIPE_LEFT,
            GestureType.EDGE_SWIPE_RIGHT,
            GestureType.EDGE_SWIPE_BOTTOM -> HapticIntensity.MEDIUM
        }
        
        perform(intensity)
    }
    
    /**
     * Perform snap point haptic (for zoom snap points)
     */
    fun performSnapPoint() {
        if (!preferences.hapticFeedbackEnabled) return
        perform(HapticIntensity.LIGHT)
    }
    
    /**
     * Perform view-based haptic feedback (for Compose integration)
     */
    fun performViewHaptic(view: View, hapticConstant: Int = HapticFeedbackConstants.VIRTUAL_KEY) {
        if (!preferences.hapticFeedbackEnabled) return
        view.performHapticFeedback(hapticConstant)
    }
    
    /**
     * Custom pattern-based haptic feedback
     */
    fun performPattern(pattern: LongArray, amplitudes: IntArray) {
        if (!preferences.hapticFeedbackEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
    
    /**
     * Check if haptic feedback is available on this device
     */
    fun isHapticAvailable(): Boolean {
        return vibrator.hasVibrator()
    }
    
    /**
     * Cancel any ongoing haptic feedback
     */
    fun cancel() {
        vibrator.cancel()
    }
}

/**
 * Haptic intensity levels
 */
enum class HapticIntensity {
    LIGHT,       // Subtle tick (zoom snap points)
    MEDIUM,      // Standard click (undo/redo, edge swipes)
    HEAVY,       // Strong feedback (UI toggle)
    DOUBLE_CLICK // Special pattern
}

/**
 * Gesture types for haptic feedback mapping
 */
enum class GestureType {
    TWO_FINGER_UNDO,
    THREE_FINGER_REDO,
    FOUR_FINGER_UI_TOGGLE,
    PINCH_ZOOM,
    TWO_FINGER_PAN,
    LONG_PRESS_EYEDROPPER,
    EDGE_SWIPE_LEFT,
    EDGE_SWIPE_RIGHT,
    EDGE_SWIPE_BOTTOM
}

/**
 * Common haptic patterns for specific gestures
 */
object HapticPatterns {
    // Long-press progress pattern (subtle pulse)
    val LONG_PRESS_PROGRESS = longArrayOf(0, 10, 50, 10) to intArrayOf(0, 50, 0, 50)
    
    // Zoom snap pattern (quick tick)
    val ZOOM_SNAP = longArrayOf(0, 5) to intArrayOf(0, 100)
    
    // UI toggle pattern (double tap)
    val UI_TOGGLE = longArrayOf(0, 20, 20, 20) to intArrayOf(0, 200, 0, 200)
    
    // Gesture success pattern
    val SUCCESS = longArrayOf(0, 15, 10, 15) to intArrayOf(0, 150, 0, 100)
    
    // Gesture error pattern
    val ERROR = longArrayOf(0, 50) to intArrayOf(0, 255)
}
