package com.artboard.ui.gestures

import android.content.Context
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset

/**
 * Main gesture coordinator that manages all gesture detection and routing
 * 
 * Integrates:
 * - MultiFingerDetector (2/3/4 finger taps)
 * - ZoomPanHandler (pinch zoom, two-finger pan)
 * - LongPressHandler (eyedropper)
 * - EdgeSwipeDetector (panel reveals)
 * - PalmRejection (ignore palm touches)
 * - HapticFeedbackManager (tactile feedback)
 * 
 * Based on GESTURE_SYSTEM.md specification
 */
class GestureHandler(
    context: Context,
    screenWidth: Float,
    screenHeight: Float,
    private val callbacks: GestureCallbacks
) {
    private val preferences = GesturePreferences(context)
    private val hapticManager = HapticFeedbackManager(context, preferences)
    
    // Gesture detectors
    private val multiFingerDetector = MultiFingerDetector()
    private val zoomPanHandler = ZoomPanHandler(
        onZoom = { scale, focus -> handleZoom(scale, focus) },
        onPan = { offset -> handlePan(offset) },
        onZoomSnapPoint = { hapticManager.performSnapPoint() }
    )
    private val longPressHandler = LongPressHandler(
        longPressDuration = preferences.longPressDuration
    )
    private val edgeSwipeDetector = EdgeSwipeDetector(
        screenWidth = screenWidth,
        screenHeight = screenHeight
    )
    private val palmRejection = PalmRejection(enabled = preferences.palmRejectionEnabled)
    
    // Current interaction mode
    private var currentMode = InteractionMode.IDLE
    private var activePointerCount = 0
    
    /**
     * Main entry point for all touch events
     */
    fun onTouchEvent(event: MotionEvent): GestureResult {
        // Update pointer count
        activePointerCount = event.pointerCount
        
        // Palm rejection check first
        if (palmRejection.shouldReject(event)) {
            return GestureResult.Rejected(GestureRejectionReason.PALM)
        }
        
        // Detect current interaction mode
        currentMode = detectMode(event)
        
        // Route to appropriate handler based on mode
        return when (currentMode) {
            InteractionMode.IDLE -> GestureResult.PassThrough
            InteractionMode.DRAWING -> handleDrawing(event)
            InteractionMode.NAVIGATION -> handleNavigation(event)
            InteractionMode.MULTI_FINGER -> handleMultiFinger(event)
            InteractionMode.LONG_PRESS -> handleLongPress(event)
            InteractionMode.EDGE_SWIPE -> handleEdgeSwipe(event)
        }
    }
    
    /**
     * Detect the current interaction mode based on touch state
     */
    private fun detectMode(event: MotionEvent): InteractionMode {
        // Check for edge swipe first (single finger at edge)
        if (activePointerCount == 1 && event.actionMasked == MotionEvent.ACTION_DOWN) {
            val position = Offset(event.x, event.y)
            if (isNearEdge(position)) {
                return InteractionMode.EDGE_SWIPE
            }
        }
        
        // Check for long-press (single finger, stationary)
        if (activePointerCount == 1 && longPressHandler.isActive()) {
            return InteractionMode.LONG_PRESS
        }
        
        // Multi-finger gestures
        return when {
            activePointerCount >= 3 -> InteractionMode.MULTI_FINGER
            activePointerCount == 2 -> InteractionMode.NAVIGATION
            activePointerCount == 1 -> {
                // Could be drawing or long-press starting
                if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                    InteractionMode.DRAWING
                } else {
                    InteractionMode.IDLE
                }
            }
            else -> InteractionMode.IDLE
        }
    }
    
    /**
     * Handle drawing mode (single finger/stylus)
     */
    private fun handleDrawing(event: MotionEvent): GestureResult {
        // Check for long-press activation
        if (preferences.longPressEyedropperEnabled) {
            when (val result = longPressHandler.onTouch(event)) {
                is LongPressResult.Triggered -> {
                    hapticManager.perform(HapticIntensity.MEDIUM)
                    callbacks.onEyedropper(result.position)
                    return GestureResult.Consumed(GestureType.LONG_PRESS_EYEDROPPER)
                }
                is LongPressResult.Moving -> {
                    callbacks.onEyedropperMove(result.position)
                    return GestureResult.Consumed(GestureType.LONG_PRESS_EYEDROPPER)
                }
                is LongPressResult.Released -> {
                    callbacks.onEyedropperRelease(result.position)
                    return GestureResult.Consumed(GestureType.LONG_PRESS_EYEDROPPER)
                }
                else -> {
                    // Continue with normal drawing
                }
            }
        }
        
        // Pass through to drawing system
        return GestureResult.PassThrough
    }
    
    /**
     * Handle navigation mode (two-finger zoom/pan)
     */
    private fun handleNavigation(event: MotionEvent): GestureResult {
        if (!preferences.pinchZoomEnabled && !preferences.twoFingerPanEnabled) {
            return GestureResult.PassThrough
        }
        
        when (val state = zoomPanHandler.onTouchEvent(event)) {
            is GestureState.Zooming -> {
                if (preferences.pinchZoomEnabled) {
                    return GestureResult.Consumed(GestureType.PINCH_ZOOM)
                }
            }
            is GestureState.Panning -> {
                if (preferences.twoFingerPanEnabled) {
                    return GestureResult.Consumed(GestureType.TWO_FINGER_PAN)
                }
            }
            else -> {}
        }
        
        return GestureResult.PassThrough
    }
    
    /**
     * Handle multi-finger gestures (3+ fingers)
     */
    private fun handleMultiFinger(event: MotionEvent): GestureResult {
        val tapGesture = multiFingerDetector.detectTap(event)
        
        return when (tapGesture) {
            is TapGesture.TwoFinger -> {
                if (preferences.twoFingerUndoEnabled) {
                    hapticManager.perform(HapticIntensity.MEDIUM)
                    callbacks.onUndo()
                    GestureResult.Consumed(GestureType.TWO_FINGER_UNDO)
                } else {
                    GestureResult.PassThrough
                }
            }
            
            is TapGesture.ThreeFinger -> {
                if (preferences.threeFingerRedoEnabled) {
                    hapticManager.perform(HapticIntensity.MEDIUM)
                    callbacks.onRedo()
                    GestureResult.Consumed(GestureType.THREE_FINGER_REDO)
                } else {
                    GestureResult.PassThrough
                }
            }
            
            is TapGesture.FourFinger -> {
                if (preferences.fourFingerUIToggleEnabled) {
                    hapticManager.perform(HapticIntensity.HEAVY)
                    callbacks.onToggleUI()
                    GestureResult.Consumed(GestureType.FOUR_FINGER_UI_TOGGLE)
                } else {
                    GestureResult.PassThrough
                }
            }
            
            else -> GestureResult.PassThrough
        }
    }
    
    /**
     * Handle long-press mode (eyedropper)
     */
    private fun handleLongPress(event: MotionEvent): GestureResult {
        // Already handled in drawing mode
        return GestureResult.PassThrough
    }
    
    /**
     * Handle edge swipe gestures
     */
    private fun handleEdgeSwipe(event: MotionEvent): GestureResult {
        when (val result = edgeSwipeDetector.onTouchEvent(event)) {
            is EdgeSwipeResult.Triggered -> {
                val gestureEnabled = when (result.edge) {
                    Edge.LEFT -> preferences.edgeSwipeLeftEnabled
                    Edge.RIGHT -> preferences.edgeSwipeRightEnabled
                    Edge.BOTTOM -> preferences.edgeSwipeBottomEnabled
                    Edge.TOP -> false // Not used
                }
                
                if (gestureEnabled) {
                    hapticManager.perform(HapticIntensity.MEDIUM)
                    callbacks.onEdgeSwipe(result.edge)
                    
                    val gestureType = when (result.edge) {
                        Edge.LEFT -> GestureType.EDGE_SWIPE_LEFT
                        Edge.RIGHT -> GestureType.EDGE_SWIPE_RIGHT
                        Edge.BOTTOM -> GestureType.EDGE_SWIPE_BOTTOM
                        Edge.TOP -> GestureType.EDGE_SWIPE_BOTTOM // Fallback
                    }
                    
                    return GestureResult.Consumed(gestureType)
                }
            }
            else -> {}
        }
        
        return GestureResult.PassThrough
    }
    
    /**
     * Handle zoom callback
     */
    private fun handleZoom(scale: Float, focus: Offset) {
        callbacks.onZoom(scale, focus)
    }
    
    /**
     * Handle pan callback
     */
    private fun handlePan(offset: Offset) {
        callbacks.onPan(offset)
    }
    
    /**
     * Check if position is near screen edge
     */
    private fun isNearEdge(position: Offset): Boolean {
        // Use edge detector's logic
        return position.x < 60f || 
               position.y < 60f ||
               position.x > edgeSwipeDetector.screenWidth - 60f ||
               position.y > edgeSwipeDetector.screenHeight - 60f
    }
    
    /**
     * Get current gesture preferences
     */
    fun getPreferences(): GesturePreferences = preferences
    
    /**
     * Get haptic manager for manual control
     */
    fun getHapticManager(): HapticFeedbackManager = hapticManager
    
    /**
     * Reset all gesture detectors
     */
    fun reset() {
        multiFingerDetector.reset()
        zoomPanHandler.reset()
        longPressHandler.reset()
        edgeSwipeDetector.reset()
        palmRejection.reset()
        currentMode = InteractionMode.IDLE
    }
    
    /**
     * Update screen dimensions (call on configuration change)
     */
    fun updateScreenSize(width: Float, height: Float) {
        // EdgeSwipeDetector doesn't have updateScreenSize, would need to recreate
        // For now, this is a placeholder for future enhancement
    }
}

