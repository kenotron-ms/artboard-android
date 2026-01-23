package com.artboard.ui.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Controls auto-hide behavior for UI elements
 * Manages timing logic for showing/hiding UI after inactivity
 */
class AutoHideController(
    private val hideDelay: Long = 3000L,
    private val onHide: () -> Unit
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Start the auto-hide timer
     * UI will hide after [hideDelay] milliseconds
     */
    fun start() {
        stop()
        job = scope.launch {
            delay(hideDelay)
            onHide()
        }
    }
    
    /**
     * Stop the auto-hide timer
     * UI will not hide automatically
     */
    fun stop() {
        job?.cancel()
        job = null
    }
    
    /**
     * Pause the timer (same as stop for this implementation)
     */
    fun pause() {
        stop()
    }
    
    /**
     * Reset the timer - restart from beginning
     */
    fun reset() {
        start()
    }
    
    /**
     * Clean up resources when done
     */
    fun cleanup() {
        stop()
        scope.cancel()
    }
}
