package com.artboard.ui.layers

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.BlendMode
import com.artboard.data.model.Layer
import com.artboard.ui.layers.components.BlendModeSelector
import com.artboard.ui.layers.components.LayerCard
import com.artboard.ui.layers.components.LayerOptionsMenu

/**
 * Layer Panel - Swipeable bottom sheet for visual layer management
 * 
 * Features:
 * - 60% height bottom sheet with 16dp rounded top corners
 * - Swipeable drag handle
 * - Layer cards with thumbnails
 * - Gesture controls: swipe, pinch, drag to reorder
 * - Active layer highlighted with blue border
 * - 18 blend modes
 * - Opacity control per layer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerPanel(
    viewModel: LayerPanelViewModel,
    onDismiss: () -> Unit
) {
    val layers by viewModel.layers.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()
    
    var layerWithOptions by remember { mutableStateOf<String?>(null) }
    var layerForBlendMode by remember { mutableStateOf<String?>(null) }
    var layerForRename by remember { mutableStateOf<String?>(null) }
    
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White,
        dragHandle = {
            // Custom drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            Color(0xFF444444),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f) // 60% of screen height
        ) {
            // Header
            LayerPanelHeader(
                layerCount = layers.size,
                onAddLayer = { viewModel.addLayer() }
            )
            
            Divider(color = Color(0xFF333333))
            
            // Layer list (top layer first in UI)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                reverseLayout = true // Top layer at top of UI
            ) {
                items(
                    items = layers.sortedByDescending { it.position },
                    key = { layer -> layer.id }
                ) { layer ->
                    LayerCard(
                        layer = layer,
                        isActive = layer.id == activeLayerId,
                        onClick = {
                            viewModel.setActiveLayer(layer.id)
                        },
                        onLongPress = {
                            layerWithOptions = layer.id
                        },
                        onVisibilityToggle = {
                            viewModel.toggleVisibility(layer.id)
                        },
                        onLockToggle = {
                            viewModel.toggleLock(layer.id)
                        },
                        onOpacityChange = { opacity ->
                            viewModel.changeOpacity(layer.id, opacity)
                        },
                        onBlendModeClick = {
                            layerForBlendMode = layer.id
                        },
                        onSwipeDelete = {
                            viewModel.deleteLayer(layer.id)
                        },
                        onSwipeDuplicate = {
                            viewModel.duplicateLayer(layer.id)
                        },
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    )
                }
            }
        }
    }
    
    // Layer options menu (long-press)
    layerWithOptions?.let { layerId ->
        val layer = layers.find { it.id == layerId }
        if (layer != null) {
            LayerOptionsMenu(
                layer = layer,
                onRename = {
                    layerForRename = layerId
                    layerWithOptions = null
                },
                onDuplicate = {
                    viewModel.duplicateLayer(layerId)
                    layerWithOptions = null
                },
                onMergeDown = {
                    viewModel.mergeLayerDown(layerId)
                    layerWithOptions = null
                },
                onClear = {
                    viewModel.clearLayer(layerId)
                    layerWithOptions = null
                },
                onDelete = {
                    viewModel.deleteLayer(layerId)
                    layerWithOptions = null
                },
                onDismiss = { layerWithOptions = null }
            )
        }
    }
    
    // Blend mode selector
    layerForBlendMode?.let { layerId ->
        val layer = layers.find { it.id == layerId }
        if (layer != null) {
            BlendModeSelector(
                currentMode = layer.blendMode,
                onModeSelected = { mode ->
                    viewModel.changeBlendMode(layerId, mode)
                    layerForBlendMode = null
                },
                onDismiss = { layerForBlendMode = null }
            )
        }
    }
    
    // Rename dialog
    layerForRename?.let { layerId ->
        val layer = layers.find { it.id == layerId }
        if (layer != null) {
            RenameLayerDialog(
                currentName = layer.name,
                onRename = { newName ->
                    viewModel.renameLayer(layerId, newName)
                    layerForRename = null
                },
                onDismiss = { layerForRename = null }
            )
        }
    }
}

/**
 * Layer panel header with title and add button
 */
@Composable
private fun LayerPanelHeader(
    layerCount: Int,
    onAddLayer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Layers ($layerCount)",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        // Add layer button
        IconButton(
            onClick = onAddLayer,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Layer",
                tint = Color(0xFF4A90E2),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Rename layer dialog
 */
@Composable
private fun RenameLayerDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A2A),
        title = {
            Text(
                text = "Rename Layer",
                style = TextStyle(
                    fontSize = 18.sp,
                    color = Color.White
                )
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4A90E2),
                    unfocusedBorderColor = Color(0xFF444444),
                    cursorColor = Color(0xFF4A90E2)
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text(
                    text = "Rename",
                    color = if (newName.isNotBlank()) Color(0xFF4A90E2) else Color(0xFF666666)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF888888)
                )
            }
        }
    )
}

/**
 * Extension for layer panel - handles pinch to delete gesture
 * (Called from parent composable)
 */
fun Modifier.detectLayerPinch(
    onPinchDelete: (layerId: String) -> Unit
): Modifier {
    // TODO: Implement multi-touch pinch detection
    // This requires custom gesture detection that tracks multiple pointers
    // For MVP, we have swipe-to-delete which is sufficient
    return this
}
