package com.artboard.ui.gestures

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * User preferences for gesture system
 * Allows users to enable/disable individual gestures
 * 
 * Based on GESTURE_SYSTEM.md specification (AC10)
 */
class GesturePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "gesture_preferences"
        
        // Preference keys
        private const val KEY_TWO_FINGER_UNDO = "two_finger_undo_enabled"
        private const val KEY_THREE_FINGER_REDO = "three_finger_redo_enabled"
        private const val KEY_FOUR_FINGER_UI_TOGGLE = "four_finger_ui_toggle_enabled"
        private const val KEY_PINCH_ZOOM = "pinch_zoom_enabled"
        private const val KEY_TWO_FINGER_PAN = "two_finger_pan_enabled"
        private const val KEY_LONG_PRESS_EYEDROPPER = "long_press_eyedropper_enabled"
        private const val KEY_EDGE_SWIPE_LEFT = "edge_swipe_left_enabled"
        private const val KEY_EDGE_SWIPE_RIGHT = "edge_swipe_right_enabled"
        private const val KEY_EDGE_SWIPE_BOTTOM = "edge_swipe_bottom_enabled"
        private const val KEY_PALM_REJECTION = "palm_rejection_enabled"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"
        private const val KEY_GESTURE_HINTS = "gesture_hints_enabled"
        
        // Long-press duration preference
        private const val KEY_LONG_PRESS_DURATION = "long_press_duration_ms"
        private const val DEFAULT_LONG_PRESS_DURATION = 1000L
    }
    
    // Gesture enable/disable states
    var twoFingerUndoEnabled: Boolean
        get() = prefs.getBoolean(KEY_TWO_FINGER_UNDO, true)
        set(value) = prefs.edit().putBoolean(KEY_TWO_FINGER_UNDO, value).apply()
    
    var threeFingerRedoEnabled: Boolean
        get() = prefs.getBoolean(KEY_THREE_FINGER_REDO, true)
        set(value) = prefs.edit().putBoolean(KEY_THREE_FINGER_REDO, value).apply()
    
    var fourFingerUIToggleEnabled: Boolean
        get() = prefs.getBoolean(KEY_FOUR_FINGER_UI_TOGGLE, true)
        set(value) = prefs.edit().putBoolean(KEY_FOUR_FINGER_UI_TOGGLE, value).apply()
    
    var pinchZoomEnabled: Boolean
        get() = prefs.getBoolean(KEY_PINCH_ZOOM, true)
        set(value) = prefs.edit().putBoolean(KEY_PINCH_ZOOM, value).apply()
    
    var twoFingerPanEnabled: Boolean
        get() = prefs.getBoolean(KEY_TWO_FINGER_PAN, true)
        set(value) = prefs.edit().putBoolean(KEY_TWO_FINGER_PAN, value).apply()
    
    var longPressEyedropperEnabled: Boolean
        get() = prefs.getBoolean(KEY_LONG_PRESS_EYEDROPPER, true)
        set(value) = prefs.edit().putBoolean(KEY_LONG_PRESS_EYEDROPPER, value).apply()
    
    var edgeSwipeLeftEnabled: Boolean
        get() = prefs.getBoolean(KEY_EDGE_SWIPE_LEFT, true)
        set(value) = prefs.edit().putBoolean(KEY_EDGE_SWIPE_LEFT, value).apply()
    
    var edgeSwipeRightEnabled: Boolean
        get() = prefs.getBoolean(KEY_EDGE_SWIPE_RIGHT, true)
        set(value) = prefs.edit().putBoolean(KEY_EDGE_SWIPE_RIGHT, value).apply()
    
    var edgeSwipeBottomEnabled: Boolean
        get() = prefs.getBoolean(KEY_EDGE_SWIPE_BOTTOM, true)
        set(value) = prefs.edit().putBoolean(KEY_EDGE_SWIPE_BOTTOM, value).apply()
    
    var palmRejectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PALM_REJECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_PALM_REJECTION, value).apply()
    
    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()
    
    var gestureHintsEnabled: Boolean
        get() = prefs.getBoolean(KEY_GESTURE_HINTS, true)
        set(value) = prefs.edit().putBoolean(KEY_GESTURE_HINTS, value).apply()
    
    // Long-press duration setting
    var longPressDuration: Long
        get() = prefs.getLong(KEY_LONG_PRESS_DURATION, DEFAULT_LONG_PRESS_DURATION)
        set(value) = prefs.edit().putLong(KEY_LONG_PRESS_DURATION, value.coerceIn(500L, 2000L)).apply()
    
    /**
     * Reset all preferences to defaults
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Check if any gestures are disabled
     */
    fun hasDisabledGestures(): Boolean {
        return !twoFingerUndoEnabled ||
               !threeFingerRedoEnabled ||
               !fourFingerUIToggleEnabled ||
               !pinchZoomEnabled ||
               !twoFingerPanEnabled ||
               !longPressEyedropperEnabled ||
               !edgeSwipeLeftEnabled ||
               !edgeSwipeRightEnabled ||
               !edgeSwipeBottomEnabled ||
               !palmRejectionEnabled
    }
    
    /**
     * Get summary of enabled gestures
     */
    fun getEnabledGesturesSummary(): List<String> {
        val enabled = mutableListOf<String>()
        
        if (twoFingerUndoEnabled) enabled.add("Two-finger undo")
        if (threeFingerRedoEnabled) enabled.add("Three-finger redo")
        if (fourFingerUIToggleEnabled) enabled.add("Four-finger UI toggle")
        if (pinchZoomEnabled) enabled.add("Pinch zoom")
        if (twoFingerPanEnabled) enabled.add("Two-finger pan")
        if (longPressEyedropperEnabled) enabled.add("Long-press eyedropper")
        if (edgeSwipeLeftEnabled) enabled.add("Edge swipe left (brush)")
        if (edgeSwipeRightEnabled) enabled.add("Edge swipe right (color)")
        if (edgeSwipeBottomEnabled) enabled.add("Edge swipe bottom (layers)")
        if (palmRejectionEnabled) enabled.add("Palm rejection")
        
        return enabled
    }
}

