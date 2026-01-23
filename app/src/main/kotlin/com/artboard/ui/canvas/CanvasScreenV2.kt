package com.artboard.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artboard.data.model.Brush
import com.artboard.ui.brush.BrushSelectorPopover
import com.artboard.ui.color.ColorPickerPopover
import com.artboard.ui.edge.LeftEdgeControls
import com.artboard.ui.edge.RightEdgeCategoryBar
import com.artboard.ui.edge.ToolCategory
import com.artboard.ui.layers.LayerPanelPopover

/**
 * Canvas Screen V2 - Edge-Based UI Integration
 * 
 * A full-screen canvas experience with edge-based controls that maximizes
 * canvas visibility while keeping tools always accessible.
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
 * 
 * Key Features:
 * - Full-screen canvas in center
 * - Left edge: LeftEdgeControls (always visible, fades during drawing)
 * - Right edge: RightEdgeCategoryBar (always visible, fades during drawing)
 * - Popovers: Appear when category buttons tapped (one at a time)
 * - Auto-hide: Controls fade to 30% after 2s of continuous drawing
 * - Four-finger tap: Toggle complete UI visibility
 * 
 * Based on:
 * - design/components/EdgeControlBar.md
 * - docs/TOOL_ORGANIZATION_PHILOSOPHY.md
 * 
 * @param projectId Optional project ID to load
 * @param viewModel Canvas ViewModel for state management
 * @param onNavigateBack Callback to navigate back to gallery
 */
