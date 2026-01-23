package com.artboard.ui.canvas

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.artboard.data.model.Brush
import com.artboard.data.model.Layer
import com.artboard.ui.edge.ToolCategory

/**
 * Complete UI state for the canvas screen with edge-based controls.
 * 
 * This represents the full state of the canvas screen, including:
 * - Drawing tool state (brush, color, size, opacity)
 * - UI visibility state (popovers, auto-hide)
 * - Layer state
 * - History state (undo/redo)
 * 
 * Design Philosophy (from TOOL_ORGANIZATION_PHILOSOPHY.md):
 * - Canvas first: 95%+ of screen dedicated to artwork
 * - Progressive disclosure: Only show complexity when needed
 * - Edge-based categories always accessible
 * 
 * Screen Layout:
 * ```
 * ┌────────────────────────────────────────────────────────────────┐
 * │ ┌────┐                                               ┌────┐   │
 * │ │SIZE│                                               │ 🖌 │   │
 * │ │ ▓▓ │                                               ├────┤   │
 * │ │ ░░ │           ┌─────────────────┐                 │ ◐  │   │
 * │ ├────┤           │                 │                 ├────┤   │
 * │ │ ◉  │           │  Active Popover │                 │ ☷  │   │
 * │ ├────┤           │  (if any)       │                 ├────┤   │
 * │ │OPAC│           │    280dp        │                 │ ⬚  │   │
 * │ │ ▓▓ │           └─────────────────┘                 ├────┤   │
 * │ │ ░░ │                                               │ ⚙  │   │
 * │ └────┘              [CANVAS]                         ├────┤   │
 * │                                                      │ ↶↷ │   │
 * │  48dp                                                 56dp    │
 * └────────────────────────────────────────────────────────────────┘
 * ```
 */
@Immutable
data class CanvasUIState(
    // ═══════════════════════════════════════════════════════════════
    // DRAWING STATE
    // ═══════════════════════════════════════════════════════════════
    
    /** Current brush size in pixels (1-500) */
    val brushSize: Float = 20f,
    
    /** Current brush opacity (0.0-1.0) */
    val brushOpacity: Float = 1f,
    
    /** Current drawing color */
    val currentColor: Color = Color.Black,
    
    /** Previous color (for swap functionality) */
    val previousColor: Color = Color.White,
    
    /** Currently selected brush */
    val currentBrush: Brush = Brush.pen(),
    
    /** Whether eyedropper mode is active */
    val isEyedropperActive: Boolean = false,
    
    // ═══════════════════════════════════════════════════════════════
    // UI STATE
    // ═══════════════════════════════════════════════════════════════
    
    /** Currently open popover category, null if all closed */
    val activePopover: ToolCategory? = null,
    
    /** Alpha for edge controls (1.0 = visible, 0.3 = faded during drawing) */
    val edgeControlsAlpha: Float = 1f,
    
    /** Whether user is currently drawing on canvas */
    val isDrawing: Boolean = false,
    
    /** Whether UI is completely hidden (four-finger toggle) */
    val isUIHidden: Boolean = false,
    
    /** Category with currently selected tool (shows indicator dot) */
    val selectedCategory: ToolCategory = ToolCategory.BRUSH,
    
    // ═══════════════════════════════════════════════════════════════
    // LAYER STATE
    // ═══════════════════════════════════════════════════════════════
    
    /** List of all layers in the project */
    val layers: List<Layer> = emptyList(),
    
    /** ID of the currently selected layer */
    val selectedLayerId: String? = null,
    
    /** Index of the active layer (for drawing) */
    val activeLayerIndex: Int = 0,
    
    // ═══════════════════════════════════════════════════════════════
    // HISTORY STATE
    // ═══════════════════════════════════════════════════════════════
    
    /** Whether undo action is available */
    val canUndo: Boolean = false,
    
    /** Whether redo action is available */
    val canRedo: Boolean = false,
    
    // ═══════════════════════════════════════════════════════════════
    // CANVAS STATE
    // ═══════════════════════════════════════════════════════════════
    
    /** Canvas zoom level (1.0 = 100%) */
    val zoomLevel: Float = 1f,
    
    /** Canvas pan offset X */
    val panOffsetX: Float = 0f,
    
    /** Canvas pan offset Y */
    val panOffsetY: Float = 0f,
    
    /** Canvas rotation in degrees */
    val canvasRotation: Float = 0f
) {
    // ═══════════════════════════════════════════════════════════════
    // COMPUTED PROPERTIES
    // ═══════════════════════════════════════════════════════════════
    
    /** Whether any popover is currently open */
    val hasOpenPopover: Boolean get() = activePopover != null
    
    /** Whether edge controls should be interactive (not hidden) */
    val areControlsInteractive: Boolean get() = !isUIHidden
    
    /** Get the active layer */
    val activeLayer: Layer? get() = layers.getOrNull(activeLayerIndex)
    
    /** Get selected layer by ID */
    val selectedLayer: Layer? get() = layers.find { it.id == selectedLayerId }
    
    /** Number of layers in the project */
    val layerCount: Int get() = layers.size
    
    /** Zoom level as percentage string (e.g., "100%") */
    val zoomPercentage: String get() = "${(zoomLevel * 100).toInt()}%"
    
    // ═══════════════════════════════════════════════════════════════
    // STATE UPDATE HELPERS
    // ═══════════════════════════════════════════════════════════════
    
    /** Update brush size, clamping to valid range */
    fun withBrushSize(size: Float): CanvasUIState = 
        copy(brushSize = size.coerceIn(1f, 500f))
    
    /** Update brush opacity, clamping to valid range */
    fun withBrushOpacity(opacity: Float): CanvasUIState = 
        copy(brushOpacity = opacity.coerceIn(0f, 1f))
    
    /** Update current color and move previous current to previousColor */
    fun withColor(color: Color): CanvasUIState = 
        copy(currentColor = color, previousColor = currentColor)
    
    /** Swap current and previous colors */
    fun swapColors(): CanvasUIState = 
        copy(currentColor = previousColor, previousColor = currentColor)
    
    /** Open a popover, closing any currently open one */
    fun openPopover(category: ToolCategory): CanvasUIState = 
        copy(activePopover = category)
    
    /** Close all popovers */
    fun closePopover(): CanvasUIState = 
        copy(activePopover = null)
    
    /** Toggle a popover - close if open, open if closed */
    fun togglePopover(category: ToolCategory): CanvasUIState =
        if (activePopover == category) closePopover() else openPopover(category)
    
    /** Set drawing state and adjust edge controls alpha */
    fun setDrawing(drawing: Boolean): CanvasUIState = copy(
        isDrawing = drawing,
        // Close popovers when starting to draw
        activePopover = if (drawing) null else activePopover
    )
    
    /** Fade edge controls to specified alpha (for auto-hide) */
    fun fadeControls(alpha: Float): CanvasUIState = 
        copy(edgeControlsAlpha = alpha.coerceIn(0f, 1f))
    
    /** Toggle complete UI visibility (four-finger tap) */
    fun toggleUIVisibility(): CanvasUIState = 
        copy(isUIHidden = !isUIHidden)
    
    /** Show UI after being hidden */
    fun showUI(): CanvasUIState = 
        copy(isUIHidden = false, edgeControlsAlpha = 1f)
    
    /** Select a layer by index */
    fun selectLayer(index: Int): CanvasUIState = copy(
        activeLayerIndex = index.coerceIn(0, (layers.size - 1).coerceAtLeast(0)),
        selectedLayerId = layers.getOrNull(index)?.id
    )
    
    companion object {
        /** Default state for a new canvas */
        val DEFAULT = CanvasUIState()
        
        /** Auto-hide alpha when drawing continuously */
        const val AUTO_HIDE_ALPHA = 0.3f
        
        /** Full alpha when controls are active */
        const val FULL_ALPHA = 1f
        
        /** Delay before auto-hide triggers (milliseconds) */
        const val AUTO_HIDE_DELAY_MS = 2000L
    }
}

