package com.artboard.ui.edge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Left Edge Controls - Always-visible sliders and eyedropper.
 * 
 * This is the main container for the left edge of the Artboard canvas,
 * providing always-visible controls that artists use constantly (every stroke):
 * - Brush size slider (logarithmic, 1-500px)
 * - Eyedropper button (color picker shortcut)
 * - Brush opacity slider (linear, 0-100%)
 * 
 * Layout:
 * ```
 * ┌────┐
 * │ ○  │ ← Size icon (small)
 * │ ┃  │
 * │ ┃  │ Size Slider
 * │ ┃  │
 * │ ●  │ ← Size icon (large)
 * ├────┤
 * │ ◉  │ ← Eyedropper button
 * ├────┤
 * │ ●  │ ← Opacity icon (solid)
 * │ ┃  │
 * │ ┃  │ Opacity Slider
 * │ ┃  │
 * │ ○  │ ← Opacity icon (faded)
 * └────┘
 *   48dp
 * ```
 * 
 * Based on design/components/EdgeControlBar.md and TOOL_ORGANIZATION_PHILOSOPHY.md
 * 
 * @param brushSize Current brush size in pixels (1-500)
 * @param brushOpacity Current brush opacity (0.0-1.0)
 * @param currentColor Current brush color (for eyedropper tint)
 * @param isEyedropperActive Whether eyedropper mode is active
 * @param onSizeChange Callback when brush size changes
 * @param onOpacityChange Callback when opacity changes
 * @param onEyedropperClick Callback when eyedropper button is tapped
 * @param modifier Modifier for the component
 * @param controlsAlpha Alpha for auto-hide (0.3 when drawing, 1.0 when idle)
 * @param sliderHeight Height of each slider
 * @param enabled Whether controls are interactive
 */
@Composable
fun LeftEdgeControls(
    brushSize: Float,
    brushOpacity: Float,
    currentColor: Color,
    isEyedropperActive: Boolean,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onEyedropperClick: () -> Unit,
    modifier: Modifier = Modifier,
    controlsAlpha: Float = 1f,
    sliderHeight: Dp = 180.dp,
    enabled: Boolean = true
) {
    // Animate alpha changes smoothly
    val animatedAlpha by animateFloatAsState(
        targetValue = controlsAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "controlsAlpha"
    )
    
    // Total width: 48dp controls + 8dp inset = 56dp
    val edgeInset = 8.dp
    val controlWidth = 48.dp
    
    Box(
        modifier = modifier
            .width(controlWidth + edgeInset)
            .fillMaxHeight()
            .alpha(animatedAlpha),
        contentAlignment = Alignment.CenterStart
    ) {
        // Main controls column with edge inset
        Column(
            modifier = Modifier
                .width(controlWidth)
                .padding(start = edgeInset)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Size Slider (top)
            SizeSlider(
                sizePx = brushSize,
                onSizeChange = onSizeChange,
                height = sliderHeight,
                enabled = enabled
            )
            
            // Eyedropper Button (middle)
            EyedropperButton(
                currentColor = currentColor,
                isActive = isEyedropperActive,
                onClick = onEyedropperClick,
                enabled = enabled
            )
            
            // Opacity Slider (bottom)
            OpacitySlider(
                opacity = brushOpacity,
                onOpacityChange = onOpacityChange,
                height = sliderHeight,
                enabled = enabled
            )
        }
    }
}

/**
 * Compact version of LeftEdgeControls for smaller screens.
 * Uses shorter sliders and smaller spacing.
 */
@Composable
fun LeftEdgeControlsCompact(
    brushSize: Float,
    brushOpacity: Float,
    currentColor: Color,
    isEyedropperActive: Boolean,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onEyedropperClick: () -> Unit,
    modifier: Modifier = Modifier,
    controlsAlpha: Float = 1f,
    enabled: Boolean = true
) {
    LeftEdgeControls(
        brushSize = brushSize,
        brushOpacity = brushOpacity,
        currentColor = currentColor,
        isEyedropperActive = isEyedropperActive,
        onSizeChange = onSizeChange,
        onOpacityChange = onOpacityChange,
        onEyedropperClick = onEyedropperClick,
        modifier = modifier,
        controlsAlpha = controlsAlpha,
        sliderHeight = 140.dp,
        enabled = enabled
    )
}