@Composable
fun CanvasScreenV2(
    projectId: String? = null,
    viewModel: CanvasViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    // ═══════════════════════════════════════════════════════════════
    // STATE COLLECTION
    // ═══════════════════════════════════════════════════════════════
    
    val project by viewModel.project.collectAsState()
    val currentBrush by viewModel.currentBrush.collectAsState()
    val currentColorInt by viewModel.currentColor.collectAsState()
    val activeLayerIndex by viewModel.activeLayerIndex.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isDrawing by viewModel.isDrawing.collectAsState()
    
    // Convert color int to Compose Color
    val currentColor = remember(currentColorInt) { Color(currentColorInt) }
    
    // ═══════════════════════════════════════════════════════════════
    // UI CONTROLLERS
    // ═══════════════════════════════════════════════════════════════
    
    val edgeController = rememberEdgeUIController()
    val popoverManager = edgeController.popoverManager
    
    // Auto-hide manager for edge controls
    val autoHideManager = rememberAutoHideManager { alpha ->
        edgeController.fadeControls(alpha)
    }
    
    // Multi-finger gesture detection for four-finger tap
    var fingerCount by remember { mutableIntStateOf(0) }
    
    // Track previous color for color picker
    var previousColor by remember { mutableStateOf(Color.White) }
    
    // ═══════════════════════════════════════════════════════════════
    // STATE SYNCHRONIZATION
    // ═══════════════════════════════════════════════════════════════
    
    // Sync ViewModel state to EdgeUIController
    LaunchedEffect(currentBrush) {
        edgeController.selectBrush(currentBrush)
    }
    
    LaunchedEffect(canUndo, canRedo) {
        edgeController.updateHistoryState(canUndo, canRedo)
    }
    
    // Wire up controller callbacks to ViewModel
    LaunchedEffect(Unit) {
        edgeController.onBrushSizeChange = { size ->
            // TODO: viewModel.setBrushSize(size)
        }
        edgeController.onOpacityChange = { opacity ->
            // TODO: viewModel.setOpacity(opacity)
        }
        edgeController.onColorChange = { color ->
            viewModel.setColor(color.toArgb())
        }
        edgeController.onBrushChange = { brush ->
            viewModel.setBrush(brush)
        }
        edgeController.onUndo = { viewModel.undo() }
        edgeController.onRedo = { viewModel.redo() }
        
        // Auto-hide callbacks
        edgeController.onAutoHideStart = { autoHideManager.onDrawingStarted() }
        edgeController.onAutoHideReset = { autoHideManager.onControlInteraction() }
        edgeController.onAutoHidePause = { autoHideManager.onDrawingEnded() }
    }
    
    // Track drawing state for auto-hide
    LaunchedEffect(isDrawing) {
        if (isDrawing) {
            autoHideManager.onDrawingStarted()
        } else {
            autoHideManager.onDrawingEnded()
        }
    }
    
    // Track popover state for auto-hide
    LaunchedEffect(popoverManager.activePopover) {
        if (popoverManager.hasOpenPopover()) {
            autoHideManager.onPopoverOpened()
        } else {
            autoHideManager.onPopoverClosed()
        }
    }
    
    // Load project if provided
    LaunchedEffect(projectId) {
        if (projectId != null) {
            // TODO: Load project from repository
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            autoHideManager.cleanup()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ANIMATED VALUES
    // ═══════════════════════════════════════════════════════════════
    
    val edgeAlpha by animateFloatAsState(
        targetValue = if (edgeController.isUIHidden) 0f else edgeController.edgeControlsAlpha,
        animationSpec = tween(
            durationMillis = if (edgeController.edgeControlsAlpha < 1f) {
                AutoHideManager.FADE_DURATION_MS
            } else {
                AutoHideManager.RESTORE_DURATION_MS
            }
        ),
        label = "edgeAlpha"
    )
    
    // ═══════════════════════════════════════════════════════════════
    // MAIN LAYOUT
    // ═══════════════════════════════════════════════════════════════
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // Dark canvas background
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        // Track finger count for four-finger detection
                        // Note: Full multi-finger detection requires custom handling
                    },
                    onTap = { offset ->
                        // Tap on canvas area (outside controls) dismisses popover
                        if (popoverManager.hasOpenPopover()) {
                            popoverManager.closeAll()
                        }
                    }
                )
            }
    ) {
        // ═══════════════════════════════════════════════════════════
        // LAYER 1: CANVAS (bottom layer)
        // ═══════════════════════════════════════════════════════════
        
        project?.let { proj ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 56.dp, // Left edge controls width
                        end = 56.dp    // Right edge controls width
                    )
            ) {
                AndroidView(
                    factory = { context ->
                        CanvasView(context).apply {
                            initialize(proj.width, proj.height)
                            setLayers(proj.layers, activeLayerIndex)
                            setBrush(currentBrush)
                            setColor(currentColorInt)
                            
                            onStrokeBegin = {
                                viewModel.onDrawingStarted()
                                edgeController.onDrawingStarted()
                            }
                            
                            onStrokeEnd = {
                                viewModel.onDrawingEnded()
                                edgeController.onDrawingEnded()
                            }
                        }
                    },
                    update = { view ->
                        view.setLayers(proj.layers, activeLayerIndex)
                        view.setBrush(currentBrush)
                        view.setColor(currentColorInt)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // ═══════════════════════════════════════════════════════════
        // LAYER 2: LEFT EDGE CONTROLS
        // ═══════════════════════════════════════════════════════════
        
        AnimatedVisibility(
            visible = !edgeController.isUIHidden,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            LeftEdgeControls(
                brushSize = edgeController.brushSize,
                brushOpacity = edgeController.brushOpacity,
                currentColor = currentColor,
                isEyedropperActive = edgeController.isEyedropperActive,
                onSizeChange = { edgeController.setBrushSize(it) },
                onOpacityChange = { edgeController.setBrushOpacity(it) },
                onEyedropperClick = { edgeController.toggleEyedropper() },
                controlsAlpha = edgeAlpha,
                enabled = !popoverManager.hasOpenPopover()
            )
        }
        
        // ═══════════════════════════════════════════════════════════
        // LAYER 3: RIGHT EDGE CATEGORY BAR
        // ═══════════════════════════════════════════════════════════
        
        AnimatedVisibility(
            visible = !edgeController.isUIHidden,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(modifier = Modifier.alpha(edgeAlpha)) {
                RightEdgeCategoryBar(
                    activeCategory = popoverManager.activePopover,
                    selectedCategory = edgeController.selectedCategory,
                    canUndo = edgeController.canUndo,
                    canRedo = edgeController.canRedo,
                    onCategorySelected = { category ->
                        edgeController.onCategoryTapped(category)
                    },
                    onUndo = { edgeController.undo() },
                    onRedo = { edgeController.redo() }
                )
            }
        }
        
        // ═══════════════════════════════════════════════════════════
        // LAYER 4: POPOVERS (one at a time)
        // ═══════════════════════════════════════════════════════════
        
        // Brush Selector Popover
        BrushSelectorPopover(
            isVisible = popoverManager.isOpen(ToolCategory.BRUSH),
            currentBrush = currentBrush,
            anchorY = popoverManager.getAnchorOffset(ToolCategory.BRUSH).toFloat(),
            onBrushSelected = { brush ->
                edgeController.selectBrush(brush)
            },
            onDismiss = { popoverManager.closeAll() }
        )
        
        // Color Picker Popover
        ColorPickerPopover(
            isVisible = popoverManager.isOpen(ToolCategory.COLOR),
            currentColor = currentColor,
            anchorOffset = popoverManager.getAnchorOffset(ToolCategory.COLOR),
            onColorChange = { color ->
                edgeController.setColor(color)
            },
            onDismiss = { popoverManager.closeAll() },
            onEyedropperClick = {
                edgeController.toggleEyedropper()
            }
        )
        
        // Layers Popover
        LayerPanelPopover(
            isVisible = popoverManager.isOpen(ToolCategory.LAYERS),
            layers = project?.layers ?: emptyList(),
            activeLayerIndex = activeLayerIndex,
            anchorOffset = popoverManager.getAnchorOffset(ToolCategory.LAYERS),
            onLayerSelected = { index ->
                viewModel.setActiveLayer(index)
            },
            onLayerVisibilityToggled = { index ->
                viewModel.toggleLayerVisibility(index)
            },
            onLayerAdded = {
                viewModel.addLayer()
            },
            onLayerDeleted = { index ->
                viewModel.deleteLayer(index)
            },
            onDismiss = { popoverManager.closeAll() }
        )
        
        // Transform Popover - placeholder
        if (popoverManager.isOpen(ToolCategory.TRANSFORM)) {
            TransformPopoverPlaceholder(
                onDismiss = { popoverManager.closeAll() }
            )
        }
        
        // Settings Popover - placeholder
        if (popoverManager.isOpen(ToolCategory.SETTINGS)) {
            SettingsPopoverPlaceholder(
                onDismiss = { popoverManager.closeAll() },
                onNavigateBack = onNavigateBack
            )
        }
        
        // ═══════════════════════════════════════════════════════════
        // LAYER 5: SCRIM (when popover is open)
        // ═══════════════════════════════════════════════════════════
        
        // Note: Scrim is handled within each popover component
    }
}

/**
 * Placeholder for Transform popover (to be implemented).
 */
@Composable
private fun TransformPopoverPlaceholder(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // TODO: Implement TransformPopover
        // Will include: Freeform, Uniform, Distort, Warp, Flip H/V
    }
}