/**
 * Interaction modes for gesture routing
 */
enum class InteractionMode {
    IDLE,           // No active gesture
    DRAWING,        // Single finger/stylus drawing
    NAVIGATION,     // Two-finger zoom/pan
    MULTI_FINGER,   // 3+ finger gestures (undo/redo/UI toggle)
    LONG_PRESS,     // Long-press eyedropper
    EDGE_SWIPE      // Edge swipe panel reveal
}

/**
 * Sealed class representing gesture processing results
 */
sealed class GestureResult {
    /** Gesture was consumed by the handler */
    data class Consumed(val type: GestureType) : GestureResult()
    
    /** Gesture was rejected (e.g., palm rejection) */
    data class Rejected(val reason: GestureRejectionReason) : GestureResult()
    
    /** Gesture not handled, pass to next handler */
    object PassThrough : GestureResult()
}

/**
 * Rejection reasons for diagnostic purposes
 */
enum class GestureRejectionReason {
    PALM,                   // Palm rejection
    DISABLED,              // Gesture disabled in preferences
    CONFLICTING_GESTURE    // Another gesture is active
}

/**
 * Callback interface for gesture actions
 */
interface GestureCallbacks {
    fun onUndo()
    fun onRedo()
    fun onToggleUI()
    fun onZoom(scale: Float, focus: Offset)
    fun onPan(offset: Offset)
    fun onEyedropper(position: Offset)
    fun onEyedropperMove(position: Offset)
    fun onEyedropperRelease(position: Offset)
    fun onEdgeSwipe(edge: Edge)
}
