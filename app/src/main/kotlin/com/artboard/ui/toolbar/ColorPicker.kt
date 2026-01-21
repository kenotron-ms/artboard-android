package com.artboard.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ColorPicker(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
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
                    text = "Choose Color",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Color grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(colorPalette) { color ->
                        ColorSwatch(
                            color = color,
                            isSelected = color == currentColor,
                            onClick = {
                                onColorSelected(color)
                                onDismiss()
                            }
                        )
                    }
                }
                
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
fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color, CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.Gray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

// Predefined color palette
val colorPalette = listOf(
    Color.Black,
    Color.White,
    Color.Gray,
    Color.Red,
    Color(0xFFFF6B6B),
    Color(0xFFFF8E53),
    Color(0xFFFFA726),
    Color.Yellow,
    Color(0xFFFFEB3B),
    Color(0xFFC6FF00),
    Color.Green,
    Color(0xFF4CAF50),
    Color(0xFF00BCD4),
    Color.Cyan,
    Color.Blue,
    Color(0xFF2196F3),
    Color(0xFF3F51B5),
    Color(0xFF673AB7),
    Color.Magenta,
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF795548),
    Color(0xFF607D8B),
    Color(0xFF455A64),
)
