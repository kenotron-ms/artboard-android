package com.artboard.ui.color.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Horizontal row of recent colors for quick selection.
 * 
 * Design spec:
 * - 8 recent colors in a row
 * - 32×32dp swatches with 4dp gap
 * - Tap to select
 * - Current color highlighted with border
 * 
 * Layout:
 * ```
 * [■][■][■][■][■][■][■][■]   Recent colors
 * ```
 * 
 * Fits within 280dp popover width:
 * - 8 swatches × 32dp = 256dp
 * - 7 gaps × 4dp = 28dp
 * - Total = 284dp (with padding adjustment)
 * 
 * @param recentColors List of recent color ARGB values (max 8 shown)
 * @param currentColorArgb Current selected color ARGB (for highlighting)
 * @param onColorSelected Callback when a color is selected
 * @param modifier Modifier for the component
 */
@Composable
fun RecentColorsRow(
    recentColors: List<Int>,
    currentColorArgb: Int?,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentColors.isEmpty()) {
        // Show placeholder swatches when empty
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            repeat(8) {
                EmptyColorSwatch()
            }
        }
        return
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        // Show up to 8 recent colors
        recentColors.take(8).forEach { colorArgb ->
            RecentColorSwatch(
                color = Color(colorArgb),
                isSelected = colorArgb == currentColorArgb,
                onClick = { onColorSelected(colorArgb) }
            )
        }
        
        // Fill remaining slots with empty swatches
        val emptySlots = (8 - recentColors.size).coerceAtLeast(0)
        repeat(emptySlots) {
            EmptyColorSwatch()
        }
    }
}

/**
 * Individual recent color swatch (32×32dp)
 */
@Composable
private fun RecentColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.85f
            isSelected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swatch_scale"
    )
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            )
            .clickable {
                isPressed = true
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
                isPressed = false
            }
    )
}

/**
 * Empty placeholder swatch
 */
@Composable
private fun EmptyColorSwatch() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2A2A2A))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
    )
}
