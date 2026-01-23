package com.artboard.data.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface for tracking progress of long-running operations
 */
interface ProgressTracker {
    fun updateProgress(progress: Float, message: String)
}

/**
 * Progress tracker that updates a UI state flow
 */
class UIProgressTracker(
    private val onUpdate: (Float, String) -> Unit
) : ProgressTracker {
    override fun updateProgress(progress: Float, message: String) {
        onUpdate(progress.coerceIn(0f, 1f), message)
    }
}

/**
 * Progress state for UI display
 */
data class ProgressState(
    val progress: Float = 0f,
    val message: String = "",
    val isVisible: Boolean = false
)

/**
 * Progress tracker with state flow for Compose UI
 */
class FlowProgressTracker : ProgressTracker {
    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state.asStateFlow()
    
    override fun updateProgress(progress: Float, message: String) {
        _state.value = ProgressState(
            progress = progress.coerceIn(0f, 1f),
            message = message,
            isVisible = true
        )
    }
    
    fun hide() {
        _state.value = ProgressState(isVisible = false)
    }
    
    fun show(message: String) {
        _state.value = ProgressState(
            progress = 0f,
            message = message,
            isVisible = true
        )
    }
}

/**
 * No-op progress tracker for when progress tracking is not needed
 */
object NoOpProgressTracker : ProgressTracker {
    override fun updateProgress(progress: Float, message: String) {
        // Do nothing
    }
}
