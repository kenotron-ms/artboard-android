package com.artboard.ui.toolbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.artboard.data.model.Brush
import com.artboard.data.model.BrushType

@Composable
fun BrushSelector(
    currentBrush: Brush,
    onBrushSelected: (Brush) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Choose Brush",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(brushPresets) { preset ->
                        BrushItem(
                            brush = preset,
                            isSelected = preset.type == currentBrush.type,
                            onClick = {
                                onBrushSelected(preset)
                                onDismiss()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Size slider
                Text(
                    text = "Size: ${currentBrush.size.toInt()}px",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Slider(
                    value = currentBrush.size,
                    onValueChange = { onBrushSelected(currentBrush.copy(size = it)) },
                    valueRange = 1f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun BrushItem(
    brush: Brush,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (brush.type) {
                    BrushType.PENCIL -> Icons.Default.Edit
                    BrushType.PEN -> Icons.Default.Create
                    BrushType.AIRBRUSH -> Icons.Default.Face
                    BrushType.MARKER -> Icons.Default.Star
                    BrushType.ERASER -> Icons.Default.Delete
                    BrushType.CALLIGRAPHY -> Icons.Default.Brush
                },
                contentDescription = brush.type.name,
                tint = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = brush.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Size: ${brush.size.toInt()}px",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

val brushPresets = listOf(
    Brush.pencil(),
    Brush.pen(),
    Brush.marker(),
    Brush.airbrush(),
    Brush.eraser(),
    Brush.calligraphy(),
    Brush.markerChisel()
)
