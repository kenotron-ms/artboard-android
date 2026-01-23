package com.artboard.ui.toolbar

import androidx.compose.runtime.*
import com.artboard.data.model.Brush
import com.artboard.ui.brush.BrushSelectorPanel

/**
 * Brush selector entry point
 * Now delegates to the new visual brush library panel with live previews
 */
@Composable
fun BrushSelector(
    currentBrush: Brush,
    onBrushSelected: (Brush) -> Unit,
    onDismiss: () -> Unit
) {
    BrushSelectorPanel(
        currentBrush = currentBrush,
        onBrushSelected = onBrushSelected,
        onDismiss = onDismiss
    )
}

// Legacy brush presets list - kept for backward compatibility
// New visual brush selector uses ViewModel to manage brush library
val brushPresets = listOf(
    Brush.pencil(),
    Brush.pen(),
    Brush.marker(),
    Brush.airbrush(),
    Brush.eraser(),
    Brush.calligraphy(),
    Brush.markerChisel()
)
