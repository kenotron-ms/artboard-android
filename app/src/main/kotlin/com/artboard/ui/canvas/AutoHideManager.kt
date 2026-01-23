package com.artboard.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced auto-hide manager for edge-based canvas UI.
 * 
 * Implements the auto-hide behavior specified in the design:
 * 
 * Timing:
 * ```
 * 0ms     - Touch begins (drawing starts)
 *           └─ UI at normal opacity (1.0)
 * 
 * 2000ms  - Continuous drawing
 *           └─ UI begins fade (300ms transition)
 * 
 * 2300ms  - UI at 30% opacity (barely visible)
 *           └─ Canvas at ~98% effective space
 * 
 * Touch ends:
 * 0ms     - Touch released
 *           └─ UI fade-in begins (200ms)
 * 
 * 200ms   - UI at full opacity
 *           └─ Ready for tool access
 * ```
 * 
 * Key Behaviors:
 * 1. **Drawing starts** → Start auto-hide timer (2 seconds)
 * 2. **2s of drawing** → Fade edges to 30% opacity
 * 3. **Touch edge control** → Restore to 100%, reset timer
 * 4. **Tap category button** → Toggle popover, restore 100%
 * 5. **Four-finger tap** → Toggle UI completely on/off
 * 
 * Edge tap during hidden:
 * - Single tap on edge area → UI appears instantly
 * - Begin drawing → UI stays hidden
 * 
 * Settings (customizable):
 * - auto_hide_enabled: boolean (default: true)
 * - auto_hide_delay_ms: int (default: 2000)
 * - auto_hide_opacity: float (default: 0.3)
 * 
 * @param hideDelayMs Delay before fading starts (default: 2000ms)
 * @param fadedAlpha Target alpha when faded (default: 0.3)
 * @param onAlphaChange Callback when alpha should change
 */
