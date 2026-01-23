package com.artboard.ui.brush

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Brush

/**
 * Compact 2-column brush grid for the popover
 * 
 * Per design spec:
 * - 2 columns (fits 280dp width)
 * - Card size: 120×80dp each
 * - 8dp spacing between cards
 * - 12dp padding around grid
 * - Smooth crossfade on category switch (150ms)
 * 
 * Layout within 280dp popover:
 * - 12dp left padding
 * - 120dp card
 * - 8dp gap
 * - 120dp card  
 * - 12dp right padding
 * = 272dp (fits within 280dp)
 * 
 * @param brushes List of brushes to display
 * @param selectedBrush Currently selected brush
 * @param onBrushSelected Callback when brush is selected
 * @param onFavoriteToggle Callback to toggle favorite status
 * @param isFavorite Function to check if brush is favorited
 */
@Composable
fun CompactBrushGrid(
    brushes: List<Brush>,
    selectedBrush: Brush,
    onBrushSelected: (Brush) -> Unit,
    onFavoriteToggle: (Brush) -> Unit,
    isFavorite: (Brush) -> Boolean,
    modifier: Modifier = Modifier
) {
    // Crossfade animation when brushes list changes (category switch)
    Crossfade(
        targetState = brushes,
        animationSpec = tween(150),
        modifier = modifier,
        label = "brush_grid_crossfade"
    ) { currentBrushes ->
        if (currentBrushes.isEmpty()) {
            // Empty state
            EmptyBrushGridState(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = currentBrushes,
                    key = { "${it.type.name}-${it.hashCode()}" }
                ) { brush ->
                    CompactBrushCard(
                        brush = brush,
                        isSelected = brush == selectedBrush,
                        isFavorite = isFavorite(brush),
                        onClick = { onBrushSelected(brush) },
                        onFavoriteClick = { onFavoriteToggle(brush) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(150),
                            fadeOutSpec = tween(150),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    )
                }
            }
        }
    }
}

/**
 * Empty state shown when no brushes in category
 */
@Composable
private fun EmptyBrushGridState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No brushes",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "This category is empty",
                fontSize = 12.sp,
                color = Color(0xFF636366)
            )
        }
    }
}
