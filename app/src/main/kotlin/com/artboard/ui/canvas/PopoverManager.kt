package com.artboard.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.artboard.ui.edge.ToolCategory

/**
 * Manages popover state and coordination for the canvas screen.
 * 
 * Key Behaviors (from design spec):
 * 1. Only ONE popover open at a time
 * 2. Tapping another category: Close current, open new
 * 3. Tapping same category: Toggle closed
 * 4. Tapping outside: Close all
 * 5. Drawing on canvas: Close all
 * 6. Swipe popover right: Close (gesture dismissal)
 * 
 * Popover Layout Positioning:
 * ```
 * ┌──────────────────────────────────────────────────┬────┐
 * │                                                  │ 🖌 │ ← Brush button
 * │              ┌─────────────────┐                 ├────┤
 * │              │                 │ ← Popover       │ ◐  │ ← Color button  
 * │              │  280dp wide     │   anchored to   ├────┤
 * │              │  Max 450dp tall │   active button │ ☷  │ ← Layers button
 * │              │                 │                 ├────┤
 * │              └─────────────────┘                 │ ⬚  │
 * │                   CANVAS                         ├────┤
 * │                                                  │ ⚙  │
 * │                                                  ├────┤
 * │                                                  │ ↶↷ │
 * └──────────────────────────────────────────────────┴────┘
 * ```
 * 
 * @see ToolCategory for available categories
 */
@Stable
class PopoverManager {
    
    /**
     * Currently open popover category, or null if no popover is open.
     */
    var activePopover: ToolCategory? by mutableStateOf(null)
        private set
    
    /**
     * Whether the popover is in the process of closing (for animation).
     */
    var isClosing: Boolean by mutableStateOf(false)
        private set
    
    /**
     * Callback invoked when a popover opens.
     */
    var onPopoverOpened: ((ToolCategory) -> Unit)? = null
    
    /**
     * Callback invoked when a popover closes.
     */
    var onPopoverClosed: ((ToolCategory?) -> Unit)? = null
    
    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Toggle a popover for the given category.
     * - If the category is already active, close it
     * - If another category is active, close it and open the new one
     * - If no category is active, open this one
     * 
     * @param category The category to toggle
     */
    fun toggle(category: ToolCategory) {
        val previous = activePopover
        
        activePopover = if (activePopover == category) {
            // Same category - close it
            onPopoverClosed?.invoke(category)
            null
        } else {
            // Different category or none - close previous, open new
            if (previous != null) {
                onPopoverClosed?.invoke(previous)
            }
            onPopoverOpened?.invoke(category)
            category
        }
    }
    
    /**
     * Open a specific popover, closing any currently open one.
     * 
     * @param category The category to open
     */
    fun open(category: ToolCategory) {
        val previous = activePopover
        if (previous != null && previous != category) {
            onPopoverClosed?.invoke(previous)
        }
        
        if (activePopover != category) {
            activePopover = category
            onPopoverOpened?.invoke(category)
        }
    }
    
    /**
     * Close the currently open popover (if any).
     * Call this when:
     * - User taps outside the popover
     * - User starts drawing on canvas
     * - User swipes to dismiss
     */
    fun closeActive() {
        val previous = activePopover
        if (previous != null) {
            activePopover = null
            onPopoverClosed?.invoke(previous)
        }
    }
    
    /**
     * Close all popovers immediately.
     * Same as closeActive but semantically clearer for "close all" scenarios.
     */
    fun closeAll() {
        closeActive()
    }
    
    /**
     * Check if a specific category's popover is currently open.
     * 
     * @param category The category to check
     * @return true if this category's popover is open
     */
    fun isOpen(category: ToolCategory): Boolean {
        return activePopover == category
    }
    
    /**
     * Check if any popover is currently open.
     * 
     * @return true if any popover is open
     */
    fun hasOpenPopover(): Boolean {
        return activePopover != null
    }
    
