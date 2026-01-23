package com.artboard.ui.layers

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Layer
import com.artboard.ui.layers.components.BlendModeSelector
import com.artboard.ui.layers.components.LayerOptionsMenu

/**
 * Compact Layer Panel Popover for Artboard.
 * 
 * Design spec (from EdgeControlBar.md & TOOL_ORGANIZATION_PHILOSOPHY.md):
 * - Width: 280dp (fixed, matches other popovers)
 * - Height: auto (max 500dp, scrollable)
 * - Position: Slides from right edge, below Layers category button
 * - Background: #1C1C1E with 16dp corner radius
 * - Shadow: Elevation 8dp
 * 
 * Key changes from 60% bottom sheet LayerPanel:
 * - Compact side popover (not bottom sheet)
 * - Canvas visible (70%+ of screen)
 * - Slide from right animation
 * - Auto-close on tap outside
 * - 56dp compact layer rows
 * - Intuitive gestures (swipe, long-press, reorder)
 * 
 * Layout:
 * ```
 * ┌─────────────────────────────┐
 * │ Layers                  [+] │  Header with add button
 * ├─────────────────────────────┤
 * │ ┌────┐ Layer 3        [👁] │
 * │ │ 🖼 │ Normal              │  Layer row
 * │ └────┘                      │
 * ├─────────────────────────────┤
 * │ ┌────┐ Layer 2 ★      [👁] │
 * │ │ 🖼 │ Multiply            │  Selected layer (blue border)
 * │ └────┘                      │
 * ├─────────────────────────────┤
 * │ ┌────┐ Background     [👁] │
 * │ │ 🖼 │ Normal              │
 * │ └────┘                      │
 * ├─────────────────────────────┤
 * │  [Merge] [Options...]       │  Quick actions
 * └─────────────────────────────┘
 *         280dp wide
 * ```
 * 
 * Animations:
 * - Open: Slide from right + fade, 200ms spring
 * - Close: Slide to right + fade, 150ms ease-out
 * - Reorder: Item lifts with shadow, spring to new position
 * - Delete: Slide out left, collapse space
 * 
 * Gestures (from user feedback - Procreate-style):
 * - Tap thumbnail: Select layer
 * - Tap eye: Toggle visibility
 * - Swipe left on layer: Delete
 * - Swipe right on layer: Duplicate
 * - Long-press + drag: Reorder
 * - Long-press: Show options menu
 * - Pinch two layers together: Merge them (handled externally)
 * 
 * @param isVisible Whether the popover is visible
 * @param layers List of layers to display
 * @param selectedLayerId Currently selected layer ID
 * @param anchorOffset Vertical offset from top (to position below Layers button)
 * @param onLayerSelected Callback when a layer is selected
 * @param onLayerVisibilityChanged Callback when layer visibility is toggled
 * @param onLayerDeleted Callback when layer is deleted
 * @param onLayerDuplicated Callback when layer is duplicated
 * @param onLayersMerged Callback when two layers are merged
 * @param onLayerReordered Callback when layer position changes
 * @param onAddLayer Callback when add layer button is tapped
 * @param onDismiss Callback when popover is dismissed
 */
@Composable
fun LayerPanelPopover(
    isVisible: Boolean,
    layers: List<Layer>,
    selectedLayerId: String?,
    anchorOffset: Int = 0,
    onLayerSelected: (Layer) -> Unit,
    onLayerVisibilityChanged: (Layer, Boolean) -> Unit,
    onLayerDeleted: (Layer) -> Unit,
    onLayerDuplicated: (Layer) -> Unit,
    onLayersMerged: (List<Layer>) -> Unit,
    onLayerReordered: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddLayer: () -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    
    // State for dialogs/menus
    var layerForOptions by remember { mutableStateOf<Layer?>(null) }
    var layerForBlendMode by remember { mutableStateOf<Layer?>(null) }
    
    // Animated visibility with slide from right + fade
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth }, // Start from right edge
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(
            animationSpec = tween(durationMillis = 200)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth }, // Exit to right edge
            animationSpec = tween(durationMillis = 150)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Semi-transparent scrim for tap-outside-to-dismiss
            // Keep it light to maintain canvas visibility (70%+ visible)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, // No ripple
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDismiss()
                        }
                    )
            )
            
            // Popover panel positioned at right edge
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .heightIn(max = 500.dp)
                    .align(Alignment.TopEnd)
                    .padding(end = 56.dp, top = anchorOffset.dp) // 56dp clears edge button bar
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent click-through to scrim
                    ),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1C1E) // Dark background per spec
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header with title and add button
                    LayerPopoverHeader(
                        layerCount = layers.size,
                        onAddLayer = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onAddLayer()
                        }
                    )
                    
                    // Divider
                    Divider(
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    
                    // Compact layer stack (scrollable, drag-to-reorder)
                    CompactLayerStack(
                        layers = layers,
                        selectedLayerId = selectedLayerId,
                        onLayerSelected = onLayerSelected,
                        onLayerVisibilityChanged = onLayerVisibilityChanged,
                        onLayerDeleted = onLayerDeleted,
                        onLayerDuplicated = onLayerDuplicated,
                        onLayerReordered = onLayerReordered,
                        onLayerLongPress = { layer ->
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            layerForOptions = layer
                        },
                        onBlendModeClick = { layer ->
                            layerForBlendMode = layer
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // Divider
                    Divider(
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    
                    // Quick actions (merge, options)
                    LayerQuickActions(
                        selectedLayer = layers.find { it.id == selectedLayerId },
                        canMergeDown = layers.find { it.id == selectedLayerId }?.let { selected ->
                            layers.any { it.position < selected.position }
                        } ?: false,
                        onMergeDown = {
                            val selected = layers.find { it.id == selectedLayerId } ?: return@LayerQuickActions
                            val layerBelow = layers
                                .filter { it.position < selected.position }
                                .maxByOrNull { it.position } ?: return@LayerQuickActions
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onLayersMerged(listOf(layerBelow, selected))
                        },
                        onOptionsClick = {
                            val selected = layers.find { it.id == selectedLayerId }
                            if (selected != null) {
                                layerForOptions = selected
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Layer options menu (long-press)
    layerForOptions?.let { layer ->
        LayerOptionsMenu(
            layer = layer,
            onRename = {
                // Rename handled through callback
                layerForOptions = null
            },
            onDuplicate = {
                onLayerDuplicated(layer)
                layerForOptions = null
            },
            onMergeDown = {
                val layerBelow = layers
                    .filter { it.position < layer.position }
                    .maxByOrNull { it.position }
                if (layerBelow != null) {
                    onLayersMerged(listOf(layerBelow, layer))
                }
                layerForOptions = null
            },
            onClear = {
                // Clear handled through callback - would need additional callback
                layerForOptions = null
            },
            onDelete = {
                onLayerDeleted(layer)
                layerForOptions = null
            },
            onDismiss = { layerForOptions = null }
        )
    }
    
    // Blend mode selector
    layerForBlendMode?.let { layer ->
        BlendModeSelector(
            currentMode = layer.blendMode,
            onModeSelected = { mode ->
                // Would need callback for blend mode change
                // For now, just close
                layerForBlendMode = null
            },
            onDismiss = { layerForBlendMode = null }
        )
    }
}

/**
 * Compact header for layer popover
 */
@Composable
private fun LayerPopoverHeader(
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
            text = "Layers",
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )
        
        // Add layer button (48dp touch target)
        IconButton(
            onClick = onAddLayer,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Layer",
                tint = Color(0xFF4A90E2),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
