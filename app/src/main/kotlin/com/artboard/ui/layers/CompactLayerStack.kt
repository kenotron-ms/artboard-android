package com.artboard.ui.layers

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Layer

/**
 * Compact vertical layer stack for the layer panel popover.
 * 
 * Features:
 * - Vertical scrollable list of compact layer rows
 * - Selected layer highlighted
 * - Maximum height with scrolling
 * - Swipe gestures for delete/duplicate
 * 
 * Note: Drag-to-reorder has been simplified to use long-press + menu
 * for layer reordering to avoid third-party dependencies.
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
    val listState = rememberLazyListState()
    
    // Sort layers by position descending (top layer first in UI)
    val sortedLayers = remember(layers) {
        layers.sortedByDescending { it.position }
    }
    
    if (sortedLayers.isEmpty()) {
        EmptyLayerStack(
            onAddLayer = { /* handled by parent */ },
            modifier = modifier
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(
                items = sortedLayers,
                key = { layer -> layer.id }
            ) { layer ->
                CompactLayerRow(
                    layer = layer,
                    isSelected = layer.id == selectedLayerId,
                    isDragging = false,
                    elevation = 0.dp,
                    onTap = { onLayerSelected(layer) },
                    onVisibilityToggle = { 
                        onLayerVisibilityChanged(layer, !layer.isVisible) 
                    },
                    onSwipeDelete = { onLayerDeleted(layer) },
                    onSwipeDuplicate = { onLayerDuplicated(layer) },
                    onLongPress = { 
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLayerLongPress(layer) 
                    },
                    onBlendModeClick = { onBlendModeClick(layer) },
                    dragModifier = Modifier
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
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No layers",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp
        )
    }
}
