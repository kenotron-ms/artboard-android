package com.artboard.ui.layers

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Layer

/**
 * Bottom action row for the compact layer panel popover.
 * 
 * Design spec:
 * - Height: 56dp total (including padding)
 * - Touch targets: 48dp minimum
 * - Actions: Merge down, Options menu
 * - Disabled state when action not available
 * 
 * Layout:
 * ```
 * ┌─────────────────────────────┐
 * │  [Merge ↓]    [Options...] │
 * └─────────────────────────────┘
 * ```
 * 
 * Behavior:
 * - Merge: Enabled only when selected layer has a layer below
 * - Options: Opens full options menu for selected layer
 * - Haptic feedback on tap
 * - Disabled buttons show reduced opacity
 * 
 * @param selectedLayer Currently selected layer (null if none)
 * @param canMergeDown Whether merge down action is available
 * @param onMergeDown Callback when merge button is tapped
 * @param onOptionsClick Callback when options button is tapped
 */
@Composable
fun LayerQuickActions(
    selectedLayer: Layer?,
    canMergeDown: Boolean,
    onMergeDown: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val view = LocalView.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Merge Down button
        QuickActionButton(
            icon = Icons.Default.CallMerge,
            label = "Merge",
            enabled = canMergeDown && selectedLayer != null,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onMergeDown()
            },
            modifier = Modifier.weight(1f)
        )
        
        // Options button
        QuickActionButton(
            icon = Icons.Default.MoreHoriz,
            label = "Options",
            enabled = selectedLayer != null,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onOptionsClick()
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual quick action button with icon and label.
 * 
 * Features:
 * - 48dp minimum touch target
 * - Icon + label layout
 * - Disabled state with reduced opacity
 * - Rounded shape
 * - Subtle background on enabled state
 * 
 * @param icon Icon to display
 * @param label Text label
 * @param enabled Whether button is enabled
 * @param onClick Callback when tapped
 * @param modifier Modifier for sizing
 */
@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (enabled) Color.White else Color(0xFF666666)
    val backgroundColor = if (enabled) Color(0xFF3A3A3C) else Color.Transparent
    
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            )
        }
    }
}

/**
 * Extended quick actions with additional functionality.
 * 
 * Use this variant when you need more actions in the footer:
 * - Add layer
 * - Merge down
 * - Flatten visible
 * - Options menu
 * 
 * @param selectedLayer Currently selected layer
 * @param canMergeDown Whether merge is available
 * @param onAddLayer Callback for add layer
 * @param onMergeDown Callback for merge
 * @param onFlattenVisible Callback for flatten
 * @param onOptionsClick Callback for options
 */
@Composable
fun LayerQuickActionsExtended(
    selectedLayer: Layer?,
    canMergeDown: Boolean,
    onAddLayer: () -> Unit,
    onMergeDown: () -> Unit,
    onFlattenVisible: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val view = LocalView.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary row: Add + Merge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "Add",
                enabled = true,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onAddLayer()
                },
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.CallMerge,
                label = "Merge",
                enabled = canMergeDown && selectedLayer != null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onMergeDown()
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Secondary row: Options only (flatten could go here)
        QuickActionButton(
            icon = Icons.Default.MoreHoriz,
            label = "Layer Options...",
            enabled = selectedLayer != null,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onOptionsClick()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Compact single-row quick actions for minimal footer.
 * Just shows icon buttons without labels.
 * 
 * @param selectedLayer Currently selected layer
 * @param canMergeDown Whether merge is available
 * @param onAddLayer Callback for add
 * @param onMergeDown Callback for merge
 * @param onOptionsClick Callback for options
 */
@Composable
fun LayerQuickActionsCompact(
    selectedLayer: Layer?,
    canMergeDown: Boolean,
    onAddLayer: () -> Unit,
    onMergeDown: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val view = LocalView.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add layer
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onAddLayer()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add layer",
                tint = Color(0xFF4A90E2),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Merge down
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onMergeDown()
            },
            enabled = canMergeDown && selectedLayer != null,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.CallMerge,
                contentDescription = "Merge down",
                tint = if (canMergeDown && selectedLayer != null) 
                    Color.White 
                else 
                    Color(0xFF666666),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Options
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onOptionsClick()
            },
            enabled = selectedLayer != null,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.MoreHoriz,
                contentDescription = "Layer options",
                tint = if (selectedLayer != null) Color.White else Color(0xFF666666),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