    /**
     * Get the vertical anchor position for the popover based on the category.
     * Each category button has a specific position on the right edge bar.
     * 
     * Button positions (from top of edge bar):
     * - BRUSH: 0dp (top)
     * - COLOR: 52dp (48 + 4 gap)
     * - LAYERS: 104dp
     * - TRANSFORM: 156dp
     * - SETTINGS: 208dp
     * 
     * The popover is anchored to be centered on its button where possible,
     * but constrained to stay within screen bounds.
     * 
     * @param category The category to get anchor position for
     * @return Vertical offset in dp from top of screen
     */
    fun getAnchorOffset(category: ToolCategory): Int {
        return when (category) {
            ToolCategory.BRUSH -> 60      // Below status bar + some padding
            ToolCategory.COLOR -> 112     // 60 + 52
            ToolCategory.LAYERS -> 164    // 60 + 104
            ToolCategory.TRANSFORM -> 216 // 60 + 156
            ToolCategory.SETTINGS -> 268  // 60 + 208
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ANIMATION SUPPORT
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Begin closing animation.
     * Call this before animating the popover out.
     */
    fun beginClosing() {
        isClosing = true
    }
    
    /**
     * Complete closing animation.
     * Call this after the closing animation finishes.
     */
    fun completeClosing() {
        isClosing = false
        closeActive()
    }
}

/**
 * Remember a PopoverManager instance.
 * 
 * Usage:
 * ```kotlin
 * val popoverManager = rememberPopoverManager()
 * 
 * // In category button click handler
 * popoverManager.toggle(ToolCategory.BRUSH)
 * 
 * // In canvas touch handler
 * if (popoverManager.hasOpenPopover()) {
 *     popoverManager.closeAll()
 * }
 * 
 * // Check if specific popover is open
 * if (popoverManager.isOpen(ToolCategory.COLOR)) {
 *     // Show color picker popover
 * }
 * ```
 */
@Composable
fun rememberPopoverManager(): PopoverManager {
    return remember { PopoverManager() }
}

/**
 * Popover configuration constants.
 */
object PopoverConfig {
    /** Width of all popovers */
    const val POPOVER_WIDTH_DP = 280
    
    /** Maximum height of popovers */
    const val POPOVER_MAX_HEIGHT_DP = 450
    
    /** Padding from the edge bar to the popover */
    const val POPOVER_EDGE_PADDING_DP = 8
    
    /** Clear distance from right edge bar (56dp bar + 8dp padding) */
    const val RIGHT_EDGE_CLEAR_DP = 64
    
    /** Minimum distance from top of screen */
    const val MIN_TOP_PADDING_DP = 48
    
    /** Minimum distance from bottom of screen */
    const val MIN_BOTTOM_PADDING_DP = 48
    
    /** Animation duration for popover opening (ms) */
    const val OPEN_ANIMATION_MS = 200
    
    /** Animation duration for popover closing (ms) */
    const val CLOSE_ANIMATION_MS = 150
    
    /** Scrim alpha when popover is open */
    const val SCRIM_ALPHA = 0.15f
    
    /** Corner radius for popover surface */
    const val CORNER_RADIUS_DP = 16
    
    /** Elevation/shadow for popover */
    const val ELEVATION_DP = 8
    
    /** Swipe threshold to dismiss popover (as fraction of width) */
    const val SWIPE_DISMISS_THRESHOLD = 0.4f
}

/**
 * Popover transition state for animations.
 */
enum class PopoverTransitionState {
    /** Popover is completely hidden */
    HIDDEN,
    /** Popover is animating in */
    ENTERING,
    /** Popover is fully visible */
    VISIBLE,
    /** Popover is animating out */
    EXITING
}

/**
 * Information about a popover for layout and animation.
 */
data class PopoverInfo(
    /** The category this popover is for */
    val category: ToolCategory,
    
    /** Vertical anchor offset from top of screen */
    val anchorOffset: Int,
    
    /** Current transition state */
    val transitionState: PopoverTransitionState,
    
    /** Width of the popover in dp */
    val width: Int = PopoverConfig.POPOVER_WIDTH_DP,
    
    /** Maximum height of the popover in dp */
    val maxHeight: Int = PopoverConfig.POPOVER_MAX_HEIGHT_DP
)

/**
 * Extension to create PopoverInfo for a category.
 */
fun PopoverManager.getPopoverInfo(category: ToolCategory): PopoverInfo? {
    return if (isOpen(category)) {
        PopoverInfo(
            category = category,
            anchorOffset = getAnchorOffset(category),
            transitionState = if (isClosing) {
                PopoverTransitionState.EXITING
            } else {
                PopoverTransitionState.VISIBLE
            }
        )
    } else {
        null
    }
}