/**
 * Placeholder for Settings popover (to be implemented).
 */
@Composable
private fun SettingsPopoverPlaceholder(
    onDismiss: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // TODO: Implement SettingsPopover
        // Will include: Gallery (back), Canvas, Export, Import, Preferences, Help
    }
}

/**
 * Layer Panel Popover - Compact layer management.
 * 
 * Placeholder implementation - the full implementation would be
 * similar to ColorPickerPopover and BrushSelectorPopover.
 */
@Composable
fun LayerPanelPopover(
    isVisible: Boolean,
    layers: List<com.artboard.data.model.Layer>,
    activeLayerIndex: Int,
    anchorOffset: Int,
    onLayerSelected: (Int) -> Unit,
    onLayerVisibilityToggled: (Int) -> Unit,
    onLayerAdded: () -> Unit,
    onLayerDeleted: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            // TODO: Implement full LayerPanelPopover
            // This is a placeholder - would show layer stack, add button,
            // layer cards with visibility toggles, etc.
            // 
            // Layout:
            // ┌─────────────────────────────────┐
            // │ LAYERS                    [+]   │
            // │─────────────────────────────────│
            // │ ┌─────┐ Layer 3      👁 🔒     │
            // │ │thumb│ Normal • 100%          │
            // │ └─────┘                        │
            // │ ┌─────┐ Layer 2 ◀    👁        │
            // │ │thumb│ Multiply • 85%         │
            // │ └─────┘                        │
            // │ ┌─────┐ Background   👁        │
            // │ │thumb│ Normal • 100%          │
            // │ └─────┘                        │
            // └─────────────────────────────────┘
        }
    }
}
