package com.artboard.ui.transform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.artboard.data.model.Layer
import com.artboard.ui.transform.components.TransformBounds
import com.artboard.ui.transform.components.TransformHeaderBar
import com.artboard.ui.transform.components.TransformStatusBar
import com.artboard.ui.transform.components.TransformToolbar
import com.artboard.ui.transform.components.calculateScalePercentage

/**
 * Transform mode screen - main UI for layer transformations
 * 
 * Features:
 * - Visual bounds with corner/edge handles
 * - Drag to move, pinch to scale, rotate with two fingers
 * - Snap at 15°, 45°, 90° angles with haptic feedback
 * - Flip horizontal/vertical buttons
 * - Real-time preview
 * - Apply/Cancel with undo support
 */
@Composable
fun TransformMode(
    viewModel: TransformViewModel,
    layerIndex: Int,
    layers: List<Layer>,
    canvasBounds: Rect,
    onComplete: (List<Layer>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transformType by viewModel.transformType.collectAsState()
    val currentTransform by viewModel.currentTransform.collectAsState()
    val isApplying by viewModel.isApplying.collectAsState()
    val canApply = viewModel.canApply()
    
    // Initialize transform mode
    LaunchedEffect(layerIndex, layers) {
        viewModel.enterTransformMode(layerIndex, layers)
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Transform bounds and gesture handling
        TransformBounds(
            bounds = canvasBounds,
            transform = currentTransform,
            transformType = transformType,
            onTransformChange = { newTransform ->
                viewModel.updateTransform(newTransform)
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // UI overlay
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header bar with Cancel/Apply
            TransformHeaderBar(
                onCancel = {
                    viewModel.cancelTransform { updatedLayers ->
                        onCancel()
                    }
                },
                onApply = {
                    viewModel.applyTransform { updatedLayers ->
                        onComplete(updatedLayers)
                    }
                },
                canApply = canApply,
                isApplying = isApplying
            )
            
            // Status bar showing scale and rotation
            TransformStatusBar(
                scalePercentage = calculateScalePercentage(currentTransform),
                rotationAngle = currentTransform.rotation,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Bottom toolbar
        TransformToolbar(
            transformType = transformType,
            onTransformTypeChange = { type ->
                viewModel.setTransformType(type)
            },
            onFlipHorizontal = {
                viewModel.flipHorizontal()
            },
            onFlipVertical = {
                viewModel.flipVertical()
            },
            onRotate90 = {
                viewModel.rotate90Clockwise()
            },
            onReset = {
                viewModel.resetTransform()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Simplified transform mode for quick access
 * Use this when you just need basic transform without full UI
 */
@Composable
fun QuickTransformMode(
    layerIndex: Int,
    layers: List<Layer>,
    canvasBounds: Rect,
    viewModel: TransformViewModel,
    onComplete: (List<Layer>) -> Unit
) {
    val currentTransform by viewModel.currentTransform.collectAsState()
    val transformType by viewModel.transformType.collectAsState()
    
    // Initialize
    LaunchedEffect(layerIndex, layers) {
        viewModel.enterTransformMode(layerIndex, layers)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        TransformBounds(
            bounds = canvasBounds,
            transform = currentTransform,
            transformType = transformType,
            onTransformChange = { newTransform ->
                viewModel.updateTransform(newTransform)
            }
        )
    }
}