/**
 * Events that can occur in the canvas UI.
 * Used for one-way communication from UI to ViewModel.
 */
sealed interface CanvasUIEvent {
    // Drawing events
    data class BrushSizeChanged(val size: Float) : CanvasUIEvent
    data class BrushOpacityChanged(val opacity: Float) : CanvasUIEvent
    data class ColorChanged(val color: Color) : CanvasUIEvent
    data class BrushChanged(val brush: Brush) : CanvasUIEvent
    data object EyedropperToggled : CanvasUIEvent
    data object ColorSwapped : CanvasUIEvent
    
    // UI events
    data class PopoverToggled(val category: ToolCategory) : CanvasUIEvent
    data object PopoverDismissed : CanvasUIEvent
    data object UIToggled : CanvasUIEvent
    data object DrawingStarted : CanvasUIEvent
    data object DrawingEnded : CanvasUIEvent
    data class EdgeTapped(val edge: CanvasEdge) : CanvasUIEvent
    data object FourFingerTap : CanvasUIEvent
    
    // Layer events
    data class LayerSelected(val index: Int) : CanvasUIEvent
    data class LayerVisibilityToggled(val index: Int) : CanvasUIEvent
    data class LayerOpacityChanged(val index: Int, val opacity: Float) : CanvasUIEvent
    data object LayerAdded : CanvasUIEvent
    data class LayerDeleted(val index: Int) : CanvasUIEvent
    
    // History events
    data object UndoRequested : CanvasUIEvent
    data object RedoRequested : CanvasUIEvent
    
    // Canvas navigation events
    data class ZoomChanged(val zoom: Float) : CanvasUIEvent
    data class PanChanged(val x: Float, val y: Float) : CanvasUIEvent
    data class RotationChanged(val degrees: Float) : CanvasUIEvent
    data object ResetView : CanvasUIEvent
}

/**
 * Canvas edges for edge tap detection.
 */
enum class CanvasEdge {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
}

/**
 * Result of a popover action - used to communicate back to the screen.
 */
sealed interface PopoverResult {
    /** Popover was dismissed without selection */
    data object Dismissed : PopoverResult
    
    /** Brush was selected from brush popover */
    data class BrushSelected(val brush: Brush) : PopoverResult
    
    /** Color was selected from color popover */
    data class ColorSelected(val color: Color) : PopoverResult
    
    /** Layer was selected from layers popover */
    data class LayerSelected(val index: Int) : PopoverResult
    
    /** Action was selected from settings popover */
    data class ActionSelected(val action: String) : PopoverResult
}