@Stable
class AutoHideManager(
    private val hideDelayMs: Long = DEFAULT_HIDE_DELAY_MS,
    private val fadedAlpha: Float = DEFAULT_FADED_ALPHA,
    private val onAlphaChange: (Float) -> Unit
) {
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /** Whether auto-hide is enabled */
    var isEnabled: Boolean by mutableStateOf(true)
    
    /** Whether user is currently drawing */
    var isDrawing: Boolean by mutableStateOf(false)
        private set
    
    /** Current alpha value for edge controls */
    var currentAlpha: Float by mutableFloatStateOf(FULL_ALPHA)
        private set
    
    /** Whether UI is completely hidden (four-finger toggle) */
    var isCompletelyHidden: Boolean by mutableStateOf(false)
        private set
    
    /** Whether a popover is currently open (pauses auto-hide) */
    var hasOpenPopover: Boolean by mutableStateOf(false)
    
    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Called when user starts drawing on canvas.
     * Starts the auto-hide countdown.
     */
    fun onDrawingStarted() {
        isDrawing = true
        
        if (!isEnabled || isCompletelyHidden) return
        
        startFadeTimer()
    }
    
    /**
     * Called when user stops drawing.
     * Restores UI to full visibility.
     */
    fun onDrawingEnded() {
        isDrawing = false
        
        if (!isEnabled || isCompletelyHidden) return
        
        cancelFadeTimer()
        restoreAlpha()
    }
    
    /**
     * Called when user interacts with edge controls.
     * Restores UI and resets the timer.
     */
    fun onControlInteraction() {
        if (isCompletelyHidden) return
        
        restoreAlpha()
        
        // If still drawing, restart the timer
        if (isDrawing && isEnabled) {
            startFadeTimer()
        }
    }
    
    /**
     * Called when a popover opens.
     * Pauses auto-hide while popover is open.
     */
    fun onPopoverOpened() {
        hasOpenPopover = true
        cancelFadeTimer()
        restoreAlpha()
    }
    
    /**
     * Called when popover closes.
     * Resumes auto-hide if still drawing.
     */
    fun onPopoverClosed() {
        hasOpenPopover = false
        
        // Resume timer if still drawing
        if (isDrawing && isEnabled && !isCompletelyHidden) {
            startFadeTimer()
        }
    }
    
    /**
     * Toggle complete UI visibility (four-finger tap).
     */
    fun toggleUIVisibility() {
        isCompletelyHidden = !isCompletelyHidden
        
        if (isCompletelyHidden) {
            // Completely hide - set alpha to 0
            cancelFadeTimer()
            setAlpha(0f)
        } else {
            // Show - restore to full alpha
            restoreAlpha()
            
            // Start timer if drawing
            if (isDrawing && isEnabled) {
                startFadeTimer()
            }
        }
    }
    
    /**
     * Show UI if completely hidden (edge tap gesture).
     */
    fun showIfHidden() {
        if (isCompletelyHidden) {
            isCompletelyHidden = false
            restoreAlpha()
        }
    }
    
    /**
     * Force fade to the target alpha immediately.
     * Used for testing or programmatic control.
     */
    fun forceFade() {
        if (!isCompletelyHidden) {
            setAlpha(fadedAlpha)
        }
    }
    
    /**
     * Reset to full visibility.
     */
    fun reset() {
        cancelFadeTimer()
        isDrawing = false
        isCompletelyHidden = false
        restoreAlpha()
    }
    
    /**
     * Clean up resources.
     * Call this when the controller is no longer needed.
     */
    fun cleanup() {
        cancelFadeTimer()
        scope.cancel()
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════
    
    private fun startFadeTimer() {
        // Don't start if popover is open
        if (hasOpenPopover) return
        
        cancelFadeTimer()
        
        timerJob = scope.launch {
            delay(hideDelayMs)
            
            // Only fade if still drawing and no popover
            if (isDrawing && !hasOpenPopover && !isCompletelyHidden) {
                setAlpha(fadedAlpha)
            }
        }
    }
    
    private fun cancelFadeTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    private fun restoreAlpha() {
        setAlpha(FULL_ALPHA)
    }
    
    private fun setAlpha(alpha: Float) {
        currentAlpha = alpha
        onAlphaChange(alpha)
    }
    
    companion object {
        /** Default delay before auto-hide (2 seconds) */
        const val DEFAULT_HIDE_DELAY_MS = 2000L
        
        /** Default faded alpha (30%) */
        const val DEFAULT_FADED_ALPHA = 0.3f
        
        /** Full visibility alpha */
        const val FULL_ALPHA = 1f
        
        /** Completely hidden alpha */
        const val HIDDEN_ALPHA = 0f
        
        /** Fade animation duration */
        const val FADE_DURATION_MS = 300
        
        /** Restore animation duration */
        const val RESTORE_DURATION_MS = 200
    }
}

/**
 * Remember an AutoHideManager instance.
 * 
 * Usage:
 * ```kotlin
 * val autoHideManager = rememberAutoHideManager { alpha ->
 *     edgeController.fadeControls(alpha)
 * }
 * 
 * // In drawing callbacks
 * onDrawingStarted = { autoHideManager.onDrawingStarted() }
 * onDrawingEnded = { autoHideManager.onDrawingEnded() }
 * 
 * // On control interaction
 * onClick = {
 *     autoHideManager.onControlInteraction()
 *     // handle click
 * }
 * ```
 * 
 * @param hideDelayMs Delay before fading (default: 2000ms)
 * @param fadedAlpha Target alpha when faded (default: 0.3)
 * @param onAlphaChange Callback when alpha changes
 */
@Composable
fun rememberAutoHideManager(
    hideDelayMs: Long = AutoHideManager.DEFAULT_HIDE_DELAY_MS,
    fadedAlpha: Float = AutoHideManager.DEFAULT_FADED_ALPHA,
    onAlphaChange: (Float) -> Unit
): AutoHideManager {
    val manager = remember(hideDelayMs, fadedAlpha) {
        AutoHideManager(
            hideDelayMs = hideDelayMs,
            fadedAlpha = fadedAlpha,
            onAlphaChange = onAlphaChange
        )
    }
    
    // Cleanup on dispose
    DisposableEffect(manager) {
        onDispose {
            manager.cleanup()
        }
    }
    
    return manager
}

/**
 * State for multi-finger gesture detection.
 * Used to detect four-finger tap for UI toggle.
 */
class MultiFingerGestureState {
    /** Number of fingers currently touching */
    var fingerCount: Int by mutableStateOf(0)
        private set
    
    /** Whether a four-finger tap was detected */
    var fourFingerTapDetected: Boolean by mutableStateOf(false)
        private set
    
    /** Timestamp of last touch down */
    private var touchDownTime: Long = 0
    
    /** Maximum tap duration (ms) */
    private val maxTapDuration = 300L
    
    /**
     * Called when touch begins.
     * @param count Number of fingers touching
     */
    fun onTouchDown(count: Int) {
        fingerCount = count
        touchDownTime = System.currentTimeMillis()
        fourFingerTapDetected = false
    }
    
    /**
     * Called when touch ends.
     * @param count Number of fingers that were touching
     */
    fun onTouchUp(count: Int) {
        val duration = System.currentTimeMillis() - touchDownTime
        
        // Detect four-finger tap (quick touch with 4 fingers)
        if (count >= 4 && duration < maxTapDuration) {
            fourFingerTapDetected = true
        }
        
        fingerCount = 0
    }
    
    /**
     * Consume the four-finger tap detection.
     * Call this after handling the gesture.
     */
    fun consumeFourFingerTap() {
        fourFingerTapDetected = false
    }
}

/**
 * Remember a MultiFingerGestureState instance.
 */
@Composable
fun rememberMultiFingerGestureState(): MultiFingerGestureState {
    return remember { MultiFingerGestureState() }
}
