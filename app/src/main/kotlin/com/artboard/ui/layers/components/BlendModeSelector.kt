package com.artboard.ui.layers.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.artboard.data.model.BlendMode

/**
 * Blend mode selector dialog with all 18 modes
 * Displays in a grid layout with visual previews
 */
@Composable
fun BlendModeSelector(
    currentMode: BlendMode,
    onModeSelected: (BlendMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Blend Mode",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Blend mode list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    // Group blend modes by category
                    item {
                        CategoryHeader("Basic")
                    }
                    
                    items(basicModes) { mode ->
                        BlendModeItem(
                            mode = mode,
                            isSelected = mode == currentMode,
                            onClick = {
                                onModeSelected(mode)
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryHeader("Darken")
                    }
                    
                    items(darkenModes) { mode ->
                        BlendModeItem(
                            mode = mode,
                            isSelected = mode == currentMode,
                            onClick = {
                                onModeSelected(mode)
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryHeader("Lighten")
                    }
                    
                    items(lightenModes) { mode ->
                        BlendModeItem(
                            mode = mode,
                            isSelected = mode == currentMode,
                            onClick = {
                                onModeSelected(mode)
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryHeader("Contrast")
                    }
                    
                    items(contrastModes) { mode ->
                        BlendModeItem(
                            mode = mode,
                            isSelected = mode == currentMode,
                            onClick = {
                                onModeSelected(mode)
                                onDismiss()
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryHeader("Component")
                    }
                    
                    items(componentModes) { mode ->
                        BlendModeItem(
                            mode = mode,
                            isSelected = mode == currentMode,
                            onClick = {
                                onModeSelected(mode)
                                onDismiss()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF4A90E2)
                    )
                }
            }
        }
    }
}

/**
 * Category header for blend mode groups
 */
@Composable
private fun CategoryHeader(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF888888)
        ),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    )
}

/**
 * Individual blend mode item
 */
@Composable
fun BlendModeItem(
    mode: BlendMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF2A4A6A) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mode.displayName(),
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                ),
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF4A90E2),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Blend mode categories for organized display
private val basicModes = listOf(
    BlendMode.NORMAL,
    BlendMode.ADD
)

private val darkenModes = listOf(
    BlendMode.DARKEN,
    BlendMode.MULTIPLY,
    BlendMode.COLOR_BURN
)

private val lightenModes = listOf(
    BlendMode.LIGHTEN,
    BlendMode.SCREEN,
    BlendMode.COLOR_DODGE
)

private val contrastModes = listOf(
    BlendMode.OVERLAY,
    BlendMode.SOFT_LIGHT,
    BlendMode.HARD_LIGHT,
    BlendMode.DIFFERENCE,
    BlendMode.EXCLUSION
)

private val componentModes = listOf(
    BlendMode.HUE,
    BlendMode.SATURATION,
    BlendMode.COLOR,
    BlendMode.LUMINOSITY
)
