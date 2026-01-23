package com.artboard.ui.selection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.artboard.ui.selection.SelectionToolType
import com.artboard.ui.selection.SelectionViewModel

/**
 * Selection toolbar component
 * Contains tool buttons, settings, and action buttons
 */
@Composable
fun SelectionToolbar(
    viewModel: SelectionViewModel,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val selectedTool by viewModel.selectionTool.collectAsState()
    val featherRadius by viewModel.featherRadius.collectAsState()
    val tolerance by viewModel.tolerance.collectAsState()
    val selectionMask by viewModel.selectionMask.collectAsState()
    
    var showSettings by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xE0000000),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tool selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionToolButton(
                icon = Icons.Default.Edit,  // Lasso icon (Edit is close enough)
                label = "Lasso",
                isSelected = selectedTool == SelectionToolType.LASSO,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    viewModel.setSelectionTool(SelectionToolType.LASSO)
                }
            )
            
            SelectionToolButton(
                icon = Icons.Default.CheckBoxOutlineBlank,  // Rectangle
                label = "Rectangle",
                isSelected = selectedTool == SelectionToolType.RECTANGLE,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    viewModel.setSelectionTool(SelectionToolType.RECTANGLE)
                }
            )
            
            SelectionToolButton(
                icon = Icons.Default.Circle,  // Ellipse
                label = "Ellipse",
                isSelected = selectedTool == SelectionToolType.ELLIPSE,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    viewModel.setSelectionTool(SelectionToolType.ELLIPSE)
                }
            )
            
            SelectionToolButton(
                icon = Icons.Default.TouchApp,  // Magic wand (TouchApp is close)
                label = "Magic",
                isSelected = selectedTool == SelectionToolType.MAGIC_WAND,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    viewModel.setSelectionTool(SelectionToolType.MAGIC_WAND)
                }
            )
        }
        
        // Settings toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showSettings = !showSettings
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (showSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (showSettings) "Hide settings" else "Show settings",
                tint = Color(0xFF4A90E2),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showSettings) "Hide Settings" else "Show Settings",
                color = Color(0xFF4A90E2),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Settings panel
        if (showSettings) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Feather slider
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Feather",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${featherRadius.toInt()}px",
                            color = Color(0xFF4A90E2),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Slider(
                        value = featherRadius,
                        onValueChange = { viewModel.setFeatherRadius(it) },
                        valueRange = 0f..50f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4A90E2),
                            activeTrackColor = Color(0xFF4A90E2),
                            inactiveTrackColor = Color(0x404A90E2)
                        )
                    )
                }
                
                // Tolerance slider (for magic wand only)
                if (selectedTool == SelectionToolType.MAGIC_WAND) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tolerance",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = tolerance.toString(),
                                color = Color(0xFF4A90E2),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Slider(
                            value = tolerance.toFloat(),
                            onValueChange = { viewModel.setTolerance(it.toInt()) },
                            valueRange = 0f..255f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4A90E2),
                                activeTrackColor = Color(0xFF4A90E2),
                                inactiveTrackColor = Color(0x404A90E2)
                            )
                        )
                    }
                }
            }
        }
        
        // Action buttons
        val mask = selectionMask
        if (mask != null && !mask.isEmpty()) {
            Divider(
                color = Color(0x40FFFFFF),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SelectionActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.copySelection()
                    }
                )
                
                SelectionActionButton(
                    icon = Icons.Default.ContentCut,
                    label = "Cut",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.cutSelection()
                    }
                )
                
                SelectionActionButton(
                    icon = Icons.Default.Delete,
                    label = "Clear",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.clearSelectedArea()
                    }
                )
                
                SelectionActionButton(
                    icon = Icons.Default.Flip,
                    label = "Invert",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.invertSelection()
                    }
                )
            }
            
            // Deselect button
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    viewModel.clearSelection()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Deselect",
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Select All button when no selection
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    // Get canvas dimensions from active layer
                    viewModel.activeLayer.value?.let { layer ->
                        viewModel.selectAll(layer.bitmap.width, layer.bitmap.height)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Select All",
                    color = Color(0xFF4A90E2),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Selection tool button component
 */
@Composable
private fun SelectionToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color(0xFF4A90E2) else Color.Transparent
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color(0xFFB0B0B0),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/**
 * Selection action button component
 */
@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp
        )
    }
}
