package com.artboard.ui.layers

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Layer
import org.burnoutcrew.reorderable.*

/**
 * Compact vertical layer stack with drag-to-reorder support.
 * 
 * Features:
 * - Vertical scrollable list of compact layer rows
 * - Long-press + drag to reorder layers
 * - Animated item placement with spring physics
 * - Selected layer highlighted
 * - Maximum height with scrolling
 * 
 * Gesture handling:
 * - Long-press: Initiates drag mode OR shows options menu (if no drag)
 * - Drag during long-press: Reorder layers
 * - Release: Animate to new position with spring
 * 
 * Animation specs:
 * - Reorder: Spring animation (damping 0.7, stiffness medium)
 * - Item lift: 4dp elevation increase during drag
 * - Collapse: 200ms ease-out when item deleted
 * 
 * @param layers List of layers (sorted by position, top layer first in UI)
 * @param selectedLayerId Currently selected layer ID
 * @param onLayerSelected Callback when layer is tapped to select
 * @param onLayerVisibilityChanged Callback when eye icon toggled
 * @param onLayerDeleted Callback when layer is deleted (swipe left)
 * @param onLayerDuplicated Callback when layer is duplicated (swipe right)
 * @param onLayerReordered Callback when layer position changes (from, to indices)
 * @param onLayerLongPress Callback when layer is long-pressed (show options)
 * @param onBlendModeClick Callback when blend mode badge is tapped
 * @param modifier Modifier for the stack container
 */
@Composable
fun CompactLayerStack(
    layers: List<Layer>,
    selectedLayerId: String?,
    onLayerSelected: (Layer) -> Unit,
    onLayerVisibilityChanged: (Layer, Boolean) -> Unit,
    onLayerDeleted: (Layer) -> Unit,
    onLayerDuplicated: (Layer) -> Unit,
    onLayerReordered: (fromIndex: Int, toIndex: Int) -> Unit,
    onLayerLongPress: (Layer) -> Unit,
    onBlendModeClick: (Layer) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    
    // Sort layers by position descending (top layer first in UI)
    val sortedLayers = remember(layers) {
        layers.sortedByDescending { it.position }
    }
    
    // Reorderable state for drag-to-reorder
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            // Haptic feedback on move
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        },
        onDragEnd = { from, to ->
            if (from != to) {
                // Convert UI indices back to layer positions
                // UI is reversed (top layer first), so we need to map correctly
                val fromLayerIndex = sortedLayers.size - 1 - from
                val toLayerIndex = sortedLayers.size - 1 - to
                onLayerReordered(fromLayerIndex, toLayerIndex)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        }
    )
    
    LazyColumn(
        state = reorderableState.listState,
        modifier = modifier
            .fillMaxWidth()
            .reorderable(reorderableState)
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = sortedLayers,
            key = { layer -> layer.id }
        ) { layer ->
            // Reorderable item wrapper
            ReorderableItem(
                reorderableState = reorderableState,
                key = layer.id
            ) { isDragging ->
                // Animate elevation when dragging
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "dragElevation"
                )
                
                CompactLayerRow(
                    layer = layer,
                    isSelected = layer.id == selectedLayerId,
                    isDragging = isDragging,
                    elevation = elevation,
                    onTap = { onLayerSelected(layer) },
                    onVisibilityToggle = { 
                        onLayerVisibilityChanged(layer, !layer.isVisible) 
                    },
                    onSwipeDelete = { onLayerDeleted(layer) },
                    onSwipeDuplicate = { onLayerDuplicated(layer) },
                    onLongPress = { onLayerLongPress(layer) },
                    onBlendModeClick = { onBlendModeClick(layer) },
                    dragModifier = Modifier.detectReorderAfterLongPress(reorderableState)
                )
            }
        }
    }
}

/**
 * Empty state when no layers exist (shouldn't happen normally)
 */
@Composable
fun EmptyLayerStack(
    onAddLayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "No layers",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = androidx.compose.ui.graphics.Color(0xFF666666)
            )
        )
    }
}
