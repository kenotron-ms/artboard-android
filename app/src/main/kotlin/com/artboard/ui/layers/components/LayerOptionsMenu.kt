package com.artboard.ui.layers.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.artboard.data.model.Layer

/**
 * Context menu for layer operations
 * Shown on long-press or More button tap
 */
@Composable
fun LayerOptionsMenu(
    layer: Layer,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMergeDown: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2A2A2A),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .padding(vertical = 8.dp)
            ) {
                // Header with layer name
                Text(
                    text = layer.name,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                
                Divider(color = Color(0xFF444444))
                
                // Menu items
                MenuItemRow(
                    icon = Icons.Default.Edit,
                    text = "Rename",
                    onClick = {
                        onRename()
                        onDismiss()
                    }
                )
                
                MenuItemRow(
                    icon = Icons.Default.ContentCopy,
                    text = "Duplicate",
                    onClick = {
                        onDuplicate()
                        onDismiss()
                    }
                )
                
                MenuItemRow(
                    icon = Icons.Default.ArrowDownward,
                    text = "Merge Down",
                    onClick = {
                        onMergeDown()
                        onDismiss()
                    }
                )
                
                Divider(color = Color(0xFF444444), modifier = Modifier.padding(vertical = 4.dp))
                
                // Destructive actions
                MenuItemRow(
                    icon = Icons.Default.Clear,
                    text = "Clear Layer",
                    textColor = Color(0xFFFF9800), // Orange warning
                    onClick = {
                        showClearConfirmation = true
                    }
                )
                
                MenuItemRow(
                    icon = Icons.Default.Delete,
                    text = "Delete Layer",
                    textColor = Color(0xFFCC0000), // Red danger
                    onClick = {
                        showDeleteConfirmation = true
                    }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        ConfirmationDialog(
            title = "Delete Layer?",
            message = "Are you sure you want to delete \"${layer.name}\"? This cannot be undone.",
            confirmText = "Delete",
            confirmColor = Color(0xFFCC0000),
            onConfirm = {
                onDelete()
                onDismiss()
                showDeleteConfirmation = false
            },
            onDismiss = {
                showDeleteConfirmation = false
            }
        )
    }
    
    // Clear confirmation dialog
    if (showClearConfirmation) {
        ConfirmationDialog(
            title = "Clear Layer?",
            message = "Are you sure you want to clear all content from \"${layer.name}\"?",
            confirmText = "Clear",
            confirmColor = Color(0xFFFF9800),
            onConfirm = {
                onClear()
                onDismiss()
                showClearConfirmation = false
            },
            onDismiss = {
                showClearConfirmation = false
            }
        )
    }
}

/**
 * Menu item row with icon and text
 */
@Composable
private fun MenuItemRow(
    icon: ImageVector,
    text: String,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = text,
            style = TextStyle(
                fontSize = 15.sp,
                color = textColor
            )
        )
    }
}

/**
 * Confirmation dialog for destructive actions
 */
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A2A),
        title = {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 18.sp,
                    color = Color.White
                )
            )
        },
        text = {
            Text(
                text = message,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFFAAAAAA)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = confirmColor
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF4A90E2)
                )
            }
        }
    )
}