/**
 * Gesture preference data class for Compose state
 */
data class GestureSettings(
    val twoFingerUndoEnabled: Boolean = true,
    val threeFingerRedoEnabled: Boolean = true,
    val fourFingerUIToggleEnabled: Boolean = true,
    val pinchZoomEnabled: Boolean = true,
    val twoFingerPanEnabled: Boolean = true,
    val longPressEyedropperEnabled: Boolean = true,
    val edgeSwipeLeftEnabled: Boolean = true,
    val edgeSwipeRightEnabled: Boolean = true,
    val edgeSwipeBottomEnabled: Boolean = true,
    val palmRejectionEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val gestureHintsEnabled: Boolean = true,
    val longPressDuration: Long = 1000L
) {
    companion object {
        fun fromPreferences(prefs: GesturePreferences): GestureSettings {
            return GestureSettings(
                twoFingerUndoEnabled = prefs.twoFingerUndoEnabled,
                threeFingerRedoEnabled = prefs.threeFingerRedoEnabled,
                fourFingerUIToggleEnabled = prefs.fourFingerUIToggleEnabled,
                pinchZoomEnabled = prefs.pinchZoomEnabled,
                twoFingerPanEnabled = prefs.twoFingerPanEnabled,
                longPressEyedropperEnabled = prefs.longPressEyedropperEnabled,
                edgeSwipeLeftEnabled = prefs.edgeSwipeLeftEnabled,
                edgeSwipeRightEnabled = prefs.edgeSwipeRightEnabled,
                edgeSwipeBottomEnabled = prefs.edgeSwipeBottomEnabled,
                palmRejectionEnabled = prefs.palmRejectionEnabled,
                hapticFeedbackEnabled = prefs.hapticFeedbackEnabled,
                gestureHintsEnabled = prefs.gestureHintsEnabled,
                longPressDuration = prefs.longPressDuration
            )
        }
    }
    
    fun applyToPreferences(prefs: GesturePreferences) {
        prefs.twoFingerUndoEnabled = twoFingerUndoEnabled
        prefs.threeFingerRedoEnabled = threeFingerRedoEnabled
        prefs.fourFingerUIToggleEnabled = fourFingerUIToggleEnabled
        prefs.pinchZoomEnabled = pinchZoomEnabled
        prefs.twoFingerPanEnabled = twoFingerPanEnabled
        prefs.longPressEyedropperEnabled = longPressEyedropperEnabled
        prefs.edgeSwipeLeftEnabled = edgeSwipeLeftEnabled
        prefs.edgeSwipeRightEnabled = edgeSwipeRightEnabled
        prefs.edgeSwipeBottomEnabled = edgeSwipeBottomEnabled
        prefs.palmRejectionEnabled = palmRejectionEnabled
        prefs.hapticFeedbackEnabled = hapticFeedbackEnabled
        prefs.gestureHintsEnabled = gestureHintsEnabled
        prefs.longPressDuration = longPressDuration
    }
}
