package com.artboard.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.artboard.data.model.Brush
import com.artboard.ui.edge.ToolCategory

/**
 * Central controller for all edge-based UI in the canvas screen.
 * 
 * Coordinates between:
 * - Left edge controls (size, opacity, eyedropper)
 * - Right edge category bar (brush, color, layers, transform, settings)
 * - Popovers that appear when categories are tapped
 * - Auto-hide behavior during drawing
 * - Four-finger tap UI toggle
 * 
 * State Flow:
 * ```
 * User Interaction → EdgeUIController → State Update → UI Recomposition
 *                         ↓
 *                   PopoverManager (coordinates popovers)
 *                         ↓
 *                   AutoHideController (manages fading)
 * ```
 * 
 * Key Responsibilities:
 * 1. Track all UI state in one place for consistency
 * 2. Coordinate popover open/close (only one at a time)
 * 3. Manage auto-hide timing and opacity
 * 4. Handle gesture conflicts between canvas and controls
 * 5. Restore UI visibility on edge/control interaction
 * 
 * @see PopoverManager for popover-specific logic
 * @see CanvasUIState for the complete state model
 */
@Stable
class EdgeUIController {
    
    // ═══════════════════════════════════════════════════════════════
    // DRAWING TOOL STATE
    // ═══════════════════════════════════════════════════════════════
    
    /** Current brush size in pixels (1-500) */
    var brushSize: Float by mutableFloatStateOf(20f)
        private set
    
    /** Current brush opacity (0.0-1.0) */
    var brushOpacity: Float by mutableFloatStateOf(1f)
        private set
    
    /** Current drawing color */
    var currentColor: Color by mutableStateOf(Color.Black)
        private set
    
    /** Previous color for swap functionality */
    var previousColor: Color by mutableStateOf(Color.White)
        private set
    
    /** Currently selected brush */
    var currentBrush: Brush by mutableStateOf(Brush.pen())
        private set
    
    /** Whether eyedropper mode is active */
    var isEyedropperActive: Boolean by mutableStateOf(false)
        private set
    
    // ═══════════════════════════════════════════════════════════════
    // UI VISIBILITY STATE
    // ═══════════════════════════════════════════════════════════════
    
    /** Alpha for edge controls (1.0 = visible, 0.3 = faded) */
    var edgeControlsAlpha: Float by mutableFloatStateOf(1f)
        private set
    
    /** Whether UI is completely hidden (four-finger toggle) */
    var isUIHidden: Boolean by mutableStateOf(false)
        private set
    
    /** Whether user is currently drawing */
    var isDrawing: Boolean by mutableStateOf(false)
        private set
    
    /** Category with the currently selected tool (shows indicator dot) */
    var selectedCategory: ToolCategory by mutableStateOf(ToolCategory.BRUSH)
        private set
    
    // ═══════════════════════════════════════════════════════════════
    // POPOVER MANAGEMENT (delegated)
    // ═══════════════════════════════════════════════════════════════
    
    /** Popover manager for coordination */
    val popoverManager = PopoverManager()
    
    /** Currently active popover (convenience accessor) */
    val activePopover: ToolCategory? get() = popoverManager.activePopover
    
    /** Whether any popover is open (convenience accessor) */
    val hasOpenPopover: Boolean get() = popoverManager.hasOpenPopover()
    
    // ═══════════════════════════════════════════════════════════════
    // HISTORY STATE
    // ═══════════════════════════════════════════════════════════════
    
    /** Whether undo is available */
    var canUndo: Boolean by mutableStateOf(false)
        private set
    
    /** Whether redo is available */
    var canRedo: Boolean by mutableStateOf(false)
        private set
    
    // ═══════════════════════════════════════════════════════════════
    // CALLBACKS (set by CanvasScreenV2)
    // ═══════════════════════════════════════════════════════════════
    
    /** Called when brush size changes */
    var onBrushSizeChange: ((Float) -> Unit)? = null
    
    /** Called when opacity changes */
    var onOpacityChange: ((Float) -> Unit)? = null
    
    /** Called when color changes */
    var onColorChange: ((Color) -> Unit)? = null
    
