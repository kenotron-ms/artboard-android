package com.artboard.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artboard.ui.canvas.components.FloatingToolbar
import com.artboard.ui.canvas.components.StatusBar
import com.artboard.ui.canvas.components.ZoomableCanvas
import com.artboard.ui.common.AutoHideController

/**
 * Main canvas screen for immersive drawing experience
 * Features:
 * - Full-screen canvas with auto-hiding UI
 * - Floating translucent toolbar
 * - Optional status bar
 * - Four-finger tap to toggle UI
 * - Edge tap to reveal UI
 * - 60 FPS drawing with tilt support
 */
@Composable
fun CanvasScreen(
    projectId: String? = null,
    viewModel: CanvasViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val project by viewModel.project.collectAsState()
    val currentBrush by viewModel.currentBrush.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val activeLayerIndex by viewModel.activeLayerIndex.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val toolbarVisible by viewModel.toolbarVisible.collectAsState()
    val statusBarVisible by viewModel.statusBarVisible.collectAsState()
    
    var showBrushSelector by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLayerPanel by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    var canvasFps by remember { mutableStateOf(0f) }
    
    // Auto-hide controller
    val autoHideController = remember {
        AutoHideController(
            hideDelay = 3000L,
            onHide = { viewModel.hideUI() }
        )
    }
    
    // Load project if provided
    LaunchedEffect(projectId) {
        if (projectId != null) {
            // TODO: Load project from repository
            // For now, using default project from ViewModel
        }
    }
    
    // Reset auto-hide on any interaction
    LaunchedEffect(toolbarVisible, showBrushSelector, showColorPicker, showLayerPanel, showMenu) {
        if (showBrushSelector || showColorPicker || showLayerPanel || showMenu) {
            autoHideController.pause()
        } else if (toolbarVisible) {
            autoHideController.start()
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            autoHideController.cleanup()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // App background
            .detectFourFingerTap {
                viewModel.toggleUI()
                if (toolbarVisible) {
                    autoHideController.start()
                }
            }
            .detectEdgeTap { edge ->
                if (edge == Edge.TOP && !toolbarVisible) {
                    viewModel.showUI()
                    autoHideController.start()
                }
            }
    ) {
        // Main canvas with zoom support
        project?.let { proj ->
            ZoomableCanvas(
                modifier = Modifier.fillMaxSize()
            ) {
                AndroidView(
                    factory = { context ->
                        CanvasView(context).apply {
                            initialize(proj.width, proj.height)
                            setLayers(proj.layers, activeLayerIndex)
                            setBrush(currentBrush)
                            setColor(currentColor)
                            
                            onStrokeBegin = {
                                viewModel.onDrawingStarted()
                                autoHideController.pause()
                            }
                            
                            onStrokeEnd = {
                                viewModel.onDrawingEnded()
                                autoHideController.start()
                            }
                            
                            onStrokeUpdate = {
                                // Update FPS periodically
                                canvasFps = getFps()
                            }
                        }
                    },
                    update = { view ->
                        view.setLayers(proj.layers, activeLayerIndex)
                        view.setBrush(currentBrush)
                        view.setColor(currentColor)
                        canvasFps = view.getFps()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Floating toolbar at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            FloatingToolbar(
                currentBrush = currentBrush,
                currentColor = Color(currentColor),
                canUndo = canUndo,
                canRedo = canRedo,
                isVisible = toolbarVisible,
                onBrushClick = {
                    showBrushSelector = true
                    viewModel.onInteraction()
                },
                onColorClick = {
                    showColorPicker = true
                    viewModel.onInteraction()
                },
                onUndoClick = {
                    viewModel.undo()
                    viewModel.onInteraction()
                    autoHideController.reset()
                },
                onRedoClick = {
                    viewModel.redo()
                    viewModel.onInteraction()
                    autoHideController.reset()
                },
                onLayersClick = {
                    showLayerPanel = true
                    viewModel.onInteraction()
                },
                onMenuClick = {
                    showMenu = true
                    viewModel.onInteraction()
                }
            )
        }
        
        // Status bar at bottom
        project?.let { proj ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                StatusBar(
                    activeLayer = proj.layers.getOrNull(activeLayerIndex),
                    canvasInfo = "${proj.width}×${proj.height}",
                    isVisible = statusBarVisible,
                    fps = canvasFps
                )
            }
        }
    }
    
    // TODO: Panel overlays (will be implemented in separate components)
    // For now, these are placeholders
    if (showBrushSelector) {
        // BrushSelectorPanel(...)
        showBrushSelector = false // Auto-close for now
    }
    
    if (showColorPicker) {
        // ColorPickerPanel(...)
        showColorPicker = false // Auto-close for now
    }
    
    if (showLayerPanel) {
        // LayerPanel(...)
        showLayerPanel = false // Auto-close for now
    }
    
    if (showMenu) {
        // MenuPanel(...)
        showMenu = false // Auto-close for now
    }
}