/**
 * State holder for LeftEdgeControls.
 * Manages brush size, opacity, and eyedropper mode.
 */
class LeftEdgeControlsState(
    initialSize: Float = 20f,
    initialOpacity: Float = 1f,
    initialColor: Color = Color.Black
) {
    var brushSize by mutableStateOf(initialSize)
        private set
    
    var brushOpacity by mutableStateOf(initialOpacity)
        private set
    
    var currentColor by mutableStateOf(initialColor)
        private set
    
    var isEyedropperActive by mutableStateOf(false)
        private set
    
    fun updateSize(size: Float) {
        brushSize = size.coerceIn(1f, 500f)
    }
    
    fun updateOpacity(opacity: Float) {
        brushOpacity = opacity.coerceIn(0f, 1f)
    }
    
    fun updateColor(color: Color) {
        currentColor = color
    }
    
    fun toggleEyedropper() {
        isEyedropperActive = !isEyedropperActive
    }
    
    fun activateEyedropper() {
        isEyedropperActive = true
    }
    
    fun deactivateEyedropper() {
        isEyedropperActive = false
    }
}

/**
 * Remember a LeftEdgeControlsState.
 */
@Composable
fun rememberLeftEdgeControlsState(
    initialSize: Float = 20f,
    initialOpacity: Float = 1f,
    initialColor: Color = Color.Black
): LeftEdgeControlsState {
    return remember {
        LeftEdgeControlsState(initialSize, initialOpacity, initialColor)
    }
}

/**
 * Extension to create LeftEdgeControls from state.
 */
@Composable
fun LeftEdgeControls(
    state: LeftEdgeControlsState,
    modifier: Modifier = Modifier,
    controlsAlpha: Float = 1f,
    sliderHeight: Dp = 180.dp,
    enabled: Boolean = true
) {
    LeftEdgeControls(
        brushSize = state.brushSize,
        brushOpacity = state.brushOpacity,
        currentColor = state.currentColor,
        isEyedropperActive = state.isEyedropperActive,
        onSizeChange = { state.updateSize(it) },
        onOpacityChange = { state.updateOpacity(it) },
        onEyedropperClick = { state.toggleEyedropper() },
        modifier = modifier,
        controlsAlpha = controlsAlpha,
        sliderHeight = sliderHeight,
        enabled = enabled
    )
}

/**
 * Preview helper - LeftEdgeControls with default values for testing.
 */
@Composable
fun LeftEdgeControlsPreview(
    modifier: Modifier = Modifier
) {
    val state = rememberLeftEdgeControlsState(
        initialSize = 20f,
        initialOpacity = 0.8f,
        initialColor = Color(0xFF007AFF)
    )
    
    LeftEdgeControls(
        state = state,
        modifier = modifier
    )
}

/**
 * Specifications for LeftEdgeControls dimensions.
 * Based on design/components/EdgeControlBar.md
 */
object LeftEdgeSpecs {
    /** Total width of the edge control bar (controls + inset) */
    val TotalWidth = 56.dp
    
    /** Width of the control elements */
    val ControlWidth = 48.dp
    
    /** Inset from screen edge */
    val EdgeInset = 8.dp
    
    /** Default slider height */
    val DefaultSliderHeight = 180.dp
    
    /** Compact slider height for smaller screens */
    val CompactSliderHeight = 140.dp
    
    /** Spacing between controls */
    val ControlSpacing = 8.dp
    
    /** Touch target minimum (Material Design) */
    val MinTouchTarget = 48.dp
    
    /** Auto-hide opacity when drawing */
    val AutoHideAlpha = 0.3f
    
    /** Full opacity when idle */
    val FullAlpha = 1f
    
    /** Auto-hide delay in milliseconds */
    val AutoHideDelayMs = 2000L
}