    /** Called when brush changes */
    var onBrushChange: ((Brush) -> Unit)? = null
    
    /** Called when eyedropper is toggled */
    var onEyedropperToggle: ((Boolean) -> Unit)? = null
    
    /** Called when undo is requested */
    var onUndo: (() -> Unit)? = null
    
    /** Called when redo is requested */
    var onRedo: (() -> Unit)? = null
    
    /** Called when auto-hide timer should start */
    var onAutoHideStart: (() -> Unit)? = null
    
    /** Called when auto-hide timer should reset */
    var onAutoHideReset: (() -> Unit)? = null
    
    /** Called when auto-hide timer should pause */
    var onAutoHidePause: (() -> Unit)? = null
    
    // ═══════════════════════════════════════════════════════════════
    // DRAWING TOOL ACTIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Update brush size.
     * @param size New size in pixels (will be clamped to 1-500)
     */
    fun setBrushSize(size: Float) {
        brushSize = size.coerceIn(1f, 500f)
        onBrushSizeChange?.invoke(brushSize)
        restoreUIAndResetTimer()
    }
    
    /**
     * Update brush opacity.
     * @param opacity New opacity (will be clamped to 0-1)
     */
    fun setBrushOpacity(opacity: Float) {
        brushOpacity = opacity.coerceIn(0f, 1f)
        onOpacityChange?.invoke(brushOpacity)
        restoreUIAndResetTimer()
    }
    
    /**
     * Update current color.
     * Previous color is saved for swap functionality.
     * @param color New drawing color
     */
    fun setColor(color: Color) {
        if (color != currentColor) {
            previousColor = currentColor
            currentColor = color
            onColorChange?.invoke(color)
        }
        restoreUIAndResetTimer()
    }
    
    /**
     * Swap current and previous colors.
     */
    fun swapColors() {
        val temp = currentColor
        currentColor = previousColor
        previousColor = temp
        onColorChange?.invoke(currentColor)
        restoreUIAndResetTimer()
    }
    
    /**
     * Select a new brush.
     * @param brush The brush to select
     */
    fun selectBrush(brush: Brush) {
        currentBrush = brush
        selectedCategory = ToolCategory.BRUSH
        onBrushChange?.invoke(brush)
        popoverManager.closeAll() // Close popover after selection
        restoreUIAndResetTimer()
    }
    
    /**
     * Toggle eyedropper mode.
     */
    fun toggleEyedropper() {
        isEyedropperActive = !isEyedropperActive
        onEyedropperToggle?.invoke(isEyedropperActive)
        restoreUIAndResetTimer()
    }
    
    /**
     * Deactivate eyedropper mode (after color is picked).
     */
    fun deactivateEyedropper() {
        if (isEyedropperActive) {
            isEyedropperActive = false
            onEyedropperToggle?.invoke(false)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CATEGORY/POPOVER ACTIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Handle category button tap.
     * - Toggles the popover for that category
     * - Restores UI visibility
     * - Resets auto-hide timer
     * 
     * @param category The category that was tapped
     */
    fun onCategoryTapped(category: ToolCategory) {
        popoverManager.toggle(category)
        restoreUIAndResetTimer()
        
        // Pause auto-hide while popover is open
        if (popoverManager.hasOpenPopover()) {
            onAutoHidePause?.invoke()
        } else {
            onAutoHideStart?.invoke()
        }
    }
    
    /**
     * Dismiss any open popover.
     * Called when user taps outside popover or starts drawing.
     */
    fun dismissPopover() {
        popoverManager.closeAll()
        onAutoHideStart?.invoke()
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DRAWING STATE ACTIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Called when user starts drawing on canvas.
     * - Closes any open popover
     * - Pauses auto-hide timer (drawing handles its own timing)
     * - Sets drawing state
     */
    fun onDrawingStarted() {
        isDrawing = true
        popoverManager.closeAll()
        deactivateEyedropper()
        onAutoHidePause?.invoke()
    }
    
    /**
     * Called when user stops drawing.
     * - Clears drawing state
     * - Restarts auto-hide timer
     */
    fun onDrawingEnded() {
        isDrawing = false
        onAutoHideStart?.invoke()
    }
    
    /**
     * Fade edge controls to the specified alpha.
     * Called by auto-hide controller after drawing timeout.
     * 
     * @param alpha Target alpha (0.3 for faded, 1.0 for visible)
     */
    fun fadeControls(alpha: Float) {
        edgeControlsAlpha = alpha.coerceIn(0f, 1f)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UI VISIBILITY ACTIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Toggle complete UI visibility (four-finger tap).
     */
    fun toggleUIVisibility() {
        isUIHidden = !isUIHidden
        if (!isUIHidden) {
            edgeControlsAlpha = 1f
            onAutoHideStart?.invoke()
        }
    }
    
    /**
     * Show UI if hidden (edge tap when hidden).
     */
    fun showUI() {
        if (isUIHidden) {
            isUIHidden = false
        }
        edgeControlsAlpha = 1f
        onAutoHideStart?.invoke()
    }
    
    /**
     * Restore full UI visibility and reset auto-hide timer.
     * Called on any user interaction with controls.
     */
    fun restoreUIAndResetTimer() {
        if (isUIHidden) return // Don't restore if explicitly hidden
        
        edgeControlsAlpha = 1f
        onAutoHideReset?.invoke()
    }
    
    // ═══════════════════════════════════════════════════════════════
    // HISTORY ACTIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Request undo action.
     */
    fun undo() {
        onUndo?.invoke()
        restoreUIAndResetTimer()
    }
    
    /**
     * Request redo action.
     */
    fun redo() {
        onRedo?.invoke()
        restoreUIAndResetTimer()
    }
    
    /**
     * Update history availability state.
     * Called by ViewModel when history changes.
     */
    fun updateHistoryState(canUndo: Boolean, canRedo: Boolean) {
        this.canUndo = canUndo
        this.canRedo = canRedo
    }
    
    // ═══════════════════════════════════════════════════════════════
    // STATE SNAPSHOT
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get current state as an immutable CanvasUIState.
     * Useful for passing to composables that don't need the controller.
     */
    fun toUIState(layers: List<com.artboard.data.model.Layer> = emptyList()): CanvasUIState {
        return CanvasUIState(
            brushSize = brushSize,
            brushOpacity = brushOpacity,
            currentColor = currentColor,
            previousColor = previousColor,
            currentBrush = currentBrush,
            isEyedropperActive = isEyedropperActive,
            activePopover = activePopover,
            edgeControlsAlpha = edgeControlsAlpha,
            isDrawing = isDrawing,
            isUIHidden = isUIHidden,
            selectedCategory = selectedCategory,
            layers = layers,
            canUndo = canUndo,
            canRedo = canRedo
        )
    }
}

/**
 * Remember an EdgeUIController instance.
 * 
 * Usage:
 * ```kotlin
 * val edgeController = rememberEdgeUIController()
 * 
 * // Wire up to ViewModel
 * LaunchedEffect(Unit) {
 *     edgeController.onBrushSizeChange = { viewModel.setBrushSize(it) }
 *     edgeController.onColorChange = { viewModel.setColor(it) }
 *     // ... etc
 * }
 * 
 * // Use in composables
 * LeftEdgeControls(
 *     brushSize = edgeController.brushSize,
 *     onSizeChange = { edgeController.setBrushSize(it) }
 * )
 * ```
 */
@Composable
fun rememberEdgeUIController(): EdgeUIController {
    return remember { EdgeUIController() }
}

/**
 * Initialize EdgeUIController with values from ViewModel.
 * Call this when the controller is first created or when
 * ViewModel state needs to be synced.
 */
fun EdgeUIController.initializeFrom(
    brushSize: Float = 20f,
    brushOpacity: Float = 1f,
    currentColor: Color = Color.Black,
    currentBrush: Brush = Brush.pen(),
    canUndo: Boolean = false,
    canRedo: Boolean = false
) {
    // Use reflection-free approach: create new values
    // The private setters ensure state consistency
    setBrushSize(brushSize)
    setBrushOpacity(brushOpacity)
    setColor(currentColor)
    selectBrush(currentBrush)
    updateHistoryState(canUndo, canRedo)
}
